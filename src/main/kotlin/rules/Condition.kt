package rules

import extraction.*
import generation.FeatureType
import org.intellij.lang.annotations.RegExp
import java.lang.StringBuilder

data class Condition(
    val offset: Int,
    val condition: TokenCondition
) {
    override fun toString(): String {
        val builder = StringBuilder()

        builder.append(if (offset == 0) "[" else "(")
        builder.append(condition.toString())
        builder.append(if (offset == 0) "]" else ")")

        return builder.toString()
    }
}

sealed class TokenCondition(val type: Type) {
    enum class Type {
        ANY, OR, AND, NOT,
        TEXT, LEMMA, POS, CHUNK_TAG, CHUNK_POS, SENT_POS, RELATION
    }

    abstract fun check(token: Token): Boolean
}

val ANY_CONDITION = AnyCondition()

class AnyCondition : TokenCondition(Type.ANY) {
    override fun check(token: Token) = true
    override fun toString() = "<any>"
}

class ORCondition(val conditions: List<TokenCondition>) : TokenCondition(Type.OR) {
    override fun check(token: Token) = conditions.any { it.check(token) }
    override fun toString() = conditions.joinToString(" | ")
}

class ANDCondition(val conditions: List<TokenCondition>) : TokenCondition(Type.AND) {
    override fun check(token: Token) = conditions.all { it.check(token) }
    override fun toString() = conditions.joinToString(" & ")
}

class NOTCondition(val condition: TokenCondition) : TokenCondition(Type.NOT) {
    override fun check(token: Token) = !condition.check(token)
    override fun toString() = "!($condition)"
}

class TextCondition(@RegExp val regexString: String) : TokenCondition(Type.TEXT) {
    private val regex = regexString.toRegex()
    override fun check(token: Token) = regex.matches(token.text)
    override fun toString() = "Text: $regex"
}

class LemmaCondition(@RegExp val regexString: String) : TokenCondition(Type.LEMMA) {
    private val regex = regexString.toRegex()
    override fun check(token: Token) = regex.matches(token.lemma ?: "")
    override fun toString() = "Lemma: ${regex.pattern.removeSuffix("\\E").removePrefix("\\Q")}"
}

class POSCondition(val tags: Set<PennPOSTag>) : TokenCondition(Type.POS) {
    override fun check(token: Token) = token.pos in tags
    override fun toString() = "POS: ${tags.joinToString(" | ")}"
}

class ChunkTagCondition(val chunks: Set<PennChunkTag>) : TokenCondition(Type.CHUNK_TAG) {
    override fun check(token: Token) = token.chunkTag in chunks
    override fun toString() = "Chunk: ${chunks.joinToString(" | ")}"
}

class ChunkPosCondition(val chunkPositions: Set<PennChunkPos>) : TokenCondition(Type.CHUNK_POS) {
    override fun check(token: Token) = token.chunkPos in chunkPositions
    override fun toString() = "ChunkP: ${chunkPositions.joinToString(" | ") {
        when (it) {
            PennChunkPos.BEGIN -> "B"
            PennChunkPos.MIDDLE -> "I"
            PennChunkPos.END -> "E"
        }
    }}"
}

class PositionInSentenceCondition(val positions: Set<SentPosition>) : TokenCondition(Type.SENT_POS) {
    override fun check(token: Token) = token.sentPos in positions
    override fun toString() = "SentP: ${positions.joinToString(" | ") {
        when (it) {
            SentPosition.SENT_START -> "START"
            SentPosition.SENT_MIDDLE -> "MID"
            SentPosition.SENT_END -> "END"
        }
    }}"
}

class RelationCondition(val relations: Set<String>) : TokenCondition(Type.RELATION) {
    override fun check(token: Token) = token.relation in relations
    override fun toString() = "Relation: ${relations.joinToString(" | ")}"
}

fun Set<FeatureType>.toCondition(token: Token): TokenCondition {
    fun FeatureType.toCondition() = when (this) {
        FeatureType.LEMMA -> LemmaCondition(Regex.escape(token.lemma!!))
        FeatureType.POS -> POSCondition(setOf(token.pos!!))
        FeatureType.CHUNK_TAG -> ChunkTagCondition(setOf(token.chunkTag!!))
        FeatureType.CHUNK_POS -> ChunkPosCondition(setOf(token.chunkPos!!))
        FeatureType.SENT_POS -> PositionInSentenceCondition(setOf(token.sentPos!!))
        FeatureType.RELATION -> RelationCondition(setOf(token.relation!!))
    }

    if (isEmpty()) return ANY_CONDITION
    if (this.size == 1) return first().toCondition()
    return ANDCondition(map { it.toCondition() })
}
