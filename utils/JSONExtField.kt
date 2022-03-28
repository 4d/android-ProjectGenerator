import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.FORMAT_KEY
import ProjectEditorConstants.ICON_KEY
import ProjectEditorConstants.ID_KEY
import ProjectEditorConstants.INVERSENAME_KEY
import ProjectEditorConstants.KIND_KEY
import ProjectEditorConstants.LABEL_KEY
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.RELATEDDATACLASS_KEY
import ProjectEditorConstants.RELATEDENTITIES_KEY
import ProjectEditorConstants.RELATEDTABLENUMBER_KEY
import ProjectEditorConstants.SHORTLABEL_KEY
import org.json.JSONObject

fun getFormFields(fieldList: List<String>, dataModelName: String, catalogDef: CatalogDef): List<Field> {
    val fields = mutableListOf<Field>()
    fieldList.forEach { fieldString ->
        fields.add(retrieveJSONObject(fieldString).getFormField(dataModelName, catalogDef))
    }
    return fields
}

fun JSONObject?.getFormField(dataModelName: String, catalogDef: CatalogDef): Field {
    val field = Field(name = "")
    this?.getSafeString(LABEL_KEY)?.let { field.label = it }
    this?.getSafeString(SHORTLABEL_KEY)?.let { field.shortLabel = it }
    this?.getSafeInt(FIELDTYPE_KEY).let { field.fieldType = it }
    this?.getSafeInt(ID_KEY).let { field.id = it.toString() }
    this?.getSafeInt(RELATEDTABLENUMBER_KEY).let { field.relatedTableNumber = it }
    this?.getSafeString(INVERSENAME_KEY).let { field.inverseName = it }
    this?.getSafeString(NAME_KEY)?.let {
        field.name = it
        field.fieldTypeString = typeStringFromTypeInt(field.fieldType)
    }
    this?.getSafeString(KIND_KEY)?.let { field.kind = it }
    this?.getSafeString(FORMAT_KEY)?.let { field.format = it }
    this?.getSafeString(ICON_KEY)?.let { iconPath ->
        if (iconPath.contains(".")) {
            field.icon = correctIconPath(iconPath)
        }
    }
    this?.getSafeString(RELATEDDATACLASS_KEY).let {
        field.relatedDataClass = it
        field.fieldTypeString = it
    }
    this?.getSafeString(RELATEDENTITIES_KEY).let {
        field.relatedEntities = it
        field.fieldTypeString = "Entities<${it?.tableNameAdjustment()}>"
    }
    this?.getSafeString("path")?.let { path ->
        if (field.kind == "alias") {
            val unAliasedPath = unAliasPath(path, dataModelName, catalogDef)
            field.path = unAliasedPath
            Log.d("Form field creation, path : $path, unaliased path : $unAliasedPath")
            if (path == unAliasedPath && !unAliasedPath.contains(".")) { // path : FirstName, name : First
                field.name = path
                field.kind = ""

                Log.d("GET FIELD FOR FORM:")
                Log.d("- path is : ${path}")
                Log.d("- unaliased path is : ${field.path}")
                Log.d("- field is : ${field}")
                // path doesn't contains "." so it can't be relation.object of type 38 Object
                // so if it's type 38 object, it's a relation

                // if is N-1 relation
                val catalogRelation: Relation? = catalogDef.dataModelAliases.find { it.name == dataModelName }?.relations?.find { it.name == unAliasedPath }
                if (catalogRelation?.type == RelationType.MANY_TO_ONE) {
//                    field.fieldTypeString = destBeforeField(catalogDef, dataModelName, path)
                    field.fieldTypeString = catalogRelation.target
                    field.fieldType = null
                    field.variableType = VariableType.VAR.string
                    field.isToMany = false
                    field.relatedDataClass = catalogRelation.target
                    field.inverseName = catalogRelation.inverseName
                }
            }
        }
    }
    Log.d("form field extracted: $field")
    return field
}

