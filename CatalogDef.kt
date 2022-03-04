import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.STRING_TYPE
import org.json.JSONObject
import java.io.File

class CatalogDef(catalogFile: File) {

    lateinit var dataModelAliases: List<DataModelAlias>

    companion object {
        lateinit var baseCatalogDef: List<DataModelAlias>
//        val relations = mutableListOf<Relation>()
    }

    private lateinit var jsonObj: JSONObject

    init {
        val jsonString = catalogFile.readFile()
        Log.d(
            "==================================\n" +
                    "CatalogDef init\n" +
                    "==================================\n"
        )

        if (jsonString.isEmpty()) {
            throw Exception("Json file ${catalogFile.name} is empty")
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            dataModelAliases = getCatalogDef()
            baseCatalogDef = dataModelAliases.toList()
            Log.d("> DataModels list successfully read.")

        } ?: kotlin.run {
            Log.e("Could not read global json object from file ${catalogFile.name}")
        }
    }

    private fun getCatalogDef(): List<DataModelAlias> {
        val dataModelAliases = mutableListOf<DataModelAlias>()
        val dataModels = jsonObj.getSafeObject("structure")?.getSafeArray("definition").getObjectListAsString()

        dataModels.forEach { dataModelString ->
            retrieveJSONObject(dataModelString)?.getDataModelCatalog()?.let { dataModelCatalog ->
                dataModelAliases.add(dataModelCatalog)
            }
        }
        return dataModelAliases
    }

    private fun JSONObject.getDataModelCatalog(): DataModelAlias? {
        val name = this.getSafeString("name")
        val tableNumber = this.getSafeInt("tableNumber")
        if (name == null || tableNumber == null)
            return null

        val dataModel = DataModelAlias(tableNumber = tableNumber, name = name)

        val fields = mutableListOf<FieldCatalog>()
        val relations = mutableListOf<Relation>()

        this.getSafeArray("fields")?.getObjectListAsString()?.forEach { fieldString ->
            retrieveJSONObject(fieldString)?.getFieldCatalog(tableNumber.toString())?.let { field ->
                fields.add(field)
            }
        }
        fields.forEach { field ->
            if (field.isRelation()) {
                val subFields = baseCatalogDef.find { it.name == field.relatedDataClass }?.fields?.convertToField()
                field.createRelation(name, subFields)?.let { relation ->
                    relations.add(relation)
                    fields.remove(field)
                    if (relation.type == RelationType.MANY_TO_ONE){
                        val relationKeyField = buildNewKeyFieldCatalog(relation.name, tableNumber.toString())
                        fields.add(relationKeyField)
                    } else {
                        val relationSizeField = buildNewSizeFieldCatalog(relation.name, tableNumber.toString())
                        fields.add(relationSizeField)
                    }
                }
            }
        }

        dataModel.fields = fields
        dataModel.relations = relations
        return dataModel
    }

    private fun JSONObject.getFieldCatalog(dataModelId: String): FieldCatalog? {
        val name = this.getSafeString("name") ?: return null
        val fieldCatalog = FieldCatalog(name = name, dataModelId = dataModelId)
        this.getSafeString("kind")?.let { fieldCatalog.kind = it }
        this.getSafeString("relatedDataClass")?.let { fieldCatalog.relatedDataClass = it }
        this.getSafeString("inverseName")?.let { fieldCatalog.inverseName = it }
        this.getSafeString("id")?.let { fieldCatalog.id = it } ?: kotlin.run { fieldCatalog.id = name }
        this.getSafeBoolean("isToOne")?.let { fieldCatalog.isToOne = it }
        this.getSafeBoolean("isToMany")?.let { fieldCatalog.isToMany = it }
        this.getSafeInt("fieldType")?.let { fieldCatalog.fieldType = it }
        this.getSafeInt("relatedTableNumber")?.let { fieldCatalog.relatedTableNumber = it }
        this.getSafeString("path")?.let { fieldCatalog.path = it }

        when {
            fieldCatalog.isToMany == true -> {
                fieldCatalog.fieldTypeString = "Entities<${fieldCatalog.relatedDataClass?.tableNameAdjustment()}>"
                fieldCatalog.isToOne = false
                fieldCatalog.variableType = VariableType.VAR.string
            }
            fieldCatalog.isToOne == true -> {
                fieldCatalog.fieldTypeString = fieldCatalog.relatedDataClass
                fieldCatalog.isToMany = false
            }
            else -> {
                fieldCatalog.fieldTypeString = typeStringFromTypeInt(fieldCatalog.fieldType)
                fieldCatalog.isToMany = false
                fieldCatalog.isToOne = false
            }
        }
        return fieldCatalog
    }
}

