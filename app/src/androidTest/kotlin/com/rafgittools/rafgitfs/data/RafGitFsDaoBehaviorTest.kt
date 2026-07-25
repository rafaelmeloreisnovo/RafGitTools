package com.rafgittools.rafgitfs.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rafgittools.data.cache.CacheDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RafGitFsDaoBehaviorTest {
    private lateinit var database: CacheDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CacheDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun cacheEvictionNeverReturnsPinnedRowsAndReceiptsAreAppendOnly() = runBlocking {
        val profile = profile()
        database.storageProfileDao().upsert(profile)

        database.contentCacheDao().upsert(cache("pinned", 100, pinned = true, accessedAt = 1))
        database.contentCacheDao().upsert(cache("old", 200, pinned = false, accessedAt = 2))
        database.contentCacheDao().upsert(cache("new", 300, pinned = false, accessedAt = 3))

        assertEquals(600L, database.contentCacheDao().totalBytes(profile.profileId))
        val candidates = database.contentCacheDao().evictionCandidates(profile.profileId, 10)
        assertEquals(listOf("old", "new"), candidates.map { it.cacheKey })
        assertFalse(candidates.any { it.pinned })

        val receipt = receipt("receipt-1", "request-1")
        database.operationReceiptDao().append(receipt)
        assertNotNull(database.operationReceiptDao().getByRequestId("request-1"))

        var duplicateRejected = false
        try {
            database.operationReceiptDao().append(receipt("receipt-2", "request-1"))
        } catch (_: Exception) {
            duplicateRejected = true
        }
        assertTrue("duplicate request receipt must fail closed", duplicateRejected)
    }

    @Test
    fun deletingLocalProfileCascadesOnlyReconstructibleState() = runBlocking {
        val profile = profile()
        database.storageProfileDao().upsert(profile)
        database.repositoryRefDao().upsertAll(
            listOf(
                RepositoryRefEntity(
                    profileId = profile.profileId,
                    repositoryFullName = "owner/repo",
                    refName = "main",
                    refType = "BRANCH",
                    gitSha = "abc123",
                    isDefault = true,
                    lastIndexedAt = 1
                )
            )
        )
        database.operationReceiptDao().append(receipt("receipt-1", "request-1"))

        database.storageProfileDao().deleteLocalProfile(profile.profileId)

        assertEquals(
            null,
            database.repositoryRefDao().get(profile.profileId, "owner/repo", "main")
        )
        assertNotNull(
            "append-only audit survives local profile deletion",
            database.operationReceiptDao().getByRequestId("request-1")
        )
    }

    private fun profile() = StorageProfileEntity(
        profileId = "test-profile",
        displayName = "Test profile",
        provider = "GITHUB",
        scope = "AUTHENTICATED_USER",
        owner = "owner",
        selectedRepositoriesJson = "[]",
        defaultRef = "main",
        accessMode = "READ_ONLY",
        cachePolicy = "ON_DEMAND",
        writePolicy = "BLOCKED",
        maxCacheBytes = 1024L,
        receiptRequired = true,
        protectedBranchWrite = false,
        deleteEnabled = false,
        claimAllowed = false,
        isEnabled = true,
        createdAt = 1,
        updatedAt = 1
    )

    private fun cache(key: String, size: Long, pinned: Boolean, accessedAt: Long) =
        ContentCacheEntity(
            cacheKey = key,
            profileId = "test-profile",
            repositoryFullName = "owner/repo",
            refName = "main",
            path = "$key.txt",
            gitSha = "sha-$key",
            localPath = "/cache/$key.txt",
            sizeBytes = size,
            cacheState = if (pinned) "PINNED_OFFLINE" else "CONTENT_CACHED",
            pinned = pinned,
            checksumAlgorithm = "SHA-256",
            checksumHex = "hash-$key",
            createdAt = 1,
            lastAccessedAt = accessedAt,
            expiresAt = null
        )

    private fun receipt(receiptId: String, requestId: String) = OperationReceiptEntity(
        receiptId = receiptId,
        requestId = requestId,
        profileId = "test-profile",
        operationType = "LIST_TREE",
        finalPhase = "RECEIPT",
        allowed = true,
        result = "SUCCESS",
        evidenceState = "OBSERVED",
        target = "owner/repo@main:",
        observedSha = "abc123",
        requestHash = "request-hash",
        receiptHash = "receipt-hash-$receiptId",
        hashAlgorithm = "SHA-256",
        fOkJson = "[]",
        fGapJson = "[]",
        fNextJson = "[]",
        createdAt = 1,
        claimAllowed = false
    )
}
