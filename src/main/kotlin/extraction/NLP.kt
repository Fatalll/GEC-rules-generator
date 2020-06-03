package extraction

import rules.MOSHI
import com.squareup.moshi.Types
import org.clulab.processors.Sentence
import org.clulab.processors.clu.tokenizer.EnglishSentenceSplitter
import org.clulab.processors.clu.tokenizer.RawToken
import org.clulab.processors.fastnlp.FastNLPProcessor
import scala.collection.JavaConverters
import java.io.File
import java.lang.IllegalStateException

object NLP {
    val processor: FastNLPProcessor by lazy { FastNLPProcessor(true, true, true, 0) }
    private val splitter = EnglishSentenceSplitter()

    fun preprocessPlainFile(inputFile: String, outputFileJson: String) {
        val text = TextBlock::class.java.getResource(inputFile).readText()

        val data = text.split("\n\n", "\r\n\r\n")
        val sentences = ArrayList<List<Token>>()
        println("Blocks total: ${data.size}")
        for ((counter, block) in data.withIndex()) {
            val document = processor.annotate(block, true)
            println("Block ${counter + 1}, chars: ${block.length}, sents: ${document.sentences().size}")
            for (sentence in document.sentences()) {
                sentences.add(sentenceToTokens(sentence))
            }
        }

        val sentenceType = Types.newParameterizedType(MutableList::class.java, Token::class.java)
        val textType = Types.newParameterizedType(MutableList::class.java, sentenceType)
        val adapter = MOSHI.adapter<List<List<Token>>>(textType)

        with (File(outputFileJson)) {
            createNewFile()
            writeBytes(adapter.toJson(sentences).toByteArray())
        }

        println("Done")
    }

    fun preprocessPlainTokenizedFile(inputFile: String, outputFileJson: String) {
        val blocks = ArrayList<List<Token>>()
        NLP::class.java.getResourceAsStream(inputFile).reader().forEachLine { line ->
            blocks.add(line.convertToTokens().flatten())
        }

        val sentenceType = Types.newParameterizedType(MutableList::class.java, Token::class.java)
        val textType = Types.newParameterizedType(MutableList::class.java, sentenceType)
        val adapter = MOSHI.adapter<List<List<Token>>>(textType)

        with (File(outputFileJson)) {
            createNewFile()
            writeBytes(adapter.toJson(blocks).toByteArray())
        }

        println("Done")
    }

    fun preprocessDatasetBlocks(inputErrorFile: String = "/data/train/'M_DET'.type",
                                inputCorrectFile: String = "/data/train/correct.type",
                                outputFileJson: String = "train.json") {
        val correctBlocks = ArrayList<List<Token>>()
        NLP::class.java.getResourceAsStream(inputCorrectFile).reader().forEachLine { line ->
            correctBlocks.add(line.convertToTokens().flatten())
        }

        var isGrammarError = false
        val blocks = ArrayList<DatasetBlock>()
        NLP::class.java.getResourceAsStream(inputErrorFile).reader().forEachLine { line ->
            if (line.isNotBlank()) {
                if (!isGrammarError) {
                    isGrammarError = true
                    blocks.add(DatasetBlock(line.convertToTokens().flatten()))
                } else {
                    val match = DatasetBlock.GrammarError.GRAMMAR_ERROR_PATTERN.find(line)
                    require(match != null)

                    val errorFrom = match.groupValues[1].toInt()
                    val errorTo = match.groupValues[2].toInt()

                    val correctFrom = match.groupValues[5].toInt()
                    val correctTo = match.groupValues[6].toInt()

                    val fixes: List<Token> = correctBlocks[blocks.size - 1].slice(correctFrom until correctTo)

                    blocks.last().errors.add(DatasetBlock.GrammarError(errorFrom, errorTo, fixes))
                }
            } else {
                isGrammarError = false
            }
        }

        val type = Types.newParameterizedType(MutableList::class.java, DatasetBlock::class.java)
        val adapter = MOSHI.adapter<List<DatasetBlock>>(type).indent(" ")

        with(File(outputFileJson)) {
            createNewFile()
            writeBytes(adapter.toJson(blocks).toByteArray())
        }
    }

