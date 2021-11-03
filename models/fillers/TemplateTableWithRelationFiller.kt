data class TemplateTableWithRelationFiller(val name: String)

fun DataModel.getTemplateTableWithRelationFiller() = TemplateTableWithRelationFiller(name = this.name.tableNameAdjustment())