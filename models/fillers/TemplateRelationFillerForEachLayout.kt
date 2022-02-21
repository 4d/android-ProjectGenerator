import java.util.*

data class TemplateRelationFillerForEachLayout(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val tableNameLowercase: String,
    val associatedViewId: Int,
    val isSubRelation: Boolean,
    val subRelation_inverse_name: String,
    val navbarTitle: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String
)

fun getTemplateRelationFillerForLayout(
    source: String,
    target: String,
    relationName: String,
    inverseName: String,
    viewId: Int,
    navbarTitle: String
): TemplateRelationFillerForEachLayout =
    TemplateRelationFillerForEachLayout(
        relation_source = source.tableNameAdjustment(),
        relation_target = target.tableNameAdjustment(),
        relation_name = relationName.fieldAdjustment(),
        inverse_name = inverseName.fieldAdjustment(),
        tableNameLowercase = source.dataBindingAdjustment().decapitalize(Locale.getDefault()),
        associatedViewId = viewId,
        isSubRelation = relationName.fieldAdjustment().contains("."),
        subRelation_inverse_name = getSubRelationInverseName(relationName),
        navbarTitle = navbarTitle,
        relation_source_camelCase = source.dataBindingAdjustment(),
        relation_target_camelCase = target.dataBindingAdjustment()
    )

fun getSubRelationInverseName(relationName: String): String = if (relationName.fieldAdjustment().contains("."))
    relationName.fieldAdjustment().split(".").getOrNull(0) ?: ""
else
    ""
