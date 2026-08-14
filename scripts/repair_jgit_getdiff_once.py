#!/usr/bin/env python3
from pathlib import Path

path = Path("app/src/main/kotlin/com/rafgittools/data/git/JGitService.kt")
text = path.read_text(encoding="utf-8")
start_marker = "    suspend fun getDiff(\n"
end_marker = "    /**\n     * Get diff between two commits\n"
start = text.find(start_marker)
end = text.find(end_marker, start + 1)
if start < 0 or end < 0:
    raise SystemExit(f"fail-closed: getDiff markers missing start={start} end={end}")
if text.find(start_marker, start + 1) >= 0:
    raise SystemExit("fail-closed: multiple getDiff markers found")
if "Keep scan + format on the same DiffFormatter" in text[start:end]:
    print("getDiff repair already present")
    raise SystemExit(0)

replacement = '''    suspend fun getDiff(
        repoPath: String,
        cached: Boolean = false
    ): Result<List<GitDiff>> = withContext(Dispatchers.IO) {
        runCatching {
        openRepository(repoPath).getOrThrow().use { git ->
            val repository = git.repository
            repository.newObjectReader().use { reader ->
                val outputStream = java.io.ByteArrayOutputStream()
                org.eclipse.jgit.diff.DiffFormatter(outputStream).use { formatter ->
                    formatter.setRepository(repository)
                    formatter.setDetectRenames(true)

                    val oldTree: org.eclipse.jgit.treewalk.AbstractTreeIterator
                    val newTree: org.eclipse.jgit.treewalk.AbstractTreeIterator
                    if (cached) {
                        val headTreeId = repository.resolve(Constants.HEAD + "^{tree}")
                            ?: throw IllegalStateException("No HEAD tree found")
                        oldTree = org.eclipse.jgit.treewalk.CanonicalTreeParser().apply {
                            reset(reader, headTreeId)
                        }
                        newTree = org.eclipse.jgit.dircache.DirCacheIterator(
                            repository.readDirCache()
                        )
                    } else {
                        oldTree = org.eclipse.jgit.dircache.DirCacheIterator(
                            repository.readDirCache()
                        )
                        newTree = org.eclipse.jgit.treewalk.FileTreeIterator(repository)
                    }

                    // Keep scan + format on the same DiffFormatter. Its ContentSource
                    // retains FileTreeIterator access for unstaged working-tree blobs
                    // that are not yet materialized in the Git object database.
                    val diffs = formatter.scan(oldTree, newTree)
                    diffs.map { diffEntry ->
                        outputStream.reset()
                        formatter.format(diffEntry)
                        formatter.flush()

                        val diffContent = outputStream.toString("UTF-8")
                        val hunks = parseDiffHunks(diffContent)
                        logDiffAudit(repoPath, diffEntry, diffContent)

                        val oldContent = runCatching {
                            val oid = diffEntry.oldId.toObjectId()
                            if (oid != org.eclipse.jgit.lib.ObjectId.zeroId())
                                repository.open(oid).bytes.toString(Charsets.UTF_8)
                                    .takeUnless { s -> s.take(512).any { c -> c == '\\u0000' } }
                            else null
                        }.getOrNull()

                        val newContent = if (!cached && diffEntry.newPath != "/dev/null") {
                            runCatching {
                                val file = java.io.File(repoPath, diffEntry.newPath)
                                if (!file.isFile || file.length() > 1_000_000L) null
                                else file.readBytes().let { bytes ->
                                    if (bytes.take(512).any { it.toInt() == 0 }) null
                                    else String(bytes, Charsets.UTF_8)
                                }
                            }.getOrNull()
                        } else {
                            runCatching {
                                val oid = diffEntry.newId.toObjectId()
                                if (oid != org.eclipse.jgit.lib.ObjectId.zeroId())
                                    repository.open(oid).bytes.toString(Charsets.UTF_8)
                                        .takeUnless { s -> s.take(512).any { c -> c == '\\u0000' } }
                                else null
                            }.getOrNull()
                        }

                        val changeType = when (diffEntry.changeType) {
                            org.eclipse.jgit.diff.DiffEntry.ChangeType.ADD -> DiffChangeType.ADD
                            org.eclipse.jgit.diff.DiffEntry.ChangeType.MODIFY -> DiffChangeType.MODIFY
                            org.eclipse.jgit.diff.DiffEntry.ChangeType.DELETE -> DiffChangeType.DELETE
                            org.eclipse.jgit.diff.DiffEntry.ChangeType.RENAME -> DiffChangeType.RENAME
                            org.eclipse.jgit.diff.DiffEntry.ChangeType.COPY -> DiffChangeType.COPY
                        }

                        GitDiff(
                            oldPath = if (diffEntry.oldPath != "/dev/null") diffEntry.oldPath else null,
                            newPath = if (diffEntry.newPath != "/dev/null") diffEntry.newPath else null,
                            changeType = changeType,
                            oldContent = oldContent,
                            newContent = newContent,
                            hunks = hunks
                        )
                    }
                }
            }
        }
        }
    }

'''
path.write_text(text[:start] + replacement + text[end:], encoding="utf-8")
print("repaired JGitService.getDiff")
