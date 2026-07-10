#include <jni.h>
#include <pthread.h>
#include <string.h>
#include <stdlib.h>
#include <android/log.h>
#include "llama.h"
#include "raf_kernel_api.h"

#define LOG_TAG "RafKernel"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#define RAF_MAX_TOKENS    4096
#define RAF_RESPONSE_CAP  4096
#define RAF_MAX_GEN        512

/* ── Static session state ────────────────────────────────────────────────── */
static pthread_mutex_t    s_mutex   = PTHREAD_MUTEX_INITIALIZER;
static struct llama_model   *s_model   = NULL;
static struct llama_context *s_ctx     = NULL;
static struct llama_sampler *s_sampler = NULL;

/* ── Minimal JSON helper: locate "content" string value start ────────────── */
static const char *json_string_value(const char *json, const char *key) {
    char search[64];
    snprintf(search, sizeof(search), "\"%s\"", key);
    const char *k = strstr(json, search);
    if (!k) return NULL;
    const char *colon = strchr(k + strlen(search), ':');
    if (!colon) return NULL;
    const char *q = strchr(colon + 1, '"');
    return q ? q + 1 : NULL;
}

/* Copy a JSON string value, unescaping \" only, until closing quote. */
static size_t json_copy_string(char *dst, size_t dst_cap, const char *src) {
    size_t i = 0;
    while (src[i] && src[i] != '"' && i < dst_cap - 1) {
        if (src[i] == '\\' && src[i + 1] == '"') {
            dst[i] = '"'; i += 2;
        } else {
            dst[i] = src[i]; i++;
        }
    }
    dst[i] = '\0';
    return i;
}

/* ── API implementation ──────────────────────────────────────────────────── */

int raf_kernel_open(const char *model_path, const char *cti_path) {
    (void)cti_path;  /* RMR-CTI integration: PENDING */
    pthread_mutex_lock(&s_mutex);

    if (s_model) {
        pthread_mutex_unlock(&s_mutex);
        return 0;  /* already open */
    }

    llama_backend_init();

    struct llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;  /* CPU-only on mobile */

    s_model = llama_model_load_from_file(model_path, &mp);
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
    llama_sampler_chain_add(s_sampler, llama_sampler_init_greedy());

    LOGI("raf_kernel_open: kernel ready, model=%s", model_path);
    pthread_mutex_unlock(&s_mutex);
    return 0;
}

int raf_kernel_chat(const char *request_json, char *response, size_t capacity) {
    if (capacity < RAF_RESPONSE_CAP) return -3;
    pthread_mutex_lock(&s_mutex);

    if (!s_model || !s_ctx) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"kernel not open\"}");
        pthread_mutex_unlock(&s_mutex);
        return -2;
    }

    const struct llama_vocab *vocab = llama_model_get_vocab(s_model);

    /* Extract text from {"role":"...","content":"..."} */
    char prompt[RAF_MAX_TOKENS * 4];
    const char *content = json_string_value(request_json, "content");
    if (!content) content = request_json;  /* fallback: treat whole string as prompt */
    size_t plen = json_copy_string(prompt, sizeof(prompt), content);
    (void)plen;

    /* Tokenize */
    llama_token tokens[RAF_MAX_TOKENS];
    int32_t n_tok = llama_tokenize(vocab, prompt, (int32_t)strlen(prompt),
                                   tokens, RAF_MAX_TOKENS, true, false);
    if (n_tok < 0) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"tokenize_failed\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Decode prompt */
    struct llama_batch batch = llama_batch_get_one(tokens, n_tok);
    if (llama_decode(s_ctx, batch) != 0) {
        snprintf(response, capacity, "{\"role\":\"error\",\"content\":\"decode_failed\"}");
        pthread_mutex_unlock(&s_mutex);
        return -1;
    }

    /* Generate response tokens */
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

    /* Emit JSON response — simple escaping of backslash and double-quote */
    size_t out = 0;
    out += (size_t)snprintf(response + out, capacity - out, "{\"role\":\"assistant\",\"content\":\"");
    for (size_t i = 0; i < gen_pos && out < capacity - 4; i++) {
        if (generated[i] == '\"' || generated[i] == '\\') {
            response[out++] = '\\';
        }
        response[out++] = generated[i];
    }
    if (out < capacity - 2) {
        response[out++] = '\"';
        response[out++] = '}';
        response[out]   = '\0';
    }

    pthread_mutex_unlock(&s_mutex);
    return 0;
}

int raf_kernel_tool_result(const char *tool_result_json, char *response, size_t capacity) {
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
    const char *model_path = (*env)->GetStringUTFChars(env, jModelPath, NULL);
    const char *cti_path   = (*env)->GetStringUTFChars(env, jCtiPath,   NULL);
    int result = raf_kernel_open(model_path, cti_path);
    (*env)->ReleaseStringUTFChars(env, jModelPath, model_path);
    (*env)->ReleaseStringUTFChars(env, jCtiPath,   cti_path);
    return (jint)result;
}

JNIEXPORT jstring JNICALL
Java_com_rafgittools_kernel_RafKernelBridge_nativeKernelChat(
        JNIEnv *env, jobject thiz, jstring jRequestJson, jint capacity) {
    (void)thiz;
    size_t cap = ((size_t)capacity < RAF_RESPONSE_CAP) ? RAF_RESPONSE_CAP : (size_t)capacity;
    char *buf = (char *)malloc(cap);
    if (!buf) return NULL;
    const char *req = (*env)->GetStringUTFChars(env, jRequestJson, NULL);
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
    size_t cap = ((size_t)capacity < RAF_RESPONSE_CAP) ? RAF_RESPONSE_CAP : (size_t)capacity;
    char *buf = (char *)malloc(cap);
    if (!buf) return NULL;
    const char *req = (*env)->GetStringUTFChars(env, jToolResultJson, NULL);
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
