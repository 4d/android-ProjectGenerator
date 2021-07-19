data class FieldMapping(
    val binding: String?,
    val choiceList: Any?,  // choiceList can be a JSONObject or a JSONArray
    val type: Any?,  // type can be a String or a JSONArray
    val name: String?,
    val imageWidth: Int?,
    val imageHeight: Int?,
    val tintable: Boolean?
)