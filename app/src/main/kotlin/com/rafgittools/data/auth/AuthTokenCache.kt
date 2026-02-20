package com.rafgittools.data.auth

import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory authentication token cache used by network components.
 */
@Singleton
class AuthTokenCache @Inject constructor() {
    @Volatile
    var token: String? = null
}

