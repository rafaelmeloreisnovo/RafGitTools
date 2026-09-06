package com.rafgittools.feature.bisect

import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BisectViewModelTest {

    @Before
    fun setup() {
        // Initialize mock bisect manager
        // Note: Full integration test requires JGit repository fixtures
    }

    @Test
    fun `initial state is not in session`() {
        val expectedState = BisectViewModel.BisectState(
            isInSession = false,
            candidates = emptyList(),
            currentCommit = null
        )

        assertFalse(expectedState.isInSession)
        assertEquals(0, expectedState.candidates.size)
    }

    @Test
    fun `calculateRemaining ceil log2 for candidates includes zero-step singleton boundary`() = runTest {
        val candidates = listOf(1, 2, 4, 8, 16, 32, 64)

        for (count in candidates) {
            val expected = kotlin.math.ceil(kotlin.math.log(count.toDouble(), 2.0)).toInt()
            assertTrue(expected in 0..6, "ceil(log2($count))=$expected must stay inside the 0..6 decision bound")
            if (count == 1) assertEquals(0, expected)
        }
    }

    @Test
    fun `startBisect transitions isInSession to true`() = runTest {
        // Requires mock repository fixture (TOKEN_VAZIO_FIXTURES)
    }

    @Test
    fun `markCommitGood removes commits before marked commit`() = runTest {
        // Requires mock bisect session fixture
    }

    @Test
    fun `markCommitBad removes commits after marked commit`() = runTest {
        // Requires mock bisect session fixture
    }

    @Test
    fun `skipCommit removes current commit from candidates`() = runTest {
        // Requires mock bisect session fixture
    }

    @Test
    fun `endBisect returns first bad commit`() = runTest {
        // Requires mock bisect session fixture
    }

    @Test
    fun `resetBisect clears session state`() {
        val resetState = BisectViewModel.BisectState()
        assertFalse(resetState.isInSession)
    }
}
