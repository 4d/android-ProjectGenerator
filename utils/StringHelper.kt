import PathHelperConstants.XML_EXT
import PathHelperConstants.XML_TXT_EXT
import java.text.Normalizer

fun String.isNumber(): Boolean = if (this.isEmpty()) false else this.all { Character.isDigit(it) }

/**
 * File extensions
 */

fun String.addXmlSuffix() = this + XML_EXT

fun String.replaceXmlTxtSuffix() = if (this.endsWith(XML_TXT_EXT)) this.removeSuffix(XML_TXT_EXT).addXmlSuffix() else this

/**
 * Field / Table name adjustments
 */

fun String.tableNameAdjustment() = this.condense().capitalize().replaceSpecialChars().validateWord()

fun String.fieldAdjustment() = this.condense().replaceSpecialChars().validateWord()

fun String.dataBindingAdjustment(): String = this.condense().replaceSpecialChars().split("_")
    .joinToString("") { it.toLowerCase().capitalize() }

private fun String.condense() = this.replace("\\s".toRegex(), "")

private fun String.replaceSpecialChars(): String {
    return if (this.contains("Entities<")) {
        this.replace("[^a-zA-Z0-9<>]".toRegex(), "_").unaccent()
    } else {
        this.replace("[^a-zA-Z0-9]".toRegex(), "_").unaccent()
    }
}

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

private const val prefixReservedKeywords = "qmobile"

fun String.validateWord(): String =
    if (reservedKeywords.contains(this)) "${prefixReservedKeywords}_$this" else this

val reservedKeywords = listOf(
    "as",
    "break",
    "class",
    "continue",
    "do",
    "else",
    "false",
    "for",
    "fun",
    "if",
    "in",
    "is",
    "null",
    "object",
    "package",
    "return",
    "super",
    "this",
    "throw",
    "true",
    "try",
    "typealias",
    "typeof",
    "val",
    "var",
    "when",
    "while",
    "by",
    "catch",
    "constructor",
    "delegate",
    "dynamic",
    "field",
    "file",
    "finally",
    "get",
    "import",
    "init",
    "param",
    "property",
    "receiver",
    "set",
    "setparam",
    "where",
    "actual",
    "abstract",
    "annotation",
    "companion",
    "const",
    "crossinline",
    "data",
    "enum",
    "expect",
    "external",
    "final",
    "infix",
    "inline",
    "inner",
    "internal",
    "lateinit",
    "noinline",
    "open",
    "operator",
    "out",
    "override",
    "private",
    "protected",
    "public",
    "reified",
    "sealed",
    "suspend",
    "tailrec",
    "vararg",
    "field",
    "it"
)
