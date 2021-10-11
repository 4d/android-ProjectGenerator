data class TemplateTableFiller(
    val name: String,
    val name_original: String,
    val nameCamelCase: String,
    val concat_fields: String,
    val type: String // 'type' is here for Object type which has 'name' Map and 'type' Map<String, Any>
)
