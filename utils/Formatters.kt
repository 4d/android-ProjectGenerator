import org.json.JSONObject

fun isCustomFormatterSearchable(tableName: String, name: String, searchableFields: Map<String, List<String>>): Boolean =
    searchableFields[tableName]?.contains(name) ?: false

fun getFieldMapping(manifestContent: JSONObject, format: String, isSearchable: Boolean): FieldMapping =
    FieldMapping(
        binding = manifestContent.getSafeString("binding"),
        formatchoice = manifestContent.getSafeObject("choiceList"),
        isSearchable = isSearchable,
        formatType = format
    )