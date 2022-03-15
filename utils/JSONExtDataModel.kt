import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.EMPTY_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.FILTER_KEY
import ProjectEditorConstants.FORMAT_KEY
import ProjectEditorConstants.ICON_KEY
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.INVERSENAME_KEY
import ProjectEditorConstants.ISTOMANY_KEY
import ProjectEditorConstants.KIND_KEY
import ProjectEditorConstants.LABEL_KEY
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.RELATEDDATACLASS_KEY
import ProjectEditorConstants.RELATEDENTITIES_KEY
import ProjectEditorConstants.RELATEDTABLENUMBER_KEY
import ProjectEditorConstants.SHORTLABEL_KEY
import ProjectEditorConstants.STRING_KEY
import ProjectEditorConstants.STRING_TYPE
import ProjectEditorConstants.VALIDATED_KEY
import org.json.JSONObject

fun JSONObject.getDataModelList(catalogDef: CatalogDef, isCreateDatabaseCommand: Boolean = false): List<DataModel> {
    val dataModelList = mutableListOf<DataModel>()
    val dataModels = if (isCreateDatabaseCommand)
        this.getSafeObject(DATAMODEL_KEY)
    else
        this.getSafeObject(PROJECT_KEY)?.getSafeObject(DATAMODEL_KEY)

    val fieldToAddList = mutableListOf<FieldToAdd>()

    val aliasToHandleList = mutableListOf<Field>()

    dataModels?.keys()?.forEach { keyDataModel ->

        val newDataModelJSONObject = dataModels.getSafeObject(keyDataModel.toString())

        newDataModelJSONObject?.getSafeObject(EMPTY_KEY)?.getSafeString(NAME_KEY)?.let { dataModelName ->

            // Remove any slave dataModel with same name added before, save its fields
            val savedFields = mutableListOf<Field>()
            val savedRelations = mutableListOf<Relation>()
            dataModelList.find { it.name == dataModelName }?.fields?.let {
                savedFields.addAll(it)
            }
            dataModelList.find { it.name == dataModelName }?.relations?.let {
                savedRelations.addAll(it)
            }
            if (dataModelList.removeIf { dataModel -> dataModel.name == dataModelName }) {
                Log.d("DataModel removed from list : $dataModelName")
            }

            val newDataModel = DataModel(id = keyDataModel.toString(), name = dataModelName, isSlave = false)
            Log.d("newDataModel.name : $dataModelName")
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(LABEL_KEY)?.let { newDataModel.label = it }
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(SHORTLABEL_KEY)
                ?.let { newDataModel.shortLabel = it }
            var missingIcon = true
            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(ICON_KEY)?.let { iconPath ->
                if (iconPath.contains(".")) {
                    newDataModel.iconPath = correctIconPath(iconPath)
                    missingIcon = false
                }
            }

            if (missingIcon) {
                newDataModel.iconPath = "nav_icon_${newDataModel.id}"
            }

            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeObject(FILTER_KEY)?.let {
                if (it.getSafeBoolean(VALIDATED_KEY) == true)
                    newDataModel.query = it.getSafeString(STRING_KEY)?.replace("\"", "'")
            }

            val fieldList = mutableListOf<Field>()
            val relationList = mutableListOf<Relation>()

            Log.d("Checking fields")

            newDataModelJSONObject.keys().forEach eachKeyField@{ keyField ->
                if (keyField !is String) return@eachKeyField
                if (keyField != EMPTY_KEY) {
                    val newFieldJSONObject: JSONObject? = newDataModelJSONObject.getSafeObject(keyField.toString())
                    val field: Field? = newFieldJSONObject?.getDataModelField(keyField, keyDataModel)

                    field?.let {

                        if (field.kind == "alias") {
                            aliasToHandleList.add(field)
                        } else {
                            Log.d("field.name : ${field.name}")
                            it.isSlave = false
                            fieldList.add(it)
                            val subFields: List<Field> = newFieldJSONObject.getSubFields()
                            getRelation(it, dataModelName, subFields)?.let { relation ->
                                Log.d("relation.name : ${relation.name}")
                                relationList.add(relation)

                                fieldList.remove(field)
                                if (relation.type == RelationType.MANY_TO_ONE) {

                                    val relationKeyField = buildNewKeyField(relation.name)
                                    fieldList.add(relationKeyField)
                                } else {
                                    val relationSizeField = buildNewSizeField(relation.name)
                                    fieldList.add(relationSizeField)

                                    Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                                    it.inverseName?.let { inverseName ->
                                        val newField = Field(
                                            name = inverseName,
                                            inverseName = it.name,
                                            relatedDataClass = dataModelName,
                                            fieldTypeString = dataModelName,
                                            relatedTableNumber = keyDataModel.toString().toIntOrNull(),
                                            relatedEntities = null,
                                            variableType = VariableType.VAR.string
                                        )
                                        Log.d("newField.name: ${newField.name}")
                                        getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                            Log.d("newRelation.name : ${newRelation.name}")
                                            val newKeyField = buildNewKeyField(newRelation.name)

                                            val fieldToAdd = FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                            fieldToAddList.add(fieldToAdd)
                                        }
                                    }
                                }
                            }

                            Log.d("Check if there is a slave table to add")

                            newFieldJSONObject.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
                                newFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->
                                    val slaveDataModel = DataModel(
                                        id = relatedTableNumber.toString(),
                                        name = relatedDataClass,
                                        isSlave = true
                                    )
                                    Log.d("slaveDataModel.name : $relatedDataClass")

                                    val slaveFieldList = mutableListOf<Field>()
                                    val slaveRelationList = mutableListOf<Relation>()

                                    newFieldJSONObject.keys().forEach eachSlaveKeyField@{ slaveKeyField ->
                                        if (slaveKeyField !is String) return@eachSlaveKeyField
                                        val newSlaveFieldJSONObject =
                                            newFieldJSONObject.getSafeObject(slaveKeyField.toString())
                                        val slaveField = newSlaveFieldJSONObject?.getDataModelField(
                                            slaveKeyField,
                                            relatedTableNumber.toString()
                                        )
                                        Log.d("slaveField.name : ${slaveField?.name}")
                                        slaveField?.let { field ->

                                            if (field.kind == "alias") {
                                                aliasToHandleList.add(field)
                                            } else {
                                                field.isSlave = true
                                                slaveFieldList.add(field)
                                                val slaveSubFields: List<Field> = newSlaveFieldJSONObject.getSubFields()
                                                getRelation(field, relatedDataClass, slaveSubFields)?.let { relation ->
                                                    Log.d("slave relation.name : ${relation.name}")
                                                    slaveRelationList.add(relation)

                                                    slaveFieldList.remove(field)
                                                    if (relation.type == RelationType.MANY_TO_ONE) {

                                                        val relationKeyField = buildNewKeyField(relation.name)
                                                        slaveFieldList.add(relationKeyField)
                                                    } else {
                                                        val relationSizeField = buildNewSizeField(relation.name)
                                                        fieldList.add(relationSizeField)

                                                        Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                                                        it.inverseName?.let {
                                                            val newField = Field(
                                                                name = relation.inverseName,
                                                                inverseName = relation.name,
                                                                relatedDataClass = relatedDataClass,
                                                                fieldTypeString = relatedDataClass,
                                                                relatedTableNumber = relatedTableNumber,
                                                                relatedEntities = null,
                                                                variableType = VariableType.VAR.string
                                                            )
                                                            Log.d("slave newField.name : ${newField.name}")
                                                            getRelation(
                                                                newField,
                                                                relation.target,
                                                                listOf()
                                                            )?.let { newRelation ->
                                                                Log.d("slave newRelation.name : ${newRelation.name}")
                                                                val newKeyField = buildNewKeyField(newRelation.name)

                                                                val fieldToAdd = FieldToAdd(
                                                                    relation.target,
                                                                    newField,
                                                                    newKeyField,
                                                                    newRelation
                                                                )
                                                                fieldToAddList.add(fieldToAdd)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Log.d("Checking if we already added this dataModel")

                                    val dataModelIndex =
                                        dataModelList.indexOfFirst { dataModel -> dataModel.name == relatedDataClass } // -1 if not found
                                    when {
                                        dataModelIndex != -1 -> { // in case we added this dataModel already
                                            slaveFieldList.forEach { slaveField ->
                                                if (dataModelList[dataModelIndex].fields?.find { field -> field.name == slaveField.name } == null) {
                                                    dataModelList[dataModelIndex].fields?.add(slaveField)
                                                }
                                            }
                                            slaveRelationList.forEach { slaveRelation ->
                                                if (dataModelList[dataModelIndex].relations?.find { relation -> relation.name == slaveRelation.name } == null) {
                                                    dataModelList[dataModelIndex].relations?.add(slaveRelation)
                                                }
                                            }
                                        }
                                        relatedDataClass == dataModelName -> { // current table has a relation of its own type
                                            savedFields.addAll(slaveFieldList)
                                            savedRelations.addAll(slaveRelationList)
                                        }
                                        else -> {
                                            slaveDataModel.fields = slaveFieldList
                                            slaveDataModel.relations = slaveRelationList
                                            dataModelList.add(slaveDataModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Log.d("Add savedFields from another definition of this table (in a relation)")
            savedFields.forEach { savedField ->
                if (!fieldList.map { it.name }.contains(savedField.name)) {
                    savedField.isSlave = true
                    fieldList.add(savedField)
                }
            }
            Log.d("Add savedRelations from another definition of this table (in a relation)")
            savedRelations.forEach { savedRelation ->
                if (!relationList.map { it.name }.contains(savedRelation.name)) {
                    relationList.add(savedRelation)
                }
            }

            newDataModel.fields = fieldList
            newDataModel.relations = relationList
            dataModelList.add(newDataModel)
        }
    }

    Log.d("Checking aliases")
    aliasToHandleList.forEach { aliasField ->

        Log.d("aliasField: $aliasField")

//        if (aliasField.fieldTypeString != EMPTY_TYPE) {
//            , alors faire une relation n1 dont le type est String)

//            dataModelList.handleAliasRelation(aliasField)

//        } else {

            aliasField.dataModelId?.let { dataModelId ->
                Log.d("dataModelId: $dataModelId")

                catalogDef.dataModelAliases.find { it.tableNumber.toString() == dataModelId }?.let { dataModelAlias ->

                    dataModelAlias.fields.find { it.name == aliasField.name }?.let { catalogField ->
                        Log.d("catalogField: $catalogField")

                        var nextTableName: String? = dataModelAlias.name

                        aliasField.path?.split(".")?.forEach { relationName ->
                            Log.d("nextTableName: $nextTableName")
                            Log.d("relationName: $relationName")
                            nextTableName = dataModelList.handleAlias(nextTableName, relationName, catalogDef) { fieldToAdd ->
                                fieldToAddList.add(fieldToAdd)
                            }
                        }

                        catalogField.relatedDataClass?.let { relatedDataClass ->
                            Log.d("relatedDataClass: $relatedDataClass")

                            val newRelationList = dataModelList.find { it.name == dataModelAlias.name }?.relations ?: mutableListOf()
                            val aliasRelation = Relation(
                                source = dataModelAlias.name,
                                target = relatedDataClass,
                                name = aliasField.name,
                                type = if (catalogField.isToMany == true) RelationType.ONE_TO_MANY else RelationType.MANY_TO_ONE,
                                subFields = listOf(),
                                inverseName = "",
                                path = catalogField.path ?: ""
                            )

                            newRelationList.add(aliasRelation)
                            dataModelList.find { it.name == dataModelAlias.name }?.relations = newRelationList
                        }
                    }
                }
            }
//        }
    }

    Log.d("Sanity check for missing DataModel definition")
    dataModelList.forEach { dataModel ->
        val sanityFieldList = mutableListOf<Field>()
        dataModel.fields?.forEach { field ->
            Log.d("field : $field")
            field.relatedTableNumber?.let { relatedTableNumber -> // relation
                if (dataModelList.map { it.id }.contains(relatedTableNumber.toString())) {
                    sanityFieldList.add(field)
                } else {
                    Log.e("Excluding unknown field $field")
                }
            } ?: kotlin.run { // not relation
                sanityFieldList.add(field)
            }
        }
        Log.d("DataModel ${dataModel.name}, previous fields size = ${dataModel.fields?.size}, new size = ${sanityFieldList.size}")
        dataModel.fields = sanityFieldList

        val sanityRelationList = mutableListOf<Relation>()
        dataModel.relations?.forEach { relation ->
            if (dataModelList.map { it.name }.contains(relation.target)) {

                val sanitySubFieldList = mutableListOf<Field>()
                relation.subFields.forEach { subField ->
                    subField.relatedTableNumber?.let { relatedTableNumber -> // relation
                        if (dataModelList.map { it.id }.contains(relatedTableNumber.toString())) {
                            sanitySubFieldList.add(subField)
                        } else {
                            Log.e("Excluding unknown subField $subField")
                        }
                    } ?: kotlin.run { // not relation
                        sanitySubFieldList.add(subField)
                    }
                }
                Log.d("Relation ${relation.name} from dataModel ${dataModel.name}, previous subField size = ${relation.subFields.size}, new size = ${sanitySubFieldList.size}")
                relation.subFields = sanitySubFieldList

                sanityRelationList.add(relation)
            } else {
                Log.e("Excluding unknown relation $relation")
            }
        }
        Log.d("dataModel ${dataModel.name}, previous relationList size = ${dataModel.relations?.size}, new size = ${sanityRelationList.size}")
        dataModel.relations = sanityRelationList
    }

    fieldToAddList.forEach { fieldToAdd ->
        Log.d("Field.name to add if not present: $fieldToAdd")
        dataModelList.find { it.name == fieldToAdd.targetTable }?.let { dm ->
            dm.fields?.let { dmFields ->
                if (!dmFields.map { it.name }.contains(fieldToAdd.field.name)) {
                    dmFields.add(fieldToAdd.field)
                }
                if (!dmFields.map { it.name }.contains(fieldToAdd.keyField.name)) {
                    dmFields.add(fieldToAdd.keyField)
                }
            }
            dm.relations?.let { dmRelations ->
                if (!dmRelations.map { it.name }.contains(fieldToAdd.relation.name)) {
                    dmRelations.add(fieldToAdd.relation)
                }
            }
        }
    }

    Log.d("Reordering fields")
    dataModelList.forEach {
        it.reOrderFields()
    }

    dataModelList.logDataModel()

    return dataModelList
}

fun List<DataModel>.logDataModel() {
    this.forEach { dm ->
        Log.d("DM name : ${dm.name}")
        Log.d("- FIELDS -------------------")
        dm.fields?.forEach { field ->
            Log.d("[${field.name}] : $field")
        }
        Log.d("- RELATIONS -------------------")
        dm.relations?.forEach { relation ->
            Log.d("[${relation.name}] : $relation")
        }
        Log.d("/////////////////////////////////////////////////////////////////////////////////")
    }
}

/**
 * Function to put relations at the end of the field lists
 */
fun DataModel.reOrderFields() {
    this.fields?.sortWith(compareBy { it.name.startsWith("__") && it.name.endsWith("Key") })
    this.fields?.sortWith(compareBy { it.name.startsWith("__") && it.name.endsWith("Size") })
    this.fields?.sortWith(nullsLast(compareBy { it.inverseName }))
}

//fun MutableList<DataModel>.handleAliasRelation(aliasField: Field) {
//// only N-1 relation, like service.Name
//    aliasField.dataModelId?.let { dataModelId ->
//
//        val sourceDM: DataModel? = this.find { it.id == dataModelId }
//        if (sourceDM == null) {
//
//        } else {
//
//            aliasField.relatedDataClass?.let { source ->
//                Relation(
//                    source = source,
//                    target = field.relatedDataClass,
//                    name = field.name,
//                    type = RelationType.MANY_TO_ONE,
//                    subFields = listOf(),
//                    inverseName = "",
//                    path = aliasField.path ?: ""
//                )
//
//            }
//
//
//
//            if (sourceDM.relations?.find { it.name == relation.name && it.source == relation.source } == null) {
//                if (sourceDM.relations == null) {
//                    sourceDM.relations = mutableListOf(relation)
//                } else {
//                    sourceDM.relations?.add(relation)
//                }
//            }
//
//        }
//    }
//}

fun MutableList<DataModel>.handleAlias(nextTableName: String?, relationName: String, catalogDef: CatalogDef, fieldToAddCallback: (fieldToAdd: FieldToAdd) -> Unit): String? {

    var returnNextTableName: String? = null
    val sourceDM: DataModel? = this.find { it.name == nextTableName }
    if (sourceDM == null) {
        Log.d("sourceDM $nextTableName doesn't exist")

        // dataModel does not exists, create one from catalog def
        catalogDef.baseCatalogDef.find { it.name == nextTableName }?.let { dmAlias ->
            Log.d("dmAlias: $dmAlias")
            dmAlias.fields.find { it.name == relationName }?.let { fieldCatalog -> // Name
                Log.d("fieldCatalog: $fieldCatalog")
                val newDM = DataModel(id = dmAlias.tableNumber.toString(), name = dmAlias.name, isSlave = true)
                val field = fieldCatalog.convertToField()
                val fieldList = mutableListOf<Field>()

                if (fieldCatalog.kind == null) {
                    fieldList.add(field)
                } else {
                    getRelation(field, dmAlias.name, listOf())?.let { relation ->
                        returnNextTableName = relation.target
                        Log.d("relation.name : ${relation.name}")
                        if (newDM.relations?.find { it.name == relation.name && it.source == relation.source } == null)
                            newDM.relations = mutableListOf(relation)
                        if (relation.type == RelationType.MANY_TO_ONE) {

                            val relationKeyField = buildNewKeyField(relation.name)
                            fieldList.add(relationKeyField)
                        } else {

                            val relationSizeField = buildNewSizeField(relation.name)
                            fieldList.add(relationSizeField)

                            Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                            field.inverseName?.let { inverseName ->
                                val newField = Field(
                                    name = inverseName,
                                    inverseName = field.name,
                                    relatedDataClass = dmAlias.name,
                                    fieldTypeString = dmAlias.name,
                                    relatedTableNumber = dmAlias.tableNumber,
                                    relatedEntities = null,
                                    variableType = VariableType.VAR.string
                                )
                                Log.d("newField.name: ${newField.name}")
                                getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                    Log.d("newRelation.name : ${newRelation.name}")
                                    val newKeyField = buildNewKeyField(newRelation.name)

                                    val fieldToAdd =
                                        FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                    fieldToAddCallback(fieldToAdd)
                                }
                            }
                        }
                    }
                }
                newDM.fields = fieldList
                this.add(newDM)
            }
        }
    } else {
        Log.d("sourceDM $nextTableName already exists")
        // dataModel exists
        catalogDef.baseCatalogDef.find { it.name == nextTableName }?.let { dmAlias ->
            Log.d("dmAlias: $dmAlias")
            dmAlias.fields.find { it.name == relationName }?.let { fieldCatalog ->
                Log.d("fieldCatalog: $fieldCatalog")
                val newList = sourceDM.fields?.toMutableList() ?: mutableListOf()
                val field = fieldCatalog.convertToField()

                if (fieldCatalog.kind == null) {
                    newList.add(field)
                } else {
                    if (field.kind == "alias") {
                     // .... ?
                    }
                    getRelation(field, dmAlias.name, listOf())?.let { relation ->
                        returnNextTableName = relation.target
                        Log.d("relation.name : ${relation.name}")
                        if (sourceDM.relations?.find { it.name == relation.name && it.source == relation.source } == null) {
                            if (sourceDM.relations == null) {
                                sourceDM.relations = mutableListOf(relation)
                            } else {
                                sourceDM.relations?.add(relation)
                            }
                        }
                        if (relation.type == RelationType.MANY_TO_ONE) {
                            val relationKeyField = buildNewKeyField(relation.name)
                            newList.takeIf { newList.find { it.name == relationKeyField.name } == null }
                                ?.add(relationKeyField)
                        } else {
                            val relationSizeField = buildNewSizeField(relation.name)
                            newList.takeIf { newList.find { it.name == relationSizeField.name } == null }
                                ?.add(relationSizeField)

                            Log.d("One to many relation, need to add the inverse many to one relation to its Entity definition")
                            field.inverseName?.let { inverseName ->
                                val newField = Field(
                                    name = inverseName,
                                    inverseName = field.name,
                                    relatedDataClass = dmAlias.name,
                                    fieldTypeString = dmAlias.name,
                                    relatedTableNumber = dmAlias.tableNumber,
                                    relatedEntities = null,
                                    variableType = VariableType.VAR.string
                                )
                                Log.d("newField.name: ${newField.name}")
                                getRelation(newField, relation.target, listOf())?.let { newRelation ->
                                    Log.d("newRelation.name : ${newRelation.name}")
                                    val newKeyField = buildNewKeyField(newRelation.name)

                                    val fieldToAdd =
                                        FieldToAdd(relation.target, newField, newKeyField, newRelation)
                                    fieldToAddCallback(fieldToAdd)
                                }
                            }
                        }
                    }
                }
                sourceDM.fields = newList
            }
        }
    }
    return returnNextTableName
}

fun JSONObject?.getDataModelField(keyField: String, dataModelId: String? = null): Field {
    val field = Field(name = "")
    this?.getSafeString(LABEL_KEY)?.let { field.label = it }
    this?.getSafeString(SHORTLABEL_KEY)?.let { field.shortLabel = it }
    this?.getSafeInt(FIELDTYPE_KEY).let { field.fieldType = it }
    this?.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { field.relatedTableNumber = it }
    this?.getSafeString(INVERSENAME_KEY)?.let { field.inverseName = it }
    this?.getSafeString(NAME_KEY)?.let { fieldName -> // BASIC FIELD
        field.name = fieldName
        field.id = keyField
        field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
    }
    this?.getSafeString(KIND_KEY)?.let {
        field.kind = it
        if (field.kind == "alias") {
            field.name = keyField
            field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
        }
    }
    this?.getSafeString(FORMAT_KEY)?.let {
        field.format = it
    }
    this?.getSafeString(ICON_KEY)?.let { iconPath -> // useful when copied to an empty list / detail form
        if (iconPath.contains(".")) {
            field.icon = correctIconPath(iconPath)
        }
    }
    this?.getSafeBoolean(ISTOMANY_KEY)?.let { isToMany -> // Slave table defined in another table will have isToMany key
        field.name = keyField
        this.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
            if (isToMany) {
                field.isToMany = true
                field.relatedEntities = relatedDataClass
                field.fieldTypeString = "Entities<${relatedDataClass.tableNameAdjustment()}>"
            } else {
                field.isToMany = false
                field.relatedDataClass = relatedDataClass
                field.fieldTypeString = relatedDataClass
            }
        }
    } ?: kotlin.run {
        this?.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass -> // Many-to-one relation
            field.isToMany = false
            field.name = keyField
            field.relatedDataClass = relatedDataClass
            field.fieldTypeString = relatedDataClass
            field.variableType = VariableType.VAR.string
        }
        this?.getSafeString(RELATEDENTITIES_KEY)?.let { relatedEntities -> // One-to-many relation
            field.isToMany = true
            field.name = keyField
            field.relatedEntities = relatedEntities
            field.fieldTypeString = "Entities<${relatedEntities.tableNameAdjustment()}>"
        }
    }
    dataModelId?.let { field.dataModelId = it }
    this?.getSafeString("path")?.let { field.path = it }
    if (field.label.isNullOrEmpty())
        field.label = field.name
    if (field.shortLabel.isNullOrEmpty())
        field.shortLabel = field.name
    return field
}

fun JSONObject?.getSubFields(): List<Field> {
    val subList = mutableListOf<Field>()
    this?.let {
        this.keys().forEach { key ->
            val aSubField: JSONObject? = this.getSafeObject(key.toString())
            aSubField?.getDataModelField(key.toString())?.let { subList.add(it) }
        }
    }
    return subList
}

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
                    path = ""
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
                    path = ""
                )
            }
        }
    }
    return null
}

fun buildNewKeyField(name: String): Field {
    val newKeyField =
        Field(name = "__${name.validateWordDecapitalized()}Key")
    Log.d("Many to One relation $name, adding new keyField.name : ${newKeyField.name}")
    newKeyField.fieldType = 0
    newKeyField.fieldTypeString = STRING_TYPE
    newKeyField.variableType = VariableType.VAR.string
    return newKeyField
}

fun buildNewSizeField(name: String): Field {
    val newSizeField =
        Field(name = "__${name.validateWordDecapitalized()}Size")
    newSizeField.fieldType = 8
    newSizeField.fieldTypeString = INT_TYPE
    newSizeField.variableType = VariableType.VAR.string
    return newSizeField
}

data class FieldToAdd(
    val targetTable: String, val field: Field, val keyField: Field, val relation: Relation
)