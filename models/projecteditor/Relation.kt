data class Relation(
    val source: String,
    val target: String,
    val name: String,
    val type: RelationType,
    var subFields: List<Field>,
    val inverseName: String,
    val path: String,
    val relation_embedded_return_type: String
)

fun destBeforeField(catalogDef: CatalogDef, source: String, path: String?): String {
    Log.d("destBeforeField, source: $source, path: $path")
    var nextTableName = source
    path?.split(".")?.forEach eachPathPart@{
        val pair = checkPath(it, nextTableName, catalogDef)
        Log.d("destBeforeField, pair: $pair")
        nextTableName = pair.first ?: return@eachPathPart
    }
    return nextTableName
}

fun getRelationType(catalogDef: CatalogDef, source: String, path: String): RelationType {
    Log.d("getRelationType, source: $source, path: $path")
    var nextSource = source
    path.split(".").forEach { part ->
        val relation = catalogDef.relations.find { it.source == nextSource && it.name == part }
        Log.d("getRelationType, relation = $relation")
        nextSource = relation?.target ?: ""
        if (relation?.type == RelationType.ONE_TO_MANY)
            return RelationType.ONE_TO_MANY
    }
    return RelationType.MANY_TO_ONE
}

// Pair<String, String> : source, path
fun getFollowingTypeToCreate(catalogDef: CatalogDef, source: String, path: String): Pair<String, String>? {
    Log.d("getFollowingTypeToCreate, source = $source, path = $path")
    val pathList = path.split(".")
    val firstTarget = catalogDef.dataModelAliases.find { it.name == source }?.relations?.find { it.name == pathList.first() }?.target ?: ""
    Log.d("getFollowingTypeToCreate, firstTarget: $firstTarget")
    if (pathList.size == 1) {
        return null
    }
    return Pair(firstTarget, path.substringAfter("."))
}

fun getRelationsToCreate(catalogDef: CatalogDef, source: String, path: String): List<Relation> {
    Log.d("getRelationsToCreate, source: $source, path: $path")
    val newRelationList = mutableListOf<Relation>()

    var nextSource = source
    var nextPath = path
    Log.d("going To enter While")
    Log.d("nextSource = $nextSource")
    Log.d("nextPath = $nextPath")
    while (nextPath.contains(".")) {
        val pair = getFollowingTypeToCreate(catalogDef, nextSource, nextPath) ?: break
        Log.d("pair = $pair")
        val nextRelation = Relation(
            source = nextSource,
            target = destBeforeField(catalogDef, nextSource, nextPath),
            name = nextPath.relationAdjustment(),
            type = getRelationType(catalogDef, nextSource, nextPath),
            subFields = listOf(),
            inverseName = "",
            path = nextPath,
            relation_embedded_return_type = buildRelationEmbeddedReturnType(catalogDef, nextSource, nextPath)
        )
        Log.d("Adding nextRelation : $nextRelation")
        newRelationList.add(nextRelation)
        nextSource = pair.first
        nextPath = pair.second
    }
    Log.d("End of while")
    return newRelationList
}

fun buildRelationEmbeddedReturnType(catalogDef: CatalogDef, source: String, path: String?): String {
    if (path != null) {
        Log.d("buildRelationEmbeddedReturnType, source: $source, path: $path")
        val pathList = path.split(".")
        val firstTarget =
            catalogDef.dataModelAliases.find { it.name == source }?.relations?.find { it.name == pathList.first() }?.target ?: ""
        Log.d("buildRelationEmbeddedReturnType, firstTarget: $firstTarget")
        if (pathList.size == 1) {
            return firstTarget
        }
        val relationName = path.substringAfter(".")
        return getEmbeddedReturnTypeName(firstTarget, relationName)
    }
    return ""
    /*var nextTableName = source
    val pathWithoutLast = path?.split(".")?.toMutableList()
    pathWithoutLast?.removeLast()
    pathWithoutLast?.forEach eachPathPart@{
        val pair = checkPath(it, nextTableName, catalogDef)
        if (path == "service.manager.service") {
            Log.d("pair = $pair")
        }
        nextTableName = pair.first ?: return@eachPathPart
    }
    val embeddedSource = nextTableName
    val relationName = path?.split(".")?.lastOrNull() ?: ""
    return if (relationName.isNotEmpty())
        embeddedSource.tableNameAdjustment() + "Relation" + relationName.dataBindingAdjustment()
    else
        ""*/
}

fun getEmbeddedReturnTypeName(first: String, second: String): String {
    return first.tableNameAdjustment() + "Relation" + second.relationAdjustment().tableNameAdjustment()
}

/**
 * Replace path alias by their own path
 * Returns a Pair of <nextTableSource, path>
 */
