package com.rafgittools.lens

import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Structural metadata-only vectorization. This is NOT semantic embedding.
 * Hosted Android/JVM SHA-256 is an adapter; no freestanding/runtime claim.
 */
enum class LensSource {
    DRIVE_PRIVATE, DRIVE_DASHBOARD, GITHUB_PRIVATE, GITHUB_PUBLIC,
    MAPA_PUBLIC, PAPER_RESEARCH, ETHICS_RESEARCH
}

enum class LensGap {
    TOKEN_VAZIO, MISSING_AUTHORITY, MISSING_EVIDENCE, UNVERIFIED, EVIDENCED_SCOPED
}

data class LensNode(
    val logicalId: String,
    val source: LensSource,
    val tokens: List<String>,
    val gap: LensGap
)

data class LensVector(
    val counts: List<Int>,
    val digestSha256: String,
    val nonzeroBuckets: Int
)

data class LensEdge(
    val from: String,
    val to: String,
    val commonTokens: Int,
    val unionTokens: Int,
    val relation: String = "LEXICAL_CANDIDATE_NOT_CAUSAL"
)

object FederatedLensCore {
    const val MAX_NODES = 64
    const val MAX_TOKENS = 16
    const val DIMENSIONS = 64
    const val MAX_PAIRS = 64
    const val CLAIM_ALLOWED = false

    private val ID = Regex("[A-Z][A-Z0-9_]{2,63}")
    private val TOKEN = Regex("[a-z0-9_\\-]{1,40}")

    fun validate(node: LensNode) {
        require(ID.matches(node.logicalId)) { "logical_id inválido (somente nome lógico público)" }
        require(node.tokens.isNotEmpty() && node.tokens.size <= MAX_TOKENS) {
            "tokens: 1..16; nunca incluir texto bruto, IDs privados ou segredos"
        }
        require(node.tokens.distinct().size == node.tokens.size) { "tokens repetidos" }
        require(node.tokens.all(TOKEN::matches)) { "token inválido" }
    }

    private fun hash(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))

    private fun hex(bytes: ByteArray): String {
        val alphabet = "0123456789abcdef"
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val x = b.toInt() and 255
            out.append(alphabet[x ushr 4])
            out.append(alphabet[x and 15])
        }
        return out.toString()
    }

    fun vectorize(node: LensNode): LensVector {
        validate(node)
        val bins = IntArray(DIMENSIONS)
        val sorted = node.tokens.sorted()
        for (token in sorted) {
            val digest = hash("rafgittools.lens.token.v1\n" + token)
            val bucket = (digest[0].toInt() and 255) % DIMENSIONS
            bins[bucket] += 1
        }
        val signature = "rafgittools.lens.node.v1\n" +
            node.logicalId + "\n" + node.source.name + "\n" +
            node.gap.name + "\n" + sorted.joinToString("\n") + "\n"
        return LensVector(bins.toList(), hex(hash(signature)), bins.count { it > 0 })
    }

    /**
     * Bounded pairwise candidate search. No correlation-to-causation promotion.
     * Private-public candidate edges remain local and must NOT be published.
     */
    fun candidates(nodes: List<LensNode>, budget: Int = MAX_PAIRS): List<LensEdge> {
        require(nodes.size <= MAX_NODES) { "excesso de nós" }
        require(nodes.map { it.logicalId }.distinct().size == nodes.size) { "ID duplicado" }
        nodes.forEach(::validate)
        val result = ArrayList<LensEdge>()
        val safeBudget = budget.coerceIn(0, MAX_PAIRS)
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                if (result.size >= safeBudget) return result
                val left = nodes[i].tokens.toSet()
                val right = nodes[j].tokens.toSet()
                val intersection = left.intersect(right).size
                if (intersection == 0) continue
                val union = left.union(right).size
                result.add(LensEdge(nodes[i].logicalId, nodes[j].logicalId, intersection, union))
            }
        }
        return result
    }

    /**
     * Bounded undirected reachability over lexical candidates. This is a
     * structural what-if projection, NOT a causal avalanche prediction.
     */
    fun lexicalReach(nodes: List<LensNode>, startId: String, maxDepth: Int = 3): List<String> {
        require(maxDepth in 0..3) { "depth outside 0..3" }
        require(nodes.any { it.logicalId == startId }) { "unknown start ID" }
        val edges = candidates(nodes)
        val seen = LinkedHashSet<String>()
        seen.add(startId)
        var frontier = listOf(startId)
        repeat(maxDepth) {
            val next = ArrayList<String>()
            for (edge in edges) {
                for (from in frontier) {
                    val neighbor = when (from) {
                        edge.from -> edge.to
                        edge.to -> edge.from
                        else -> null
                    }
                    if (neighbor != null && seen.add(neighbor)) next.add(neighbor)
                }
            }
            frontier = next
        }
        return seen.toList()
    }

    fun urgency(node: LensNode): String = when (node.gap) {
        LensGap.MISSING_AUTHORITY -> "P0 / HOLD"
        LensGap.MISSING_EVIDENCE -> "P1 / HOLD"
        LensGap.TOKEN_VAZIO -> "P1 / UNKNOWN"
        LensGap.UNVERIFIED -> "P2 / REVIEW"
        LensGap.EVIDENCED_SCOPED -> "P3 / SCOPED"
    }

    fun operationGate(node: LensNode): String = when (node.gap) {
        LensGap.EVIDENCED_SCOPED -> "METADATA_PREVIEW_ONLY"
        else -> "ROUTE_STATE_BLOCKED"
    }
}
