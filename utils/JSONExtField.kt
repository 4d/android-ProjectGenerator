import DefaultValues.NULL_FIELD_SEPARATOR
import ProjectEditorConstants.FIELDTYPE_KEY
import ProjectEditorConstants.ID_KEY
import ProjectEditorConstants.INVERSENAME_KEY
import ProjectEditorConstants.LABEL_KEY
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.NULL_KEY
import ProjectEditorConstants.RELATEDDATACLASS_KEY
import ProjectEditorConstants.RELATEDENTITIES_KEY
import ProjectEditorConstants.RELATEDTABLENUMBER_KEY
import ProjectEditorConstants.SHORTLABEL_KEY
import org.json.JSONObject

fun getFormFields(fieldList: List<String>, formType: FormType): List<Field> {
    val fields = mutableListOf<Field>()
    fieldList.forEach { fieldString ->
        val field: Field
        if (formType == FormType.DETAIL && fieldString == NULL_KEY) {
            field = Field(name = NULL_FIELD_SEPARATOR)
            fields.add(field)
        } else {
            fields.add(retrieveJSONObject(fieldString).getFormField())
        }
    }
    return fields
}

fun JSONObject?.getFormField(): Field {
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
    this?.getSafeString(RELATEDDATACLASS_KEY).let {
        field.relatedDataClass = it
        field.fieldTypeString = it
    }
    this?.getSafeString(RELATEDENTITIES_KEY).let {
        field.relatedEntities = it
        field.fieldTypeString = "Entities<$it>"
    }
    return field
}
