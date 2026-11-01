#include <jni.h>
#include <stdint.h>

#include "inventario_raf_core.h"

static void decimal_u32(uint32_t value, char output[11])
{
    char reverse[10];
    uint32_t count = 0u;
    uint32_t index;
    do {
        reverse[count++] = (char)('0' + (value % 10u));
        value /= 10u;
    } while (value != 0u && count < (uint32_t)sizeof(reverse));
    for (index = 0u; index < count; ++index) {
        output[index] = reverse[count - index - 1u];
    }
    output[count] = '\0';
}

static void decimal_i32(int32_t value, char output[12])
{
    if (value < 0) {
        output[0] = '-';
        decimal_u32((uint32_t)(-(value + 1)) + 1u, output + 1);
    } else {
        decimal_u32((uint32_t)value, output);
    }
}

JNIEXPORT jobjectArray JNICALL
Java_com_rafgittools_navigator_RafInventoryCoreBridge_nativeAnalyze(
        JNIEnv *environment,
        jobject receiver,
        jstring path_string,
        jstring mime_string,
        jboolean is_directory,
        jboolean size_known,
        jlong reported_bytes,
        jboolean parent_known)
{
    const char *path_bytes = (const char *)0;
    const char *mime_bytes = (const char *)0;
    jsize path_length;
    jsize mime_length;
    uint32_t input_flags = 0u;
    rafinv_v1_result result;
    int32_t status;
    uint32_t values[7];
    jclass string_class;
    jobjectArray output;
    jsize output_count;
    uint32_t index;
    char number[12];

    (void)receiver;
    if (path_string == (jstring)0 || mime_string == (jstring)0 ||
        reported_bytes < 0) {
        return (jobjectArray)0;
    }
    path_bytes = (*environment)->GetStringUTFChars(environment, path_string,
                                                    (jboolean *)0);
    mime_bytes = (*environment)->GetStringUTFChars(environment, mime_string,
                                                    (jboolean *)0);
    if (path_bytes == (const char *)0 || mime_bytes == (const char *)0) {
        if (path_bytes != (const char *)0) {
            (*environment)->ReleaseStringUTFChars(environment, path_string,
                                                   path_bytes);
        }
        if (mime_bytes != (const char *)0) {
            (*environment)->ReleaseStringUTFChars(environment, mime_string,
                                                   mime_bytes);
        }
        return (jobjectArray)0;
    }
    path_length = (*environment)->GetStringUTFLength(environment, path_string);
    mime_length = (*environment)->GetStringUTFLength(environment, mime_string);
    if (is_directory == JNI_TRUE) {
        input_flags |= RAFINV_V1_INPUT_DIRECTORY;
    }
    if (size_known == JNI_TRUE) {
        input_flags |= RAFINV_V1_INPUT_SIZE_KNOWN;
    }
    if (parent_known == JNI_TRUE) {
        input_flags |= RAFINV_V1_INPUT_PARENT_KNOWN;
    }
    status = rafinv_v1_analyze((const uint8_t *)path_bytes,
        (uint32_t)path_length, (const uint8_t *)mime_bytes,
        (uint32_t)mime_length, input_flags, (uint64_t)reported_bytes, &result);
    values[0] = (uint32_t)status;
    values[1] = result.token_count;
    values[2] = result.path_components;
    values[3] = result.pass_mask;
    values[4] = result.kind_flags;
    values[5] = result.size_known;
    values[6] = result.parent_known;
    output_count = (jsize)(7u + (status == RAFINV_V1_OK ? result.token_count : 0u));

    string_class = (*environment)->FindClass(environment, "java/lang/String");
    if (string_class == (jclass)0) {
        (*environment)->ReleaseStringUTFChars(environment, path_string, path_bytes);
        (*environment)->ReleaseStringUTFChars(environment, mime_string, mime_bytes);
        return (jobjectArray)0;
    }
    output = (*environment)->NewObjectArray(environment, output_count,
                                             string_class, (jobject)0);
    if (output == (jobjectArray)0) {
        (*environment)->ReleaseStringUTFChars(environment, path_string, path_bytes);
        (*environment)->ReleaseStringUTFChars(environment, mime_string, mime_bytes);
        return (jobjectArray)0;
    }
    for (index = 0u; index < 7u; ++index) {
        jstring value;
        if (index == 0u) {
            decimal_i32(status, number);
        } else {
            decimal_u32(values[index], number);
        }
        value = (*environment)->NewStringUTF(environment, number);
        if (value == (jstring)0) {
            break;
        }
        (*environment)->SetObjectArrayElement(environment, output,
                                               (jsize)index, value);
        (*environment)->DeleteLocalRef(environment, value);
    }
    if (status == RAFINV_V1_OK && index == 7u) {
        for (index = 0u; index < result.token_count; ++index) {
            char token_bytes[RAFINV_V1_MAX_PATH_BYTES + 1u];
            uint32_t byte_index;
            const rafinv_v1_token *token = &result.tokens[index];
            jstring token_string;
            for (byte_index = 0u; byte_index < token->length; ++byte_index) {
                token_bytes[byte_index] = path_bytes[token->offset + byte_index];
            }
            token_bytes[token->length] = '\0';
            token_string = (*environment)->NewStringUTF(environment, token_bytes);
            if (token_string == (jstring)0) {
                break;
            }
            (*environment)->SetObjectArrayElement(environment, output,
                (jsize)(7u + index), token_string);
            (*environment)->DeleteLocalRef(environment, token_string);
        }
    }
    (*environment)->ReleaseStringUTFChars(environment, path_string, path_bytes);
    (*environment)->ReleaseStringUTFChars(environment, mime_string, mime_bytes);
    return output;
}
