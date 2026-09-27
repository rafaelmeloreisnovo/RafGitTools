package com.rafgittools.bridge

import java.nio.file.Files
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CorpusIntakeGateTest {

    @Test
    fun catalogsJsonStructureWithoutRetainingPrivateTextInPublicProjection() {
        val dir = Files.createTempDirectory("rafgit-corpus-intake").toFile()
        try {
            val source = dir.resolve("private-conversations.json")
            source.writeText(
                """{"privateField":[1,true,null,"secret text"]}""",
                Charsets.UTF_8
            )
            val digest = sha256(source.readBytes())

            val result = CorpusIntakeGate.catalog(
                stagedFile = source,
                sourceProviderAuthority = "com.example.provider",
                sourceDisplayName = source.name,
                expectedSha256 = digest,
                createdAtEpochMs = 1234L
            )

            assertEquals("JSON", result.contentKind)
            val vector = assertNotNull(result.structuralVector).let { result.structuralVector!! }
            assertEquals(1L, vector.objects)
            assertEquals(1L, vector.arrays)
            assertEquals(1L, vector.names)
            assertEquals(1L, vector.strings)
            assertEquals(1L, vector.numbers)
            assertEquals(1L, vector.booleans)
            assertEquals(1L, vector.nulls)
            assertEquals(2, vector.maxDepth)

            val publicJson = result.publicProjectionFile.readText(Charsets.UTF_8)
            assertFalse(publicJson.contains(source.name))
            assertFalse(publicJson.contains(digest))
            assertFalse(publicJson.contains("privateField"))
            assertFalse(publicJson.contains("secret text"))
            assertTrue(publicJson.contains(result.publicRiskHandle))

            val privateManifest = result.manifestFile.readText(Charsets.UTF_8)
            assertTrue(privateManifest.contains(source.name))
            assertTrue(privateManifest.contains(digest))
            assertTrue(privateManifest.contains("rmr-zipraf-evidence-envelope-v1"))
            assertTrue(privateManifest.contains("TOKEN_VAZIO_NOT_SEALED"))
        } finally {
            dir.deleteRecursively()
        }
    }

    @Test(expected = java.io.IOException::class)
    fun rejectsChangedDigest() {
        val dir = Files.createTempDirectory("rafgit-corpus-intake-digest").toFile()
        try {
            val source = dir.resolve("fixture.json")
            source.writeText("""{"ok":true}""", Charsets.UTF_8)
            CorpusIntakeGate.catalog(
                stagedFile = source,
                sourceProviderAuthority = "provider",
                sourceDisplayName = source.name,
                expectedSha256 = "0".repeat(64),
                createdAtEpochMs = 1234L
            )
        } finally {
            dir.deleteRecursively()
        }
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
}
