#include <jni.h>
#include <pthread.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include "llama.h"
#include "raf_kernel_api.h"

#define LOG_TAG "RafKernel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define RAF_MAX_TOKENS       4096
#define RAF_RESPONSE_CAP     4096
#define RAF_MAX_RESPONSE_CAP 65536
#define RAF_MAX_GEN           512

/* ── Static session state ────────────────────────────────────────────────── */
static pthread_mutex_t      s_mutex   = PTHREAD_MUTEX_INITIALIZER;
static struct llama_model   *s_model   = NULL;
static struct llama_context *s_ctx     = NULL;
static struct llama_sampler *s_sampler = NULL;

/* ── Minimal JSON helper: locate "content" string value start ────────────── */
static const char *json_string_value(const char *json, const char *key) {
    if (!json || !key) return NULL;

    char search[64];
    int written = snprintf(search, sizeof(search), "\"%s\"", key);
    if (written <= 0 || (size_t)written >= sizeof(search)) return NULL;

    const char *k = strstr(json, search);
    if (!k) return NULL;
    const char *colon = strchr(k + strlen(search), ':');
    if (!colon) return NULL;
    const char *q = strchr(colon + 1, '"');
    return q ? q + 1 : NULL;
}

/*
 * Copies a JSON string value with separate source/destination cursors.
 * Handles the common one-byte JSON escapes. Unicode \uXXXX decoding remains
 * outside this narrow JNI boundary; Kotlin should pass normalized UTF-8.
 */
static size_t json_copy_string(char *dst, size_t dst_cap, const char *src) {
    if (!dst || dst_cap == 0 || !src) return 0;

    size_t si = 0;
    size_t di = 0;

    while (src[si] && di + 1 < dst_cap) {
        char ch = src[si++];
        if (ch == '"') break;

        if (ch == '\\' && src[si]) {
            char esc = src[si++];
            switch (esc) {
                case '"': ch = '"';  break;
                case '\\': ch = '\\'; break;
                case '/':  ch = '/';  break;
                case 'b':  ch = '\b'; break;
                case 'f':  ch = '\f'; break;
                case 'n':  ch = '\n'; break;
                case 'r':  ch = '\r'; break;
                case 't':  ch = '\t'; break;
                default:
                    /* Preserve an unsupported escape literally and safely. */
                    if (di + 2 >= dst_cap) {
                        dst[di] = '\0';
                        return di;
                    }
                    dst[di++] = '\\';
                    ch = esc;
                    break;
            }
        }

        dst[di++] = ch;
    }

    dst[di] = '\0';
    return di;
}

/* ── API implementation ──────────────────────────────────────────────────── */

