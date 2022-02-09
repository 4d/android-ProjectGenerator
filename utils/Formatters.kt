import org.json.JSONObject

fun getFieldMapping(manifestContent: JSONObject, format: String): FieldMapping =
    FieldMapping(
        binding = manifestContent.getSafeString("binding"),
        choiceList = manifestContent.getSafeObject("choiceList")?.toStringMap()
            ?: manifestContent.getSafeArray("choiceList")
                .getStringList(),  // choiceList can be a JSONObject or a JSONArray
        type = manifestContent.getSafeString("type") ?: manifestContent.getSafeArray("type")
            .getStringList(), // type can be a String or a JSONArray
        name = format,
        imageWidth = getSize(manifestContent, "width"),
        imageHeight = getSize(manifestContent, "height"),
        tintable = manifestContent.getSafeObject("assets")?.getSafeBoolean("tintable"),
        target = manifestContent.getSafeString("target") ?: manifestContent.getSafeArray("target")
            .getStringList(), // target can be a String or a JSONArray,
        capabilities = manifestContent.getSafeObject("capabilities")?.getSafeArray("android").getStringList()
    )

fun getSize(manifestContent: JSONObject, type: String): Int? =
    manifestContent.getSafeObject("assets")?.getSafeObject("size")?.getSafeInt(type)
        ?: manifestContent.getSafeObject("assets")?.getSafeInt("size")

fun FieldMapping.isValidFormatter(): Boolean =
    (this.binding == "localizedText" || this.binding == "imageNamed")
            && this.choiceList != null && this.name != null

fun FieldMapping.isValidKotlinCustomDataFormatter(): Boolean {
    val isTargetOk = when (target) {
        is String -> target == "android"
        is List<*> -> target.contains("android") || target.isEmpty()
        else -> false
    }
    return this.name != null && !this.binding.isNullOrEmpty() && isTargetOk
}