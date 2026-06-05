package com.rafgittools.bisect

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class BisectManagerTest {

    @Before
    fun setUp() {
        BisectManager.resetStateForTesting()
    }

    @Test
    fun `start returns not implemented failure and does not mutate state`() {
        val result = BisectManager.start(good = "abc123", bad = "def456")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
        assertThat(BisectManager.isBisectInProgressForTesting()).isFalse()
    }

    @Test
    fun `markGood returns not implemented failure and does not mutate state`() {
        val result = BisectManager.markGood(commit = "abc123")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
        assertThat(BisectManager.isBisectInProgressForTesting()).isFalse()
    }

    @Test
    fun `markBad returns not implemented failure and does not mutate state`() {
        val result = BisectManager.markBad(commit = "def456")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
        assertThat(BisectManager.isBisectInProgressForTesting()).isFalse()
    }

    @Test
    fun `finish returns not implemented failure and does not mutate state`() {
        val result = BisectManager.finish()

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isInstanceOf(NotImplementedError::class.java)
        assertThat(BisectManager.isBisectInProgressForTesting()).isFalse()
    }
}
