package com.rafgittools.data.auth

/**
 * Supported authentication methods.
 */
enum class AuthMethod {
    PAT,
    DEVICE_CODE,
    OAUTH_WEB,
    GH_CLI_IMPORT,
    SSH_KEY,
    OFFLINE
}
