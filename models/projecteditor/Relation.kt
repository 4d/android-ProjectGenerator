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
    println("destBeforeField, source: $source, path: $path")
    var nextTableName = source
    path?.split(".")?.forEach eachPathPart@{
        val pair = checkPath(it, nextTableName, catalogDef)
        println("destBeforeField, pair: $pair")
        nextTableName = pair.first ?: return@eachPathPart
    }
    return nextTableName
}

fun destWithField(catalogDef: CatalogDef, source: String, path: String): String {
    println("destWithField, source: $source, path: $path")
    var nextSource = source
    path.split(".").forEach { part ->
        val relation = catalogDef.relations.find { it.source == nextSource && it.name == part }
        relation?.let {
            nextSource = relation.target
        } ?: run {
            val field = catalogDef.dataModelAliases.find { it.name == nextSource }?.fields?.find { it.name == path.substringAfterLast(".") }
            println("destWithField, returns field : $field")
            return field?.fieldTypeString ?: ""
        }
    }
    return nextSource
}

fun Relation.isNotNativeType(dataModelList: List<DataModel>): Boolean = dataModelList.map { it.name }.contains(this.target)

fun findRelation(dataModelList: List<DataModel>, source: String, field: Field): Relation? {
    dataModelList.find { it.name == source }?.relations?.filter { it.isNotNativeType(dataModelList) }?.find { it.name == field.name }?.let { relation ->
        println("Found relation from name $relation")
        return relation
    } ?: kotlin.run {
        println("No relation found with same name")

        // If simple relation -> path will be null
        dataModelList.find { it.name == source }?.relations?.filter { it.isNotNativeType(dataModelList) }?.find { it.path == field.path }?.let { relation ->
            println("Found relation from path $relation")
            return relation
        } ?: kotlin.run {
            println("No relation found with same path")

            dataModelList.find { it.name == source }?.relations?.filter { it.isNotNativeType(dataModelList) }?.find { it.name == field.path }?.let { relation ->
                println("Found relation from name with path $relation")
                return relation
            }
        }
    }
    return null
}

fun findRelationFromPath(dataModelList: List<DataModel>, source: String, path: String): Relation? {
    dataModelList.find { it.name == source }?.relations?.filter { it.isNotNativeType(dataModelList) }?.find { it.name == path }?.let { relation ->
        println("Found relation from name $relation")
        return relation
    } ?: kotlin.run {
        println("No relation found with same name")

        // If simple relation -> path will be null
        dataModelList.find { it.name == source }?.relations?.filter { it.isNotNativeType(dataModelList) }?.find { it.path == path }?.let { relation ->
            println("Found relation from path $relation")
            return relation
        }
    }
    return null
}

fun getRelationType(catalogDef: CatalogDef, source: String, path: String): RelationType {
    println("getRelationType, source: $source, path: $path")
    var nextSource = source
    path.split(".").forEach { part ->
        val relation = catalogDef.relations.find { it.source == nextSource && it.name == part }
        nextSource = relation?.target ?: ""
        if (relation?.type == RelationType.ONE_TO_MANY)
            return RelationType.ONE_TO_MANY
    }
    return RelationType.MANY_TO_ONE
}

// Pair<String, String> : source, path
fun getFollowingTypeToCreate(catalogDef: CatalogDef, source: String, path: String): Pair<String, String>? {
    println("getFollowingTypeToCreate, source = $source, path = $path")
    val pathList = path.split(".")
    val firstTarget = catalogDef.dataModelAliases.find { it.name == source }?.relations?.find { it.name == pathList.first() }?.target ?: ""
    println("getFollowingTypeToCreate, firstTarget: $firstTarget")
    if (pathList.size == 1) {
        return null
    }
    return Pair(firstTarget, path.substringAfter("."))
}

