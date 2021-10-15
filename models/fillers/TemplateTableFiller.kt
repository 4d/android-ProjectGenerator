data class TemplateTableFiller(
    val name: String,
    val name_original: String,
    val nameCamelCase: String,
    val concat_fields: String,
    val concat_relations_many_to_one: String,
    val concat_relations_one_to_many: String,
    val type: String // 'type' is here for Object type which has 'name' Map and 'type' Map<String, Any>
)

fun getTemplateTableFiller(name: String): TemplateTableFiller =
    TemplateTableFiller(
        name = name,
        name_original = name,
        nameCamelCase = name.toLowerCase(),
        concat_fields = "",
        concat_relations_many_to_one = "",
        concat_relations_one_to_many = "",
        type = getTemplateTableFillerType(name)
    )

fun getTemplateTableFillerType(name: String): String =
    when (name) {
        "Map" -> "Map<String, Any>"
        else -> ""
    }

fun DataModel.getTemplateTableFiller(): TemplateTableFiller =
    TemplateTableFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameCamelCase = this.name.dataBindingAdjustment(),
        concat_fields = this.fields?.joinToString { "\"${it.name}\"" } ?: "",
        concat_relations_many_to_one = this.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
            ?.joinToString { "\"${it.name}\"" } ?: "",
        concat_relations_one_to_many = this.relationList?.filter { it.relationType == RelationType.ONE_TO_MANY }
            ?.joinToString { "\"${it.name}\"" } ?: "",
        type = this.name.tableNameAdjustment())