package com.rafgittools.data.privacy

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryPrivacyManagerTest {
    private val api = mockk<RepositoryPrivacyApi>()
    private val receiptStore = mockk<PrivacyReceiptStore>()
    private val manager = RepositoryPrivacyManager(api, receiptStore)

    @Test
    fun `fork and missing admin are blocked fail closed`() {
        val fork = repo(1, "fork", isFork = true, admin = true).toCandidate()
        val noAdmin = repo(2, "no-admin", admin = false).toCandidate()
        val eligible = repo(3, "eligible", admin = true).toCandidate()
        assertFalse(fork.eligible)
        assertTrue(fork.blockReason!!.contains("Fork"))
        assertFalse(noAdmin.eligible)
        assertTrue(noAdmin.blockReason!!.contains("TOKEN_VAZIO"))
        assertTrue(eligible.eligible)
    }

    @Test
    fun `inventory paginates and deduplicates`() = runTest {
        val first = (1L..100L).map { repo(it, "r$it") }
        val second = listOf(repo(100, "r100"), repo(101, "r101"))
        coEvery { api.listRepositories("all", "owner,organization_member", "full_name", "asc", 1, 100) } returns first
        coEvery { api.listRepositories("all", "owner,organization_member", "full_name", "asc", 2, 100) } returns second
        val result = manager.loadInventory().getOrThrow()
        assertEquals(101, result.size)
    }

    @Test
    fun `bulk PATCH is only sent after live eligible preflight`() = runTest {
        val eligibleDto = repo(1, "one")
        val eligible = eligibleDto.toCandidate()
        val blocked = repo(2, "fork", isFork = true).toCandidate()

        coEvery { api.getRepository("rafael", "one") } returns eligibleDto
        coEvery { api.updateVisibility("rafael", "one", any()) } returns eligibleDto.copy(
            isPrivate = true,
            visibility = "private"
        )
        every { receiptStore.save(any()) } returns Result.success("/private/receipt.json")

        val result = manager.makePrivate(listOf(eligible, blocked))

        assertEquals(1, result.receipt.updated)
        assertEquals(1, result.receipt.skipped)
        assertEquals(0, result.receipt.failed)
        assertEquals(PrivacyProvenanceState.DURABLE, result.provenanceState)
        coVerify(exactly = 1) { api.getRepository("rafael", "one") }
        coVerify(exactly = 1) { api.updateVisibility("rafael", "one", any()) }
        coVerify(exactly = 0) { api.updateVisibility("rafael", "fork", any()) }
    }

    @Test
    fun `receipt journal initialization failure blocks every GitHub mutation`() = runTest {
        val eligible = repo(1, "one").toCandidate()
        every { receiptStore.save(any()) } returns Result.failure(IllegalStateException("disk unavailable"))

        val result = manager.makePrivate(listOf(eligible))

        assertEquals(PrivacyProvenanceState.FAILED_BEFORE_MUTATION, result.provenanceState)
        assertEquals(1, result.receipt.notAttempted)
        assertEquals(0, result.receipt.updated)
        assertNull(result.receiptPath)
        coVerify(exactly = 0) { api.getRepository(any(), any()) }
        coVerify(exactly = 0) { api.updateVisibility(any(), any(), any()) }
    }

    @Test
    fun `stale inventory cannot authorize PATCH when live admin permission disappears`() = runTest {
        val selectedDto = repo(1, "one", admin = true)
        val selected = selectedDto.toCandidate()
        val liveWithoutAdmin = selectedDto.copy(
            permissions = PrivacyRepositoryPermissionsDto(admin = false)
        )

        coEvery { api.getRepository("rafael", "one") } returns liveWithoutAdmin
        every { receiptStore.save(any()) } returns Result.success("/private/checkpoint.json")

        val result = manager.makePrivate(listOf(selected))

        assertEquals(0, result.receipt.updated)
        assertEquals(1, result.receipt.skipped)
        assertTrue(result.receipt.mutations.single().message.contains("Live preflight blocked"))
        coVerify(exactly = 1) { api.getRepository("rafael", "one") }
        coVerify(exactly = 0) { api.updateVisibility(any(), any(), any()) }
    }

    @Test
    fun `repository identity mismatch is blocked before PATCH`() = runTest {
        val selectedDto = repo(1, "one", admin = true)
        val selected = selectedDto.toCandidate()
        coEvery { api.getRepository("rafael", "one") } returns selectedDto.copy(id = 999)
        every { receiptStore.save(any()) } returns Result.success("/private/checkpoint.json")

        val result = manager.makePrivate(listOf(selected))

        assertEquals(1, result.receipt.skipped)
        assertTrue(result.receipt.mutations.single().message.contains("identity changed"))
        coVerify(exactly = 0) { api.updateVisibility(any(), any(), any()) }
    }

    private fun repo(
        id: Long,
        name: String,
        isFork: Boolean = false,
        isPrivate: Boolean = false,
        admin: Boolean = true
    ) = PrivacyRepositoryDto(
        id = id,
        name = name,
        fullName = "rafael/$name",
        owner = PrivacyRepositoryOwnerDto("rafael", "User"),
        isPrivate = isPrivate,
        isFork = isFork,
        visibility = if (isPrivate) "private" else "public",
        permissions = PrivacyRepositoryPermissionsDto(admin = admin)
    )
}
