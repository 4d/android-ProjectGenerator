data class Relation(
    val source: String,
    val target: String,
    val name: String,
    val type: RelationType,
    var subFields: List<Field>,
    val inverseName: String,
    val path: String
)

//fun isPathEndingByField(baseCatalogDef: List<DataModelAlias>, dest: String): Boolean =
//    baseCatalogDef.find { it.name == dest } == null

fun destBeforeField(catalogDef: CatalogDef, source: String, path: String?): String {
    var nextTableName = source
    path?.split(".")?.forEach eachPathPart@{
        val pair = it.checkPath(nextTableName, catalogDef)
        nextTableName = pair.first ?: return@eachPathPart
    }
    return nextTableName
}

/**
 * Replace path alias by their own path
 * Returns a Pair of <nextTableSource, path>
 */
fun String.checkPath(source: String, catalogDef: CatalogDef): Pair<String?, String> {
    Log.d("checkPath, source: $source, name: $this")

    val relation = catalogDef.relations.firstOrNull { it.source == source && it.name == this }
    Log.d("checkPath, relation: $relation")

    return when {
        relation == null -> {
            // check if it's a field alias
            val field = catalogDef.dataModelAliases.find { it.name == source }?.fields?.find { it.name == this && it.kind == "alias" }
            if (field != null) {
                Log.d("found field is = $field")
                Pair(null, unAliasPath(field.path, source, catalogDef))
            } else {
                Pair(null, this)
            }
        } // case service.Name
        relation.path.isNotEmpty() -> { // case service.alias
            var composedPath = ""
            relation.path.split(".").forEach { name ->
                val nextPart = name.checkPath(relation.target, catalogDef)
                if (nextPart.first != null)
                    composedPath = composedPath + "." + nextPart.second
            }
            Pair(relation.target, composedPath)
        }
        else -> Pair(relation.target, this) // case service
    }
}

fun Field.getFieldAliasName(): String {
    val path = this.path // manager.service.Name
    if (path?.isNotEmpty() == true) {
        val name: String // managerServiceName
        var subPath = path.substringBeforeLast(".") // manager.service
        if (subPath.contains(".")) {
            subPath = subPath.replace(".", "_") // manager_service
            name = (subPath + "." + path.substringAfterLast(".")).fieldAdjustment() // manager_service.Name
        } else {
            name = path.fieldAdjustment()
        }
        if (this.name != name){
            Log.d("getFieldAliasName changed a name from ${this.name} to $name where path was $path")
        }
        return name
    } else {
        return this.name.fieldAdjustment()
    }
}

fun addAliasToDataModel(dataModelList: List<DataModel>, aliasToAdd: Relation) {
    val dmRelations = dataModelList.find { dm -> dm.name == aliasToAdd.source }?.relations ?: mutableListOf()
    dmRelations.add(aliasToAdd)
    dataModelList.find { dm -> dm.name == aliasToAdd.source }?.relations = dmRelations
}