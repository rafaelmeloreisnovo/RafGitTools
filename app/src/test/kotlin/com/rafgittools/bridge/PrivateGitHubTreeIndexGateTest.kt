package com.rafgittools.bridge

import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class PrivateGitHubTreeIndexGateTest {
    private val repo = "fixture/private-repo"
    private val commit = "a".repeat(40)
    private val tree = "b".repeat(40)
    private val blob = "c".repeat(40)
    private fun json(path: String = "data/chunks/chunk_01.json", size: Long = 15L, extra: String = "") =
        JsonParser.parseString(
            """{"sha":"$tree","truncated":false,"tree":[
            {"path":"$path","type":"blob","sha":"$blob","size":$size}$extra
            ]}"""
        ).asJsonObject

    @Test fun indexIsMetadataOnly() {
        val out = PrivateGitHubTreeIndexGate.buildIndex(repo,commit,tree,json())
        assertEquals(1,out.get("file_count").asInt)
        assertFalse(out.get("raw_conversations_copied").asBoolean)
        assertFalse(out.get("claim_allowed").asBoolean)
        assertFalse(out.get("content_sha256_verified").asBoolean)
        assertFalse(out.toString().contains("message_body"))
    }
    @Test fun rejectsInvalidRepositoryTarget() {
        assertThrows(IllegalArgumentException::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex("../escape",commit,tree,json())
        }
    }
    @Test fun rejectsTruncated() {
        val input=json()
        input.addProperty("truncated",true)
        assertThrows(IllegalArgumentException::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex(repo,commit,tree,input)
        }
    }
    @Test fun rejectsWrongTree() {
        assertThrows(IllegalArgumentException::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex(repo,commit,"d".repeat(40),json())
        }
    }
    @Test fun rejectsTraversal() {
        assertThrows(IllegalArgumentException::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex(repo,commit,tree,json("../secret"))
        }
    }
    @Test fun rejectsDuplicate() {
        val same = "," + """{"path":"data/chunks/chunk_01.json","type":"blob","sha":"$blob","size":3}"""
        assertThrows(IllegalArgumentException::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex(repo,commit,tree,json(extra=same))
        }
    }
    @Test fun flagsOversizeWithoutReading() {
        val out=PrivateGitHubTreeIndexGate.buildIndex(
            repo,commit,tree,json(size=94371840L)
        )
        assertEquals(1,out.get("oversize_not_scanned").asInt)
        assertEquals(94371840L,out.get("byte_count").asLong)
    }
    @Test fun rejectsMissingSha() {
        val input=json()
        input.getAsJsonArray("tree").get(0).asJsonObject.remove("sha")
        assertThrows(Exception::class.java) {
            PrivateGitHubTreeIndexGate.buildIndex(repo,commit,tree,input)
        }
    }
}
