import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.OBJECT_TYPE
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
        var icon: String? = null,
        var kind: String? = null,
        var dataModelId: String? = null,
        var path: String? = null
)

fun isPrivateRelationField(fieldName: String): Boolean = fieldName.startsWith("__") && (fieldName.endsWith("Key") || fieldName.endsWith("Size"))

fun Field.isImage() = this.fieldType == 3

fun Field.getFieldName() =
    if (this.name.contains("."))
        this.name.split(".")[1]
    else
        this.name

fun Field.getFieldKeyAccessor() =
    if (this.name.fieldAdjustment().contains("."))
        this.name.fieldAdjustment().split(".")[0] + ".__KEY"
    else
        "__KEY"

fun Field.getLayoutVariableAccessor(dataModelList: List<DataModel>): String {
    Log.d("getLayoutVariableAccessor: this = $this")
    return if (this.name.fieldAdjustment().contains(".") || (this.kind == "alias" && this.isNotNativeType(dataModelList)))
        "entityData."
    else
        "entityData.__entity."
}

fun Field.isNotNativeType(dataModelList: List<DataModel>): Boolean = dataModelList.map { it.name }.contains(this.fieldTypeString)

fun Field.getSourceTableName(dataModelList: List<DataModel>, form: Form): String {
    if (this.name.contains(".")) {

        val fieldFromDataModel: Field? =
            form.dataModel.fields?.find { it.name == this.name.split(".")[0] }

        fieldFromDataModel?.let { field ->
            return dataModelList.find { it.id == "${field.relatedTableNumber}" }?.name ?: ""
        }
        return ""
    } else {
        return form.dataModel.name
    }
}

fun Field.getLabel(): String {
    return label?.encode() ?: ""
}

fun Field.getShortLabel(): String {
    return shortLabel?.encode() ?: ""
}

fun Field.getIcon(dataModelKey: String): String {
    if (this.icon.isNullOrEmpty()) {
        if (this.id == null) {
            this.id = this.name
        }
        this.id?.let { this.id = it.toLowerCase().replace("[^a-z0-9]+".toRegex(), "_") }
        return if (this.isSlave == false)
            "field_icon_${dataModelKey}_${this.id}"
        else
            "related_field_icon_${dataModelKey}_${this.relatedTableNumber}_${this.id}"
    }
    return this.icon ?: ""
}

fun Field.isRelation(currentTable: String, dataModelList: List<DataModel>): Boolean {
    Log.d("kind is alias ? ${this.kind == "alias"}")
    Log.d("!this.inverseName.isNullOrEmpty() ? ${!this.inverseName.isNullOrEmpty()}")
    Log.d("!isFieldAlias(currentTable, dataModelList) ? ${!isFieldAlias(currentTable, dataModelList)}")
    return !this.inverseName.isNullOrEmpty() || (this.kind == "alias" && !isFieldAlias(currentTable, dataModelList) )
}

fun Field.isOneToManyRelation(currentTable: String, dataModelList: List<DataModel>): Boolean = this.relatedEntities != null && this.isRelation(currentTable, dataModelList)

fun Field.isManyToOneRelation(currentTable: String, dataModelList: List<DataModel>): Boolean = this.relatedEntities == null && this.isRelation(currentTable, dataModelList)

fun correctIconPath(iconPath: String): String {
    val correctedIconPath = iconPath
        .substring(0, iconPath.lastIndexOf('.')) // removes extension
        .replace(".+/".toRegex(), "")
        .removePrefix(File.separator)
        .toLowerCase()
        .replace("[^a-z0-9]+".toRegex(), "_")
    return correctedIconPath
}

