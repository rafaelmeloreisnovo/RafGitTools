package com.rafgittools.rafgitfs.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class RafGitFsCacheCoreTest {
    @Test
    fun `known git blob sha1 is verified`() {
        val bytes = "hello\n".toByteArray()
        assertTrue(
            RafGitFsChecksums.verifyGitBlob(
                bytes,
                "ce013625030ba8dba906f756967f9e9ca394464a"
            )
        )
        assertFalse(
            RafGitFsChecksums.verifyGitBlob(
                "hello".toByteArray(),
                "ce013625030ba8dba906f756967f9e9ca394464a"
            )
        )
    }

    @Test
    fun `cache key is deterministic and content addressed`() {
        val identity = RafGitFsCacheIdentity(
            profileId = "p1",
            repositoryFullName = "owner/repo",
            refName = "main",
            path = "docs/readme.md",
            blobSha = "ce013625030ba8dba906f756967f9e9ca394464a"
        )
        val first = RafGitFsCacheKeys.key(identity)
        val second = RafGitFsCacheKeys.key(identity)
        assertEquals(64, first.length)
        assertEquals(first, second)
        assertTrue(RafGitFsCacheKeys.relativePath(first).endsWith("$first.bin"))
    }

    @Test
    fun `same profile concurrent reservations never exceed its byte budget`() = runBlocking {
        val gate = RafGitFsCacheProfileGate()
        val budgetBytes = 8L * 1024L
        val itemBytes = 1024L
        var committedBytes = 0L
        var activeSections = 0
        var maxActiveSections = 0

        val admitted = (1..64).map {
            async(Dispatchers.Default) {
                gate.withProfile("profile-stress") {
                    activeSections += 1
                    maxActiveSections = maxOf(maxActiveSections, activeSections)
                    val fits = committedBytes + itemBytes <= budgetBytes
                    delay(1)
                    if (fits) committedBytes += itemBytes
                    activeSections -= 1
                    fits
                }
            }
        }.awaitAll()

        assertEquals(8, admitted.count { it })
        assertEquals(budgetBytes, committedBytes)
        assertEquals(1, maxActiveSections)
    }

    @Test
    fun `same stripe serializes profiles without unbounded lock allocation`() = runBlocking {
        val gate = RafGitFsCacheProfileGate()
        val profilesOnOneStripe = (0 until 4096)
            .map { it.toString() }
            .groupBy { (it.hashCode() and Int.MAX_VALUE) % 64 }
            .values.first { it.size > 1 }
            .take(2)
        var activeSections = 0
        var maxActiveSections = 0

        profilesOnOneStripe.map { profileId ->
            async(Dispatchers.Default) {
                gate.withProfile(profileId) {
                    activeSections += 1
                    maxActiveSections = maxOf(maxActiveSections, activeSections)
                    delay(1)
                    activeSections -= 1
                }
            }
        }.awaitAll()

        assertEquals(1, maxActiveSections)
    }

    @Test
    fun `all canonical states remain explicit`() {
        assertEquals(
            setOf(
                "REMOTE_ONLY", "METADATA_CACHED", "PARTIAL", "CONTENT_CACHED",
                "PINNED_OFFLINE", "STALE", "CORRUPTED"
            ),
            RafGitFsCacheState.values().map { it.name }.toSet()
        )
    }
}
