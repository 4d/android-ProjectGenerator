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
        var path: String? = null,
        var subFieldsForAlias: List<Field>? = null,
        var parentIfSlave: Field? = null,
        var grandParentsIfSlave: Field? = null
)

fun isPrivateRelationField(fieldName: String): Boolean = fieldName.startsWith("__") && fieldName.endsWith("Key")

fun Field.isImage() = this.fieldType == 3

fun Field.getImageFieldName() = this.path?.substringAfterLast(".") ?: ""

fun Field.getFieldKeyAccessor(dataModelList: List<DataModel>): String {
    if (this.isFieldAlias(dataModelList)) {
        Log.d("getFieldKeyAccessor, aliasField here, field is $this")
        val path = this.path ?: ""
        if (path.contains(".")) {
            var name = ""
            var nextPath = path.substringBeforeLast(".")
            while (nextPath.contains(".")) {

                name += nextPath.relationAdjustment() + "."
                Log.d("building name = $name")

                nextPath = nextPath.substringAfter(".")
            }
            val returnName = name + nextPath.relationAdjustment() + "." + path.substringAfterLast(".").fieldAdjustment()
            Log.d("getFieldKeyAccessor returnName: $returnName")
            return returnName.substringBeforeLast(".") + ".__KEY"
        } else {
            return "__KEY"
        }
    } else {
        return "__KEY"
    }
}

fun Field.getLayoutVariableAccessor(): String {
    Log.d("getLayoutVariableAccessor: this = $this")
    return if (this.name.fieldAdjustment().contains(".") || (this.kind == "alias" /*&& this.isNotNativeType(dataModelList)*/ && this.path?.contains(".") == true))
        "entityData."
    else
        "entityData.__entity."
}

fun Field.isNotNativeType(dataModelList: List<DataModel>): Boolean = dataModelList.map { it.name }.contains(this.fieldTypeString) || this.fieldTypeString?.startsWith("Entities<") == true

fun Field.isNativeType(dataModelList: List<DataModel>): Boolean = this.isNotNativeType(dataModelList).not()

fun Field.getLabel(): String {
    return label?.encode() ?: ""
}

fun Field.getShortLabel(): String {
    return shortLabel?.encode() ?: ""
}

fun Field.getIcon(dataModelKey: String, nameIfSlave: String): String {
    Log.d("getIcon, field $this")
    if (this.icon.isNullOrEmpty()) {
        this.id = when {
            this.isSlave == true -> nameIfSlave
            this.id == null -> this.name
            else -> this.id
        }
        this.id?.let { this.id = it.toLowerCase().replace("[^a-z0-9]+".toRegex(), "_") }
        return if (this.isSlave == true)
            "related_field_icon_${dataModelKey}_${this.relatedTableNumber}_${this.id}"
        else
            "field_icon_${dataModelKey}_${this.id}"
    }
    return this.icon ?: ""
}

fun Field.isRelation(dataModelList: List<DataModel>): Boolean {
    Log.d("isRelation, field : $this")
    Log.d("kind is alias ? ${this.kind == "alias"}")
    Log.d("!this.inverseName.isNullOrEmpty() ? ${!this.inverseName.isNullOrEmpty()}")
    Log.d("!isFieldAlias(currentTable, dataModelList) ? ${!isFieldAlias(dataModelList)}")
    return !this.inverseName.isNullOrEmpty() || (this.kind == "alias" && !isFieldAlias(dataModelList) )
}

fun Field.isOneToManyRelation(dataModelList: List<DataModel>): Boolean = this.relatedEntities != null && this.isRelation(dataModelList)

fun Field.isManyToOneRelation(dataModelList: List<DataModel>): Boolean = this.relatedEntities == null && this.isRelation(dataModelList)

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
    Log.d("getDataModelField [${field.name}], field: $field")
    val partCount = field.name.count { it == '.'}
    val dataModel = dataModelList.find { it.id == form.dataModel.id }
    if (field.name.contains(".")) {
        Log.d("getDataModelField field.name contains '.'")
        dataModel?.relations?.find { it.name == field.name.split(".")[0] }?.let { fieldInRelationList ->
            Log.d("fieldInRelationList: $fieldInRelationList")
             fieldInRelationList.subFields.find { it.name == field.name.split(".")[1] }?.let { son ->
                 Log.d("son: $son")
                 if (partCount > 1) {
                     son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                         Log.d("grandSon: $grandSon")
                         return grandSon
                     }
                 } else {
                     return son
                 }
             }
        }
        Log.d("getDataModelField [${field.name}] not found in relations, going to check in fields")
        dataModel?.fields?.find { it.name == field.name.split(".")[0] }?.let { fieldInFieldList ->
            Log.d("fieldInFieldList: $fieldInFieldList")
            fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                Log.d("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                        Log.d("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        Log.d("getDataModelField [${field.name}] not found in fields, going to check fieldAliases in fields")
        dataModel?.fields?.find { it.path == field.name && field.kind == "alias" }?.let { aliasFieldInFieldList ->
            Log.d("aliasFieldInFieldList: $aliasFieldInFieldList")
            return aliasFieldInFieldList
        }

    } else {
        Log.d("getDataModelField field.name doesn't contain '.'")

        dataModelList.find { it.id == form.dataModel.id }?.fields?.find { it.name == field.name }?.let { field ->
            Log.d("Found field with this name: $field")
            return field
        }

        val fieldPath = field.path ?: ""
        val pathPartCount = fieldPath.count { it == '.'}
        if (fieldPath.contains(".")) {         // TODO: TO CHECK
            Log.d("getDataModelField fieldPath contains '.'")
            dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
                Log.d("fieldInFieldList: $fieldInFieldList")
                fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                    Log.d("son: $son")
                    if (pathPartCount > 1) {
                        son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                            Log.d("grandSon: $grandSon")
                            return grandSon
                        }
                    } else {
                        return son
                    }
                }
            }
        }
    }

    Log.d("getDataModelField returns null")
    return null
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
    return fieldFromDataModel?.getIcon(form.dataModel.id, field.name) ?: ""
}

fun getFormatWithFixes(dataModelList: List<DataModel>, form: Form, field: Field, pathHelper: PathHelper): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getFormatNameForType(pathHelper) ?: ""
}

fun getShortLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getShortLabel() ?: ""
}

fun getLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getLabel() ?: ""
}

fun getNavbarTitleWithFixes(dataModelList: List<DataModel>, form: Form, field: Field, source: String): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.getNavbarTitle(dataModelList, source) ?: ""
}

fun isRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    Log.d("fieldFromDataModel: $fieldFromDataModel")
    return fieldFromDataModel?.isRelation(dataModelList) ?: false
}

fun isOneToManyRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.isOneToManyRelation(dataModelList) ?: false
}

fun isManyToOneRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form, field)
    return fieldFromDataModel?.isManyToOneRelation(dataModelList) ?: false
}