package com.rafgittools

import com.rafgittools.domain.model.GitAuthor
import com.rafgittools.domain.model.GitCommit
import com.rafgittools.domain.model.GitRepository
import com.rafgittools.domain.model.github.GithubIssue
import com.rafgittools.domain.model.github.GithubLabel
import com.rafgittools.domain.model.github.GithubUser
import com.rafgittools.domain.repository.Credentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Utilities para testes do RafGitTools.
 */
object TestFixtures {
    fun createMockGitRepository() = GitRepository(
        id = "test-repo",
        name = "TestRepo",
        path = "/tmp/test",
        remoteUrl = "https://github.com/test/repo",
        currentBranch = "main",
        lastUpdated = System.currentTimeMillis()
    )

    fun createMockGitCommit(message: String = "Test commit") = GitCommit(
        sha = "abc123",
        message = message,
        author = GitAuthor("Test Author", "test@example.com"),
        committer = GitAuthor("Test Author", "test@example.com"),
        timestamp = System.currentTimeMillis(),
        parents = emptyList()
    )

    fun createMockIssue(number: Int = 1) = GithubIssue(
        id = number.toLong(),
        number = number,
        title = "Test Issue $number",
        body = "Test body",
        state = "open",
        user = GithubUser(
            id = 1,
            login = "testuser",
            avatarUrl = "",
            htmlUrl = "",
            type = "User"
        ),
        labels = listOf(
            GithubLabel(
                id = 1,
                name = "bug",
                color = "ff0000",
                description = "Bug"
            )
        ),
        assignees = emptyList(),
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        closedAt = null,
        htmlUrl = "https://github.com/test/repo/issues/$number",
        commentsCount = 0
    )

    fun createTokenCredentials() = Credentials.Token("test_token_123")

    fun createSshCredentials() = Credentials.SshKey(
        privateKeyPath = "/tmp/id_ed25519",
        passphrase = null
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTestRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
