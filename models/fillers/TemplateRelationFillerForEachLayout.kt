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
    val relation_target_camelCase: String,
    val isAlias: Boolean,
    val pathToOneWithoutFirst: String,
    val pathToManyWithoutFirst: String
)

fun getTemplateRelationFillerForLayout(
    source: String,
    target: String,
    relationName: String,
    inverseName: String,
    viewId: Int,
    navbarTitle: String,
    aliasRelation: Relation,
    catalogDef: CatalogDef
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
        relation_target_camelCase = target.dataBindingAdjustment(),
        isAlias = aliasRelation.path.isNotEmpty(),
        pathToOneWithoutFirst = aliasRelation.path.substringAfter("."),
        pathToManyWithoutFirst = getPathToManyWithoutFirst(aliasRelation, catalogDef)
    )

fun getPathToManyWithoutFirst(aliasRelation: Relation, catalogDef: CatalogDef): String {
    Log.d("getPathToManyWithoutFirst, aliasRelation = $aliasRelation")
    var path = ""
    var openedBraces = 0
    val pathList = aliasRelation.path.split(".")
    pathList.forEachIndexed { index, pathPart ->
        Log.d("getPathToManyWithoutFirst, pathPart = $pathPart")
        catalogDef.relations.find { it.source == aliasRelation.source && it.name == path }?.let { relation ->
            Log.d("getPathToManyWithoutFirst, relation = $relation")
            if (index == 0) {
                if (relation.type == RelationType.ONE_TO_MANY) {
                    path = "map { it."
                    openedBraces++
                }
            } else {
                path += pathPart
                if (relation.type == RelationType.ONE_TO_MANY) {
                    path += "?.map { it."
                    openedBraces++
                }
            }
        }
    }
    // remove suffix in case it ends by a 1-N relation
    path = path.removeSuffix("?.map { it.")
    for (i in 0 until openedBraces) {
        path += " }"
    }
    Log.d("final path = $path")
    return path
}

fun getSubRelationInverseName(relationName: String): String = if (relationName.fieldAdjustment().contains("."))
    relationName.fieldAdjustment().split(".").getOrNull(0) ?: ""
else
    ""
