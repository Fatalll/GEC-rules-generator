import com.squareup.moshi.Types
import extraction.NLP
import extraction.Token
import extraction.loadDataset
import generation.Graph
import generation.generateDirtyGraph
import generation.generator
import generation.precision
import rules.MOSHI
import rules.RuleWithDynamicErrors
import rules.RuleWithErrors
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

fun generate(train: String, test: String, results: String) {
    println(train)
    val rulesType = Types.newParameterizedType(MutableList::class.java, RuleWithErrors::class.java)
    val adapter = MOSHI.adapter<List<RuleWithErrors>>(rulesType).indent(" ")

    val blocks = loadDataset(train)
    val graph = Graph(3).apply {
        load(blocks)
    }

    val errorsCount = blocks.map { it.errors.size }.sum()

    val errorsGraph = generateDirtyGraph(blocks)

    val rules = ArrayList<RuleWithErrors>()

    println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
    for ((index, rule) in generator(blocks, graph, errorsGraph, errorsCount, 5).withIndex()) {
        println("$index, ${rule.tp}, ${rule.fp}")
        rules.add(rule)
    }

    val overallTP = HashSet<RuleWithErrors.Match>()
    val overallFP = HashSet<RuleWithErrors.Match>()
    val filtered = LinkedList(rules.groupBy { it.origin }.map { RuleWithDynamicErrors(it.value.min()!!, overallTP, overallFP) }.toSortedSet())

    val result = ArrayList<RuleWithErrors>()
    while (filtered.isNotEmpty()) {
        val rule = filtered.pop()

        if (precision(rule.tp, rule.fp) > 0.8 && rule.tp > 1) {
            result.add(rule.base)
//            println("residual TP: ${rule.tp}, residual FP: ${rule.fp}\n" +
//                    "TP: ${rule.base.tp}, FP: ${rule.base.fp}, HFP: ## Pure: ${rule.base.info.pure}, Dirty: ${rule.base.info.dirty}")

            overallTP.addAll(rule.base.TP)
            overallFP.addAll(rule.base.FP)

            filtered.sort()
        }
    }

    println("Errors count: $errorsCount")
    println("TP: ${overallTP.size}")
    println("FP: ${overallFP.size}")

    val testBlocks = loadDataset(test)
    val testedResult = result.filter { rule ->
        val TP = HashSet<RuleWithErrors.Match>()
        val FP = HashSet<RuleWithErrors.Match>()

        for ((index, block) in testBlocks.withIndex()) {
            val (blockTP, blockFP) = rule.rule.calcStatisticsForBlock(block, index)
            TP.addAll(blockTP)
            FP.addAll(blockFP)
        }

       // println("${TP.size} ${FP.filter { it.error != null }.size} ${FP.size}")

        precision(TP.size + rule.tp, FP.size + rule.fp) > 0.7 //|| (TP.size == 0 && FP.size == 0 && rule.calcMetric() > 0.7 && rule.tp - rule.fp > 3)
    }

    println("Removed after validation: ${result.size - testedResult.size}")
    println(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))

    with (File(results)) {
        createNewFile()
        writeBytes(adapter.toJson(testedResult).toByteArray())
    }
    println()
}

fun main() {
    val rulesType = Types.newParameterizedType(MutableList::class.java, RuleWithErrors::class.java)
    val rulesAdapter = MOSHI.adapter<List<RuleWithErrors>>(rulesType).indent(" ")

    val sentenceType = Types.newParameterizedType(MutableList::class.java, Token::class.java)
    val textType = Types.newParameterizedType(MutableList::class.java, sentenceType)
    val textAdapter = MOSHI.adapter<List<List<Token>>>(textType)

    val rules = ArrayList<RuleWithErrors>()
    for (file in File("data/results").listFiles() + File("data/results/ignore").listFiles()) {
        if (file.name == "ignore") continue
        rules.addAll(rulesAdapter.fromJson(file.readText()) ?: emptyList())
    }

//    val rules = rulesAdapter.fromJson(File("data/results/'R_PREP'.json").readText()) ?: emptyList()
    val text = textAdapter.fromJson(File("test_origin.json").readText()) ?: emptyList()

    with (File("final_result.txt")) {
        createNewFile()
        writeText("")
        for (sentence in text) {
            var fixed = sentence
            for (rule in rules) {
                fixed = rule.rule.correct(fixed).toList()
            }
            appendText(fixed.map { it.text }.joinToString(" "))
            appendText("\n")
        }
    }
}

//fun main() {
//    val rulesType = Types.newParameterizedType(MutableList::class.java, RuleWithErrors::class.java)
//    val rulesAdapter = MOSHI.adapter<List<RuleWithErrors>>(rulesType).indent(" ")
//
//    data class xxx (var count: Int = 0, var TP: Int = 0, var FP: Int = 0, var FN: Int = 0)
//    val map = HashMap<String, xxx>()
//    for (file in File("data/results").listFiles() + File("data/results/ignore").listFiles()) {
//        if (file.name == "ignore") continue
//        val rules = rulesAdapter.fromJson(file.readText()) ?: emptyList()
//
//        val blocks = loadDataset("data/train/${file.name}")
//        val errorsCount = blocks.map { it.errors.size }.sum()
//
//        val x = map.getOrPut(file.name.substring(2)) {
//            xxx()
//        }
//
//        x.count += rules.count()
//        x.TP += rules.flatMap { it.TP }.distinct().count()
//        x.FP += rules.flatMap { it.FP }.distinct().count()
//        x.FN += errorsCount - rules.flatMap { it.TP }.distinct().count()
//
//        println("${file.name}\t${rules.count()}\t${rules.flatMap { it.TP }.distinct().count()}\t${rules.flatMap { it.FP }.distinct().count()}\t${errorsCount - rules.flatMap { it.TP }.distinct().count()}")
//    }
//
//    for ((n, x) in map) {
//        println("$n\t${x.count}\t${x.TP}\t${x.FP}\t${x.FN}")
//    }
//
//}
