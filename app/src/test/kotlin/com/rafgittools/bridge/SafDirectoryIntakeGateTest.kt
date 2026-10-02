package com.rafgittools.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafDirectoryIntakeGateTest {
    @Test
    fun safeRelativePathPreservesNestedFolderStructure() {
        assertEquals(
            "NOVOexport/Conversation Chunks/part-001.json",
            SafDirectoryIntakeGate.safeRelativePath(
                "NOVOexport/Conversation Chunks",
                "part-001.json"
            )
        )
    }

    @Test
    fun inventoryFingerprintDoesNotDependOnProviderEnumerationOrder() {
        val files = listOf(
            SafDirectoryFingerprintFile("b.json", "application/json", 20L, "doc-b"),
            SafDirectoryFingerprintFile("a.json", "application/json", 10L, "doc-a")
        )
        val first = SafDirectoryIntakeGate.inventoryFingerprint(
            "drive.provider",
            "a".repeat(64),
            listOf("z-folder", "a-folder"),
            files
        )
        val reordered = SafDirectoryIntakeGate.inventoryFingerprint(
            "drive.provider",
            "a".repeat(64),
            listOf("a-folder", "z-folder"),
            files.reversed()
        )
        assertEquals(first, reordered)
        assertTrue(first.matches(Regex("^[a-f0-9]{64}$")))
    }

    @Test
    fun inventoryFingerprintChangesWhenIndexedPathChanges() {
        val first = SafDirectoryIntakeGate.inventoryFingerprint(
            "drive.provider",
            "b".repeat(64),
            emptyList(),
            listOf(SafDirectoryFingerprintFile("a.json", "application/json", 10L, "doc-a"))
        )
        val changed = SafDirectoryIntakeGate.inventoryFingerprint(
            "drive.provider",
            "b".repeat(64),
            emptyList(),
            listOf(SafDirectoryFingerprintFile("b.json", "application/json", 10L, "doc-a"))
        )
        assertNotEquals(first, changed)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsParentTraversalSegment() {
        SafDirectoryIntakeGate.safePathSegment("..")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsPathSeparatorInProviderDisplayName() {
        SafDirectoryIntakeGate.safePathSegment("../private.txt")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsControlCharactersInProviderDisplayName() {
        SafDirectoryIntakeGate.safePathSegment("line\nbreak.txt")
    }
}
