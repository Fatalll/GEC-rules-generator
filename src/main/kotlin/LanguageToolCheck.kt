import com.squareup.moshi.Types
import rules.*
import extraction.Token
import extraction.loadDataset
import org.languagetool.AnalyzedToken
import org.languagetool.JLanguageTool
import org.languagetool.Languages
import org.languagetool.rules.CategoryId
import java.io.File
import java.lang.StringBuilder

val disabled = listOf(
    "ABUNDANCE",
    "ABUNDANCE",
    "ACCEDE_TO",
    "ACCELERATE",
    "ACCENTUATE",
    "ACCOMMODATION",
    "ACCOMPANY",
    "ACCOMPLISH",
    "ACCORDING_TO",
    "ACCURATE",
    "ACQUIRE",
    "ADJECTIVE_IN_ATTRIBUTE",
    "ADJECTIVE_IN_ATTRIBUTE",
    "ADVERB_VERB_ADVERB_REPETITION",
    "ADVERB_WORD_ORDER",
    "ADVERB_WORD_ORDER",
    "ADVISE_VBG",
    "AFFECT_EFFECT",
    "AFFECT_EFFECT",
    "AGREEMENT_THEIR_HIS",
    "AGREE_WITH_THE_FACT",
    "ALLOW_TO",
    "ALLOW_TO",
    "ALLOW_TO_DO",
    "ALL_OF_THE",
    "ALL_OF_THE",
    "ALSO_OTHER",
    "AM_I",
    "AM_I",
    "AM_PM_OCLOCK",
    "ANALYSIS_IF",
    "ANINFOR_EVERY_DAY",
    "ANINFOR_EVERY_DAY",
    "ANYMORE",
    "ANYMORE",
    "ANYMORE_ADVERB",
    "ANY_MORE",
    "AN_ANOTHER",
    "APARTMENT-FLAT",
    "APARTMENT-FLAT",
    "APOSTROPHE_IN_DATES",
    "APPLE_PRODUCTS",
    "APPLE_PRODUCTS",
    "APPLY_FOR",
    "ARRIVAL_TO_THE_HOUSE",
    "ASK_TO",
    "ASSIST_ASSISTANCE",
    "AS_ADJ_AS",
    "AS_A_MATTER_OF_FACT",
    "AS_OF_YET",
    "AS_PER",
    "AS_WELL_AS_BETTER",
    "AT_ALL_TIMES",
    "AT_OVER_THE_WEEKEND",
    "AT_YOUR_EARLIEST_CONVENIENCE",
    "AU",
    "A_BIT_OF",
    "A_PLURAL",
    "A_PLURAL",
    "BASIC_FUNDAMENTALS",
    "BASIS_ON_A",
    "BECAUSE",
    "BECAUSE_OF_THE_FACT_THAT",
    "BEEN_PART_AGREEMENT",
    "BEEN_PART_AGREEMENT",
    "BE_A_X_ONE",
    "BE_CAUSE",
    "BE_CAUSE",
    "BORED_OF",
    "BU",
    "BUY_TWO_GET_ONE_FREE",
    "BY_MEANS_OF",
    "BY_PASSIVE_PARTICIPLE_BE",
    "CAPITALIZATION_NNP_DERIVED",
    "CAUSE_BECAUSE",
    "CA_BRAND_NEW",
    "CD_00_O_CLOCK",
    "CHILDISH_LANGUAGE",
    "CHILDISH_LANGUAGE",
    "CLEAN_UP",
    "COLLECTIVE_NOUN_VERB_AGREEMENT_VBD",
    "COLLECTIVE_NOUN_VERB_AGREEMENT_VBP",
    "COLLECTIVE_NOUN_VERB_AGREEMENT_VBP",
    "COME_IN_TO",
    "COMMA_AFTER_A_MONTH",
    "COMMA_COMPOUND_SENTENCE",
    "COMMA_PARENTHESIS_WHITESPACE",
    "COMPARISONS_AS_ADJECTIVE_AS",
    "COMP_THAN",
    "CONCENTRATE_WITH_ON",
    "CONFUSION_OF_GOLD_GOLF",
    "CONFUSION_OF_MARS_MARS",
    "CONFUSION_OF_OUR_OUT",
    "CONSEQUENCES_OF_FOR",
    "CURRENCY",
    "CURRENTLY",
    "DASH_RULE",
    "DASH_RULE",
    "DAY_TO_DAY_HYPHEN",
    "DEPEND_ON",
    "DESPITE_THE_FACT",
    "DIFFERENT_THAN",
    "DIS",
    "DISCUSSIONS_AROUND",
    "DOES_NP_VBZ",
    "DONT_T",
    "DOUBLE_PUNCTUATION",
    "DO_A_PARTY",
    "DRAW_ATTENTION",
    "DT_PRP",
    "DT_PRP",
    "DURING_OVER_THE_WEEKENDS",
    "DURING_OVER_THE_WEEKENDS",
    "EACH_AND_EVERY_NOUN",
    "EACH_EVERY_NNS",
    "EMPHATIC_REFLEXIVE_PRONOUNS",
    "ENGLISH_WORD_REPEAT_RULE",
    "EN_DIACRITICS_REPLACE",
    "EN_DIACRITICS_REPLACE",
    "EN_GB_SIMPLE_REPLACE",
    "EN_GB_SIMPLE_REPLACE",
    "EN_QUOTES",
    "EN_WORD_COHERENCY",
    "ESSENTIAL_ESSENTIALLY",
    "EVERYONE_OF",
    "EVERYONE_OF",
    "EVERY_NOW_AND_THEN",
    "EVER_EVERY",
    "EXTREME_ADJECTIVES",
    "EXTREME_ADJECTIVES",
    "FELLOW_CLASSMATE",
    "FIRST_OF_ALL",
    "FIRST_OF_ALL",
    "FOCUS_IN",
    "FOLLOW_A_COURSE",
    "FOR_THE_PURPOSE_OF",
    "FREE_LANCE",
    "FROM_X_Y",
    "FROM_X_Y",
    "GENERAL_XX",
    "GLAD_WITH_ABOUT",
    "GONNA",
    "GOT_GOTTEN",
    "GO_GERUND",
    "GRADUATE_FROM",
    "GUILTY_FOR_OF",
    "HASNT_IRREGULAR_VERB",
    "HASNT_IRREGULAR_VERB",
    "HAVE_A_TENDENCY",
    "HAVE_CD_YEARS",
    "HAVE_THE_ABILITY_TO",
    "HELL",
    "HELP_TO_FIND",
    "HELP_TO_FIND",
    "HE_VERB_AGR",
    "HUMANS_BEINGS",
    "IF_IS",
    "IF_WOULD_HAVE_VBN",
    "INDEPENDENTLY_FROM_OF",
    "INTERJECTIONS_PUNCTUATION",
    "INTERJECTIONS_PUNCTUATION",
    "IN_FROM_THE_PERSPECTIVE",
    "IN_ON_AN_ALBUM",
    "IN_OR_WITH_REGARDS_TO_OF",
    "IN_TERMS_OF",
    "IN_THE_CASE_OF",
    "IN_THE_MOMENT",
    "IN_THIS_MOMENT",
    "IN_THIS_MOMENT",
    "IN_WHO",
    "IS_WAS",
    "ITS_IS",
    "I_A",
    "I_IF",
    "I_IF",
    "I_MOVING",
    "I_THIN",
    "KEY_WORDS",
    "KIND_OF_A",
    "KNEW_NEW",
    "KNOW_NOW",
    "LARGE_NUMBER_OF",
    "LARGE_NUMBER_OF",
    "LAUGH_OF_AT",
    "LEARN_NNNNS_ON_DO",
    "LESS_MORE_THEN",
    "LICENCE_LICENSE_NOUN_SINGULAR",
    "LICENCE_LICENSE_NOUN_SINGULAR",
    "LIGHT_WEIGHT",
    "LITTLE_BIT",
    "LITTLE_BIT",
    "LOCATED_ON_AT",
    "LOSE_LIVES",
    "LOSE_LOSS",
    "LOTS_OF_NN",
    "LOT_OF",
    "MAJORITY",
    "MAKE_AN_ATTEMPT",
    "MANY_FEW_UNCOUNTABLE",
    "MANY_FEW_UNCOUNTABLE",
    "MANY_KINDS_OF",
    "MANY_NN_U",
    "MANY_NN_U",
    "MEAN_FOR_TO",
    "METRIC_UNITS_EN_IMPERIAL",
    "METRIC_UNITS_EN_IMPERIAL",
    "METRIC_UNITS_EN_US",
    "METRIC_UNITS_EN_US",
    "MINUETS",
    "MISSING_COMMA_AFTER_WEEKDAY",
    "MISSING_COMMA_BETWEEN_DAY_AND_YEAR",
    "MISSING_GENITIVE",
    "MISSING_GENITIVE",
    "MONTH_OF_XXXX",
    "MOST_SOME_OF_NNS",
    "MOST_SOME_OF_NNS",
    "MULTIPLICATION_SIGN",
    "MUST_BE_DO",
    "NEEDNT_TO_DO_AND_DONT_NEED_DO",
    "NON_STANDARD_COMMA",
    "NON_STANDARD_COMMA",
    "NOT_ABLE",
    "NOT_ACCEPT",
    "NOT_CERTAIN",
    "NOT_MANY",
    "NOT_THE_SAME",
    "NOUN_AROUND_IT",
    "NOUN_AROUND_IT",
    "NOW",
    "NOW",
    "NO_COMMA_BEFORE_INDIRECT_QUESTION",
    "NUMEROUS_DIFFERENT",
    "NUMEROUS_DIFFERENT",
    "OBJECTIVE_CASE",
    "OBTAIN",
    "OFT_HE",
    "ONE_PLURAL",
    "ON_THE_OTHER_HAND",
    "ORDINAL_NUMBER_SUFFIX",
    "OTHER_WISE_OTHERWISE",
    "OUTSIDE_OF",
    "OUTSIDE_OF",
    "OXFORD_SPELLING_ADJECTIVES",
    "OXFORD_SPELLING_ADJECTIVES",
    "OXFORD_SPELLING_ISE_VERBS",
    "OXFORD_SPELLING_ISE_VERBS",
    "OXFORD_SPELLING_NOUNS",
    "OXFORD_SPELLING_NOUNS",
    "O_CLOCK",
    "PAST_EXPERIENCE_MEMORY",
    "PAST_TIME",
    "PAYED",
    "PEOPLE_VBZ",
    "PEOPLE_VBZ",
    "PERS_PRONOUN_AGREEMENT",
    "PERS_PRONOUN_AGREEMENT",
    "PERS_PRON_CONTRACTION",
    "PHRASE_REPETITION",
    "PHRASE_REPETITION",
    "PH_D",
    "PLAY_GAMES",
    "PLAY_WITH_FOR",
    "PLURAL_VERB_AFTER_THIS",
    "POSSESSIVE_APOSTROPHE",
    "POSSESSIVE_APOSTROPHE",
    "PROFANITY",
    "PRP_PAST_PART",
    "PRP_VBG",
    "PRP_VBG",
    "PUNCTUATION_PARAGRAPH_END",
    "RATHER_THEN",
    "REASON_IS_BECAUSE",
    "REFERRING_BACK",
    "RELY_ON",
    "RELY_ON",
    "REPEAT_AGAIN",
    "ROYAL_MAIL",
    "SEND_PRP_AN_EMAIL",
    "SEND_PRP_AN_EMAIL",
    "SHORT_SUPERLATIVES",
    "SHORT_SUPERLATIVES",
    "SHOULD_BE_DO",
    "SINGULAR_AGREEMENT_SENT_START",
    "SOME_OF_THE",
    "SOME_OF_THE",
    "SOONER_RATHER_THAN_LATER",
    "SO_AS_TO",
    "STEPS_TO_DO",
    "SUFFICIENT",
    "SUMMER_TIME",
    "SUPERLATIVE_THAN",
    "SUPERLATIVE_THAN",
    "TAKE_A_BATH",
    "TAKE_A_BATH",
    "THAN_I",
    "THERE_RE_MANY",
    "THE_ARE",
    "THE_ARE",
    "THE_BEST_WAY",
    "THE_FALL_SEASON",
    "THE_FLEW",
    "THE_QUESTION_WH",
    "THE_SOME_DAY",
    "THE_SUPERLATIVE",
    "THE_THEY",
    "THIS_NNS_VB",
    "TIME_NOW",
    "TOO_DETERMINER",
    "TO_DO_HYPHEN",
    "TO_TOO",
    "TO_TOO",
    "TRAVEL_WITH_BY",
    "TRAVEL_WITH_BY",
    "TRUNK_BOOT",
    "TRY_AND",
    "TWO_CONNECTED_MODAL_VERBS",
    "TWO_CONNECTED_MODAL_VERBS",
    "UNIT_SPACE",
    "UNIT_SPACE",
    "UNLIKELY_OPENING_PUNCTUATION",
    "USE_TO_VERB",
    "USE_TO_VERB",
    "VB_A_WHILE",
    "VITAMIN_C",
    "DELETE_SPACE",
    "WELL_IN_ON",
    "WERE_VBB",
    "WHEN_WHERE",
    "WHETHER",
    "WHOS",
    "WHO_NOUN",
    "WIFI",
    "WITH_OR_IN_REFERENCE_OR_REGARD_TO",
    "WITH_OUT",
    "WORTHWHILE",
    "WORTH_THAN",
    "WRITE_IN_MY_OWN_PAGE",
    "WRONG_APOSTROPHE",
    "WRONG_APOSTROPHE",
    "WRONG_GENITIVE_APOSTROPHE",
    "YESTERDAY_NIGHT",
    "YOUR",
    "YOUR_S"
)

