data class TemplateRelationFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String
)

fun Relation.getTemplateRelationFiller(): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = this.source.tableNameAdjustment(),
        relation_target = this.target.tableNameAdjustment(),
        relation_name = this.name.fieldAdjustment(),
        inverse_name = this.inverseName
    )