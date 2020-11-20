import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.EMPTY_KEY
import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.FILTER_KEY
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

    dataModels?.names()?.let {
        for (i in 0 until dataModels.names().length()) {
            val keyDataModel = dataModels.names().getString(i)
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
                if (dataModelList.removeIf { dataModel ->  dataModel.name == dataModelName}) {
                    println("DataModel removed from list : $dataModelName")
                }

                val newDataModel = DataModel(name = dataModelName, isSlave = false)
                newDataModel.id = keyDataModel.toString()
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(LABEL_KEY)?.let { newDataModel.label = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(SHORTLABEL_KEY)?.let { newDataModel.shortLabel = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(ICON_KEY)?.let { newDataModel.iconPath = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeObject(FILTER_KEY)?.let {
                    if (it.getSafeBoolean(VALIDATED_KEY) == true)
                        newDataModel.query = it.getSafeString(STRING_KEY)
                }

                val fieldList = mutableListOf<Field>()
                val relationList = mutableListOf<Relation>()

                newDataModelJSONObject.names()?.let {
                    for (j in 0 until newDataModelJSONObject.names().length()) {
                        val keyField = newDataModelJSONObject.names().getString(j)
                        if (keyField == EMPTY_KEY) continue
                        val newFieldJSONObject = newDataModelJSONObject.getSafeObject(keyField.toString())
                        val field = newFieldJSONObject?.getDataModelField(keyField)
                        field?.let {
                            fieldList.add(it)
                            getRelation(it, dataModelName)?.let { relation ->
                                relationList.add(relation)

                                if (relation.relationType == RelationType.MANY_TO_ONE) {
                                    val relationKeyField = Field(name = "__${relation.name}Key")
                                    relationKeyField.fieldType = 0
                                    relationKeyField.fieldTypeString = typeStringFromTypeInt(relationKeyField.fieldType)
                                    relationKeyField.variableType = VariableType.VAR.string
                                    fieldList.add(relationKeyField)
                                }
                            }

                            // Check if there is a slave table to add
                            newFieldJSONObject.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
                                val slaveDataModel = DataModel(name = relatedDataClass, isSlave = true)
                                newFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->
                                    slaveDataModel.id = relatedTableNumber.toString()

                                    val slaveFieldList = mutableListOf<Field>()
                                    val slaveRelationList = mutableListOf<Relation>()

                                    val inverseField = Field(name = "")
                                    newFieldJSONObject.getSafeString(INVERSENAME_KEY)?.let { inverseName -> inverseField.name = inverseName }
                                    inverseField.relatedEntities = dataModelName
                                    inverseField.fieldTypeString = "Entities<$dataModelName>"
                                    slaveFieldList.add(inverseField)

                                    for (k in 0 until newFieldJSONObject.names().length()) {
                                        val slaveKeyField = newFieldJSONObject.names().getString(k)
                                        val newSlaveFieldJSONObject = newFieldJSONObject.getSafeObject(slaveKeyField.toString())
                                        val slaveField = newSlaveFieldJSONObject?.getDataModelField(slaveKeyField)
                                        slaveField?.let { field ->
                                            slaveFieldList.add(field)
                                            getRelation(field, relatedDataClass)?.let { relation ->
                                                slaveRelationList.add(relation)
                                            }
                                        }
                                    }

                                    // checking if we already added this dataModel
                                    val dataModelIndex = dataModelList.indexOfFirst { dataModel ->  dataModel.name == relatedDataClass } // -1 if not found
                                    when {
                                        dataModelIndex != -1 -> { // in case we did add this dataModel already
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

                    // Add savedFields from another definition of this table (in a relation)
                    savedFields.forEach { savedField ->
                        if (!fieldList.map { it.name }.contains(savedField.name)) {
                            fieldList.add(savedField)
                        }
                    }
                    savedRelations.forEach { savedRelations ->
                        if (!relationList.map { it.name }.contains(savedRelations.name)) {
                            relationList.add(savedRelations)
                        }
                    }

                    newDataModel.fields = fieldList
                    newDataModel.relationList = relationList
                    dataModelList.add(newDataModel)
                }
            }
        }
    }
    dataModelList.forEach {
        it.reOrderFields()
    }
    return dataModelList
}

/**
 * Function to put relations at the end of the field lists
 */
fun DataModel.reOrderFields() {
    this.fields?.sortWith(compareBy { it.name.startsWith("__") && it.name.endsWith("Key")})
    this.fields?.sortWith(nullsLast(compareBy {it.inverseName}))
}

fun JSONObject?.getDataModelField(keyField: String): Field {
    val field = Field(name = "")
    this?.getSafeString(LABEL_KEY)?.let { field.label = it }
    this?.getSafeString(SHORTLABEL_KEY)?.let { field.shortLabel = it }
    this?.getSafeInt(FIELDTYPE_KEY).let { field.fieldType = it }
    this?.getSafeInt(RELATEDTABLENUMBER_KEY).let { field.relatedTableNumber = it }
    this?.getSafeString(INVERSENAME_KEY)?.let { field.inverseName = it }
    this?.getSafeString(NAME_KEY)?.let { fieldName -> // BASIC FIELD
        field.name = fieldName
        field.id = keyField
        field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
    }
    this?.getSafeBoolean(ISTOMANY_KEY)?.let { isToMany -> // Slave table defined in another table will have isToMany key
        field.name = keyField
        this.getSafeString(RELATEDDATACLASS_KEY)?.let { relatedDataClass ->
            if (isToMany) {
                field.relatedEntities = relatedDataClass
                field.fieldTypeString = "Entities<$relatedDataClass>"
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
        }
        this?.getSafeString(RELATEDENTITIES_KEY)?.let { relatedEntities -> // One-to-many relation
            field.name = keyField
            field.relatedEntities = relatedEntities
            field.fieldTypeString = "Entities<$relatedEntities>"
        }
    }
    if (field.label.isNullOrEmpty())
        field.label = field.name
    if (field.shortLabel.isNullOrEmpty())
        field.shortLabel = field.name
    return field
}

fun getRelation(field: Field, tableName: String): Relation? {
    field.relatedEntities?.let {
        return Relation(source = tableName, target = it, name = field.name, relationType = RelationType.ONE_TO_MANY)
    }
    field.relatedDataClass?.let {
        return Relation(source = tableName, target = it, name = field.name, relationType = RelationType.MANY_TO_ONE)
    }
    return null
}
