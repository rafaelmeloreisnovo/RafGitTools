package com.rafgittools.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RafBenchmarkCoreTest {
    @Test fun allowedWorkerCountsAreBounded() {
        assertEquals(listOf(1, 2, 4, 8), RafBenchmarkCore.allowedWorkers)
        assertEquals(listOf(256, 1024, 4096, 16384), RafBenchmarkCore.allowedBufferKiB)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownWorkerCount() { RafBenchmarkCore.validate(16, 256) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedMemory() { RafBenchmarkCore.validate(1, 150 * 1024) }

    @Test fun deterministicDataWithinSameWorkerConfiguration() {
        val one = RafBenchmarkCore.run(2, 256)
        val two = RafBenchmarkCore.run(2, 256)
        assertEquals(one.outputSha256, two.outputSha256)
        assertEquals(one.checksum, two.checksum)
        assertEquals(256L * 1024L * RafBenchmarkCore.ROUNDS, one.processedBytes)
        assertTrue(one.elapsedNs > 0)
        assertTrue(one.throughputMiBs > 0.0)
        assertFalse(one.claimAllowed)
        assertEquals("EVIDENCED_LOCAL_ONLY", one.state)
    }

    @Test fun multipleWorkersReturnExpectedDigestSize() {
        val result = RafBenchmarkCore.run(8, 256)
        assertEquals(64, result.outputSha256.length)
        assertFalse(result.claimAllowed)
        assertEquals(8, result.workers)
    }

    @Test fun creditsAreAttachedToTheActualScreen() {
        val credits = RafBenchmarkCredits.benchmarkScreen
        assertTrue(credits.any { it.origin == CreditOrigin.PROJECT_AUTHORIAL })
        assertTrue(credits.any { it.origin == CreditOrigin.EXTERNAL_API })
        assertTrue(credits.filter { it.origin != CreditOrigin.PROJECT_AUTHORIAL }
            .all { it.source.startsWith("https://") && it.license.isNotBlank() })
    }
}
