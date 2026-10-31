package com.rafgittools.benchmark

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RafBenchmarkCoreTest {
    @Test fun boundedWorkersBuffersAndCatalog() {
        assertEquals(listOf(1, 2, 4, 8), RafBenchmarkCore.allowedWorkers)
        assertEquals(listOf(256, 2048, 16384, 65536, 153600), RafBenchmarkCore.allowedBufferKiB)
        assertEquals(90, RafApplicationsCatalog.routes.size)
        assertEquals(90, RafApplicationsCatalog.routes.map { it.id }.distinct().size)
        assertEquals(8, RafApplicationsCatalog.groups.size)
        assertEquals("K090", RafApplicationsCatalog.routes.last().id)
        assertTrue(RafApplicationsCatalog.routes.all { it.stage ==
            RafApplicationsCatalog.Stage.CATALOG_ONLY })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnknownWorkerCount() { RafBenchmarkCore.validate(16, 256) }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedMemory() { RafBenchmarkCore.validate(1, 150 * 1024) }

    @Test fun workerCountDoesNotChangeDataOrChecksumForSameWorkload() {
        RafBenchmarkCore.allowedWorkloads.forEach { workload ->
            val baseline = RafBenchmarkCore.run(1, 256, workload)
            listOf(2, 4, 8).forEach { count ->
                val compared = RafBenchmarkCore.run(count, 256, workload)
                assertEquals(workload.name, baseline.outputSha256, compared.outputSha256)
                assertEquals(workload.name, baseline.checksum, compared.checksum)
                assertEquals(baseline.processedBytes, compared.processedBytes)
                assertEquals(256L * 1024L * RafBenchmarkCore.ROUNDS, compared.processedBytes)
                assertTrue(compared.elapsedNs > 0L)
                assertFalse(compared.claimAllowed)
                assertEquals("EVIDENCED_LOCAL_ONLY", compared.state)
            }
        }
    }

    @Test fun repeatedRunIsByteReproducible() {
        val a = RafBenchmarkCore.run(4, 1024)
        val b = RafBenchmarkCore.run(4, 1024)
        assertEquals(a.outputSha256, b.outputSha256)
        assertEquals(a.checksum, b.checksum)
    }

    @Test fun unknownKernelNeverPromotesToExecuted() {
        assertEquals(RafApplicationsCatalog.Stage.CATALOG_ONLY,
            RafApplicationsCatalog.byId("K090")?.stage)
    }

    @Test fun attributionsAreScopedToTheScreen() {
        val credits = RafBenchmarkCredits.benchmarkScreen
        assertTrue(credits.any { it.origin == CreditOrigin.PROJECT_AUTHORIAL })
        assertTrue(credits.any { it.origin == CreditOrigin.EXTERNAL_API })
        assertTrue(credits.filter { it.origin != CreditOrigin.PROJECT_AUTHORIAL }
            .all { it.source.startsWith("https://") && it.license.isNotBlank() })
    }
}
