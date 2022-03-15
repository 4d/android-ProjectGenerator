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

fun Relation.checkSubRelations(): List<TemplateRelationFiller> {
    val subTemplateRelationFillerList = mutableListOf<TemplateRelationFiller>()
    this.subFields.filter { it.relatedEntities != null }.forEach { oneToManySubRelation ->
        val target = oneToManySubRelation.relatedEntities?.tableNameAdjustment()
        val relationName = "${this.name.fieldAdjustment()}_${oneToManySubRelation.name.fieldAdjustment()}"
        val originalSubRelationName = "${this.name.fieldAdjustment()}.${oneToManySubRelation.name.fieldAdjustment()}"
        val inverseName = oneToManySubRelation.inverseName?.fieldAdjustment()
        if (target != null && inverseName != null) {
            val subTemplateRelationFiller = getSubTemplateRelationFiller(this.source, target, relationName, inverseName, originalSubRelationName)
            subTemplateRelationFillerList.add(subTemplateRelationFiller)
        }
    }
    return subTemplateRelationFillerList
}