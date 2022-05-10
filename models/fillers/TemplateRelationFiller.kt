data class TemplateRelationFiller(
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val inverse_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val inverse_name_cap: String = inverse_name.capitalize(),
    val isSubRelation: Boolean,
    val originalSubRelationName: String,
    val relation_source_camelCase: String,
    val relation_target_camelCase: String,
    val relation_name_original: String,
    val isAlias: Boolean,
    val path: String,
    val relation_embedded_return_type: String,
    val key_name: String,
    val firstIsToMany: Boolean,
    val firstTarget: String
)

fun Relation.getTemplateRelationFiller(catalogDef: CatalogDef): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = this.source.tableNameAdjustment(),
        relation_target = this.target.tableNameAdjustment(),
        relation_name = this.name.relationAdjustment(),
        inverse_name = this.inverseName.fieldAdjustment(),
        isSubRelation = false,
        originalSubRelationName = "",
        relation_source_camelCase = this.source.dataBindingAdjustment(),
        relation_target_camelCase = this.target.dataBindingAdjustment(),
        relation_name_original = this.name,
        isAlias = this.path.isNotEmpty(),
        path = this.path,
        relation_embedded_return_type = getEmbeddedReturnType(this),
        key_name = getKeyName(catalogDef,this),
        firstIsToMany = getFirstIsToMany(catalogDef, this),
        firstTarget = getFirstTarget(catalogDef, this)
    )

fun getFirstIsToMany(catalogDef: CatalogDef, relation: Relation): Boolean {
    if (relation.path.isEmpty()) {
        return relation.type == RelationType.ONE_TO_MANY
    } else {
        val firstRelationName = relation.path.split(".")[0]
        catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->
            return firstRelation.type == RelationType.ONE_TO_MANY
        }
    }
    return false
}

fun getFirstTarget(catalogDef: CatalogDef, relation: Relation): String {
    if (relation.path.isEmpty()) {
        return relation.target.tableNameAdjustment()
    } else {
        val firstRelationName = relation.path.split(".")[0]
        catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->
            return firstRelation.target.tableNameAdjustment()
        }
    }
    return ""
}

fun getEmbeddedReturnType(relation: Relation): String {
    return if (relation.path.contains(".")) relation.relation_embedded_return_type else relation.target
}

fun getKeyName(catalogDef: CatalogDef, relation: Relation): String {
    Log.d("getKeyName, relation: $relation")
    var key = ""

    if (relation.path.isEmpty()) {
        key = if (relation.type == RelationType.ONE_TO_MANY)
            relation.inverseName
        else
            relation.name
    } else {
        if (relation.path.count { it == '.' } > 0) {
            Log.d("relation.path contains at least one '.'")
            val firstRelationName = relation.path.split(".")[0]
            // la relation du premier element avant "."
            catalogDef.relations.find { it.source == relation.source && it.name == firstRelationName }?.let { firstRelation ->
                Log.d("firstRelation = $firstRelation")

                key = if (firstRelation.type == RelationType.ONE_TO_MANY)
                    firstRelation.inverseName
                else
                    firstRelation.name
                Log.d("key = $key")
            }
        } else if (relation.path.count { it == '.' } == 0){ // serv
            key = relation.path.fieldAdjustment()
        }
    }
    return key
}

private fun getSubTemplateRelationFiller(firstRelation: Relation, secondRelation: Relation, name: String, inverseName: String, originalSubRelationName: String): TemplateRelationFiller =
    TemplateRelationFiller(
        relation_source = firstRelation.source.tableNameAdjustment(),
        relation_target = secondRelation.target.tableNameAdjustment(),
        relation_name = name.relationAdjustment(),
        inverse_name = inverseName.fieldAdjustment(),
        isSubRelation = true,
        originalSubRelationName = originalSubRelationName,
        relation_source_camelCase = firstRelation.source.dataBindingAdjustment(),
        relation_target_camelCase = secondRelation.target.dataBindingAdjustment(),
        relation_name_original = name,
        isAlias = false,
        path = firstRelation.name + "." + secondRelation.name,
        relation_embedded_return_type = getEmbeddedReturnTypeName(secondRelation.source, secondRelation.name),
        key_name = firstRelation.name,
        firstIsToMany = firstRelation.type == RelationType.ONE_TO_MANY,
        firstTarget = firstRelation.target.tableNameAdjustment()
    )