int raf_kernel_open(const char *model_path, const char *cti_path) {
    (void)cti_path;  /* RMR-CTI integration: PENDING */
    if (!model_path || model_path[0] == '\0') return -1;

    pthread_mutex_lock(&s_mutex);

    if (s_model) {
        pthread_mutex_unlock(&s_mutex);
        return 0;  /* already open */
    }

    llama_backend_init();

    struct llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;  /* CPU-only on mobile */

    /* llama_model_params is passed by value in the locked llama API. */
    s_model = llama_model_load_from_file(model_path, mp);
    if (!s_model) {
        LOGE("raf_kernel_open: failed to load model from %s", model_path);
        llama_backend_free();
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    struct llama_context_params cp = llama_context_default_params();
    cp.n_ctx   = 2048;
    cp.n_batch = 512;

    s_ctx = llama_init_from_model(s_model, cp);
    if (!s_ctx) {
        LOGE("raf_kernel_open: failed to create context");
        llama_model_free(s_model);
        s_model = NULL;
        llama_backend_free();
        pthread_mutex_unlock(&s_mutex);
        return -2;
    }

    struct llama_sampler_chain_params sp = llama_sampler_chain_default_params();
    s_sampler = llama_sampler_chain_init(sp);
    if (!s_sampler) {
        LOGE("raf_kernel_open: failed to create sampler chain");
        llama_free(s_ctx);
        s_ctx = NULL;
        llama_model_free(s_model);
        s_model = NULL;
        llama_backend_free();
        pthread_mutex_unlock(&s_mutex);
        return -2;
    }
    llama_sampler_chain_add(s_sampler, llama_sampler_init_greedy());

    LOGI("raf_kernel_open: kernel ready, model=%s", model_path);
    pthread_mutex_unlock(&s_mutex);
    return 0;
}

int raf_kernel_chat(const char *request_json, char *response, size_t capacity) {
    if (!request_json || !response) return -1;
    if (capacity < RAF_RESPONSE_CAP) return -3;
    pthread_mutex_lock(&s_mutex);

    if (!s_model || !s_ctx || !s_sampler) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"kernel not open\"}");
        pthread_mutex_unlock(&s_mutex);
        return -2;
    }

    const struct llama_vocab *vocab = llama_model_get_vocab(s_model);
    if (!vocab) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"vocab_unavailable\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Extract text from {"role":"...","content":"..."}. */
    char prompt[RAF_MAX_TOKENS * 4];
    const char *content = json_string_value(request_json, "content");
    if (!content) content = request_json;  /* fallback: plain prompt */
    size_t plen = json_copy_string(prompt, sizeof(prompt), content);
    if (plen == 0) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"empty_prompt\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Tokenize. */
    llama_token tokens[RAF_MAX_TOKENS];
    int32_t n_tok = llama_tokenize(vocab, prompt, (int32_t)plen,
                                   tokens, RAF_MAX_TOKENS, true, false);
    if (n_tok <= 0) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"tokenize_failed\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Decode prompt. */
    struct llama_batch batch = llama_batch_get_one(tokens, n_tok);
    if (llama_decode(s_ctx, batch) != 0) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"decode_failed\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Generate response tokens. */
    char generated[RAF_RESPONSE_CAP] = {0};
    size_t gen_pos = 0;
    int32_t n_gen  = 0;

    while (n_gen < RAF_MAX_GEN && gen_pos < RAF_RESPONSE_CAP - 64) {
        llama_token tok = llama_sampler_sample(s_sampler, s_ctx, -1);
        if (llama_vocab_is_eog(vocab, tok)) break;

        char piece[64];
        int32_t piece_len = llama_token_to_piece(vocab, tok, piece, sizeof(piece), 0, false);
        if (piece_len > 0 && gen_pos + (size_t)piece_len < RAF_RESPONSE_CAP - 1) {
            memcpy(generated + gen_pos, piece, (size_t)piece_len);
            gen_pos += (size_t)piece_len;
        }

        llama_token next_tokens[1] = {tok};
        struct llama_batch next_batch = llama_batch_get_one(next_tokens, 1);
        if (llama_decode(s_ctx, next_batch) != 0) break;
        n_gen++;
    }
    generated[gen_pos] = '\0';

    /* Emit JSON response — escaping backslash and double-quote. */
    size_t out = 0;
    out += (size_t)snprintf(response + out, capacity - out, "{\"role\":\"assistant\",\"content\":\"");
    for (size_t i = 0; i < gen_pos && out < capacity - 4; i++) {
        if (generated[i] == '"' || generated[i] == '\\') {
            response[out++] = '\\';
        }
        response[out++] = generated[i];
    }
    if (out < capacity - 2) {
        response[out++] = '"';
        response[out++] = '}';
        response[out]   = '\0';
    }

    pthread_mutex_unlock(&s_mutex);
    return 0;
}

int raf_kernel_tool_result(const char *tool_result_json, char *response, size_t capacity) {
    if (!tool_result_json || !response) return -1;
    if (capacity < RAF_RESPONSE_CAP) return -3;
    /*
     * First cut: inject tool result as a user turn and continue generation.
     * Full multi-turn tool call loop is PENDING.
     */
    return raf_kernel_chat(tool_result_json, response, capacity);
}

