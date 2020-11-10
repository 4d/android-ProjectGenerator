import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.EMPTY_KEY
import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.FILTER_KEY
import ProjectEditorConstants.ICON_KEY
import ProjectEditorConstants.INVERSENAME_KEY
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

                // Remove any slave dataModel with same name added before
                if (dataModelList.removeIf { dataModel ->  dataModel.name == dataModelName}) {
                    println("DataModel removed from list : $dataModelName")
                }

                val dataModel = DataModel(name = dataModelName, isSlave = false)
                dataModel.id = keyDataModel.toString()
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(LABEL_KEY)?.let { dataModel.label = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(SHORTLABEL_KEY)?.let { dataModel.shortLabel = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeString(ICON_KEY)?.let { dataModel.iconPath = it }
                newDataModelJSONObject.getSafeObject(EMPTY_KEY)?.getSafeObject(FILTER_KEY)?.let {
                    if (it.getSafeBoolean(VALIDATED_KEY) == true)
                        dataModel.query = it.getSafeString(STRING_KEY)
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
                                if ((dataModelList.find { dataModel ->  dataModel.name == relatedDataClass} == null) && (relatedDataClass != dataModelName)) { // in case we didn't add this dataModel already, or it's not current dataModel behind parsed
                                    val slaveDataModel = DataModel(name = relatedDataClass, isSlave = true)
                                    newFieldJSONObject.getSafeInt(RELATEDTABLENUMBER_KEY)?.let { relatedTableNumber ->
                                        slaveDataModel.id = relatedTableNumber.toString()

                                        val slaveFieldList = mutableListOf<Field>()

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
                                                    relationList.add(relation)
                                                }
                                            }
                                        }

                                        slaveDataModel.fields = slaveFieldList
                                        println("Adding slave dataModel : ${slaveDataModel.name}")
                                        dataModelList.add(slaveDataModel)
                                    }
                                } else {
                                    println("DataModel already present in list : $relatedDataClass")
                                }
                            }
                        }
                    }

                    dataModel.fields = fieldList
                    dataModel.relationList = relationList
                    println("Adding dataModel : ${dataModel.name}")
                    dataModelList.add(dataModel)
                }
            }
        }
    }
    return dataModelList
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
