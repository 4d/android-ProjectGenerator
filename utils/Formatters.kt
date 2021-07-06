import org.json.JSONObject

fun getFieldMapping(manifestContent: JSONObject, format: String): FieldMapping =
    FieldMapping(
        binding = manifestContent.getSafeString("binding"),
        choiceList = manifestContent.getSafeObject("choiceList")?.toStringMap()
            ?: manifestContent.getSafeArray("choiceList")
                .getStringList(),  // choiceList can be a JSONObject or a JSONArray
        name = format,
        imageWidth = getSize(manifestContent, "width"),
        imageHeight = getSize(manifestContent, "height"),
        tintable = manifestContent.getSafeObject("assets")?.getSafeBoolean("tintable")
    )

fun getSize(manifestContent: JSONObject, type: String): Int? =
    manifestContent.getSafeObject("assets")?.getSafeObject("size")?.getSafeInt(type)
        ?: manifestContent.getSafeObject("assets")?.getSafeInt("size")

fun isValidFormatter(fieldMapping: FieldMapping): Boolean =
    (fieldMapping.binding == "localizedText" || fieldMapping.binding == "imageNamed")
            && fieldMapping.choiceList != null && fieldMapping.name != null