fun Field.getFormatNameForType(pathHelper: PathHelper): String {
    Log.d("getFormatNameForType, [${this.name}] field: $this")
    val format = this.format
    if (format.equals("integer")) {
        return when (this.fieldType) {
            6 -> "boolInteger" // Boolean
            11 -> "timeInteger" // Time
            else -> "integer"
        }
    }
    if (format.isNullOrEmpty()) {
        if (this.kind == "alias" && this.path?.contains(".") == true)
            return ""
        return when (typeFromTypeInt(this.fieldType)) {
            BOOLEAN_TYPE -> "falseOrTrue"
            DATE_TYPE -> "mediumDate"
            TIME_TYPE -> "mediumTime"
            INT_TYPE -> "integer"
            FLOAT_TYPE -> "decimal"
            OBJECT_TYPE-> "yaml"
            else -> ""
        }
    } else {
        if (format.startsWith("/")) {

            val formatPath = pathHelper.getCustomFormatterPath(format)
            getManifestJSONContent(formatPath)?.let {

                val fieldMapping = getFieldMapping(it, format)
                return if (fieldMapping.isValidFormatter() || fieldMapping.isValidKotlinCustomDataFormatter()) {
                    format
                } else {
                    when (typeFromTypeInt(this.fieldType)) {
                        OBJECT_TYPE -> "yaml"
                        else -> ""
                    }
                }
            }
        }
        return format
    }
}

fun getDataModelField(dataModelList: List<DataModel>, form: Form, field: Field): Field? {
    if (field.name.contains(".")) {
        val dataModel = dataModelList.find { it.id == form.dataModel.id }
        val relationList = dataModel?.relations
        val fieldInRelationList = relationList?.find { it.name == field.name.split(".")[0] }
        val subFields = fieldInRelationList?.subFields
        val subField = subFields?.find { it.name == field.name.split(".")[1] }
        return subField
    } else {
        val dataModelField = dataModelList.find { it.id == form.dataModel.id }?.fields?.find { it.name == field.name }
        return dataModelField
    }
}

/**
 * Returns true if it has %length% placeholder AND it is a 1-N relation
 */

fun hasLabelPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val label = getLabelWithFixes(dataModelList, form, field)
    return hasPercentPlaceholder(label, dataModelList, form, field)
}

fun hasShortLabelPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val shortLabel = getShortLabelWithFixes(dataModelList, form, field)
    return hasPercentPlaceholder(shortLabel, dataModelList, form, field)
}

fun hasPercentPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, form, field)
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, form, field)
    return hasLengthPlaceholder || hasFieldPlaceholder
}

fun hasLengthPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    if (!isRelationWithFixes(dataModelList, form, field)) return false
    if (!isOneToManyRelationWithFixes(dataModelList, form, field)) return false
    return label.contains("%length%")
}

fun getLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, formType: FormType): String {
    val label = getLabelWithFixes(dataModelList, form, field)
    return replacePercentPlaceholder(label, dataModelList, form, field, formType)
}

fun getShortLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, formType: FormType): String {
    val shortLabel = getShortLabelWithFixes(dataModelList, form, field)
    return replacePercentPlaceholder(shortLabel, dataModelList, form, field, formType)
}

fun replacePercentPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field, formType: FormType): String {
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, form, field)
    val labelWithLength = if (hasLengthPlaceholder) {
        replaceLengthPlaceholder(label, field, formType)
    } else {
        label
    }
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, form, field)
    return if (hasFieldPlaceholder) {
        replaceFieldPlaceholder(labelWithLength, field, formType)
    } else {
        cleanPrefixSuffix("\"$labelWithLength\"")
    }
}

fun cleanPrefixSuffix(label: String): String {
    return label.removePrefix("\"\" + ").removeSuffix(" + \"\"").removePrefix("\" + ").removeSuffix(" + \"")
}

fun replaceLengthPlaceholder(label: String, field: Field, formType: FormType): String {
    var labelWithLength = ""
    val relationName = field.name.fieldAdjustment().replace(".", "_")
    val lengthPlaceholder = "%length%"
    val sizeReplacement = if (formType == FormType.LIST)
        "$relationName.size"
    else
        "viewModel.$relationName.size"
    labelWithLength = label.replace(lengthPlaceholder, "\" + $sizeReplacement + \"")
    return labelWithLength
}

fun hasFieldPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    if (!isRelationWithFixes(dataModelList, form, field)) return false
    if (!isManyToOneRelationWithFixes(dataModelList, form, field)) return false
    val dataModel = form.dataModel
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    regex.findAll(label).forEach { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        // Verify that fieldName exists in source dataModel
        if (fieldName.contains(".")) {
            val baseFieldName = fieldName.split(".")[0]
            val relatedFieldName = fieldName.split(".")[1]
            val baseField = dataModel.fields?.find { it.name.fieldAdjustment() == baseFieldName.fieldAdjustment() }
            val relatedDataModel = dataModelList.find { it.id == "${baseField?.relatedTableNumber}" }
            if (relatedDataModel?.fields?.find { it.name.fieldAdjustment() == relatedFieldName.fieldAdjustment() } != null)
                return true
        } else {
            val relatedDataModel = dataModelList.find { it.id == field.relatedTableNumber.toString() }
            if (relatedDataModel?.fields?.find { it.name.fieldAdjustment() == fieldName.fieldAdjustment() } != null)
                return true
        }
    }
    return false
}

