package rules

import com.squareup.moshi.JsonClass
import extraction.DatasetBlock
import extraction.Token
import generation.F_05
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.min

@JsonClass(generateAdapter = true)
data class Rule(val detector: Detector, val redactor: Redactor) {
    @Transient
    private val base = min(0, redactor.minOffset)

    data class Detector(val conditions: List<Condition>) {
        fun match(tokens: List<Token>, anchor: Int): Boolean {
            return conditions.all { condition ->
                if (anchor + condition.offset !in tokens.indices) false
                else condition.condition.check(tokens[anchor + condition.offset])
            }
        }
    }

    data class Redactor(val modifications: List<Modifications>) {
        val minOffset: Int = modifications.minBy { it.offset }?.offset ?: 0
        val maxOffset: Int = modifications.maxBy { it.offset }?.offset ?: 0
    }

    override fun toString(): String {
        val builder = StringBuilder()

        var prev = 100
        builder.append(detector.conditions.sortedBy { it.offset }.flatMap {
            sequence {
                while (it.offset > prev + 1) {
                    prev += 1
                    if (prev == 0) yield("[<any>]")
                    else yield("<any>")
                }

                prev = it.offset
                yield(it.toString())
            }.toList()
        }.joinToString(" "))
        builder.append("\n")
        builder.append(redactor.modifications.joinToString("\n"))

        return builder.toString()
    }

    fun check(sentence: List<Token>): SortedSet<Int> {
        return sentence.indices.filter { index -> detector.match(sentence, index) }.toSortedSet()
    }

    fun calcStatisticsForBlock(block: DatasetBlock, blockIndex: Int): Pair<Set<RuleWithErrors.Match>, Set<RuleWithErrors.Match>> {
        val TP = HashSet<RuleWithErrors.Match>()
        val FP = HashSet<RuleWithErrors.Match>()

        for (offset in check(block.sentences)) {
            val indexOfError = block.invertedErrors[offset]
            if (indexOfError == null) {
                val begin = offset + redactor.minOffset
                val end = offset + redactor.maxOffset

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
                    FP.add(RuleWithErrors.Match(blockIndex, offset, indexOfError))
                }
            } else {
                val foundError = block.errors[indexOfError]
                val begin = offset + redactor.minOffset
                val end = Integer.max(offset + redactor.maxOffset, foundError.to)

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
                    TP.add(RuleWithErrors.Match(blockIndex, offset, indexOfError))
                } else {
                    FP.add(RuleWithErrors.Match(blockIndex, offset, indexOfError))
                }
            }
        }

        return TP to FP
    }

    fun correct(sentence: List<Token>): Sequence<Token> {
        data class PlannedModification(val index: Int, val modification: Modifications) : Comparable<PlannedModification> {
            override fun compareTo(other: PlannedModification) = index.compareTo(other.index)
        }

        return sequence<Token> {
            var last: Token? = null
            val queue = PriorityQueue<PlannedModification>()
            sentence.withIndex().forEach { (index, token) ->
                if (detector.match(sentence, index - base)) {
                    redactor.modifications.forEach { modification ->
                        queue.add(PlannedModification(index - base + modification.offset, modification))
                    }
                }

                @Suppress("NAME_SHADOWING") var token: Token? = token
                while (queue.isNotEmpty() && queue.first().index == index) {
                    val planned = queue.poll()
                    if (token == null) continue

                    for (insertion in planned.modification.insertions) {
                        val new = insertion.getTokenForInsertion(token)
                        if (new != null) {
                            if (new.text != last?.text && new.text != token.text) {
                                last = new
                                yield(new)
                            }
                        }
                    }

                    for (replacement in planned.modification.replacements) {
                        token = replacement.getTokenForReplacement(token!!)
                    }

                    for (deletion in planned.modification.deletions) {
                        if (deletion.isDeleteNeeded(token!!)) {
                            token = null
                        }
                    }
                }

                if (token != null) {
                    last = token
                    yield(token)
                }
            }
        }
    }
}

@JsonClass(generateAdapter = true)
data class RuleWithErrors(val rule: Rule, val info: Info, val origin: Match,
                          val TP: MutableSet<Match> = HashSet(),
                          val FP: MutableSet<Match> = HashSet(),
                          val fn: Int) : Comparable<RuleWithErrors> {
    data class Info(val pure: Float, val dirty: Float)
    data class Match(val block: Int, val position: Int, val error: Int? = null)

    val tp: Int
        get() = TP.size

    val fp: Int
        get() = FP.size

    val matches: Int
        get() = tp + fp

    fun calcMetric(): Float = F_05(tp, fn, fp)

    override fun compareTo(other: RuleWithErrors): Int {
        val first = other.calcMetric().compareTo(calcMetric())
        return if (first == 0) {
            val second = other.tp.compareTo(tp)
            if (second == 0) {
                info.pure.compareTo(other.info.pure)
            } else second
        } else first
    }

    override fun toString(): String {
        return buildString {
            append("TP: $tp, FP: $fp, fix errs: ${FP.count { it.error != null }}\n")
            append(rule.toString())
        }
    }
}

data class RuleWithDynamicErrors(val base: RuleWithErrors,
                                 val overallTP: Set<RuleWithErrors.Match>,
                                 val overallFP: Set<RuleWithErrors.Match>) : Comparable<RuleWithDynamicErrors> {
    val tp: Int
        get() = base.TP.count { it !in overallTP }

    val fp: Int
        get() = base.FP.count { it !in overallFP }

    val matches: Int
        get() = tp + fp

    fun calcMetric(): Float = F_05(tp, base.fn, fp)

    override fun compareTo(other: RuleWithDynamicErrors): Int {
        val first = other.calcMetric().compareTo(calcMetric())
        return if (first == 0) {
            val second = other.tp.compareTo(tp)
            if (second == 0) {
                base.info.pure.compareTo(other.base.info.pure)
            } else second
        } else first
    }
}