fun getRelationsToCreate(catalogDef: CatalogDef, source: String, path: String): MutableList<Relation> {
    println("getRelationsToCreate, source: $source, path: $path")
    val newRelationList = mutableListOf<Relation>()

    var nextSource = source
    var nextPath = path
    println("going To enter While")
    println("nextSource = $nextSource")
    println("nextPath = $nextPath")
    while (nextPath.contains(".")) {
        val pair = getFollowingTypeToCreate(catalogDef, nextSource, nextPath) ?: break
        println("pair = $pair")
        val target = destBeforeField(catalogDef, nextSource, nextPath)
        val nextRelation = Relation(
            source = nextSource,
            target = target,
            name = nextPath.relationNameAdjustment(),
            type = getRelationType(catalogDef, nextSource, nextPath),
            subFields = listOf(),
            inverseName = "",
            path = nextPath,
            relation_embedded_return_type = buildRelationEmbeddedReturnType(catalogDef, nextSource, nextPath)
        )
        println("Adding nextRelation : $nextRelation")
        newRelationList.add(nextRelation)
        nextSource = pair.first
        nextPath = pair.second
    }
    println("End of while")
    return newRelationList
}

fun buildRelationEmbeddedReturnType(catalogDef: CatalogDef, source: String, path: String?): String {
    if (path != null) {
        println("buildRelationEmbeddedReturnType, source: $source, path: $path")
        val pathList = path.split(".")
        val firstTarget =
            catalogDef.dataModelAliases.find { it.name == source }?.relations?.find { it.name == pathList.first() }?.target ?: ""
        println("buildRelationEmbeddedReturnType, firstTarget: $firstTarget")
        if (pathList.size == 1) {
            return firstTarget
        }
        val relationName = path.substringAfter(".")
        return getEmbeddedReturnTypeName(firstTarget, relationName)
    }
    return ""
}

fun getEmbeddedReturnTypeName(first: String, second: String): String {
    return first.tableNameAdjustment() + "Relation" + second.relationNameAdjustment().tableNameAdjustment()
}

/**
 * Replace path alias by their own path
 * Returns a Pair of <nextTableSource, path>
 */
fun checkPath(pathPart: String, source: String, catalogDef: CatalogDef): Pair<String?, String> {
    println("checkPath, source: $source, name: $pathPart")

    val relation = catalogDef.relations.firstOrNull { it.source == source && it.name == pathPart }
    println("checkPath, relation: $relation")

    return when {
        relation == null -> {
            // check if it's a field alias
            val field = catalogDef.dataModelAliases.find { it.name == source }?.fields?.find { it.name == pathPart && it.kind == "alias" }
            println("found field is = $field")
            if (field != null) {
                val nextTableName = catalogDef.dataModelAliases.find { it.tableNumber == field.relatedTableNumber }?.name
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

fun Field.getFieldAliasName(dataModelList: List<DataModel>): String {
    if (this.isFieldAlias(dataModelList)) {
        println("getFieldAliasName, aliasField here, field is $this")
        val path = this.path ?: ""
        if (path.contains(".")) {
            var name = ""
            var nextPath = path.substringBeforeLast(".")
            while (nextPath.contains(".")) {

                name += nextPath.relationNameAdjustment() + "."
                println("building name = $name")

                nextPath = nextPath.substringAfter(".")
            }
            val returnName = name + nextPath.relationNameAdjustment() + "." + path.substringAfterLast(".").fieldAdjustment()
            println("getFieldAliasName returnName: $returnName")
            return returnName
        } else {
            return path.fieldAdjustment()
        }
    } else {
        println("getFieldAliasName kept name, ${this.name}")
        println("field is $this")
        return this.name.fieldAdjustment()
    }
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

// REMINDER : kind == "alias" condition removed because alias.FirstName is not an alias kind
fun Field.isFieldAlias(dataModelList: List<DataModel>): Boolean {
    val isFieldAlias = path?.isNotEmpty() == true /*&& kind == "alias"*/ && this.isNativeType(dataModelList)
    println("isFieldAlias [${this.name}]: $isFieldAlias, field : $this")
    return isFieldAlias
}

fun getRelation(field: Field, tableName: String, subFields: List<Field>): Relation? {
    println("getRelation, field: $field")
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