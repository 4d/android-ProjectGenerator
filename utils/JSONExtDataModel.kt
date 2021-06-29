import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.EMPTY_KEY
import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.FILTER_KEY
import ProjectEditorConstants.FORMAT_KEY
import ProjectEditorConstants.ICON_KEY
import ProjectEditorConstants.INVERSENAME_KEY
import ProjectEditorConstants.ISTOMANY_KEY
import ProjectEditorConstants.LABEL_KEY
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.RELATEDDATACLASS_KEY
import ProjectEditorConstants.RELATEDENTITIES_KEY
import ProjectEditorConstants.RELATEDTABLENUMBER_KEY
import ProjectEditorConstants.SHORTLABEL_KEY
import ProjectEditorConstants.STRING_KEY
import ProjectEditorConstants.VALIDATED_KEY
import org.json.JSONObject

fun JSONObject.getDataModelList(): List<DataModel> {
    val dataModelList = mutableListOf<DataModel>()
    val dataModels = this.getSafeObject(PROJECT_KEY)?.getSafeObject(DATAMODEL_KEY)

    dataModels?.keys()?.forEach { keyDataModel ->

        val newDataModelJSONObject = dataModels.getSafeObject(keyDataModel.toString())

        newDataModelJSONObject?.getSafeObject(EMPTY_KEY)?.getSafeString(NAME_KEY)?.let { dataModelName ->

            // Remove any slave dataModel with same name added before, save its fields
            val savedFields = mutableListOf<Field>()
            val savedRelations = mutableListOf<Relation>()
            dataModelList.find { it.name == dataModelName }?.fields?.let {
                savedFields.addAll(it)
            }
            dataModelList.find { it.name == dataModelName }?.relationList?.let {
                savedRelations.addAll(it)
            }
            if (dataModelList.removeIf { dataModel -> dataModel.name == dataModelName }) {
                Log.d("DataModel removed from list : $dataModelName")
            }

            val newDataModel = DataModel(id = keyDataModel.toString(), name = dataModelName, isSlave = false)
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

            Log.d("newDataModel.iconPath = ${newDataModel.iconPath}")

            newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeObject(FILTER_KEY)?.let {
                if (it.getSafeBoolean(VALIDATED_KEY) == true)
                    newDataModel.query = it.getSafeString(STRING_KEY)
            }

            val fieldList = mutableListOf<Field>()
            val relationList = mutableListOf<Relation>()

            newDataModelJSONObject.keys().forEach eachKeyField@{ keyField ->
                if (keyField !is String) return@eachKeyField
                if (keyField != EMPTY_KEY) {
                    val newFieldJSONObject: JSONObject? = newDataModelJSONObject.getSafeObject(keyField.toString())
                    val field: Field? = newFieldJSONObject?.getDataModelField(keyField)
                    field?.let {
                        it.isSlave = false
                        fieldList.add(it)
                        val subFields: List<Field> = newFieldJSONObject.getSubFields()
                        getRelation(it, dataModelName, subFields)?.let { relation ->
                            relationList.add(relation)

                            if (relation.relationType == RelationType.MANY_TO_ONE) {
                                val relationKeyField =
                                    Field(name = "__${relation.name.validateWordDecapitalized()}Key")
                                Log.d("Many to One relation: adding relationKeyField = $relationKeyField")
                                relationKeyField.fieldType = 0
                                relationKeyField.fieldTypeString = typeStringFromTypeInt(relationKeyField.fieldType)
                                relationKeyField.variableType = VariableType.VAR.string
                                fieldList.add(relationKeyField)
                            }
                        }

                        // Check if there is a slave table to add
                        newFieldJSONObject.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
                            newFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->
                                val slaveDataModel = DataModel(
                                    id = relatedTableNumber.toString(),
                                    name = relatedDataClass,
                                    isSlave = true
                                )

                                val slaveFieldList = mutableListOf<Field>()
                                val slaveRelationList = mutableListOf<Relation>()

                                newFieldJSONObject.keys().forEach eachSlaveKeyField@{ slaveKeyField ->
                                    if (slaveKeyField !is String) return@eachSlaveKeyField
                                    val newSlaveFieldJSONObject =
                                        newFieldJSONObject.getSafeObject(slaveKeyField.toString())
                                    val slaveField = newSlaveFieldJSONObject?.getDataModelField(slaveKeyField)
                                    slaveField?.let { field ->
                                        field.isSlave = true
                                        slaveFieldList.add(field)
                                        val slaveSubFields: List<Field> = newSlaveFieldJSONObject.getSubFields()
                                        getRelation(field, relatedDataClass, slaveSubFields)?.let { relation ->
                                            slaveRelationList.add(relation)

                                            if (relation.relationType == RelationType.MANY_TO_ONE) {
                                                val relationKeyField =
                                                    Field(name = "__${relation.name.validateWordDecapitalized()}Key")
                                                Log.d("Many to One relation: adding relationKeyField = $relationKeyField")
                                                relationKeyField.fieldType = 0
                                                relationKeyField.fieldTypeString =
                                                    typeStringFromTypeInt(relationKeyField.fieldType)
                                                relationKeyField.variableType = VariableType.VAR.string
                                                slaveFieldList.add(relationKeyField)
                                            }
                                        }
                                    }
                                }

                                // checking if we already added this dataModel
                                val dataModelIndex =
                                    dataModelList.indexOfFirst { dataModel -> dataModel.name == relatedDataClass } // -1 if not found
                                when {
                                    dataModelIndex != -1 -> { // in case we did added this dataModel already
                                        slaveFieldList.forEach { slaveField ->
                                            if (dataModelList[dataModelIndex].fields?.find { field -> field.name == slaveField.name } == null) {
                                                dataModelList[dataModelIndex].fields?.add(slaveField)
                                            }
                                        }
                                        slaveRelationList.forEach { slaveRelation ->
                                            if (dataModelList[dataModelIndex].relationList?.find { relation -> relation.name == slaveRelation.name } == null) {
                                                dataModelList[dataModelIndex].relationList?.add(slaveRelation)
                                            }
                                        }
                                    }
                                    relatedDataClass == dataModelName -> { // current table has a relation of its own type
                                        savedFields.addAll(slaveFieldList)
                                        savedRelations.addAll(slaveRelationList)
                                    }
                                    else -> {
                                        slaveDataModel.fields = slaveFieldList
                                        slaveDataModel.relationList = slaveRelationList
                                        dataModelList.add(slaveDataModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add savedFields from another definition of this table (in a relation)
            savedFields.forEach { savedField ->
                if (!fieldList.map { it.name }.contains(savedField.name)) {
                    savedField.isSlave = true
                    fieldList.add(savedField)
                }
            }
            savedRelations.forEach { savedRelation ->
                if (!relationList.map { it.name }.contains(savedRelation.name)) {
                    relationList.add(savedRelation)
                }
            }

            newDataModel.fields = fieldList
            newDataModel.relationList = relationList
            dataModelList.add(newDataModel)
        }
    }
//    }
    dataModelList.forEach {
        it.reOrderFields()
    }
    return dataModelList
}

/**
 * Function to put relations at the end of the field lists
 */
fun DataModel.reOrderFields() {
    this.fields?.sortWith(compareBy { it.name.startsWith("__") && it.name.endsWith("Key") })
    this.fields?.sortWith(nullsLast(compareBy { it.inverseName }))
}

fun JSONObject?.getDataModelField(keyField: String): Field {
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
                field.relatedEntities = relatedDataClass
                field.fieldTypeString = "Entities<${relatedDataClass.tableNameAdjustment()}>"
            } else {
                field.relatedDataClass = relatedDataClass
                field.fieldTypeString = relatedDataClass
            }
        }
    } ?: kotlin.run {
        this?.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass -> // Many-to-one relation
            field.name = keyField
            field.relatedDataClass = relatedDataClass
            field.fieldTypeString = relatedDataClass
            field.variableType = VariableType.VAR.string
        }
        this?.getSafeString(RELATEDENTITIES_KEY)?.let { relatedEntities -> // One-to-many relation
            field.name = keyField
            field.relatedEntities = relatedEntities
            field.fieldTypeString = "Entities<${relatedEntities.tableNameAdjustment()}>"
        }
    }
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
    field.relatedEntities?.let {
        return Relation(
            source = tableName,
            target = it,
            name = field.name,
            relationType = RelationType.ONE_TO_MANY,
            subFields = subFields
        )
    }
    field.relatedDataClass?.let {
        return Relation(
            source = tableName,
            target = it,
            name = field.name,
            relationType = RelationType.MANY_TO_ONE,
            subFields = subFields
        )
    }
    return null
}
