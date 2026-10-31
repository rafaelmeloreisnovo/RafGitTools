package com.rafgittools.benchmark

import java.security.MessageDigest
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Authorial, bounded synthetic microbenchmark.
 * Same buffer contents and sum for every worker partition; verified with same-workload tests.
 * Reports effective bytes processed, NOT intrinsic DRAM/L3 bandwidth or CPU frequency.
 */
object RafBenchmarkCore {
    val allowedWorkers = listOf(1, 2, 4, 8)
    val allowedBufferKiB = listOf(256, 2048, 16384, 65536, 153600)
    const val ROUNDS = 4
    enum class Workload { STREAM_WRITE, CACHE_REUSE, COMPUTE_ONLY }
    val allowedWorkloads = Workload.values().toList()

    data class Result(
        val workers: Int,
        val bufferKiB: Int,
        val rounds: Int,
        val elapsedNs: Long,
        val processedBytes: Long,
        val checksum: Long,
        val outputSha256: String,
        val state: String = "EVIDENCED_LOCAL_ONLY",
        val claimAllowed: Boolean = false,
        val workload: Workload = Workload.STREAM_WRITE
    ) {
        val throughputMiBs: Double get() =
            if (elapsedNs > 0L) processedBytes.toDouble() * 1_000_000_000.0 /
                elapsedNs.toDouble() / 1_048_576.0 else 0.0
    }

    fun validate(workers: Int, bufferKiB: Int) {
        require(workers in allowedWorkers) { "WORKER_NOT_ALLOWED" }
        require(bufferKiB in allowedBufferKiB) { "BUFFER_NOT_ALLOWED" }
    }

    private fun byteAt(index: Int): Int = (index * 31 xor 0x5a) and 255

    /**
     * Worker count changes only partitioning, not the algorithm, bytes or correctness.
     * Pool construction is excluded from the measured interval, and a start latch
     * synchronizes worker entry. Runtime scheduling/thermal/JIT still affect measurements.
     */
    fun run(
        workers: Int,
        bufferKiB: Int,
        workload: Workload = Workload.STREAM_WRITE
    ): Result {
        validate(workers, bufferKiB)
        val data = ByteArray(bufferKiB * 1024) { byteAt(it).toByte() }
        val pool = Executors.newFixedThreadPool(workers)
        val ready = CountDownLatch(workers)
        val start = CountDownLatch(1)
        try {
            val futures = (0 until workers).map { worker ->
                pool.submit(Callable<Long> {
                    val lo = data.size * worker / workers
                    val hi = data.size * (worker + 1) / workers
                    ready.countDown()
                    require(start.await(10, TimeUnit.SECONDS)) { "START_TIMEOUT" }
                    var sum = 0L
                    repeat(ROUNDS) { round ->
                        var i = lo
                        while (i < hi) {
                            val v = when (workload) {
                                Workload.STREAM_WRITE -> (byteAt(i) xor round) and 255
                                Workload.CACHE_REUSE -> data[i].toInt() and 255
                                Workload.COMPUTE_ONLY -> (byteAt(i) xor (round * 17)) and 255
                            }
                            if (workload == Workload.STREAM_WRITE) data[i] = v.toByte()
                            sum += (i.toLong() + 1L) * (v.toLong() + 1L)
                            i++
                        }
                    }
                    sum
                })
            }
            require(ready.await(10, TimeUnit.SECONDS)) { "WORKERS_NOT_READY" }
            val t0 = System.nanoTime()
            start.countDown()
            val sums = futures.map { it.get(30, TimeUnit.SECONDS) }
            val elapsed = (System.nanoTime() - t0).coerceAtLeast(1L)
            val hash = MessageDigest.getInstance("SHA-256")
                .digest(data).joinToString("") { "%02x".format(it.toInt() and 255) }
            return Result(workers, bufferKiB, ROUNDS, elapsed,
                data.size.toLong() * ROUNDS, sums.fold(0L, Long::plus), hash,
                workload = workload)
        } finally {
            start.countDown()
            pool.shutdownNow()
        }
    }
}
