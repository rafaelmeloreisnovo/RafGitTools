package com.rafgittools.data.drive

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Real Google Drive REST API, never Android SAF or phone-local file paths.
 * All writes create NEW children only, after explicit folder selection.
 * No sources are downloaded, modified, moved or deleted.
 *
 * Authorization tokens come from Google Identity AuthorizationClient, not GitHub PAT.
 * Never serialize tokens or include them in an exception, receipt or manifest.
 */
class GoogleDriveCloudRepository(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .callTimeout(45, TimeUnit.SECONDS).build()
) {
    companion object {
        // Full Drive scope is restricted. Required to create children under ANY
        // user-chosen existing folder; Google Cloud OAuth app review is an external gate.
        const val SCOPE = "https://www.googleapis.com/auth/drive"
        const val FOLDER_MIME = "application/vnd.google-apps.folder"
        const val MAX_DOCUMENTS = 5_000
        const val SHARD_BYTES = 256 * 1024
        const val MAX_SHARDS = 256
        const val MAX_INDEX_BYTES = 32 * 1024 * 1024
        private const val MAX_HTTP_BYTES = 2 * 1024 * 1024
        private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,256}")

        internal fun checkedId(id: String): String {
            require(ID_PATTERN.matches(id)) { "BLOCKED_DRIVE_INVALID_FOLDER_ID" }
            return id
        }

        internal fun checkedName(name: String): String {
            require(name.isNotBlank() && name.length <= 512 &&
                name != "." && name != ".." && !name.contains('/') &&
                !name.contains('\\') && name.none { it.code < 32 }) {
                "BLOCKED_DRIVE_UNSAFE_NAME"
            }
            return name
        }

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
    }

    data class Item(
        val id: String,
        val name: String,
        val mime: String,
        val size: Long?,
        val canAddChildren: Boolean?
    ) {
        val folder: Boolean get() = mime == FOLDER_MIME
    }

    data class Page(val items: List<Item>, val next: String?)
    data class Selection(val id: String, val name: String)
    data class Receipt(val folderName: String, val records: Int, val shards: Int, val manifestSha256: String)

    private fun endpoint(path: String): HttpUrl =
        HttpUrl.Builder().scheme("https").host("www.googleapis.com")
            .addPathSegments(path).build()

    private fun readBounded(response: okhttp3.Response): ByteArray {
        val stream = response.body?.byteStream() ?: throw IOException("BLOCKED_DRIVE_EMPTY_RESPONSE")
        return stream.use { input ->
            val buffer = ByteArray(8192)
            val output = ByteArrayOutputStream()
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                if (n == 0) continue
                if (output.size() > MAX_HTTP_BYTES - n) throw IOException("BLOCKED_DRIVE_RESPONSE_LIMIT")
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        }
    }

    private fun perform(token: String, request: Request, expectedCode: Int): ByteArray {
        require(token.isNotBlank()) { "BLOCKED_DRIVE_NOT_AUTHORIZED" }
        http.newCall(request.newBuilder().header("Authorization", "Bearer " + token)
            .header("Cache-Control", "no-store").build()).execute().use { response ->
            if (response.code != expectedCode) {
                // No provider error body: it may contain paths or other private metadata.
                throw IOException("BLOCKED_DRIVE_HTTP_" + response.code)
            }
            return readBounded(response)
        }
    }

    suspend fun list(
        token: String,
        folderId: String,
        pageToken: String? = null,
        pageSize: Int = 30
    ): Page = withContext(Dispatchers.IO) {
        checkedId(folderId)
        require(pageSize in 1..100) { "BLOCKED_DRIVE_PAGE_SIZE" }
        require(pageToken == null || pageToken.length in 1..4096) { "BLOCKED_DRIVE_PAGE_TOKEN" }

        val url = endpoint("drive/v3/files").newBuilder()
            .addQueryParameter("q", "'" + folderId + "' in parents and trashed = false")
            .addQueryParameter("pageSize", pageSize.toString())
            .addQueryParameter("fields",
                "nextPageToken,incompleteSearch,files(id,name,mimeType,size,capabilities(canAddChildren))")
            .addQueryParameter("orderBy", "folder,name")
            .addQueryParameter("supportsAllDrives", "true")
            .addQueryParameter("includeItemsFromAllDrives", "true")
            .apply { if (pageToken != null) addQueryParameter("pageToken", pageToken) }
            .build()
        val payload = perform(token, Request.Builder().url(url).get().build(), 200)
        val json = JsonParser.parseString(String(payload, Charsets.UTF_8)).asJsonObject
        if (json.get("incompleteSearch")?.asBoolean == true) {
            throw IOException("BLOCKED_DRIVE_INCOMPLETE_SEARCH")
        }
        val files = json.getAsJsonArray("files") ?: JsonArray()
        val items = ArrayList<Item>(files.size())
        for (entry in files) {
            val f = entry.asJsonObject
            val id = checkedId(f.get("id").asString)
            val name = checkedName(f.get("name").asString)
            val mime = f.get("mimeType")?.asString ?: "application/octet-stream"
            val rawSize = f.get("size")
            val size = if (rawSize == null || rawSize.isJsonNull) null
                else rawSize.asLong.also { require(it >= 0L) { "BLOCKED_DRIVE_NEGATIVE_SIZE" } }
            val permission = f.getAsJsonObject("capabilities")?.get("canAddChildren")
            items += Item(id, name, mime, size,
                if (permission == null || permission.isJsonNull) null else permission.asBoolean)
        }
        val next = json.get("nextPageToken")
            ?.takeUnless { it.isJsonNull }?.asString?.takeUnless { it.isBlank() }
        Page(items, next)
    }

    private data class Row(val path: String, val parent: String, val item: Item)

    private suspend fun inventory(token: String, source: Selection): List<Row> {
        checkedId(source.id)
        val pending = ArrayDeque<Pair<String, String>>()
        pending.addLast(source.id to "")
        val visited = HashSet<String>()
        val paths = HashSet<String>()
        val rows = ArrayList<Row>()
        while (pending.isNotEmpty()) {
            val (folderId, parentPath) = pending.removeFirst()
            if (!visited.add(folderId)) throw IOException("BLOCKED_DRIVE_DUPLICATE_FOLDER")
            var next: String? = null
            val pageTokens = HashSet<String>()
            do {
                val page = list(token, folderId, next, 100)
                for (item in page.items) {
                    val path = if (parentPath.isBlank()) item.name else parentPath + "/" + item.name
                    if (!paths.add(path)) throw IOException("BLOCKED_DRIVE_DUPLICATE_PATH")
                    if (rows.size >= MAX_DOCUMENTS) {
                        throw IOException("BLOCKED_DRIVE_INVENTORY_OVER_5000")
                    }
                    rows += Row(path, parentPath, item)
                    if (item.folder) pending.addLast(item.id to path)
                }
                next = page.next
                if (next != null && !pageTokens.add(next)) {
                    throw IOException("BLOCKED_DRIVE_REPEATED_PAGE_TOKEN")
                }
            } while (next != null)
        }
        return rows.sortedBy { it.path }
    }

    private fun jsonWrite(token: String, url: HttpUrl, body: JsonObject): JsonObject {
        val request = Request.Builder().url(url)
            .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        val bytes = perform(token, request, 200)
        return JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject
    }

    private fun newFolder(token: String, parentId: String, name: String): String {
        checkedId(parentId)
        val metadata = JsonObject().apply {
            addProperty("name", checkedName(name))
            addProperty("mimeType", FOLDER_MIME)
            add("parents", JsonArray().apply { add(parentId) })
        }
        val result = jsonWrite(token,
            endpoint("drive/v3/files").newBuilder()
                .addQueryParameter("supportsAllDrives", "true")
                .addQueryParameter("fields", "id,name,mimeType,parents").build(),
            metadata)
        require(result.get("name")?.asString == name &&
            result.get("mimeType")?.asString == FOLDER_MIME) {
            "BLOCKED_DRIVE_FOLDER_CREATE_MISMATCH"
        }
        return checkedId(result.get("id").asString)
    }

    private fun uploadAndReadback(
        token: String,
        parentId: String,
        name: String,
        bytes: ByteArray,
        mime: String
    ): Pair<String, String> {
        require(bytes.size <= SHARD_BYTES || (name == "MANIFEST.json" && bytes.size <= MAX_HTTP_BYTES)) {
            "BLOCKED_DRIVE_UPLOAD_TOO_LARGE"
        }
        val meta = JsonObject().apply {
            addProperty("name", checkedName(name))
            addProperty("mimeType", mime)
            add("parents", JsonArray().apply { add(checkedId(parentId)) })
        }
        val body = MultipartBody.Builder()
            .setType("multipart/related".toMediaType())
            .addPart(Headers.headersOf("Content-Type", "application/json; charset=UTF-8"),
                meta.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
            .addPart(Headers.headersOf("Content-Type", mime),
                bytes.toRequestBody(mime.toMediaType()))
            .build()
        val url = endpoint("upload/drive/v3/files").newBuilder()
            .addQueryParameter("uploadType", "multipart")
            .addQueryParameter("supportsAllDrives", "true")
            .addQueryParameter("fields", "id,name,size,md5Checksum")
            .build()
        val result = JsonParser.parseString(
            String(perform(token, Request.Builder().url(url).post(body).build(), 200), Charsets.UTF_8)
        ).asJsonObject
        val id = checkedId(result.get("id").asString)
        if (result.get("name")?.asString != name) throw IOException("BLOCKED_DRIVE_UPLOAD_NAME_MISMATCH")
        val storedSize = result.get("size")
        if (storedSize != null && !storedSize.isJsonNull && storedSize.asLong != bytes.size.toLong()) {
            throw IOException("BLOCKED_DRIVE_UPLOAD_SIZE_MISMATCH")
        }
        val readUrl = endpoint("drive/v3/files/" + id).newBuilder()
            .addQueryParameter("alt", "media")
            .addQueryParameter("supportsAllDrives", "true").build()
        val remote = perform(token, Request.Builder().url(readUrl).get().build(), 200)
        if (remote.size != bytes.size || sha256(remote) != sha256(bytes)) {
            throw IOException("BLOCKED_DRIVE_READBACK_SHA256_MISMATCH")
        }
        return id to sha256(remote)
    }

    /**
     * Create a new index folder in user-selected Drive destination. Commit is
     * the LAST uploaded MANIFEST.json. Failed runs remain partial (no delete).
     */
    suspend fun publish(
        token: String,
        source: Selection,
        destination: Selection
    ): Receipt = withContext(Dispatchers.IO) {
        val rows = inventory(token, source) // scan FIRST; incomplete inventory does not write
        val runName = "HYPERATLAS_CLOUD_V1_" + UUID.randomUUID().toString()
        val runId = newFolder(token, destination.id, runName)
        val parts = JsonArray()
        val buffer = ByteArrayOutputStream(SHARD_BYTES)
        val indexDigest = MessageDigest.getInstance("SHA-256")
        var rowsInShard = 0
        var total = 0
        var countShards = 0
        var archiveCandidates = 0

        fun lineOf(row: Row): ByteArray {
            val json = JsonObject().apply {
                addProperty("type", if (row.item.folder) "DIRECTORY" else "FILE")
                addProperty("path", row.path)
                addProperty("parent", row.parent)
                addProperty("mime", row.item.mime)
                if (row.item.size == null) add("size_bytes", JsonNull.INSTANCE)
                else addProperty("size_bytes", row.item.size)
                addProperty("archive_candidate",
                    row.path.lowercase().endsWith(".zip") ||
                    row.path.lowercase().endsWith(".zipraf"))
            }
            return (json.toString() + "\n").toByteArray(Charsets.UTF_8)
        }

        fun flush() {
            if (rowsInShard == 0) return
            if (countShards >= MAX_SHARDS) throw IOException("BLOCKED_DRIVE_INDEX_PART_LIMIT")
            val bytes = buffer.toByteArray()
            val name = "index-%05d.jsonl".format(java.util.Locale.ROOT, countShards + 1)
            val (fileId, hash) = uploadAndReadback(token, runId, name, bytes, "application/json")
            parts.add(JsonObject().apply {
                addProperty("ordinal", countShards + 1)
                addProperty("name", name)
                addProperty("file_id", fileId)
                addProperty("bytes", bytes.size)
                addProperty("sha256", hash)
                addProperty("records", rowsInShard)
            })
            countShards++
            buffer.reset()
            rowsInShard = 0
        }

        for (row in rows) {
            val line = lineOf(row)
            if (line.size > SHARD_BYTES || total > MAX_INDEX_BYTES - line.size) {
                throw IOException("BLOCKED_DRIVE_INDEX_LIMIT")
            }
            if (buffer.size() + line.size > SHARD_BYTES) flush()
            buffer.write(line)
            indexDigest.update(line)
            total += line.size
            rowsInShard++
            if (row.path.endsWith(".zip", true) || row.path.endsWith(".zipraf", true)) archiveCandidates++
        }
        flush()
        val manifest = JsonObject().apply {
            addProperty("schema", "rafgittools.hyperatlas.drive-api/v1")
            addProperty("source_scope", "USER_SELECTED_GOOGLE_DRIVE_FOLDER_ONLY")
            addProperty("source_folder_id", source.id)
            addProperty("destination_folder_id", destination.id)
            addProperty("run_folder_id", runId)
            addProperty("run_folder_name", runName)
            addProperty("relation_model", "CONTAINS(parent,path)")
            addProperty("source_bytes_read", false)
            addProperty("source_mutated", false)
            addProperty("archive_members_read", false)
            addProperty("claim_allowed", false)
            addProperty("device_tested", false)
            addProperty("records", rows.size)
            addProperty("folders", rows.count { it.item.folder })
            addProperty("files", rows.count { !it.item.folder })
            addProperty("archive_candidates", archiveCandidates)
            addProperty("index_bytes", total)
            addProperty("index_sha256", indexDigest.digest()
                .joinToString("") { "%02x".format(it) })
            add("parts", parts)
        }
        val (_, hash) = uploadAndReadback(token, runId, "MANIFEST.json",
            (manifest.toString() + "\n").toByteArray(Charsets.UTF_8), "application/json")
        Receipt(runName, rows.size, countShards, hash)
    }
}
