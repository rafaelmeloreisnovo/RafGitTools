package com.rafgittools.domain.error

import com.google.common.truth.Truth.assertThat
import java.io.IOException
import java.net.UnknownHostException
import org.junit.Test

class AppErrorTest {

    @Test
    fun `toAppError maps cleartext traffic to friendly network error`() {
        val exception = IOException("CLEARTEXT communication to http://example.com not permitted by network security policy")

        val error = exception.toAppError()

        assertThat(error).isInstanceOf(AppError.NetworkError.CleartextNotPermitted::class.java)
        assertThat(error.message).contains("Cleartext HTTP traffic is blocked")
    }

    @Test
    fun `toAppError maps unknown host to no connection`() {
        val exception = UnknownHostException("api.github.com")

        val error = exception.toAppError()

        assertThat(error).isInstanceOf(AppError.NetworkError.NoConnection::class.java)
    }
}
