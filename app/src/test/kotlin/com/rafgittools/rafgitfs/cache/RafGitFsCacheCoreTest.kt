package com.rafgittools.rafgitfs.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
