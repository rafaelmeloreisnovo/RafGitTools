package com.rafgittools.data.git

import com.rafgittools.CoroutineTestRule
import com.rafgittools.core.logging.DiffAuditLogger
import com.rafgittools.domain.model.GitAuthor
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertFailsWith

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
    fun `openRepository should propagate cancellation`() = runTest {
        val cancelledJob = Job().apply { cancel() }

        assertFailsWith<CancellationException> {
            withContext(coroutineContext + cancelledJob) {
                jgitService.openRepository("/tmp/non-existent-repo")
            }
        }
    }

    @Test
    fun `getStatus should propagate cancellation`() = runTest {
        val repoDir = createRepositoryWithInitialCommit("status-cancel")
        val cancelledJob = Job().apply { cancel() }

        assertFailsWith<CancellationException> {
            withContext(coroutineContext + cancelledJob) {
                jgitService.getStatus(repoDir.absolutePath)
            }
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
