import PathHelperConstants.XML_EXT
import PathHelperConstants.XML_TXT_EXT
import java.text.Normalizer
import java.util.*

fun String.isNumber(): Boolean = if (this.isEmpty()) false else this.all { Character.isDigit(it) }

/**
 * File extensions
 */

fun String.addXmlSuffix() = this + XML_EXT

fun String.replaceXmlTxtSuffix() =
    if (this.endsWith(XML_TXT_EXT)) this.removeSuffix(XML_TXT_EXT).addXmlSuffix() else this

/**
 * Field / Table name adjustments
 */

fun String.tableNameAdjustment() =
    this.condense().capitalize(Locale.getDefault()).replaceSpecialChars().firstCharForTable().validateWord()

fun String.fieldAdjustment() = this.condense().replaceSpecialChars().decapitalizeExceptID().validateWord()

fun String.dataBindingAdjustment(): String = this.condense().replaceSpecialChars().firstCharForTable()
    .split("_").joinToString("") { it.toLowerCase().capitalize() }

private fun String.condense() = this.replace("\\s".toRegex(), "")

private fun String.replaceSpecialChars(): String {
    return if (this.contains("Entities<")) {
        this.unaccent().replace("[^a-zA-Z0-9._<>]".toRegex(), "_")
    } else {
        this.unaccent().replace("[^a-zA-Z0-9._]".toRegex(), "_")
    }
}

private fun String.decapitalizeExceptID() = if (this == "ID") this else this.decapitalize(Locale.getDefault())

private fun String.firstCharForTable(): String =
    if (this.startsWith("_"))
        "Q$this"
    else
        this

private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

private fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}

private const val prefixReservedKeywords = "qmobile"

fun String.validateWord(): String {
    return this.split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "${prefixReservedKeywords}_$it" else it
    }
}

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
