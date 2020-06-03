@file:Suppress("UnstableApiUsage")

package generation

import com.google.common.collect.MinMaxPriorityQueue
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import extraction.DatasetBlock
import extraction.Token
import rules.*
import java.lang.IllegalArgumentException
import java.lang.Integer.max
import java.util.*
import kotlin.collections.HashSet


private val ANY_CONDITIONS = setOf(Condition(0, ANY_CONDITION))

private data class ProtoDetectorPart(val pure: Float, val dirty: Float,
                                     val features: List<EnumSet<FeatureType>>) : Comparable<ProtoDetectorPart> {
    override fun compareTo(other: ProtoDetectorPart) = (pure).compareTo(other.pure)
}

private const val OFFSET = 1

@ExperimentalCoroutinesApi
private fun generateRuleBases(sentencePartWithError: List<Token>, graph: Graph, errorsGraph: Graph, max: Int = 10000): Queue<ProtoDetectorPart> {
    return runBlocking {
        val producer = produce { FEATURE_TRIPLES.forEach { send(it) } }

        (0..Runtime.getRuntime().availableProcessors() - 2).map {
            async(Dispatchers.Default) {
                val queue = MinMaxPriorityQueue.maximumSize(max).create<ProtoDetectorPart>()
                for (features in producer) {
                    val pscore = Graphs.PURE.calculateScoreForFeatures(sentencePartWithError, features)
                    val pure = (pscore.count * pscore.penalty) / Graphs.PURE.count.toFloat()
                    if (pure < 0.01) {
                        val dscore = errorsGraph.calculateScoreForFeatures(sentencePartWithError, features)
                        val dirty = (dscore.count * dscore.penalty) / (errorsGraph.count).toFloat()

                        if (dscore.count > 4 && dirty < 0.3 && dirty - pure > 0f) {
                            queue.add(ProtoDetectorPart(pure, dirty, features))
                        }
                    }
                }

                queue
            }
        }.map { it.await() }.reduce { acc, queue ->
            acc.addAll(queue)
            acc
        }
    }
}