data class DataModelAlias(
    var tableNumber: Int,
    var name: String,
    var fields: MutableList<FieldCatalog> = mutableListOf(),
    var relations: MutableList<Relation> = mutableListOf(),
    var id: String? = null,
    var label: String? = null,
    var shortLabel: String? = null,
    var query: String? = null,
    var iconPath: String? = null,
    var isSlave: Boolean? = null
) {

//    fun completeDataModel(keyDataModel: String, newDataModelJSONObject: JSONObject?) {
//        id = keyDataModel
//        isSlave = false
//
//        var missingIcon = true
//
//        newDataModelJSONObject?.getSafeObject("")?.let { dmJson ->
//            dmJson.getSafeString("label")?.let { label = it }
//            dmJson.getSafeString("shortLabel")?.let { shortLabel = it }
//            dmJson.getSafeObject("filter")?.let {
//                if (it.getSafeBoolean("validated") == true)
//                    query = it.getSafeString("string")?.replace("\"", "'")
//            }
//            dmJson.getSafeString("icon")?.let { path ->
//                if (path.contains(".")) {
//                    iconPath = correctIconPath(path)
//                    missingIcon = false
//                }
//            }
//        }
//        if (missingIcon) {
//            iconPath = "nav_icon_${id}"
//        }
//
//        completeFields(newDataModelJSONObject)
//        reOrderFields()
//    }
//
//    private fun completeFields(newDataModelJSONObject: JSONObject?) {
//        newDataModelJSONObject?.keys()?.forEach eachKeyField@{ keyField ->
//            if (keyField !is String) return@eachKeyField
//            if (keyField != "") {
//
//                val newFieldJSONObject = newDataModelJSONObject.getSafeObject(keyField.toString())
//
//                val fieldCatalog = fields.find { it.id == keyField }
//                fieldCatalog?.let {
//                    fieldCatalog.completeField(newFieldJSONObject)
//
//                    if (fieldCatalog.isRelation()) {
//                        val subFields = fieldCatalog.getSubFields(newFieldJSONObject)
//                        fieldCatalog.createRelation(name, subFields)?.let { relation ->
//                            relations.add(relation)
//                            CatalogDef.relations.add(relation)
//                            if (relation.isToOne){
//                                val relationKeyField = buildNewKeyFieldCatalog(relation.name, tableNumber.toString())
//                                fields.add(relationKeyField)
//                            } else {
//                                val relationSizeField = buildNewSizeFieldCatalog(relation.name, tableNumber.toString())
//                                fields.add(relationSizeField)
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
}

data class FieldCatalog(
    var name: String,
    var dataModelId: String,
    var kind: String? = null,
    var relatedDataClass: String? = null,
    var fieldTypeString: String? = null,
    var isToOne: Boolean? = null,
    var isToMany: Boolean? = null,
    var fieldType: Int? = null,
    var inverseName: String? = null,
    var relatedTableNumber: Int? = null,
    var path: String? = null,
    var id: String? = null,
    var variableType: String = VariableType.VAL.string
//    var label: String? = null,
//    var shortLabel: String? = null,
//    var format: String? = null,
//    var icon: String? = null,
//    var relatedEntities: String? = null,
//    var isSlave: Boolean? = null
) {
//    fun completeField(newFieldJSONObject: JSONObject?) {
//
//        newFieldJSONObject?.getSafeString("label")?.let { label = it }
//        newFieldJSONObject?.getSafeString("shortLabel")?.let { shortLabel = it }
//        newFieldJSONObject?.getSafeString("format")?.let { format = it }
//        newFieldJSONObject?.getSafeString("icon")?.let { path -> // useful when copied to an empty list / detail form
//            if (path.contains(".")) {
//                icon = correctIconPath(path)
//            }
//        }
//        if (label.isNullOrEmpty())
//            label = name
//        if (shortLabel.isNullOrEmpty())
//            shortLabel = name
//    }
//
//    fun getSubFields(newFieldJSONObject: JSONObject?): List<FieldCatalog> {
//        val subList = mutableListOf<FieldCatalog>()
//        newFieldJSONObject?.keys()?.forEach { key ->
//            CatalogDef.baseCatalogDef.find { it.id == dataModelId }?.fields?.find { it.id == key }?.let { baseFieldCatalog ->
//                val newField = baseFieldCatalog.copy()
//                newField.completeField(newFieldJSONObject)
//                subList.add(newField)
//            }
//        }
//        return subList
//    }
//
    fun isRelation() = kind == "relatedEntity" || kind == "relatedEntities"
//
    fun createRelation(currentTable: String, subFields: List<Field>?): Relation? {
        relatedDataClass?.let { dest ->
            inverseName?.let { inv ->
                return Relation(
                    source = currentTable,
                    target = dest,
                    name = name,
                    type = if (isToOne == true) RelationType.MANY_TO_ONE else RelationType.ONE_TO_MANY,
                    subFields = subFields ?: listOf(),
                    inverseName = inv,
                    path = path ?: ""
                )
            }
        }
        return null
    }

    fun convertToField(): Field {
        val field = Field(name = name)
        field.id = id
        field.fieldType = fieldType
        field.fieldTypeString = fieldTypeString
        field.relatedTableNumber = relatedTableNumber
        field.variableType = variableType
        field.kind = kind
        field.inverseName = inverseName
        when (isToMany) {
            true -> {
                field.isToMany = true
                field.relatedEntities = relatedDataClass
            }
            else -> field.isToMany = false
        }
        field.relatedDataClass = relatedDataClass
        field.dataModelId = dataModelId
        field.path = path
        return field
    }
}

fun List<FieldCatalog>.convertToField(): List<Field> {
    val fields = mutableListOf<Field>()
    this.forEach { fieldCatalog ->
        fields.add(fieldCatalog.convertToField())
    }
    return fields
}

fun buildNewKeyFieldCatalog(name: String, dataModelId: String): FieldCatalog {
    val newKeyField =
        FieldCatalog(name = "__${name.validateWordDecapitalized()}Key", dataModelId = dataModelId)
    newKeyField.fieldType = 0
    newKeyField.fieldTypeString = STRING_TYPE
    newKeyField.variableType = VariableType.VAR.string
    return newKeyField
}

fun buildNewSizeFieldCatalog(name: String, dataModelId: String): FieldCatalog {
    val newSizeField =
        FieldCatalog(name = "__${name.validateWordDecapitalized()}Size", dataModelId = dataModelId)
    newSizeField.fieldType = 8
    newSizeField.fieldTypeString = INT_TYPE
    newSizeField.variableType = VariableType.VAR.string
    return newSizeField
}