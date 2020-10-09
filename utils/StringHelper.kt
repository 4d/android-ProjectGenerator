val regex = Regex("[^a-z0-9]")

fun String.condense() = regex.replace(this.toLowerCase(), "")

fun String.addXmlSuffix() = "${this}.xml"
