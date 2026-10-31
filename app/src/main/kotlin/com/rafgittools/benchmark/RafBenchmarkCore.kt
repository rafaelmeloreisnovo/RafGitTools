package com.rafgittools.benchmark

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * RAFAELIA authorial benchmark kernel. Local, bounded, deterministic dataset,
 * no device credentials, network calls, JIT-loaded code or dynamic plugins.
 * The throughput measures THIS compute+write kernel; not DRAM bandwidth.
 */
object RafBenchmarkCore {
    val allowedWorkers = listOf(1, 2, 4, 8)
    val allowedBufferKiB = listOf(256, 1024, 4096, 16384)
    const val ROUNDS = 4

    data class Result(
        val workers: Int,
        val bufferKiB: Int,
        val rounds: Int,
        val elapsedNs: Long,
        val processedBytes: Long,
        val checksum: Long,
        val outputSha256: String,
        val state: String = "EVIDENCED_LOCAL_ONLY",
        val claimAllowed: Boolean = false
    ) {
        val throughputMiBs: Double get() =
            if (elapsedNs > 0) processedBytes.toDouble() * 1_000_000_000.0 /
                elapsedNs.toDouble() / 1_048_576.0 else 0.0
    }

    fun validate(workers: Int, bufferKiB: Int) {
        require(workers in allowedWorkers) { "Número de workers fora da allowlist" }
        require(bufferKiB in allowedBufferKiB) { "Buffer fora da allowlist" }
    }

    fun run(workers: Int, bufferKiB: Int): Result {
        validate(workers, bufferKiB)
        val data = ByteArray(bufferKiB * 1024)
        val pool = Executors.newFixedThreadPool(workers)
        val start = System.nanoTime()
        val checksums: List<Long>
        try {
            val jobs = (0 until workers).map { worker ->
                Callable<Long> {
                    val lo = data.size * worker / workers
                    val hi = data.size * (worker + 1) / workers
                    var sum = 0L
                    repeat(ROUNDS) { round ->
                        var i = lo
                        while (i < hi) {
                            val v = ((i * 31) xor (round * 17) xor (worker * 257)) and 255
                            data[i] = v.toByte()
                            sum = sum * 31L + v.toLong()
                            i++
                        }
                    }
                    sum
                }
            }
            checksums = pool.invokeAll(jobs).map { it.get() }
        } finally {
            pool.shutdown()
            if (!pool.awaitTermination(5, TimeUnit.SECONDS)) pool.shutdownNow()
        }
        val elapsed = (System.nanoTime() - start).coerceAtLeast(1L)
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") { "%02x".format(it.toInt() and 255) }
        val sum = checksums.fold(0L) { acc, value -> acc xor value }
        return Result(workers, bufferKiB, ROUNDS, elapsed,
            data.size.toLong() * ROUNDS, sum, digest)
    }
}
