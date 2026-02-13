package com.rafgittools.data.git

import com.rafgittools.CoroutineTestRule
import com.rafgittools.core.logging.DiffAuditLogger
import com.rafgittools.domain.model.GitAuthor
import io.mockk.mockk
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
