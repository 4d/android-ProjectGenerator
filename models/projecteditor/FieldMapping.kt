data class FieldMapping(
    val binding: String?,
    val choiceList: Any?,  // choiceList can be a JSONObject or a JSONArray
    val isSearchable: Boolean?,
    val name: String?,
    val imageWidth: Int?,
    val imageHeight: Int?
)