@ExperimentalCoroutinesApi
private fun calculateRuleBaseStatistics(blocks: List<DatasetBlock>, datasetGraph: Graph, sentencePartWithError: List<Token>,
                                        redactor: Rule.Redactor, base: ProtoDetectorPart
): Pair<HashSet<RuleWithErrors.Match>, HashSet<RuleWithErrors.Match>> {
    return runBlocking {
        val producer = produce(Dispatchers.Default) {
            datasetGraph.collectIndices(sentencePartWithError, base.features).forEach { send(it) }
        }

        (0..Runtime.getRuntime().availableProcessors() - 2).map {
            async(Dispatchers.Default) {
                val TP = HashSet<RuleWithErrors.Match>()
                val FP = HashSet<RuleWithErrors.Match>()

                for (index in producer) {
                    val block = blocks[index.block]
                    val indexOfError = block.invertedErrors[index.offset + OFFSET]
                    if (indexOfError == null) {
                        val begin = index.offset + OFFSET + redactor.minOffset
                        val end = index.offset + OFFSET + redactor.maxOffset

                        if (begin !in block.sentences.indices || end !in block.sentences.indices) {
                            System.err.println("can't fix error: sentence too small")
                            continue
                        }

                        val buffer = block.sentences.subList(begin, end + 1).withIndex()
                        val part = sequence<IndexedValue<Token>?> {
                            yieldAll(buffer)
                            yield(null)
                        }

                        var last: Token? = null
                        val fixed = part.flatMap<IndexedValue<Token>?, String?> { tokenWithIndex ->
                            sequence<String?> {
                                if (tokenWithIndex == null) yield(null)
                                else {
                                    var (ind, token) = tokenWithIndex
                                    var modification: Modifications? = null
                                    for (mod in redactor.modifications) {
                                        if (mod.offset == ind + redactor.minOffset) {
                                            modification = mod
                                            break
                                        }
                                    }

                                    if (modification == null) {
                                        last = token
                                        yield(token.text)
                                    } else {
                                        for (insertion in modification.insertions) {
                                            val new = insertion.getTokenForInsertion(token)
                                            if (new != null) {
                                                if (new.text != last?.text && new.text != token.text) {
                                                    last = new
                                                    yield(new.text)
                                                }
                                            }
                                        }

                                        for (replacement in modification.replacements) {
                                            token = replacement.getTokenForReplacement(token)
                                        }

                                        var isDeleted = false
                                        for (deletion in modification.deletions) {
                                            if (deletion.isDeleteNeeded(token)) {
                                                isDeleted = true
                                                break
                                            }
                                        }

                                        if (!isDeleted) {
                                            last = token
                                            yield(token.text)
                                        }
                                    }
                                }
                            }
                        }

                        if (fixed.zip(part.map { it?.value?.text }).any { it.first != it.second }) {
                            FP.add(RuleWithErrors.Match(index.block, index.offset + OFFSET, indexOfError))
                        }
                    } else {
                        val foundError = block.errors[indexOfError]
                        val begin = index.offset + OFFSET + redactor.minOffset
                        val end = max(index.offset + OFFSET + redactor.maxOffset, foundError.to)

                        if (begin !in block.sentences.indices || end !in block.sentences.indices) {
                            System.err.println("can't fix error: sentence too small")
                            continue
                        }

                        val part = sequence<IndexedValue<Token>?> {
                            yieldAll(block.sentences.subList(begin, end + 1).withIndex())
                            yield(null)
                        }

                        var last: Token? = null
                        val fixed = part.flatMap<IndexedValue<Token>?, String?> { tokenWithIndex ->
                            sequence<String?> {
                                if (tokenWithIndex == null) yield(null)
                                else {
                                    var (ind, token) = tokenWithIndex
                                    var modification: Modifications? = null
                                    for (mod in redactor.modifications) {
                                        if (mod.offset == ind + redactor.minOffset) {
                                            modification = mod
                                            break
                                        }
                                    }

                                    if (modification == null) {
                                        last = token
                                        yield(token.text)
                                    } else {
                                        for (insertion in modification!!.insertions) {
                                            val new = insertion.getTokenForInsertion(token)
                                            if (new != null) {
                                                if (new.text != last?.text && new.text != token.text) {
                                                    last = new
                                                    yield(new.text)
                                                }
                                            }
                                        }

                                        for (replacement in modification!!.replacements) {
                                            token = replacement.getTokenForReplacement(token)
                                        }

                                        var isDeleted = false
                                        for (deletion in modification!!.deletions) {
                                            if (deletion.isDeleteNeeded(token)) {
                                                isDeleted = true
                                                break
                                            }
                                        }

                                        if (!isDeleted) {
                                            last = token
                                            yield(token.text)
                                        }
                                    }
                                }
                            }
                        }

                        val corrected = part.flatMap<IndexedValue<Token>?, String?> { tokenWithIndex ->
                            sequence<String?> {
                                if (tokenWithIndex == null) yield(null)
                                else {
                                    val (ind, token) = tokenWithIndex
                                    when {
                                        ind + redactor.minOffset == 0 -> {
                                            foundError.fixes.forEach {yield(it.text) }
                                            if (foundError.to - foundError.from == 0) {
                                                yield(token.text)
                                            } else Unit
                                        }
                                        ind + redactor.minOffset in 1 until (foundError.to - foundError.from) -> Unit
                                        else -> {
                                            yield(token.text)
                                        }
                                    }
                                }
                            }
                        }

                        if (fixed.zip(corrected).all { it.first == it.second }) {
                            TP.add(RuleWithErrors.Match(index.block, index.offset + OFFSET, indexOfError))
                        } else {
                            FP.add(RuleWithErrors.Match(index.block, index.offset + OFFSET, indexOfError))
                        }
                    }
                }

                TP to FP
            }
        }.map { it.await() }.reduce { acc, stats ->
            acc.first.addAll(stats.first)
            acc.second.addAll(stats.second)
            acc
        }
    }
}

private fun getAdditionalRuleTokenPositions(ruleTokenConditionsNum: Int): List<List<Int>> {
    return when (ruleTokenConditionsNum) {
        3 -> listOf<List<Int>>(emptyList())
        4 -> listOf(listOf<Int>(-2), listOf<Int>(2))
        5 -> listOf(listOf<Int>(-3, -2), listOf<Int>(-2, 2), listOf<Int>(2, 3))
        else -> throw IllegalArgumentException("Unsupported rule size")
    }
}

