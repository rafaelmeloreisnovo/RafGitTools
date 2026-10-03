package com.rafgittools.navigator

/**
 * Canonical, public-safe contract for the private NOVOexport conversation corpus.
 *
 * The public repository knows only the logical route and filename namespace.
 * Provider file IDs, raw SAF URIs and Drive ACL details remain outside this source.
 */
object NovoexportConversationCorpus {
    const val FIRST_INDEX = 0
    const val LAST_INDEX = 50
    const val EXPECTED_COUNT = 51
    const val LOGICAL_ROUTE = "NOVOexport/01_SISTEMA_CORPUS_CUSTODIA"

    private val canonicalName = Regex("^conversations-(\\d{3})\\.json$", RegexOption.IGNORE_CASE)

    data class Coverage(
        val expectedCount: Int,
        val presentIndices: List<Int>,
        val missingIndices: List<Int>,
        val duplicateIndices: List<Int>,
        val outOfRangeNames: List<String>
    ) {
        val presentCount: Int get() = presentIndices.size
        val complete: Boolean get() =
            presentCount == expectedCount &&
                missingIndices.isEmpty() &&
                duplicateIndices.isEmpty() &&
                outOfRangeNames.isEmpty()

        fun gapRefs(): List<String> {
            if (complete) return emptyList()
            val gaps = mutableListOf("CONVERSATIONS_000_050_COVERAGE_INCOMPLETE")
            if (missingIndices.isNotEmpty()) {
                gaps += "CONVERSATIONS_MISSING_" + missingIndices.joinToString("_") { "%03d".format(it) }
            }
            if (duplicateIndices.isNotEmpty()) {
                gaps += "CONVERSATIONS_DUPLICATE_" + duplicateIndices.joinToString("_") { "%03d".format(it) }
            }
            if (outOfRangeNames.isNotEmpty()) {
                gaps += "CONVERSATIONS_OUT_OF_RANGE_PRESENT"
            }
            return gaps
        }
    }

    fun expectedFileName(index: Int): String {
        require(index in FIRST_INDEX..LAST_INDEX) { "conversation corpus index outside 000..050" }
        return "conversations-%03d.json".format(index)
    }

    fun coverage(entries: List<NovoexportSafInventory.Entry>): Coverage {
        val counts = mutableMapOf<Int, Int>()
        val outOfRange = mutableListOf<String>()

        entries.forEach { entry ->
            val name = entry.name.substringAfterLast('/')
            val match = canonicalName.matchEntire(name) ?: return@forEach
            val index = match.groupValues[1].toInt()
            if (index !in FIRST_INDEX..LAST_INDEX) {
                outOfRange += name
            } else {
                counts[index] = (counts[index] ?: 0) + 1
            }
        }

        val present = counts.keys.sorted()
        val missing = (FIRST_INDEX..LAST_INDEX).filterNot(counts::containsKey)
        val duplicates = counts.filterValues { it > 1 }.keys.sorted()
        return Coverage(
            expectedCount = EXPECTED_COUNT,
            presentIndices = present,
            missingIndices = missing,
            duplicateIndices = duplicates,
            outOfRangeNames = outOfRange.sorted()
        )
    }
}
