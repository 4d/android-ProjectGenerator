import PathHelperConstants.XML_EXT
import PathHelperConstants.XML_TXT_EXT
import java.text.Normalizer

private val regex = Regex("[^a-z0-9]")
private val REGEX_UNACCENT = "\\p{InCombiningDiacriticalMarks}+".toRegex()

fun String.condense() = regex.replace(this.toLowerCase(), "")

fun String.capitalizeWords(): String = condenseSpaces().split("_").joinToString("") { it.toLowerCase().capitalize() }

fun String.condenseSpaces() = this.replace("\\s".toRegex(), "").unaccent()

fun String.condenseSpacesCapital() = this.replace("\\s".toRegex(), "").capitalize().unaccent()

fun String.addXmlSuffix() = this + XML_EXT

fun String.replaceXmlTxtSuffix() = if (this.endsWith(XML_TXT_EXT)) this.removeSuffix(XML_TXT_EXT).addXmlSuffix() else this

fun String.isNumber(): Boolean = if (this.isNullOrEmpty()) false else this.all { Character.isDigit(it) }

fun CharSequence.unaccent(): String {
    val temp = Normalizer.normalize(this, Normalizer.Form.NFD)
    return REGEX_UNACCENT.replace(temp, "")
}