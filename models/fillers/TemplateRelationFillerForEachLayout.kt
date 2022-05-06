import java.util.*

data class TemplateRelationFillerForEachLayout(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val tableNameLowercase: String,
    val associatedViewId: Int,
    val isSubRelation: Boolean,
    val subRelation_inverse_name: String,
    val navbarTitle: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String,
    val isAlias: Boolean,
    val path: String,
    val pathToOneWithoutFirst: String,
    val pathToManyWithoutFirst: String
)

fun getTemplateRelationFillerForLayout(
    source: String,
    target: String,
    relationName: String,
    viewId: Int,
    navbarTitle: String,
    aliasRelation: Relation,
    catalogDef: CatalogDef
): TemplateRelationFillerForEachLayout =
    TemplateRelationFillerForEachLayout(
        relation_source = source.tableNameAdjustment(),
        relation_target = target.tableNameAdjustment(),
        relation_name = relationName.relationAdjustment(),
        tableNameLowercase = source.dataBindingAdjustment().decapitalize(Locale.getDefault()),
        associatedViewId = viewId,
        isSubRelation = relationName.fieldAdjustment().contains("."),
        subRelation_inverse_name = getSubRelationInverseName(relationName),
        navbarTitle = navbarTitle,
        relation_source_camelCase = source.dataBindingAdjustment(),
        relation_target_camelCase = target.dataBindingAdjustment(),
        isAlias = aliasRelation.path.contains("."),
        path = aliasRelation.path.ifEmpty { aliasRelation.name },
        pathToOneWithoutFirst = aliasRelation.path.substringAfter("."),
        pathToManyWithoutFirst = getPathToManyWithoutFirst(aliasRelation, catalogDef)
    )

fun getPathToManyWithoutFirst(aliasRelation: Relation, catalogDef: CatalogDef): String {
    Log.d("getPathToManyWithoutFirst, aliasRelation = $aliasRelation")
    var path = ""
    val pathList = aliasRelation.path.split(".")
    var nextSource = aliasRelation.source
    var previousRelationType = RelationType.MANY_TO_ONE
    var tmpNextPath = aliasRelation.path
    pathList.forEachIndexed { index, pathPart ->
        Log.d("getPathToManyWithoutFirst, pathPart = $pathPart")
        catalogDef.relations.find { it.source == nextSource && it.name == pathPart }?.let { relation ->
            Log.d("getPathToManyWithoutFirst, relation = $relation")
            if (index > 0) {
                if (path.isNotEmpty())
                    path += "?."

                Log.d("tmpNextPath = $tmpNextPath")
                if (previousRelationType == RelationType.ONE_TO_MANY) {
                    path += "map { it.${tmpNextPath.relationAdjustment()}"
                } else {
                    path += tmpNextPath.relationAdjustment()
                }
            }
            previousRelationType = relation.type
            nextSource = relation.target
            tmpNextPath = tmpNextPath.substringAfter(".")
        }
        Log.d("path building : $path")
    }
    // remove suffix in case it ends by a 1-N relation
    path = path.removeSuffix("?.map { it.")
    repeat(path.count { it == '{' }) {
        path += " }"
    }
    Log.d("final path = $path")
    return path
}

fun getSubRelationInverseName(relationName: String): String = if (relationName.fieldAdjustment().contains("."))
    relationName.fieldAdjustment().split(".").getOrNull(0) ?: ""
else
    ""
