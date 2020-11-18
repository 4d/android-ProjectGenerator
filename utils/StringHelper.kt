val regex = Regex("[^a-z0-9]")

fun String.condense() = regex.replace(this.toLowerCase(), "")

fun String.condensePropertyName() = this.replace("\\s".toRegex(), "")

fun String.addXmlSuffix() = "${this}.xml"
