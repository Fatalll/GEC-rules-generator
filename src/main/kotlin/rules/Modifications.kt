package rules

import LANG_TOOL
import extraction.PennPOSTag
import extraction.Token
import toAnalyzedToken
import java.lang.StringBuilder
import kotlin.collections.ArrayList

data class Modifications(
    val offset: Int,
    val insertions: MutableList<InsertModification> = ArrayList(),
    val replacements: MutableList<ReplaceModification> = ArrayList(),
    val deletions: MutableList<DeleteModification> = ArrayList()
) {
    override fun toString(): String {
        val builder = StringBuilder()

        builder.append("{$offset}")
        if (insertions.isNotEmpty()) builder.append("-I[${insertions.joinToString(" ")}]")
        if (replacements.isNotEmpty()) builder.append("-R[${replacements.joinToString(" -> ")}]")
        if (deletions.isNotEmpty()) builder.append("-D")

        return builder.toString()
    }
}

sealed class TokenModification(val type: Type, val required: Boolean) {
    enum class Type {
        DELETE, INSERT, REPLACE, INFLECT, CASING
    }
}

abstract class DeleteModification(type: Type, required: Boolean = true) : TokenModification(type, required) {
    abstract fun isDeleteNeeded(current: Token): Boolean
}

abstract class InsertModification(type: Type, required: Boolean = true) : TokenModification(type, required) {
    abstract fun getTokenForInsertion(next: Token): Token?
}

abstract class ReplaceModification(type: Type, required: Boolean = true) : TokenModification(type, required) {
    abstract fun getTokenForReplacement(current: Token): Token
}

class Delete(required: Boolean = true) : DeleteModification(Type.DELETE, required) {
    override fun isDeleteNeeded(current: Token) = true
    override fun toString() = ""
}

class Insert(val new: Token, required: Boolean = true) : InsertModification(Type.INSERT, required) {
    override fun getTokenForInsertion(next: Token) = new
    override fun toString() = new.text
}

class Replace(val replacement: Token, required: Boolean = true) : ReplaceModification(Type.REPLACE, required) {
    override fun getTokenForReplacement(current: Token) = replacement
    override fun toString() = "replace by ${replacement.text}"
}

class Inflect(val inflectTo: PennPOSTag, required: Boolean = true) : ReplaceModification(Type.INFLECT, required) {
    override fun getTokenForReplacement(current: Token): Token {
        val infected = LANG_TOOL.language.synthesizer?.synthesize(current.toAnalyzedToken(), inflectTo.name)?.firstOrNull() ?: return current
        return current.copy(text = infected, pos = inflectTo)
    }

    override fun toString() = "inflect to ${inflectTo.name}"
}

class Casing(val casingType: CasingType, required: Boolean = true) : ReplaceModification(Type.CASING, required) {
    enum class CasingType {
        CAPITALIZE, DECAPITALIZE
    }

    override fun getTokenForReplacement(current: Token) = when (casingType) {
        CasingType.CAPITALIZE -> current.copy(text = current.text.capitalize())
        CasingType.DECAPITALIZE -> current.copy(text = current.text.decapitalize())
    }

    override fun toString() = when (casingType) {
        CasingType.CAPITALIZE -> "capitalize"
        CasingType.DECAPITALIZE -> "decapitalize"
    }
}
