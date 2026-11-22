package com.rafgittools.setupwizard

import java.security.MessageDigest

/** V4 opt-in metadata walker: durably committed and read-back batches before checkpoints. V3 untouched. */
object HyperAtlasV4Incremental {
    const val SCHEMA = "rafgittools.hyperatlas.saf-incremental/v4"

    data class Work(val documentId: String, val path: String)
    data class Child(val documentId: String, val name: String, val mime: String,
                     val sizeBytes: Long?, val directory: Boolean)
    data class Row(val key: String, val parentKey: String, val type: String,
                   val path: String, val mime: String, val sizeBytes: Long?)
    data class Checkpoint(val schema: String = SCHEMA, val scope: String,
                          val pending: List<Work>, val visited: Set<String>,
                          val completedDirectories: Int = 0, val records: Long = 0,
                          val state: String = "IN_PROGRESS")

    interface Source { fun children(parentId: String, maxChildren: Int): List<Child> }
    interface Store {
        fun load(): Checkpoint?
        /** Must verify readback; must accept exact replay and reject divergent replay. */
        fun commitDirectory(parentId: String, rows: List<Row>)
        /** Must persist atomically; failures must leave prior checkpoint recoverable. */
        fun save(checkpoint: Checkpoint)
    }

    fun digest(s: String): String = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun nameOK(s: String): Boolean = s.isNotEmpty() && s != "." && s != ".." &&
        s.length <= 255 && s.none { it == '/' || it == '\\' || it.code < 32 }

    /** maxDirectories caps state size; no unbounded queues on a low-memory phone. */
    fun run(scope: String, rootId: String, source: Source, store: Store,
            maxDirectories: Int = 10000, maxChildrenPerDirectory: Int = 2048,
            maxRecords: Long = 250_000L): Checkpoint {
        require(scope.matches(Regex("[a-f0-9]{64}"))) { "V4_INVALID_SCOPE" }
        require(rootId.isNotEmpty()) { "V4_MISSING_ROOT" }
        require(maxDirectories in 1..100_000 && maxChildrenPerDirectory in 1..10_000 && maxRecords > 0) {
            "V4_INVALID_LIMIT"
        }
        var state = store.load() ?: Checkpoint(scope = scope,
            pending = listOf(Work(rootId, "")), visited = emptySet()).also(store::save)
        require(state.schema == SCHEMA && state.scope == scope) { "V4_CHECKPOINT_SCOPE_MISMATCH" }
        require(state.pending.size + state.visited.size <= maxDirectories) { "V4_SCOPE_LIMIT" }
        if (state.state == "COMPLETE") {
            require(state.pending.isEmpty()) { "V4_INVALID_COMPLETE" }
            return state
        }
        require(state.state == "IN_PROGRESS") { "V4_CHECKPOINT_STATE_INVALID" }
        while (state.pending.isNotEmpty()) {
            val parent = state.pending.first()
            if (parent.documentId in state.visited) {
                val remaining = state.pending.drop(1)
                state = state.copy(pending = remaining,
                    state = if (remaining.isEmpty()) "COMPLETE" else "IN_PROGRESS")
                store.save(state)
                continue
            }
            val children = source.children(parent.documentId, maxChildrenPerDirectory)
            require(children.size <= maxChildrenPerDirectory) { "V4_CHILD_LIMIT" }
            val rows = ArrayList<Row>(children.size)
            val next = ArrayList<Work>()
            val ids = HashSet<String>()
            val parentKey = digest(scope + "\u0000" + parent.documentId)
            for (child in children) {
                require(child.documentId.isNotEmpty() && ids.add(child.documentId)) {
                    "V4_INVALID_OR_DUPLICATE_CHILD_ID"
                }
                require(nameOK(child.name)) { "V4_INVALID_CHILD_NAME" }
                require(child.sizeBytes == null || child.sizeBytes >= 0) { "V4_INVALID_SIZE" }
                val path = if (parent.path.isEmpty()) child.name else parent.path + "/" + child.name
                require(path.length <= 4096) { "V4_PATH_TOO_LONG" }
                rows += Row(digest(scope + "\u0000" + child.documentId), parentKey,
                    if (child.directory) "DIRECTORY" else "FILE", path,
                    child.mime, child.sizeBytes)
                if (child.directory && child.documentId !in state.visited && child.documentId != parent.documentId) {
                    next += Work(child.documentId, path)
                }
            }
            val pending = (state.pending.drop(1) + next)
                .distinctBy { it.documentId }
            require(pending.size + state.visited.size + 1 <= maxDirectories) {
                "V4_SCOPE_LIMIT"
            }
            require(state.records <= maxRecords - rows.size) { "V4_RECORD_LIMIT" }
            // Idempotent batch first, then atomic checkpoint: crash between them replays safely.
            store.commitDirectory(parent.documentId, rows)
            state = state.copy(pending = pending,
                visited = state.visited + parent.documentId,
                completedDirectories = state.completedDirectories + 1,
                records = state.records + rows.size,
                state = if (pending.isEmpty()) "COMPLETE" else "IN_PROGRESS")
            store.save(state)
        }
        return state
    }
}
