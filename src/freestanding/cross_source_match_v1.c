#include <stddef.h>
#include <stdint.h>

/* Pure exact-byte matcher. Buffers, source identities and persistence belong
 * to the caller. Each successful call emits one offset and advances cursor. */
int rafaelia_cross_source_match_v1(const unsigned char *source, size_t source_len,
                                  const unsigned char *needle, size_t needle_len,
                                  size_t *cursor, size_t *offset)
{
    size_t i, j, last;
    if (!source || !needle || !cursor || !offset || needle_len == 0 ||
        *cursor > source_len) return -1;
    if (needle_len > source_len) { *cursor = source_len; return 0; }
    last = source_len - needle_len;
    for (i = *cursor; i <= last; ++i) {
        for (j = 0; j < needle_len && source[i + j] == needle[j]; ++j) {}
        if (j == needle_len) {
            *offset = i;
            *cursor = i + 1;
            return 1;
        }
    }
    *cursor = source_len;
    return 0;
}
