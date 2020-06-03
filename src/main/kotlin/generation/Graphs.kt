package generation

import com.google.common.collect.Iterables.retainAll
import com.google.common.collect.Sets
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import gnu.trove.THashMap
import gnu.trove.THashSet
import extraction.*
import rules.MOSHI
import java.io.File
import java.lang.Integer.min
import java.lang.Math.max
import java.util.*

object Graphs {
    val PURE by lazy {
        Moshi.Builder().build().adapter(ProbabilisticNode::class.java).fromJson(File("pure_graph3.json").readText())!!
    }
}

data class Score(val count: Int, val penalty: Float) {
    companion object {
        const val NO_PENALTY = 1.0f
    }
}
data class Index(val block: Int, val offset: Int)

@JsonClass(generateAdapter = true)
open class ProbabilisticNode(val nextByPOS: MutableMap<PennPOSTag, ProbabilisticNode> = EnumMap(PennPOSTag::class.java),
                             val nextBySent: MutableMap<SentPosition, ProbabilisticNode> = EnumMap(SentPosition::class.java),
                             val nextByChunk: MutableMap<PennChunkTag, ProbabilisticNode> = EnumMap(PennChunkTag::class.java),
                             val nextByChunkPos: MutableMap<PennChunkPos, ProbabilisticNode> = EnumMap(PennChunkPos::class.java),
                             val nextByRelation: MutableMap<String, ProbabilisticNode> = THashMap(),

                             val prevPOS: MutableSet<PennPOSTag> = EnumSet.noneOf(PennPOSTag::class.java),
                             val prevSent: MutableSet<SentPosition> = EnumSet.noneOf(SentPosition::class.java),
                             val prevChunk: MutableSet<PennChunkTag> = EnumSet.noneOf(PennChunkTag::class.java),
                             val prevChunkPos: MutableSet<PennChunkPos> = EnumSet.noneOf(PennChunkPos::class.java),
                             val prevRelation: MutableSet<String> = THashSet(),

                             var counter: Int = 0) {

    val count: Int by lazy {
        var nodes = listOf(this)
        while (true) {
            val next = nodes.flatMap { node ->
                node.nextBySent.values
            }

            if (next.isEmpty()) break
            nodes = next
        }

        nodes.sumBy { it.counter }
    }

    open fun calculateScoreForFeatures(tokens: List<Token>, features: List<EnumSet<FeatureType>>): Score {
        var penalty = 1.0f
        var sequence = sequence { yield(this@ProbabilisticNode) }
        for ((token, features) in tokens.zip(features)) {
            if (FeatureType.LEMMA in features) {
                penalty *= EnglishFrequency.getNormalizedFrequency(token.lemma!!)
            }
            sequence = sequence.flatMap { it.process(token, features) }
        }

        return Score(sequence.sumBy { it.counter }, penalty)
    }

    open fun process(token: Token, features: EnumSet<FeatureType>) = sequence<ProbabilisticNode> {
        if (features.isEmpty()) {
            yieldAll(nextBySent.values)
        } else {
            if (features.size == 1 && features.first() == FeatureType.LEMMA) {
                yieldAll(nextBySent.values)
            } else {
                val next = when (features.first()) {
                    FeatureType.RELATION -> nextByRelation[token.relation]
                    FeatureType.POS -> nextByPOS[token.pos]
                    FeatureType.CHUNK_TAG -> nextByChunk[token.chunkTag]
                    FeatureType.CHUNK_POS -> nextByChunkPos[token.chunkPos]
                    FeatureType.SENT_POS -> nextBySent[token.sentPos]
                    else -> null
                }

                if (next != null) {
                    val shouldYield = features.size == 1 || features.all { type ->
                        when (type) {
                            FeatureType.RELATION -> token.relation in next.prevRelation
                            FeatureType.POS -> token.pos in next.prevPOS
                            FeatureType.CHUNK_TAG -> token.chunkTag in next.prevChunk
                            FeatureType.CHUNK_POS -> token.chunkPos in next.prevChunkPos
                            FeatureType.SENT_POS -> token.sentPos in next.prevSent
                            else -> true
                        }
                    }

                    if (shouldYield) yield(next)
                }
            }
        }
    }
}

class Graph(val n: Int = 3) {
    private data class Node(val nextByPOS: MutableMap<PennPOSTag, MutableSet<Index>> = EnumMap(PennPOSTag::class.java),
                            val nextBySent: MutableMap<SentPosition, MutableSet<Index>> = EnumMap(SentPosition::class.java),
                            val nextByChunk: MutableMap<PennChunkTag, MutableSet<Index>> = EnumMap(PennChunkTag::class.java),
                            val nextByChunkPos: MutableMap<PennChunkPos, MutableSet<Index>> = EnumMap(PennChunkPos::class.java),
                            val nextByRelation: MutableMap<String, MutableSet<Index>> = THashMap(),
                            val nextByLemma: MutableMap<String, MutableSet<Index>> = THashMap()) {
        fun clear() {
            nextByPOS.clear()
            nextBySent.clear()
            nextByChunk.clear()
            nextByChunkPos.clear()
            nextByRelation.clear()
            nextByLemma.clear()
        }

        fun findCellsForFeatures(token: Token, features: EnumSet<FeatureType>): List<Set<Index>> {
            return features.map {
                when (it) {
                    FeatureType.RELATION -> nextByRelation[token.relation]
                    FeatureType.POS -> nextByPOS[token.pos]
                    FeatureType.CHUNK_TAG -> nextByChunk[token.chunkTag]
                    FeatureType.CHUNK_POS -> nextByChunkPos[token.chunkPos]
                    FeatureType.SENT_POS -> nextBySent[token.sentPos]
                    FeatureType.LEMMA -> nextByLemma[token.lemma]
                    else -> null
                } ?: return listOf(emptySet())
            }
        }
    }

