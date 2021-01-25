import PathHelperConstants.XML_EXT
import PathHelperConstants.XML_TXT_EXT

val regex = Regex("[^a-z0-9]")

fun String.condense() = regex.replace(this.toLowerCase(), "")

fun String.condensePropertyName() = this.replace("\\s".toRegex(), "")

fun String.addXmlSuffix() = this + XML_EXT

fun String.addXmlTxtSuffix() = this + XML_TXT_EXT

fun String.removeXmlTxtSuffix() = this.removeSuffix(XML_TXT_EXT)

fun String.replaceXmlTxtSuffix() = if (this.endsWith(XML_TXT_EXT)) this.removeXmlTxtSuffix().addXmlSuffix() else this