fun Relation.checkSubRelations(): List<TemplateRelationFiller> {
    Log.d("checkSubRelations !!")
    Log.d("relation is $this")
    val subTemplateRelationFillerList = mutableListOf<TemplateRelationFiller>()
    this.subFields.filter { it.relatedEntities != null }.forEach { oneToManySubRelationField ->
        val target = oneToManySubRelationField.relatedEntities?.tableNameAdjustment()
        val relationName = "${this.name.fieldAdjustment()}_${oneToManySubRelationField.name.fieldAdjustment()}"
        val originalSubRelationName = "${this.name.fieldAdjustment()}.${oneToManySubRelationField.name.fieldAdjustment()}"
        val inverseName = oneToManySubRelationField.inverseName?.fieldAdjustment()
        if (target != null && inverseName != null) {

            getRelation(oneToManySubRelationField, this.target, listOf())?.let { secondRelation ->

                val subTemplateRelationFiller = getSubTemplateRelationFiller(this, secondRelation, relationName, inverseName, originalSubRelationName)
                Log.d("CheckSubrelations, SHOULD ADD :")
                Log.d("subTemplateRelationFiller: $subTemplateRelationFiller")
//                subTemplateRelationFillerList.add(subTemplateRelationFiller)
            }
        }
    }
    return subTemplateRelationFillerList
}

data class TemplateRelationForRoomFiller(
    val className: String,
    val relation_source: String,
    val relation_target: String,
    val relation_name: String,
    val relation_name_cap: String = relation_name.capitalize(),
    val relation_source_camelCase: String,
    val key_name: String,
    val relation_embedded_return_type: String,
    val firstIsToMany: Boolean,
    val relation_part_name: String,
    val firstTarget: String
)

fun Relation.getTemplateRelationForRoomFiller(catalogDef: CatalogDef): TemplateRelationForRoomFiller? {
    Log.d("getTemplateRelationForRoomFiller")
    Log.d("relation: $this")
    if (this.path.isEmpty()) {
        val key = if (this.type == RelationType.ONE_TO_MANY)
            this.inverseName
        else
            this.name
        return TemplateRelationForRoomFiller(
            className = getEmbeddedReturnTypeName(source, name),
            relation_source = source,
            relation_target = target,
            relation_name = name,
            relation_source_camelCase = source.fieldAdjustment(),
            key_name = key,
            relation_embedded_return_type = target,
            firstIsToMany = this.type == RelationType.ONE_TO_MANY,
            relation_part_name = name,
            firstTarget = target
        )
    } else {
        if (this.path.count { it == '.' } > 0) {
            Log.d("path contains at least one '.'")
            val firstRelationName = this.path.split(".")[0]
            Log.d("firstRelationName = $firstRelationName")

            // la relation du premier element avant "."
            catalogDef.relations.find { it.source == this.source && it.name == firstRelationName }?.let { firstRelation ->
                Log.d("firstRelation: $firstRelation")
                val key = if (firstRelation.type == RelationType.ONE_TO_MANY)
                    firstRelation.inverseName
                else
                    firstRelation.name

                Log.d("key = $key")
                return TemplateRelationForRoomFiller(
                    className = getEmbeddedReturnTypeName(source, name),
                    relation_source = source,
                    relation_target = target,
                    relation_name = name,
                    relation_source_camelCase = source.fieldAdjustment(),
                    key_name = key,
                    relation_embedded_return_type = getEmbeddedReturnTypeName(firstRelation.target, this.path.substringAfter(".")),
                    firstIsToMany = firstRelation.type == RelationType.ONE_TO_MANY,
                    relation_part_name = if (this.path.contains(".")) this.path.relationAdjustment() else this.path.fieldAdjustment(),
                    firstTarget = getFirstTarget(catalogDef, this)
                )
            }
        }
    }
    return null
}