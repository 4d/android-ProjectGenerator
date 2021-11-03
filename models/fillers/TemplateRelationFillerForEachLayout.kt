import java.util.*

data class TemplateRelationFillerForEachLayout(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val tableNameLowercase: String,
    val associatedViewId: Int,
    val isSubRelation: Boolean
)

fun getTemplateRelationFillerForLayout(source: String, target: String, relationName: String, inverseName: String, viewId: Int): TemplateRelationFillerForEachLayout =
    TemplateRelationFillerForEachLayout(
        relation_source = source.tableNameAdjustment(),
        relation_target = target.tableNameAdjustment(),
        relation_name = relationName,
        inverse_name = inverseName,
        tableNameLowercase = source.dataBindingAdjustment().decapitalize(Locale.getDefault()),
        associatedViewId = viewId,
        isSubRelation = relationName.fieldAdjustment().contains(".")
    )