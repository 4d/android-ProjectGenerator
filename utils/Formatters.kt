import org.json.JSONObject


fun isCustomFormatterSearchable(tableName: String, name: String, searchableFields: Map<String, List<String>>): Boolean =
    searchableFields[tableName]?.contains(name) ?: false

fun getFieldMapping(manifestContent: JSONObject, format: String, isSearchable: Boolean): FieldMapping =
    FieldMapping(
        binding = manifestContent.getSafeString("binding"),
        choiceList = manifestContent.getSafeObject("choiceList")?.toStringMap()
            ?: manifestContent.getSafeArray("choiceList")
                .getStringList(),  // choiceList can be a JSONObject or a JSONArray
        isSearchable = isSearchable,
        name = format,
        imageWidth = getSize(manifestContent,"width"),
        imageHeight = getSize(manifestContent,"height")
    )

fun getSize(manifestContent: JSONObject, type: String): Int? =
    manifestContent.getSafeObject("assets")?.getSafeObject("size")?.getSafeInt(type)
        ?: manifestContent.getSafeObject("assets")?.getSafeInt("size")