fun Token.toAnalyzedToken() = AnalyzedToken(text, pos?.name, lemma)
val LANG_TOOL = JLanguageTool(Languages.getLanguageForShortCode("en-GB"))


fun main() {
    val rulesType = Types.newParameterizedType(MutableList::class.java, RuleWithErrors::class.java)
    val rulesAdapter = MOSHI.adapter<List<RuleWithErrors>>(rulesType).indent(" ")

    val sentenceType = Types.newParameterizedType(MutableList::class.java, Token::class.java)
    val textType = Types.newParameterizedType(MutableList::class.java, sentenceType)

    val rules = ArrayList<RuleWithErrors>()
//    for (file in File("data/results").listFiles()) {
//        if (file.name == "ignore") continue
//        rules.addAll(rulesAdapter.fromJson(file.readText()) ?: emptyList())
//    }

    rules.addAll(rulesAdapter.fromJson(File("data/results/ignore/'R_VERB_TENSE'.json").readText()) ?: emptyList())

    rules.sortByDescending { it.tp }

//    for (rule in rules) {
//        println(rule)
//        println()
//    }

//    for (file in File("data/train").listFiles()) {
//        generate("data/train/${file.name}", "data/test/${file.name}", "data/results/${file.name}")
//    }
    val list = listOf(
        "'M_DET'.json",
        "'M_PUNCT'.json",
        "'R_NOUN_NUM'.json",
        "'R_PREP'.json",
        "'R_VERB_FORM'.json",
        "'U_DET'.json"
    )

    for (type in list) {
        val rules = rulesAdapter.fromJson(File("data/results/$type").readText()) ?: emptyList()
        val blocks = loadDataset("data/train/$type")

        for ((block, errs) in rules.map { it.TP }.flatten().groupBy { it.block }) {
            blocks[block].fixed.addAll(errs.map { it.position })
        }

        with (File("data/validation/${type.removeSuffix("json")}txt")) {
            createNewFile()

            writeText("")
            for (rule in rules) {
                appendText("$rule\n\n")
            }

            appendText("\n")
            for (block in blocks) {
                appendText("$block\n")
            }
        }
    }

//    val english = Languages.getLanguageForShortCode("en-GB")
//    val tool = JLanguageTool(english)
//    tool.disableCategory(CategoryId("FALSE_FRIENDS"))
//    //tool.disableCategory(CategoryId("TYPOGRAPHY"))
//    for (id in disabled) {
//        tool.disableRule(id)
//    }
//
//    val text = File("final_result.txt").readText()
//    val fixed = StringBuilder()
//    for (sentence in text.split('\n')) {
//        var result = sentence
//        for (match in tool.check(sentence).sortedByDescending { it.fromPos }) {
//            val error = result.substring(match.fromPos, match.toPos)
//            if (match.rule.id == "MORFOLOGIK_RULE_EN_GB") {
//                if (error[0].isUpperCase()) continue
//                if (error.toLowerCase() in match.suggestedReplacements) continue
//                if (error == "anymore") continue
//            }
//
//            if (match.suggestedReplacements.isNotEmpty()) {
//                result = result.replaceRange(match.fromPos, match.toPos,
//                    match.suggestedReplacements.first()
//                        .replace(".", " .")
//                        .replace(",", " ,")
//                        .replace(";", " ;")
//                        .replace("!", " !")
//                        .replace("?", " ?")
//                )
//            }
//        }
//        fixed.append(result)
//        fixed.append('\n')
//    }
//
//
//    println(fixed.toString())
}