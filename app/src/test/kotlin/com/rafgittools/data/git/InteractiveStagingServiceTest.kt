package com.rafgittools.data.git

import com.google.common.truth.Truth.assertThat
import com.rafgittools.CoroutineTestRule
import com.rafgittools.core.logging.DiffAuditLogger
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertFailsWith

class InteractiveStagingServiceTest {

    @get:Rule
    val coroutineRule = CoroutineTestRule()

    private lateinit var jGitService: JGitService
    private lateinit var service: InteractiveStagingService

    @Before
    fun setup() {
        val auditLogger = mockk<DiffAuditLogger>(relaxed = true)
        jGitService = JGitService(auditLogger)
        service = InteractiveStagingService(jGitService)
    }

    @Test
    fun `stage one hunk leaves other hunk unstaged and preserves working tree`() = runTest {
        val repo = createTwoHunkFixture("interactive-stage")
        val workFile = File(repo, FILE_NAME)
        val workBytesBefore = workFile.readBytes()
        val diff = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()

        assertThat(diff.hunks).hasSize(2)
        val result = service.stageHunk(repo.absolutePath, diff, diff.hunks.first())

        assertThat(result.isSuccess).isTrue()
        assertThat(workFile.readBytes()).isEqualTo(workBytesBefore)

        val staged = jGitService.getDiff(repo.absolutePath, cached = true).getOrThrow()
        val unstaged = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow()

        assertThat(staged).hasSize(1)
        assertThat(staged.single().hunks).hasSize(1)
        assertThat(unstaged).hasSize(1)
        assertThat(unstaged.single().hunks).hasSize(1)

        val stagedText = staged.single().hunks.single().lines.joinToString("\n") { it.content }
        val unstagedText = unstaged.single().hunks.single().lines.joinToString("\n") { it.content }
        assertThat(stagedText).contains("changed-near-top")
        assertThat(stagedText).doesNotContain("changed-near-bottom")
        assertThat(unstagedText).contains("changed-near-bottom")
        assertThat(unstagedText).doesNotContain("changed-near-top")
    }

    @Test
    fun `unstage one staged hunk restores it to unstaged set only`() = runTest {
        val repo = createTwoHunkFixture("interactive-unstage")
        val initial = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()
        service.stageHunk(repo.absolutePath, initial, initial.hunks.first()).getOrThrow()

        val staged = jGitService.getDiff(repo.absolutePath, cached = true).getOrThrow().single()
        assertThat(staged.hunks).hasSize(1)

        val result = service.unstageHunk(repo.absolutePath, staged, staged.hunks.single())
        assertThat(result.isSuccess).isTrue()

        assertThat(jGitService.getDiff(repo.absolutePath, cached = true).getOrThrow()).isEmpty()
        val unstaged = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()
        assertThat(unstaged.hunks).hasSize(2)
    }

    @Test
    fun `stale hunk fails closed without changing index`() = runTest {
        val repo = createTwoHunkFixture("interactive-stale")
        val workFile = File(repo, FILE_NAME)
        val staleDiff = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()
        val staleHunk = staleDiff.hunks.first()

        val lines = workFile.readLines().toMutableList()
        lines[2] = "concurrent-change-inside-first-hunk"
        workFile.writeText(lines.joinToString("\n") + "\n")

        val result = service.stageHunk(repo.absolutePath, staleDiff, staleHunk)

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("stale")
        assertThat(jGitService.getDiff(repo.absolutePath, cached = true).getOrThrow()).isEmpty()
    }

    @Test
    fun `missing final newline is rejected without index mutation`() = runTest {
        val repo = createRepository("interactive-no-newline")
        val workFile = File(repo, FILE_NAME)
        workFile.writeText("line-01\nchanged\nline-03")
        val diff = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()

        val result = service.stageHunk(repo.absolutePath, diff, diff.hunks.single())

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()?.message).contains("Missing-final-newline")
        assertThat(jGitService.getDiff(repo.absolutePath, cached = true).getOrThrow()).isEmpty()
    }

    @Test
    fun `interactive staging propagates coroutine cancellation`() = runTest {
        val repo = createTwoHunkFixture("interactive-cancel")
        val diff = jGitService.getDiff(repo.absolutePath, cached = false).getOrThrow().single()
        val cancelledJob = Job().apply { cancel() }

        assertFailsWith<CancellationException> {
            withContext(coroutineContext + cancelledJob) {
                service.stageHunk(repo.absolutePath, diff, diff.hunks.first())
            }
        }
    }

    private fun createTwoHunkFixture(prefix: String): File {
        val repo = createRepository(prefix)
        val workFile = File(repo, FILE_NAME)
        val lines = workFile.readLines().toMutableList()
        lines[1] = "changed-near-top"
        lines[17] = "changed-near-bottom"
        workFile.writeText(lines.joinToString("\n") + "\n")
        return repo
    }

    private fun createRepository(prefix: String): File {
        val dir = Files.createTempDirectory(prefix).toFile()
        Git.init().setDirectory(dir).call().use { git ->
            val lines = (1..20).map { n -> "line-${n.toString().padStart(2, '0')}" }
            File(dir, FILE_NAME).writeText(lines.joinToString("\n") + "\n")
            git.add().addFilepattern(FILE_NAME).call()
            git.commit()
                .setMessage("initial")
                .setAuthor("Tester", "tester@example.com")
                .call()
        }
        return dir
    }

    companion object {
        private const val FILE_NAME = "sample.txt"
    }
}
