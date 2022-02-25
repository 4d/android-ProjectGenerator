import org.json.JSONObject
import java.io.File

class CatalogDef(catalogFile: File) {

    lateinit var dataModelAliases: List<DataModelAlias>

    lateinit var jsonObj: JSONObject

    init {
        val jsonString = catalogFile.readFile()
        Log.d("==================================\n" +
                "CatalogDef init\n" +
                "==================================\n")

        if (jsonString.isEmpty()) {
            throw Exception("Json file ${catalogFile.name} is empty")
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            dataModelAliases = getCatalogDef()
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

        this.getSafeArray("fields")?.getObjectListAsString()?.forEach { fieldString ->
            retrieveJSONObject(fieldString)?.getFieldCatalog()?.let { field ->
                fields.add(field)
            }
        }
        dataModel.fields = fields
        return dataModel
    }

    private fun JSONObject.getFieldCatalog(): FieldCatalog? {
        val name = this.getSafeString("name") ?: return null
        val fieldCatalog = FieldCatalog(name = name)
        this.getSafeString("kind")?.let { fieldCatalog.kind = it }
        this.getSafeString("relatedDataClass")?.let { fieldCatalog.relatedDataClass = it }
        this.getSafeBoolean("isToOne")?.let { fieldCatalog.isToOne = it }
        this.getSafeBoolean("isToMany")?.let { fieldCatalog.isToMany = it }
        this.getSafeInt("fieldType")?.let { fieldCatalog.fieldType = it }
        this.getSafeInt("relatedTableNumber")?.let { fieldCatalog.relatedTableNumber = it }
        this.getSafeString("path")?.let { fieldCatalog.path = it }
        return fieldCatalog
    }

    fun checkForAlias(dataModelName: String, field: Field, fieldList: MutableList<Field>, dataModelList: MutableList<DataModel>) {
        this.dataModelAliases.find { dataModel -> dataModel.name == dataModelName }
            ?.fields?.find { fieldCatalog -> fieldCatalog.name == field.name && fieldCatalog.kind == "alias" }?.let { alias ->

                // Add the related dataModel
                dataModelAliases.find { dm -> dm.tableNumber == alias.relatedTableNumber }?.let { dataModelAlias ->

                    val newDM = DataModel(id = dataModelAlias.tableNumber.toString(), name = dataModelAlias.name)
                    val newDMFields = mutableListOf<Field>()
                    dataModelAlias.fields?.forEach { fieldCatalog ->
                        val newField = Field(name = fieldCatalog.name)
                        field.kind = fieldCatalog.kind
                        field.relatedDataClass = fieldCatalog.relatedDataClass
                        field.isToMany = fieldCatalog.isToMany
                        field.fieldType = fieldCatalog.fieldType
                        field.relatedTableNumber = fieldCatalog.relatedTableNumber
                        newDMFields.add(newField)
                    }
                    newDM.fields = newDMFields
                    dataModelList.add(newDM)
                }

                when {
                    alias.isToOne == true -> {

                    }
                    alias.isToMany == true -> { // Add __xxxKey field
                        val aliasKeyField = buildNewKeyField(alias.name)
                        fieldList.add(aliasKeyField)

                        // required ?
//                                            val fieldToAdd = FieldToAdd(relation.target, newField, newKeyField, newRelation)
//                                            fieldToAddList.add(fieldToAdd)
                    }
                    else -> {

                    }
                }
            }
    }
}

data class DataModelAlias(
    var tableNumber: Int,
    var name: String,
    var fields: List<FieldCatalog>? = null
)

data class FieldCatalog(
    var name: String,
    var kind: String? = null,
    var relatedDataClass: String? = null,
    var isToOne: Boolean? = null,
    var isToMany: Boolean? = null,
    var fieldType: Int? = null,
    var relatedTableNumber: Int? = null,
    var path: String? = null
)