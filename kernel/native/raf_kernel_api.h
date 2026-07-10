/* raf_kernel_api.h — RAF cognitive kernel C API (JNI boundary)
 *
 * Contract:
 *   response buffers : caller provides >= 4096 bytes
 *   JSON in (chat)   : { "role": "user",      "content": "..." }
 *   JSON out (reply) : { "role": "assistant", "content": "..." }
 *   JSON out (tool)  : { "tool": "<name>",    "arguments": {...} }
 *   Return codes     :  0 = ok
 *                      -1 = model_error
 *                      -2 = context_error
 *                      -3 = buffer_too_small (capacity < 4096)
 *   Thread safety    : NOT re-entrant; guarded by internal pthread_mutex
 *   Encoding         : UTF-8 throughout; no conversion performed
 */
#pragma once
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

int  raf_kernel_open(const char *model_path, const char *cti_path);
int  raf_kernel_chat(const char *request_json, char *response, size_t capacity);
int  raf_kernel_tool_result(const char *tool_result_json, char *response, size_t capacity);
void raf_kernel_close(void);

#ifdef __cplusplus
}
#endif