@ExperimentalCoroutinesApi
@Suppress("UnstableApiUsage")
fun generator(blocks: List<DatasetBlock>, graph: Graph, errorsGraph: Graph, errorsCount: Int, ruleTokenConditionsNum: Int = 4) = sequence<RuleWithErrors> {
    for ((blockIndex, block) in blocks.withIndex()) {
        for ((errorIndex, error) in block.errors.withIndex()) {
            print("Error #$blockIndex-$errorIndex\r")
            if (error.from - OFFSET < 0 || error.from + ruleTokenConditionsNum - OFFSET > block.sentences.size) continue

            val errorTokens = block.sentences.subList(error.from, error.to)
            val sentencePartWithError = block.sentences.subList(error.from - OFFSET, error.from + 3 - OFFSET)

            val modifications = HashMap<Int, Modifications>()

            errorTokens.forEachIndexed { index, token ->
                if (error.fixes.find { it.lemma == token.lemma } == null) {
                    modifications.getOrPut(index) { Modifications(index) }.deletions.add(Delete())
                }
            }

            error.fixes.forEachIndexed { index, token ->
                val foundIndex = errorTokens.indexOfFirst { it.lemma == token.lemma }
                if (foundIndex != -1) {
                    val found = errorTokens[foundIndex]
                    val modification = when {
                        found.text == token.text -> null
                        found.text.decapitalize() == token.text -> Casing(Casing.CasingType.DECAPITALIZE)
                        found.text.capitalize() == token.text -> Casing(Casing.CasingType.CAPITALIZE)
                        else -> Inflect(token.pos!!)
                    }

                    if (modification != null) {
                        modifications.getOrPut(index) { Modifications(index) }.replacements.add(modification)
                    }
                } else {
                    modifications.getOrPut(index) { Modifications(index) }.insertions.add(Insert(token))
                }
            }

            val redactor = Rule.Redactor(modifications.values.toList())

            val rule = runBlocking {
                val producer = produce(Dispatchers.Default) { generateRuleBases(
                    sentencePartWithError,
                    graph,
                    errorsGraph
                ).sortedBy { -it.dirty }.take(1000).forEach { send(it) } }

                (0..Runtime.getRuntime().availableProcessors() - 4).map {
                    async(Dispatchers.Default) {
                        var rule: RuleWithErrors? = null
                        var rmetric = 0f
                        for (base in producer) {
                            val (TP, FP) = calculateRuleBaseStatistics(
                                blocks,
                                graph,
                                sentencePartWithError,
                                redactor,
                                base
                            )
                            if (TP.size < 3 || FP.size - TP.size > 1000) continue

                            val baseConditions = base.features.mapIndexedNotNull { index, features ->
                                val condition = features.toCondition(sentencePartWithError[index])
                                if (condition != ANY_CONDITION) {
                                    Condition(index - OFFSET, condition)
                                } else null
                            }

                            val sequence = sequence {
                                val offsets =  getAdditionalRuleTokenPositions(ruleTokenConditionsNum)
                                for (offs in offsets) {
                                    var sequence = sequence { yield(ANY_CONDITIONS) }
                                    for (offset in offs) {
                                        val tmp = sequence
                                        sequence = sequence {
                                            tmp.forEach { previous ->
                                                if (error.from + offset in block.sentences.indices) {
                                                    for (feature in featureGenerator()) {
                                                        yield(previous + setOf(Condition(offset, feature.toCondition(block.sentences[error.from + offset]))))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    for (conditions in sequence) {
                                        yield(conditions.filter { it.condition != ANY_CONDITION })
                                    }
                                }
                            }

                            for (conditions in sequence) {
                                val newFPs = FP.filter { match ->
                                    conditions.all {
                                        val sentences = blocks[match.block].sentences

                                        match.position + it.offset in sentences.indices &&
                                                it.condition.check(sentences[match.position + it.offset])
                                    }
                                }

                                val newTPs = TP.filter { match ->
                                    conditions.all {
                                        val sentences = blocks[match.block].sentences

                                        match.position + it.offset in sentences.indices &&
                                                it.condition.check(sentences[match.position + it.offset])
                                    }
                                }

                                val prec = precision(newTPs.size, newFPs.size)
                                if (prec > 0.7 && newTPs.size >= 3) {
                                    val metric = F_05(newTPs.size, max(errorsCount - newTPs.size, 0), newFPs.size)
                                    if (rule == null || rmetric <= metric) {
                                        if (rule != null && rmetric == metric) {
                                            if (rule.TP.size > newTPs.size) continue
                                        }
                                        rmetric = metric
                                        rule = RuleWithErrors(
                                            Rule(Rule.Detector((baseConditions + conditions).toList()), redactor),
                                            RuleWithErrors.Info(base.pure, base.dirty),
                                            RuleWithErrors.Match(blockIndex, error.from, errorIndex),
                                            newTPs.toMutableSet<RuleWithErrors.Match>(),
                                            newFPs.toMutableSet<RuleWithErrors.Match>(),
                                            errorsCount - newTPs.size
                                        )
                                    }
                                }
                            }
                        }
                        rule
                    }
                }.mapNotNull { it.await() }.min()
            }
            if (rule != null) yield(rule)
        }
    }
}