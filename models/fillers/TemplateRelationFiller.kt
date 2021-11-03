data class TemplateRelationFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val inverse_name_cap: String = inverse_name.capitalize()
)

fun Relation.getTemplateRelationFiller(): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = this.source.tableNameAdjustment(),
        relation_target = this.target.tableNameAdjustment(),
        relation_name = this.name.fieldAdjustment(),
        inverse_name = this.inverseName
    )

fun List<TemplateRelationFiller>.getInverseRelationsOneToMany(): List<TemplateRelationFiller> {
    val inverseRelationsOneToMany = mutableListOf<TemplateRelationFiller>()
    this.forEach {
        inverseRelationsOneToMany.add(
            TemplateRelationFiller(
                relation_source = it.relation_target,
                relation_target = it.relation_source,
                relation_name = it.inverse_name,
                inverse_name = it.inverse_name
            )
        )
    }
    return inverseRelationsOneToMany
}