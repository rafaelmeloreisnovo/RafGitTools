package com.rafgittools.core.vcs

import android.content.Context
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

data class BisectCommitInfo(
    val hash: String,
    val shortHash: String,
    val message: String,
    val author: String,
    val date: String
)

class BisectManager(private val context: Context) {

    private var bisectSession: Git? = null
    private var bisectCandidates: MutableList<RevCommit>? = null
    private var bisectIndex = 0

    fun startBisect(goodCommitHash: String, badCommitHash: String): Result<List<String>> = runCatching {
        val repoPath = context.filesDir.absolutePath
        val git = Git.open(File(repoPath))
        val repo = git.repository
        val revWalk = RevWalk(repo)

        val goodCommit = revWalk.parseCommit(repo.resolve(goodCommitHash))
        val badCommit = revWalk.parseCommit(repo.resolve(badCommitHash))

        // Get all commits between good and bad
        revWalk.markStart(badCommit)
        revWalk.markUninteresting(goodCommit)

        val candidates = mutableListOf<RevCommit>()
        for (commit in revWalk) {
            candidates.add(commit)
        }

        candidates.reverse() // Sort chronologically
        revWalk.close()

        bisectSession = git
        bisectCandidates = candidates
        bisectIndex = candidates.size / 2

        git.close()

        candidates.map { it.abbreviate(40).name }
    }

    fun markCommitGood(commitHash: String): Result<String?> = runCatching {
        val git = bisectSession ?: throw IllegalStateException("No bisect session active")
        val candidates = bisectCandidates ?: throw IllegalStateException("No bisect candidates")

        val currentIndex = candidates.indexOfFirst { it.abbreviate(40).name == commitHash }
        if (currentIndex >= 0) {
            // Remove all commits before and including this one
            candidates.subList(0, currentIndex + 1).clear()
        }

        if (candidates.isEmpty()) {
            null
        } else {
            bisectIndex = candidates.size / 2
            candidates[bisectIndex].abbreviate(40).name
        }
    }

    fun markCommitBad(commitHash: String): Result<String?> = runCatching {
        val git = bisectSession ?: throw IllegalStateException("No bisect session active")
        val candidates = bisectCandidates ?: throw IllegalStateException("No bisect candidates")

        val currentIndex = candidates.indexOfFirst { it.abbreviate(40).name == commitHash }
        if (currentIndex >= 0) {
            // Remove all commits after and including this one
            candidates.subList(currentIndex, candidates.size).clear()
        }

        if (candidates.isEmpty()) {
            null
        } else {
            bisectIndex = candidates.size / 2
            candidates[bisectIndex].abbreviate(40).name
        }
    }

    fun skipCommit(commitHash: String): Result<String?> = runCatching {
        val candidates = bisectCandidates ?: throw IllegalStateException("No bisect candidates")

        val currentIndex = candidates.indexOfFirst { it.abbreviate(40).name == commitHash }
        if (currentIndex >= 0) {
            candidates.removeAt(currentIndex)
            if (bisectIndex >= candidates.size) {
                bisectIndex = candidates.size - 1
            }
        }

        if (candidates.isEmpty()) {
            null
        } else {
            candidates[bisectIndex].abbreviate(40).name
        }
    }

    fun endBisect(): Result<String> = runCatching {
        val candidates = bisectCandidates ?: throw IllegalStateException("No bisect candidates")

        if (candidates.isEmpty()) {
            throw IllegalStateException("Bisect did not converge to a single commit")
        }

        val firstBadCommit = candidates.first().abbreviate(40).name

        resetBisect()

        firstBadCommit
    }

    fun resetBisect() {
        bisectSession?.close()
        bisectSession = null
        bisectCandidates = null
        bisectIndex = 0
    }

    fun getCommitInfo(commitHash: String): Result<BisectCommitInfo> = runCatching {
        val git = bisectSession ?: throw IllegalStateException("No bisect session active")
        val repo = git.repository
        val revWalk = RevWalk(repo)

        val commit = revWalk.parseCommit(repo.resolve(commitHash))
        val author = commit.authorIdent?.name ?: "Unknown"
        val date = formatDate(commit.authorIdent)

        revWalk.close()

        BisectCommitInfo(
            hash = commit.abbreviate(40).name,
            shortHash = commit.abbreviate(7).name,
            message = commit.shortMessage,
            author = author,
            date = date
        )
    }

    private fun formatDate(ident: PersonIdent?): String {
        return if (ident != null) {
            SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date(ident.when * 1000))
        } else {
            "Unknown"
        }
    }
}