fun checkPath(pathPart: String, source: String, catalogDef: CatalogDef): Pair<String?, String> {
//    Log.d("checkPath, source: $source, name: $pathPart")

    val relation = catalogDef.relations.firstOrNull { it.source == source && it.name == pathPart }
//    Log.d("checkPath, relation: $relation")

    return when {
        relation == null -> {
            // check if it's a field alias
            val field =
                catalogDef.dataModelAliases.find { it.name == source }?.fields?.find { it.name == pathPart && it.kind == "alias" }
            if (field != null) {
//                Log.d("found field is = $field")
                val nextTableName =
                    catalogDef.dataModelAliases.find { it.tableNumber == field.relatedTableNumber }?.name
                Pair(nextTableName, unAliasPath(field.path, source, catalogDef))
            } else {
                Pair(null, pathPart)
            }
        } // case service.Name
        relation.path.isNotEmpty() -> { // case service.alias
            var composedPath = ""
            relation.path.split(".").forEach { name ->
                val nextPart = checkPath(name, relation.target, catalogDef)
                if (nextPart.first != null)
                    composedPath = if (composedPath.isEmpty())
                        nextPart.second
                    else
                        composedPath + "." + nextPart.second
            }
            Pair(relation.target, composedPath)
        }
        else -> Pair(relation.target, pathPart) // case service
    }
}

fun Field.getFieldAliasName(currentTable: String, dataModelList: List<DataModel>): String {
    val path = this.path ?: ""
    if (this.isFieldAlias(currentTable, dataModelList) && path.isNotEmpty()) {
        Log.d("getFieldAliasName, aliasField here, field is $this")
        if (path.contains(".")) {
            var name = ""
            var nextPath = path.substringBeforeLast(".")
            while (nextPath.contains(".")) {

                name += nextPath.relationAdjustment() + "."
                Log.d("building name = $name")

                nextPath = nextPath.substringAfter(".")
            }
            val returnName = name + nextPath.relationAdjustment() + "." + path.substringAfterLast(".").fieldAdjustment()
            Log.d("getFieldAliasName returnName: $returnName")
            return returnName
        } else {
            return path.fieldAdjustment()
        }
    } else {
        Log.d("getFieldAliasName kept name, ${this.name}")
        Log.d("field is $this")
        return this.name.fieldAdjustment()
    }
}

fun addAliasToDataModel(dataModelList: List<DataModel>, aliasToAdd: Relation) {

    // TODO : HERE WE SHOULD JUST SET SOME RELATIONS FOR TEMPLATE.KT AND RELATIONS.KT AS 'TO_BE_ADDED' WE IF DON'T NEED EVERY
//    val dmRelations = dataModelList.find { dm -> dm.name == aliasToAdd.source }?.relations ?: mutableListOf()
//    dmRelations.add(aliasToAdd)
    Log.d("aliasToAdd after form def reading")
    Log.d("$aliasToAdd")
//    dataModelList.find { dm -> dm.name == aliasToAdd.source }?.relations = dmRelations
}

fun unAliasPath(path: String?, source: String, catalogDef: CatalogDef): String {
    var nextTableName = source
    var newPath = ""
    path?.split(".")?.forEach {
        val pair = checkPath(it, nextTableName, catalogDef)
        nextTableName = pair.first ?: ""
        newPath = if (newPath.isEmpty())
            pair.second
        else
            newPath + "." + pair.second
    }
    return newPath.removeSuffix(".")
}

fun Field.isFieldAlias(currentTable: String, dataModelList: List<DataModel>): Boolean {
    Log.d(
        "isFieldAlias [${this.name}]: ${
            path?.isNotEmpty() == true && kind == "alias" && !this.isNotNativeType(
                dataModelList
            )
        }, field : $this"
    )
    return path?.isNotEmpty() == true && kind == "alias" && !this.isNotNativeType(dataModelList)
//    if (path?.isNotEmpty() == true && this.isNotNativeType(dataModelList)){
//        Log.d("isFieldAlias: path: $path")
//        val aliasTarget = dataModelList.find { it.name == currentTable }?.relations?.find { it.path == path }?.target
//        Log.d("isFieldAlias: target: $aliasTarget")
//        return dataModelList.find { it.name == aliasTarget } == null
//    }
//    return false
}

/*
fun isFieldCatalogAlias(path: String?, currentTable: String, catalogDef: CatalogDef): Boolean {
    Log.d("isFieldCatalogAlias: path: $path")
    if (path == null)
        return false

    val aliasTarget = catalogDef.dataModelAliases.find { it.name == currentTable }?.fields?.find { it.path == path }?.target
    Log.d("isFieldCatalogAlias: target: $aliasTarget")
    return catalogDef.dataModelAliases.find { it.name == aliasTarget } == null && path?.isNotEmpty() == true
}*/

fun getRelation(field: Field, tableName: String, subFields: List<Field>): Relation? {
    Log.d("getRelation, field: $field")
    when (field.kind) {
        "relatedEntity" -> {
            field.relatedDataClass?.let {
                subFields.forEach { subField ->
                    subField.relatedTableNumber = field.relatedTableNumber
                    subField.dataModelId = it
                }
                return Relation(
                    source = tableName,
                    target = it,
                    name = field.name,
                    type = RelationType.MANY_TO_ONE,
                    subFields = subFields,
                    inverseName = field.inverseName ?: "",
                    path = "",
                    relation_embedded_return_type = it
                )
            }
        }
        "relatedEntities" -> {
            field.relatedEntities?.let {
                subFields.forEach { subField -> subField.dataModelId = it }
                return Relation(
                    source = tableName,
                    target = it,
                    name = field.name,
                    type = RelationType.ONE_TO_MANY,
                    subFields = subFields,
                    inverseName = field.inverseName ?: "",
                    path = "",
                    relation_embedded_return_type = it
                )
            }
        }
    }
    return null
}