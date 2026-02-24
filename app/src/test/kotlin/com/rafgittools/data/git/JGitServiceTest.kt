package com.rafgittools.data.git

import com.rafgittools.CoroutineTestRule
import com.rafgittools.core.logging.DiffAuditLogger
import com.rafgittools.domain.model.GitAuthor
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Testes de integração local para JGitService.
 */
class JGitServiceTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var jgitService: JGitService

    @Before
    fun setup() {
        val mockDiffAuditLogger = mockk<DiffAuditLogger>(relaxed = true)
        jgitService = JGitService(mockDiffAuditLogger)
    }

    @Test
    fun `cloneRepository with local source should succeed`() = runTest {
        val sourceRepo = createRepositoryWithInitialCommit("source-clone")
        val cloneDir = Files.createTempDirectory("clone-target").toFile().resolve("repo-clone")

        val result = jgitService.cloneRepository(sourceRepo.absolutePath, cloneDir.absolutePath, null)

        assertTrue(result.isSuccess)
        assertEquals("repo-clone", result.getOrNull()?.name)
        assertTrue(File(cloneDir, ".git").exists())
    }

    @Test
    fun `cloneRepository with invalid URL should fail`() = runTest {
        val cloneDir = Files.createTempDirectory("clone-invalid").toFile().resolve("repo")

        val result = jgitService.cloneRepository("invalid-url", cloneDir.absolutePath, null)

        assertTrue(result.isFailure)
    }

    @Test
    fun `commit should create commit with message and author`() = runTest {
        val repoDir = createRepositoryWithInitialCommit("commit-test")
        val trackedFile = File(repoDir, "notes.txt")
        trackedFile.appendText("\nsecond line")

        jgitService.stageFiles(repoDir.absolutePath, listOf("notes.txt")).getOrThrow()
        val commit = jgitService.commit(
            repoPath = repoDir.absolutePath,
            message = "Add second line",
            author = GitAuthor("Tester", "tester@example.com")
        ).getOrThrow()

        assertEquals("Add second line", commit.message)
        assertEquals("Tester", commit.author.name)
    }

    @Test
    fun `createBranch and deleteBranch should update branch list`() = runTest {
        val repoDir = createRepositoryWithInitialCommit("branch-test")

        val created = jgitService.createBranch(repoDir.absolutePath, "feature/test", null)
        assertTrue(created.isSuccess)

        jgitService.deleteBranch(repoDir.absolutePath, "feature/test", true).getOrThrow()

        val branches = jgitService.getBranches(repoDir.absolutePath).getOrThrow()
        assertFalse(branches.any { it.shortName == "feature/test" && it.isLocal })
    }


    @Test
    fun `forcePushWithLease with stale lease should fail`() = runTest {
        val remoteBare = Files.createTempDirectory("remote-bare").toFile()
        Git.init().setBare(true).setDirectory(remoteBare).call().close()

        val seedRepoDir = createRepositoryWithInitialCommit("seed-repo")
        Git.open(seedRepoDir).use { seedGit ->
            seedGit.remoteAdd()
                .setName("origin")
                .setUri(org.eclipse.jgit.transport.URIish(remoteBare.absolutePath))
                .call()
            seedGit.push().setRemote("origin").call()
        }

        val cloneDir = Files.createTempDirectory("lease-clone").toFile().resolve("repo")
        jgitService.cloneRepository(remoteBare.absolutePath, cloneDir.absolutePath, null).getOrThrow()

        val branch = Git.open(cloneDir).use { it.repository.branch }
        File(cloneDir, "lease.txt").writeText("lease-change")
        jgitService.stageFiles(cloneDir.absolutePath, listOf("lease.txt")).getOrThrow()
        jgitService.commit(cloneDir.absolutePath, "lease commit", GitAuthor("Tester", "tester@example.com")).getOrThrow()

        val result = jgitService.forcePushWithLease(
            repoPath = cloneDir.absolutePath,
            remote = "origin",
            branch = branch,
            expectedOldObjectId = "0000000000000000000000000000000000000000",
            credentials = null
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `getStatus should propagate cancellation from openRepository`() = runTest {
        val service = spyk(jgitService)
        coEvery { service.openRepository(any()) } throws CancellationException("cancelled")

        try {
            service.getStatus("/tmp/repo")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `getBranches should propagate cancellation from openRepository`() = runTest {
        val service = spyk(jgitService)
        coEvery { service.openRepository(any()) } throws CancellationException("cancelled")

        try {
            service.getBranches("/tmp/repo")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `listStashes should propagate cancellation from openRepository`() = runTest {
        val service = spyk(jgitService)
        coEvery { service.openRepository(any()) } throws CancellationException("cancelled")

        try {
            service.listStashes("/tmp/repo")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    @Test
    fun `listTags should propagate cancellation from openRepository`() = runTest {
        val service = spyk(jgitService)
        coEvery { service.openRepository(any()) } throws CancellationException("cancelled")

        try {
            service.listTags("/tmp/repo")
            fail("Expected CancellationException")
        } catch (_: CancellationException) {
            // expected
        }
    }

    private fun createRepositoryWithInitialCommit(prefix: String): File {
        val dir = Files.createTempDirectory(prefix).toFile()
        Git.init().setDirectory(dir).call().use { git ->
            File(dir, "README.md").writeText("initial")
            git.add().addFilepattern("README.md").call()
            git.commit().setMessage("initial commit").setAuthor("Init", "init@example.com").call()
        }
        return dir
    }
}