    private val graph = Array(n) { Node() }
    private var _count = 0

    val count: Int
        get() = _count

    private fun loadNGram(index: Index, tokens: List<IndexedValue<Token>>) {
        for ((ngram, token) in tokens.map { it.value }.withIndex()) {
            graph[ngram].nextBySent.getOrPut(token.sentPos!!) { Sets.newIdentityHashSet() }.add(index)
            graph[ngram].nextByChunk.getOrPut(token.chunkTag!!) { Sets.newIdentityHashSet() }.add(index)
            graph[ngram].nextByChunkPos.getOrPut(token.chunkPos!!) { Sets.newIdentityHashSet() }.add(index)
            graph[ngram].nextByPOS.getOrPut(token.pos!!) { Sets.newIdentityHashSet() }.add(index)
            graph[ngram].nextByRelation.getOrPut(token.relation!!.intern()) { Sets.newIdentityHashSet() }.add(index)
            graph[ngram].nextByLemma.getOrPut(token.lemma!!.intern()) { Sets.newIdentityHashSet() }.add(index)
        }
    }

    fun clear() {
        graph.forEach { it.clear() }
        _count = 0
    }

    fun loadSentences(sentences: List<List<Token>>) {
        clear()
        for ((sentenceIndex, sentence) in sentences.withIndex()) {
            sentence.withIndex().windowed(n) { tokens ->
                _count += 1
                loadNGram(Index(sentenceIndex, tokens.first().index), tokens)
            }
        }
    }

    fun load(path: String = "train.json") {
        load(loadDataset(path))
    }

    fun load(blocks: List<DatasetBlock>) {
        clear()
        for ((blockIndex, block) in blocks.withIndex()) {
            block.sentences.withIndex().windowed(n) { tokens ->
                _count += 1
                loadNGram(Index(blockIndex, tokens.first().index), tokens)
            }
        }
    }

    fun collectIndices(tokens: List<Token>, features: List<EnumSet<FeatureType>>): Sequence<Index> {
        val sets = graph.mapIndexed { index, node ->
            node.findCellsForFeatures(tokens[index], features[index])
        }.flatten().toMutableList()

        if (sets.isEmpty()) return emptySequence()

        sets.sortBy { it.size }
        sets[0] = Sets.newIdentityHashSet<Index>().apply { addAll(sets[0]) }
        return sets.reduce { acc, set -> acc.apply { retainAll(this, set) } }.asSequence()
    }

    fun calculateScoreForFeatures(tokens: List<Token>, features: List<EnumSet<FeatureType>>): Score {
        return Score(
            collectIndices(tokens, features).count(),
            Score.NO_PENALTY
        )
    }
}

private fun generateGraph(source: String = "/pure_text.json", output: String? = "pure_graph3.json", depth: Int = 3): ProbabilisticNode {
    val sentenceType = Types.newParameterizedType(MutableList::class.java, Token::class.java)
    val textType = Types.newParameterizedType(MutableList::class.java, sentenceType)
    val adapter = MOSHI.adapter<List<List<Token>>>(textType)
    val sentences = adapter.fromJson(Graphs::class.java.getResource(source).readText()) ?: emptyList()
    return generateGraphFromSentences(sentences, output, depth)
}

private fun generateGraphFromSentences(sentences: List<List<Token>>, output: String? = "pure_graph3.json", depth: Int = 3): ProbabilisticNode {
    println("Start, sentences: ${sentences.size}")
    val root = ProbabilisticNode()
    for ((index, sentence) in sentences.withIndex()) {
        print("Sentence $index\r")
        sentence.windowed(depth) { tokens ->
            var nodes = listOf(root)
            for (token in tokens) {
                nodes = nodes.flatMap { node ->
                    listOf(
                        node.nextByChunk.getOrPut(token.chunkTag!!) { ProbabilisticNode() },
                        node.nextByChunkPos.getOrPut(token.chunkPos!!) { ProbabilisticNode() },
                        node.nextByPOS.getOrPut(token.pos!!) { ProbabilisticNode() },
                        node.nextBySent.getOrPut(token.sentPos!!) { ProbabilisticNode() },
                        node.nextByRelation.getOrPut(token.relation!!.intern()) { ProbabilisticNode() }
                    )
                }

                for (node in nodes) {
                    node.prevChunk.add(token.chunkTag!!)
                    node.prevChunkPos.add(token.chunkPos!!)
                    node.prevPOS.add(token.pos!!)
                    node.prevSent.add(token.sentPos!!)
                    node.prevRelation.add(token.relation!!.intern())
                    node.counter += 1
                }
            }
        }
    }

    if (output != null) {
        with (File(output)) {
            createNewFile()
            writeBytes(MOSHI.adapter(ProbabilisticNode::class.java).toJson(root).toByteArray())
        }
    }

    println("Graph built")

    return root
}

fun generateDirtyGraph(blocks: List<DatasetBlock>) : Graph {
    val sentences = ArrayList<List<Token>>()
    for (block in blocks) {
        for (error in block.errors) {
            val errs = block.sentences.subList(max(error.from - 1, 0), min(error.from + 2, block.sentences.size))
            sentences.add(errs)
        }

    }

    return Graph(3).apply {
        loadSentences(sentences)
    }
}

fun main() {

}