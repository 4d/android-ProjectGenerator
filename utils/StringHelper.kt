import PathHelperConstants.XML_EXT
import PathHelperConstants.XML_TXT_EXT
import java.text.Normalizer
import java.util.*

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

fun String.fieldAdjustment() =
    this.condense().replaceSpecialChars().lowerCustomProperties().validateWordDecapitalized()

fun String.dataBindingAdjustment(): String =
    this.condense().replaceSpecialChars().firstCharForTable()
        .split("_")
        .joinToString("") { it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()) }

private fun String.condense() =
    if (!this.startsWith("Map<"))
        this.replace("\\s".toRegex(), "")
    else
        this

private fun String.replaceSpecialChars(): String =
    when {
        this.contains("Entities<") -> this.unaccent().replace("[^a-zA-Z0-9._<>]".toRegex(), "_")
        this.contains("Map<") -> this.unaccent().replace("[^a-zA-Z0-9._<>, ]".toRegex(), "_")
        else -> this.unaccent().replace("[^a-zA-Z0-9._]".toRegex(), "_")
    }

private fun String.lowerCustomProperties() =
    if (this in arrayOf("__KEY", "__STAMP", "__GlobalStamp", "__TIMESTAMP"))
        this
    else
        when {
//            this.startsWith("__") && this.endsWith("Key") -> this.removeSuffix("Key").toLowerCase(Locale.getDefault()) + "Key"
            this.startsWith("__") && this.endsWith("Key") -> this.removeSuffix("Key").decapitalize(Locale.getDefault()) + "Key"
//            else -> this.toLowerCase(Locale.getDefault())
            else -> this.decapitalize(Locale.getDefault())
        }

private fun String.decapitalizeExceptID() =
    if (this == "ID") this.toLowerCase(Locale.getDefault()) else this.decapitalize(Locale.getDefault())

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

fun String.validateWord(): String {
    return this.split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "qmobile_$it" else it
    }
}

fun String.validateWordDecapitalized(): String {
    return this.decapitalizeExceptID().split(".").joinToString(".") {
        if (reservedKeywords.contains(it)) "qmobile_$it" else it
    }
}

fun String.relationAdjustment(): String =
    this.split(".").map { it.tableNameAdjustment() }.joinToString("").tableNameAdjustment().decapitalize()

fun String.encode(): String = this
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "\\'") // &apos; does not work in strings.xml file

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
