import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.TIME_TYPE
import java.io.File

data class Field(
        var id: String? = null,
        var name: String,
        var label: String? = null,
        var shortLabel: String? = null,
        var fieldType: Int? = null,
        var fieldTypeString: String? = null,
        var relatedEntities: String? = null,
        var relatedTableNumber: Int? = null,
        var inverseName: String? = null,
        var relatedDataClass: String? = null,
        var variableType: String = VariableType.VAL.string,
        var isToMany: Boolean? = null,
        var isSlave: Boolean? = null,
        var format: String? = null,
        var icon: String? = null
)

fun isPrivateRelationField(fieldName: String): Boolean = fieldName.startsWith("__") && fieldName.endsWith("Key")

fun Field.isImage() = this.fieldType == 3

fun Field.getFieldName() =
    if (this.name.contains("."))
        this.name.split(".")[1]
    else
        this.name

fun Field.getFieldKeyAccessor(formType: FormType) =
    if (formType == FormType.LIST)
        if (this.name.fieldAdjustment().contains("."))
            this.name.fieldAdjustment().split(".")[0] + ".__KEY"
        else
            "__KEY"
    else
        if (this.name.fieldAdjustment().contains("."))
            this.name.fieldAdjustment().split(".")[0] + ".__KEY"
        else
            "__KEY"

fun Field.getLayoutVariableAccessor(formType: FormType) =
    if (formType == FormType.LIST)
        if (this.name.fieldAdjustment().contains("."))
            ""
        else
            "entityData."
    else
        if (this.name.fieldAdjustment().contains("."))
            "viewModel."
        else
            "viewModel.entity."

fun Field.getFieldTableName(dataModelList: List<DataModel>, form: Form): String {
    if (this.name.fieldAdjustment().contains(".")) {

        val fieldFromDataModel: Field? =
            form.dataModel.fields?.find { it.name == this.name.fieldAdjustment().split(".")[0] }

        fieldFromDataModel?.let { field ->
            return dataModelList.find { it.id == "${field.relatedTableNumber}" }?.name ?: ""
        }
        return ""
    } else {
        return form.dataModel.name
    }
}

fun Field.getLabel(): String {
    label?.let { if (it.isNotEmpty()) return it }
    return ""
}

fun Field.getShortLabel(): String {
    shortLabel?.let { if (it.isNotEmpty()) return it }
    return ""
}

fun Field.getIcon(dataModelKey: String): String {
    if (this.icon.isNullOrEmpty()) {
        return if (this.inverseName.isNullOrEmpty())
            "field_icon_${dataModelKey}_${this.id}"
        else
            "related_field_icon_${dataModelKey}_${this.relatedTableNumber}_${this.id}"
    }
    return this.icon ?: ""
}

fun correctIconPath(iconPath: String): String {
    val correctedIconPath = iconPath
        .substring(0, iconPath.lastIndexOf('.')) // removes extension
        .replace(".+/".toRegex(), "")
        .removePrefix(File.separator)
        .toLowerCase()
        .replace("[^a-z0-9]+".toRegex(), "_")

    Log.d("correctedIconPath = $correctedIconPath")
    return correctedIconPath
}

fun Field.getFormatNameForType(): String {
    val format = this.format
    if (format.equals("integer")) {
        return when (this.fieldType) {
            6 -> "boolInteger" // Boolean
            11 -> "timeInteger" // Time
            else -> "integer"
        }
    }
    if (format.isNullOrEmpty()) {
        return when (typeFromTypeInt(this.fieldType)) {
            BOOLEAN_TYPE -> "falseOrTrue"
            DATE_TYPE -> "mediumDate"
            TIME_TYPE -> "mediumTime"
            INT_TYPE -> "integer"
            FLOAT_TYPE -> "decimal"
            else -> ""
        }
    } else {
        return format
    }
}

/**
 * fieldFromDataModel is here to get the Field from dataModel instead of list/detail form as some information may be missing.
 * If it's a related field, fieldFromDataModel will be null, therefore check the field variable
*/
fun getIconWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = dataModelList.find { it.id == form.dataModel.id }?.fields?.find { it.name == field.name }
    return fieldFromDataModel?.getIcon(form.dataModel.id) ?: field.getIcon(form.dataModel.id)
}

fun getFormatWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = dataModelList.find { it.id == form.dataModel.id }?.fields?.find { it.name == field.name }
    return fieldFromDataModel?.getFormatNameForType() ?: field.getFormatNameForType()
}
