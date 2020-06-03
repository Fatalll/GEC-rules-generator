package extraction

import rules.MOSHI
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types
import rules.RuleWithErrors
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet

@JsonClass(generateAdapter = true)
data class Token(val text: String, val lemma: String? = null, val pos: PennPOSTag? = null,
                 val chunkTag: PennChunkTag? = null, val chunkPos: PennChunkPos? = null, val sentPos: SentPosition? = null,
                 val relation: String? = null, val relationIndex: Int? = null)

@JsonClass(generateAdapter = true)
data class TextBlock(val text: String, val edits: List<Edit>) {
    @JsonClass(generateAdapter = true)
    data class Edit(val from: Int, val to: Int, val fix: String)
}

@JsonClass(generateAdapter = true)
data class DatasetBlock(val sentences: List<Token>, val errors: MutableList<GrammarError> = ArrayList()) {
    @Transient
    var fixed = HashSet<Int>()

    val invertedErrors by lazy {
        HashMap<Int, Int>().apply {
            for ((index, error) in errors.withIndex()) {
                put(error.from, index)
            }
        }
    }

    @JsonClass(generateAdapter = true)
    data class GrammarError(val from: Int, val to: Int, val fixes: List<Token>) {
        companion object {
            val GRAMMAR_ERROR_PATTERN = "^Orig: \\[(\\d+), (\\d+), (?:(?:'(.*?)')|(?:\"(.*?)\"))], Cor: \\[(\\d+), (\\d+), (?:(?:'(.*?)')|(?:\"(.*?)\"))]".toRegex()
        }
    }

    override fun toString(): String {
        return buildString {
            val queue = LinkedList(errors.sortedBy { it.from })
            var tmp = queue.poll()
            for ((index, token) in sentences.withIndex()) {
                if (tmp != null) {
                    if (tmp.from == index && tmp.from != tmp.to) {
                        append("(")
                    }

                    if (tmp.to == index) {
                        if (tmp.to != tmp.from) {
                            setLength(length - 1)
                            append(")")
                        }

                        if (tmp.from in fixed) append("$")

                        append("[${tmp.fixes.joinToString(" ") { it.text }}] ")
                        tmp = queue.poll()
                    }
                }

                append(token.text)
                append(" ")
            }
        }
    }
}

fun loadRawDataset(path: String): List<TextBlock> {
    val type = Types.newParameterizedType(MutableList::class.java, TextBlock::class.java)
    val adapter = MOSHI.adapter<List<TextBlock>>(type)
    return adapter.fromJson(TextBlock::class.java.getResource(path).readText()) ?: emptyList()
}

fun loadDataset(path: String, isResource: Boolean = false): List<DatasetBlock> {
    val type = Types.newParameterizedType(MutableList::class.java, DatasetBlock::class.java)
    val adapter = MOSHI.adapter<List<DatasetBlock>>(type)

    return if (isResource) {
        adapter.fromJson(DatasetBlock::class.java.getResource(path).readText())
    } else {
        adapter.fromJson(File(path).readText())
    } ?: emptyList()
}

fun main() {
    NLP.preprocessPlainTokenizedFile("/test.txt", "test_origin.json")
}