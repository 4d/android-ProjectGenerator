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
        else -> name
    }

fun DataModel.getTemplateTableFiller(dataModelList: List<DataModel>): TemplateTableFiller {

    val manyToOneRelationList = mutableListOf<String>()
    val oneToManyRelationList = mutableListOf<String>()

    // Will add many-to-one's relations
    this.relations?.filter { it.type == RelationType.MANY_TO_ONE }?.forEach { relation ->
        manyToOneRelationList.add(relation.name)
        relation.subFields.forEach { subField ->
            if (subField.isRelation()) {
                dataModelList.find { it.name == relation.target }?.relations?.find { it.name == subField.name }?.let { subRelation ->
                    if (subRelation.type == RelationType.MANY_TO_ONE) {
                        manyToOneRelationList.add("${relation.name}.${subField.name}")
                    } else {
                        oneToManyRelationList.add("${relation.name}.${subField.name}")
                    }
                }
            }
        }
    }

    this.relations?.filter { it.type == RelationType.ONE_TO_MANY }?.forEach { relation ->
        oneToManyRelationList.add(relation.name)
    }

    return  TemplateTableFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameCamelCase = this.name.dataBindingAdjustment(),
        concat_fields = this.fields?.joinToString { "\"${it.name}\"" } ?: "",
        concat_relations_many_to_one = manyToOneRelationList.joinToString { "\"$it\"" },
        concat_relations_one_to_many = oneToManyRelationList.joinToString { "\"$it\"" },
        type = this.name.tableNameAdjustment())
}