    private fun sentenceToTokens(sentence: Sentence): List<Token> {
        val tokens = ArrayList<Token>()
        for (index in 0 until sentence.size()) {
            val text = sentence.raw()[index]
            val lemma = sentence.lemmas().get()[index]
            val posTag = PennPOSTag.values().firstOrNull { it.name == sentence.tags().get()[index] } ?: PennPOSTag.SYM
            val (chunkTag, chunkPos) = with(sentence) {
                val currentChunk = chunks().get()[index]
                val nextChunk = if (index + 1 == size()) "B-NP" else chunks().get()[index + 1]

                val chunkTag = if (currentChunk.length == 1) PennChunkTag.O else PennChunkTag.valueOf(currentChunk.substring(2))
                val chunkPos = if (currentChunk.length == 1) PennChunkPos.BEGIN else when {
                    currentChunk[0] == 'B' -> PennChunkPos.BEGIN
                    currentChunk[0] == 'I' && nextChunk[0] == 'B' -> PennChunkPos.END
                    currentChunk[0] == 'I' -> PennChunkPos.MIDDLE
                    else -> throw IllegalStateException()
                }

                chunkTag to chunkPos
            }

            val position = when (index) {
                0 -> SentPosition.SENT_START
                sentence.size() - 1 -> SentPosition.SENT_END
                else -> SentPosition.SENT_MIDDLE
            }

            val (relation, relIndex) = with(sentence.universalEnhancedDependencies().get().getIncomingEdges(index)) {
                if (isEmpty()) {
                    "" to -1
                } else {
                    get(0)._2 to get(0)._1 as Int
                }
            }

            tokens.add(
                Token(
                    text,
                    lemma,
                    posTag,
                    chunkTag,
                    chunkPos,
                    position,
                    relation,
                    relIndex
                )
            )
        }

        return tokens
    }

    private fun String.convertToTokens(): ArrayList<List<Token>> {
        var offset = 0
        val tokens = split(' ').map {
            val token = RawToken(it, offset, offset + it.length, it)
            offset += it.length + 1
            token
        }

        val sentences = JavaConverters.collectionAsScalaIterable(
            splitter.split(tokens.toTypedArray(), true).map {
                JavaConverters.collectionAsScalaIterable(it.raw().toList())
            }
        )

        val document = processor.annotateFromTokens(sentences, false)
        val block = ArrayList<List<Token>>()
        for (sentence in document.sentences()) {
            block.add(sentenceToTokens(sentence))
        }

        return block
    }
}

fun main() {
    NLP.preprocessDatasetBlocks("/data/train/'M_PUNCT'.type",
        "/data/train/correct.type", "'M_PUNCT'.train.json")
    NLP.preprocessDatasetBlocks("/data/test/'M_PUNCT'.type",
        "/data/test/correct.type", "'M_PUNCT'.test.json")

    var path = NLP::class.java.getResource("/data/test").path
    for (file in File(path).listFiles()) {
        if (file.name == "correct.type") continue
        NLP.preprocessDatasetBlocks("/data/test/${file.name}",
            "/data/test/correct.type", "data/test/${file.name.removeSuffix(".type")}.json")
    }

    path = NLP::class.java.getResource("/data/train").path
    for (file in File(path).listFiles()) {
        if (file.name == "correct.type") continue
        NLP.preprocessDatasetBlocks("/data/train/${file.name}",
            "/data/train/correct.type", "data/train/${file.name.removeSuffix(".type")}.json")
    }

    val x= NLP.processor.annotate("Yesterday morning , a group of American tourist went for a bike ride through the center of Buenos Aires City .", false)
    println(x.sentences()[0].dependencies().get())
}