void raf_kernel_close(void) {
    pthread_mutex_lock(&s_mutex);
    if (s_sampler) { llama_sampler_free(s_sampler); s_sampler = NULL; }
    if (s_ctx)     { llama_free(s_ctx);              s_ctx     = NULL; }
    if (s_model)   { llama_model_free(s_model);      s_model   = NULL; }
    llama_backend_free();
    LOGI("raf_kernel_close: kernel shut down");
    pthread_mutex_unlock(&s_mutex);
}

/* ── JNI exports ─────────────────────────────────────────────────────────── */

JNIEXPORT jint JNICALL
Java_com_rafgittools_kernel_RafKernelBridge_nativeKernelOpen(
        JNIEnv *env, jobject thiz, jstring jModelPath, jstring jCtiPath) {
    (void)thiz;
    if (!jModelPath) return (jint)-1;

    const char *model_path = (*env)->GetStringUTFChars(env, jModelPath, NULL);
    const char *cti_path = jCtiPath
        ? (*env)->GetStringUTFChars(env, jCtiPath, NULL)
        : "";

    if (!model_path || (jCtiPath && !cti_path)) {
        if (model_path) (*env)->ReleaseStringUTFChars(env, jModelPath, model_path);
        if (jCtiPath && cti_path) (*env)->ReleaseStringUTFChars(env, jCtiPath, cti_path);
        return (jint)-1;
    }

    int result = raf_kernel_open(model_path, cti_path);
    (*env)->ReleaseStringUTFChars(env, jModelPath, model_path);
    if (jCtiPath) (*env)->ReleaseStringUTFChars(env, jCtiPath, cti_path);
    return (jint)result;
}

static size_t raf_jni_response_capacity(jint requested) {
    if (requested < RAF_RESPONSE_CAP) return RAF_RESPONSE_CAP;
    if (requested > RAF_MAX_RESPONSE_CAP) return RAF_MAX_RESPONSE_CAP;
    return (size_t)requested;
}

JNIEXPORT jstring JNICALL
Java_com_rafgittools_kernel_RafKernelBridge_nativeKernelChat(
        JNIEnv *env, jobject thiz, jstring jRequestJson, jint capacity) {
    (void)thiz;
    if (!jRequestJson) return NULL;

    size_t cap = raf_jni_response_capacity(capacity);
    char *buf = (char *)malloc(cap);
    if (!buf) return NULL;

    const char *req = (*env)->GetStringUTFChars(env, jRequestJson, NULL);
    if (!req) {
        free(buf);
        return NULL;
    }

    int rc = raf_kernel_chat(req, buf, cap);
    (*env)->ReleaseStringUTFChars(env, jRequestJson, req);
    jstring result = (rc == 0) ? (*env)->NewStringUTF(env, buf) : NULL;
    free(buf);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_rafgittools_kernel_RafKernelBridge_nativeKernelToolResult(
        JNIEnv *env, jobject thiz, jstring jToolResultJson, jint capacity) {
    (void)thiz;
    if (!jToolResultJson) return NULL;

    size_t cap = raf_jni_response_capacity(capacity);
    char *buf = (char *)malloc(cap);
    if (!buf) return NULL;

    const char *req = (*env)->GetStringUTFChars(env, jToolResultJson, NULL);
    if (!req) {
        free(buf);
        return NULL;
    }

    int rc = raf_kernel_tool_result(req, buf, cap);
    (*env)->ReleaseStringUTFChars(env, jToolResultJson, req);
    jstring result = (rc == 0) ? (*env)->NewStringUTF(env, buf) : NULL;
    free(buf);
    return result;
}

JNIEXPORT void JNICALL
Java_com_rafgittools_kernel_RafKernelBridge_nativeKernelClose(
        JNIEnv *env, jobject thiz) {
    (void)env;
    (void)thiz;
    raf_kernel_close();
}
