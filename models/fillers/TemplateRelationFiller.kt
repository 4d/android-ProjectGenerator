data class TemplateRelationFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val inverse_name_cap: String = inverse_name.capitalize(),
    val isSubRelation: Boolean,
    val originalSubRelationName: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String,
    val relation_name_original: String
)

fun Relation.getTemplateRelationFiller(): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = this.source.tableNameAdjustment(),
        relation_target = this.target.tableNameAdjustment(),
        relation_name = this.name.fieldAdjustment(),
        inverse_name = this.inverseName.fieldAdjustment(),
        isSubRelation = false,
        originalSubRelationName = "",
        relation_source_camelCase = this.source.dataBindingAdjustment(),
        relation_target_camelCase = this.target.dataBindingAdjustment(),
        relation_name_original = this.name
    )

fun getSubTemplateRelationFiller(source: String, target: String, name: String, inverseName: String, originalSubRelationName: String): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = source.tableNameAdjustment(),
        relation_target = target.tableNameAdjustment(),
        relation_name = name.fieldAdjustment(),
        inverse_name = inverseName.fieldAdjustment(),
        isSubRelation = true,
        originalSubRelationName = originalSubRelationName,
        relation_source_camelCase = source.dataBindingAdjustment(),
        relation_target_camelCase = target.dataBindingAdjustment(),
        relation_name_original = name
    )

fun Relation.checkSubRelations(dataModelList: List<DataModel>): List<TemplateRelationFiller> {
    val subTemplateRelationFillerList = mutableListOf<TemplateRelationFiller>()
    this.subFields.filter { it.relatedEntities != null }.forEach { oneToManySubRelation ->
        val source = dataModelList.find { it.id == "${oneToManySubRelation.relatedTableNumber}" }?.name?.tableNameAdjustment()
        val target = oneToManySubRelation.relatedEntities?.tableNameAdjustment()
        val relationName = "${this.name.fieldAdjustment()}_${oneToManySubRelation.name.fieldAdjustment()}"
        val originalSubRelationName = "${this.name.fieldAdjustment()}.${oneToManySubRelation.name.fieldAdjustment()}"
        val inverseName = oneToManySubRelation.inverseName?.fieldAdjustment()
        if (source != null && target != null && inverseName != null) {
            val subTemplateRelationFiller = getSubTemplateRelationFiller(source, target, relationName, inverseName, originalSubRelationName)
            subTemplateRelationFillerList.add(subTemplateRelationFiller)
            Log.d("Adding subTemplateRelationFiller = $subTemplateRelationFiller")
        }
    }
    return subTemplateRelationFillerList
}

fun List<TemplateRelationFiller>.getInverseRelationsOneToMany(): List<TemplateRelationFiller> {
    val inverseRelationsOneToMany = mutableListOf<TemplateRelationFiller>()
    this.forEach {
        inverseRelationsOneToMany.add(
            TemplateRelationFiller(
                relation_source = it.relation_target,
                relation_target = it.relation_source,
                relation_name = it.inverse_name,
                inverse_name = it.inverse_name,
                isSubRelation = it.isSubRelation,
                originalSubRelationName = it.originalSubRelationName,
                relation_source_camelCase = it.relation_source_camelCase,
                relation_target_camelCase = it.relation_target_camelCase,
                relation_name_original = it.inverse_name
            )
        )
    }
    return inverseRelationsOneToMany
}