fun replaceFieldPlaceholder(label: String, field: Field, formType: FormType): String {
    val labelWithoutRemainingLength = label.replace("%length%", "")
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    val newLabel = regex.replace(labelWithoutRemainingLength) { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        if (formType == FormType.LIST)
            "\" + ${field.name.fieldAdjustment()}.${fieldName.fieldAdjustment()}.toString() + \""
        else
            "\" + viewModel.${field.name.fieldAdjustment()}.${fieldName.fieldAdjustment()}.toString() + \""
    }
    val labelWithField = "\"" + newLabel.removePrefix(" ").removeSuffix(" ") + "\""
    return cleanPrefixSuffix(labelWithField)
}

fun Field.getNavbarTitle(dataModelList: List<DataModel>, source: String): String {
    val format = this.format ?: return ""
    val dataModel = dataModelList.find { it.name.tableNameAdjustment() == source.tableNameAdjustment() }

    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    val formatWithoutRemainingLength = format.replace("%length%", "")
    val navbarTitle = regex.replace(formatWithoutRemainingLength) { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        // Verify that fieldName exists in source dataModel
        if (fieldName.contains(".")) {
            val baseFieldName = fieldName.split(".")[0]
            val relatedFieldName = fieldName.split(".")[1]
            val baseField = dataModel?.fields?.find { it.name.fieldAdjustment() == baseFieldName.fieldAdjustment() }
            val relatedDataModel = dataModelList.find { it.id == "${baseField?.relatedTableNumber}" }
            val relatedField = relatedDataModel?.fields?.find { it.name.fieldAdjustment() == relatedFieldName.fieldAdjustment() }
            if (relatedField != null) {
                "\${(anyRelatedEntity as ${relatedDataModel.name.tableNameAdjustment()}?)?.${relatedFieldName.fieldAdjustment()}.toString()}"
            } else {
                fieldName
            }
        } else {
            val field = dataModel?.fields?.find { it.name.fieldAdjustment() == fieldName.fieldAdjustment() }
            if (field != null) {
                "\${(entity as ${source.tableNameAdjustment()}?)?.${fieldName.fieldAdjustment()}.toString()}"
            } else {
                fieldName
            }
        }
    }
    return navbarTitle
}

/**
 * fieldFromDataModel is here to get the Field from dataModel instead of list/detail form as some information may be missing.
 * If it's a related field, fieldFromDataModel will be null, therefore check the field variable
*/
fun getIconWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getIcon(form.dataModel.id) ?: field.getIcon(form.dataModel.id)
}

fun getFormatWithFixes(dataModelList: List<DataModel>, form: Form, field: Field, pathHelper: PathHelper): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getFormatNameForType(pathHelper) ?: field.getFormatNameForType(pathHelper)
}

fun getShortLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getShortLabel() ?: field.getShortLabel()
}

fun getLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getLabel() ?: field.getLabel()
}

fun getNavbarTitleWithFixes(dataModelList: List<DataModel>, form: Form, field: Field, source: String): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getNavbarTitle(dataModelList, source) ?: field.getNavbarTitle(dataModelList, source)
}

fun isRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    Log.d("field from datamodel isrelatinwithfixes : $fieldFromDataModel")
    Log.d("field was $field")
    return fieldFromDataModel?.isRelation(form.dataModel.name, dataModelList) ?: field.isRelation(form.dataModel.name, dataModelList)
}

fun getInverseNameWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String? {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.inverseName ?: field.inverseName
}

fun isOneToManyRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.isOneToManyRelation(form.dataModel.name, dataModelList) ?: field.isOneToManyRelation(form.dataModel.name, dataModelList)
}

fun isManyToOneRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.isManyToOneRelation(form.dataModel.name, dataModelList) ?: field.isManyToOneRelation(form.dataModel.name, dataModelList)
}