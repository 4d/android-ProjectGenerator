import java.io.File

data class Field(
        var id: String? = null,
        var name: String,
        var label: String? = null,
        var shortLabel: String? = null,
        var fieldType: Int? = null,
        var valueType: String? = null,
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
        println("getFieldKeyAccessor, aliasField here, field is $this")
        val path = this.path ?: ""
        if (path.contains(".")) {
            var name = ""
            var nextPath = path.substringBeforeLast(".")
            while (nextPath.contains(".")) {

                name += nextPath.relationNameAdjustment() + "."
                println("building name = $name")

                nextPath = nextPath.substringAfter(".")
            }
            val returnName = name + nextPath.relationNameAdjustment() + "." + path.substringAfterLast(".").fieldAdjustment()
            println("getFieldKeyAccessor returnName: $returnName")
            return returnName.substringBeforeLast(".") + ".__KEY"
        } else {
            return "__KEY"
        }
    } else {
        return "__KEY"
    }
}

fun Field.getLayoutVariableAccessor(): String {
    println("getLayoutVariableAccessor: this = $this")
    return if (this.name.fieldAdjustment().contains(".") || (this.kind == "alias" && this.path?.contains(".") == true))
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

fun getIcon(dataModelList: List<DataModel>, form: Form, formField: Field): String {
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, formField) ?: return ""
    println("getIcon, formField $formField")
    println("getIcon, fieldFromDataModel $fieldFromDataModel")
    if (fieldFromDataModel.icon.isNullOrEmpty()) {
        fieldFromDataModel.id = when {
            fieldFromDataModel.isSlave == true -> formField.name
            fieldFromDataModel.id == null -> fieldFromDataModel.name
            else -> fieldFromDataModel.id
        }
        fieldFromDataModel.id?.let { fieldFromDataModel.id = it.toLowerCase().replace("[^a-z0-9]+".toRegex(), "_") }
        val dmKey = dataModelList.find { it.name == form.dataModel.name }?.id
        // Getting first relation
        var relatedDmKey = ""
        fieldFromDataModel.relatedTableNumber?.let { relatedTableNumber ->
            relatedDmKey = relatedTableNumber.toString()
        } ?: run {
            formField.path?.substringBefore(".")?.let { firstPart ->
                findRelationFromPath(dataModelList, form.dataModel.name, firstPart)?.let { firstRelation ->
                    relatedDmKey = dataModelList.find { it.name == firstRelation.target }?.id ?: ""
                }
            }
        }
        return if (fieldFromDataModel.isSlave == true)
            "related_field_icon_${dmKey}_${relatedDmKey}_${fieldFromDataModel.id}"
        else
            "field_icon_${dmKey}_${fieldFromDataModel.id}"
    }
    return fieldFromDataModel.icon ?: ""
}


fun Field.isRelation(dataModelList: List<DataModel>): Boolean =
    !this.inverseName.isNullOrEmpty() || (this.kind == "alias" && !isFieldAlias(dataModelList) )

fun Field.isOneToManyRelation(dataModelList: List<DataModel>): Boolean =
    this.relatedEntities != null && this.isRelation(dataModelList)

fun Field.isManyToOneRelation(dataModelList: List<DataModel>): Boolean =
    this.relatedEntities == null && this.isRelation(dataModelList)

fun correctIconPath(iconPath: String): String {
    // removes extension
    val withoutExt = if (iconPath.contains(".")) {
        iconPath.substring(0, iconPath.lastIndexOf('.'))
    } else {
        iconPath
    }
    val correctedIconPath = withoutExt
        .replace(".+/".toRegex(), "")
        .removePrefix(File.separator)
        .toLowerCase()
        .replace("[^a-z0-9]+".toRegex(), "_")
    return correctedIconPath
}

fun getFormatNameForType(pathHelper: PathHelper, dataModelList: List<DataModel>, form: Form, formField: Field): String {
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, formField) ?: return ""
    println("getFormatNameForType, [${fieldFromDataModel.name}] field: $fieldFromDataModel")
    val format = fieldFromDataModel.format
    if (format.equals("integer")) {
        return when (fieldFromDataModel.fieldType) {
            6 -> "boolInteger" // Boolean
            11 -> "timeInteger" // Time
            else -> "integer"
        }
    }
    if (format.isNullOrEmpty()) {
        if (fieldFromDataModel.kind == "alias" && fieldFromDataModel.path?.contains(".") == true)
            return ""
        return when (typeFromTypeInt(fieldFromDataModel.fieldType)) {
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

                val fieldMapping = getFieldMappingFormatter(it, format)
                return if (fieldMapping.isValidFormatter() || fieldMapping.isValidKotlinCustomDataFormatter()) {
                    format
                } else {
                    when (typeFromTypeInt(fieldFromDataModel.fieldType)) {
                        OBJECT_TYPE -> "yaml"
                        else -> ""
                    }
                }
            }
        }
        return format
    }
}

fun getDataModelField(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Field? {
    println("getDataModelField [${field.name}], field: $field")
    val partCount = field.name.count { it == '.'}
    val dataModel = dataModelList.find { it.id == dataModel.id }
    if (field.name.contains(".")) {
        println("getDataModelField field.name contains '.'")
        dataModel?.relations?.find { it.name == field.name.split(".")[0] }?.let { fieldInRelationList ->
            println("fieldInRelationList: $fieldInRelationList")
             fieldInRelationList.subFields.find { it.name == field.name.split(".")[1] }?.let { son ->
                 println("son: $son")
                 if (partCount > 1) {
                     son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                         println("grandSon: $grandSon")
                         return grandSon
                     }
                 } else {
                     return son
                 }
             }
        }
        println("getDataModelField [${field.name}] not found in relations, going to check in fields")
        dataModel?.fields?.find { it.name == field.name.split(".")[0] }?.let { fieldInFieldList ->
            println("fieldInFieldList: $fieldInFieldList")
            fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                println("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                        println("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        println("getDataModelField [${field.name}] not found in fields, going to check fieldAliases in fields")
        dataModel?.fields?.find { it.path == field.name && field.kind == "alias" }?.let { aliasFieldInFieldList ->
            println("aliasFieldInFieldList: $aliasFieldInFieldList")
            return aliasFieldInFieldList
        }

    } else {
        println("getDataModelField field.name doesn't contain '.'")

        dataModel?.fields?.find { it.name == field.name }?.let { field ->
            println("Found field with this name: $field")
            return field
        }

        val fieldPath = field.path ?: ""
        val pathPartCount = fieldPath.count { it == '.'}
        if (fieldPath.contains(".")) {         // TODO: TO CHECK
            println("getDataModelField fieldPath contains '.'")
            dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
                println("fieldInFieldList: $fieldInFieldList")
                fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
                    println("son: $son")
                    if (pathPartCount > 1) {
                        son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
                            println("grandSon: $grandSon")
                            return grandSon
                        }
                    } else {
                        return son
                    }
                }
            }
        }
    }

    println("getDataModelField returns null")
    return null
}

fun getDataModelFieldFromPath(dataModelList: List<DataModel>, fieldFromDataModel: Field, fieldPath: String): Field? {
    println("getDataModelFieldFromPath fieldPath [$fieldPath]")
    val partCount = fieldPath.count { it == '.'}
    val dataModel = dataModelList.find { it.id == fieldFromDataModel.relatedTableNumber.toString() }
    if (fieldPath.contains(".")) {
        println("getDataModelFieldFromPath field.name contains '.'")
        dataModel?.relations?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInRelationList ->
            println("fieldInRelationList: $fieldInRelationList")
            fieldInRelationList.subFields.find { it.name == fieldPath.split(".")[1] }?.let { son ->
                println("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == fieldPath.split(".")[2] }?.let { grandSon ->
                        println("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        println("getDataModelFieldFromPath [$fieldPath] not found in relations, going to check in fields")
        dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
            println("fieldInFieldList: $fieldInFieldList")
            fieldInFieldList.subFieldsForAlias?.find { it.name == fieldPath.split(".")[1] }?.let { son ->
                println("son: $son")
                if (partCount > 1) {
                    son.subFieldsForAlias?.find { it.name == fieldPath.split(".")[2] }?.let { grandSon ->
                        println("grandSon: $grandSon")
                        return grandSon
                    }
                } else {
                    return son
                }
            }
        }
        println("getDataModelFieldFromPath [$fieldPath] not found in fields, going to check fieldAliases in fields")
        dataModel?.fields?.find { it.path == fieldPath }?.let { aliasFieldInFieldList ->
            println("aliasFieldInFieldList: $aliasFieldInFieldList")
            return aliasFieldInFieldList
        }

    } else {
        println("getDataModelField field.name doesn't contain '.'")

        dataModel?.fields?.find { it.name == fieldPath }?.let { field ->
            println("Found field with this name: $field")
            return field
        }
//
//        val fieldPath = field.path ?: ""
//        val pathPartCount = fieldPath.count { it == '.'}
//        if (fieldPath.contains(".")) {         // TODO: TO CHECK
//            println("getDataModelField fieldPath contains '.'")
//            dataModel?.fields?.find { it.name == fieldPath.split(".")[0] }?.let { fieldInFieldList ->
//                println("fieldInFieldList: $fieldInFieldList")
//                fieldInFieldList.subFieldsForAlias?.find { it.name == field.name.split(".")[1] }?.let { son ->
//                    println("son: $son")
//                    if (pathPartCount > 1) {
//                        son.subFieldsForAlias?.find { it.name == field.name.split(".")[2] }?.let { grandSon ->
//                            println("grandSon: $grandSon")
//                            return grandSon
//                        }
//                    } else {
//                        return son
//                    }
//                }
//            }
//        }
    }

    println("getDataModelField returns null")
    return null
}

/**
 * Returns true if it has %length% placeholder AND it is a 1-N relation
 */

fun hasLabelPercentPlaceholder(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Boolean {
    println("hasLabelPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, field) ?: return false
    println("fieldFromDataModel: $fieldFromDataModel")
    val hasPercentPlaceholder = hasPercentPlaceholder(fieldFromDataModel.getLabel(), dataModelList, dataModel, field, fieldFromDataModel)
    println("hasPercentPlaceholder : $hasPercentPlaceholder")
    return hasPercentPlaceholder
}

fun hasShortLabelPercentPlaceholder(dataModelList: List<DataModel>, dataModel: DataModel, field: Field): Boolean {
    println("hasShortLabelPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, field) ?: return false
    println("fieldFromDataModel: $fieldFromDataModel")
    val hasPercentPlaceholder = hasPercentPlaceholder(fieldFromDataModel.getShortLabel(), dataModelList, dataModel, field, fieldFromDataModel)
    println("hasPercentPlaceholder : $hasPercentPlaceholder")
    return hasPercentPlaceholder
}

private fun hasPercentPlaceholder(label: String, dataModelList: List<DataModel>, dataModel: DataModel, formField: Field, fieldFromDataModel: Field): Boolean {
    println("hasPercentPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, fieldFromDataModel)
    println("hasLengthPlaceholder $hasLengthPlaceholder")
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, dataModel, formField)
    println("hasFieldPlaceholder $hasFieldPlaceholder")
    return hasLengthPlaceholder || hasFieldPlaceholder
}

private fun hasLengthPlaceholder(label: String, dataModelList: List<DataModel>, fieldFromDataModel: Field): Boolean {
    println("hasLengthPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val isRelation = fieldFromDataModel.isRelation(dataModelList)
    println("isRelation: $isRelation")
    if (!isRelation) return false
    val isOneToManyRelation = fieldFromDataModel.isOneToManyRelation(dataModelList)
    println("isOneToManyRelation = $isOneToManyRelation")
    if (!isOneToManyRelation) return false
    return label.contains("%length%")
}

fun hasFieldPlaceholder(label: String, dataModelList: List<DataModel>, dataModel: DataModel, formField: Field): Boolean {
    println("hasFieldPlaceholder field [${formField.name}] label [$label] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, formField) ?: return false
    println("fieldFromDataModel : $fieldFromDataModel")
    val isRelation = fieldFromDataModel.isRelation(dataModelList)
    println("isRelation: $isRelation")
    if (!isRelation) return false
    val isManyToOneRelation = fieldFromDataModel.isManyToOneRelation(dataModelList)
    println("isManyToOneRelation = $isManyToOneRelation")
    if (!isManyToOneRelation) return false
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    regex.findAll(label).forEach { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        println("Regexed fieldName: $fieldName")
        // Verify that fieldName exists in source dataModel
        val fieldExists = getDataModelFieldFromPath(dataModelList, fieldFromDataModel, fieldName) != null
        println("fieldExists: $fieldExists")
        return fieldExists
    }
    return false
}

fun getLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    println("getLabelWithPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    println("fieldFromDataModel: $fieldFromDataModel")
    return replacePercentPlaceholder(fieldFromDataModel.getLabel(), dataModelList, form, field, catalogDef)
}

fun getShortLabelWithPercentPlaceholder(dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    println("getShortLabelWithPercentPlaceholder field [${field.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    println("fieldFromDataModel: $fieldFromDataModel")
    return replacePercentPlaceholder(fieldFromDataModel.getShortLabel(), dataModelList, form, field, catalogDef)
}

fun getEntryRelation(dataModelList: List<DataModel>, source: String, formField: Field, catalogDef: CatalogDef): String {
    println("getEntryRelation field [${formField.name}] ////")
    return if (formField.name.contains(".")) {
        val relation: Relation? = findRelation(dataModelList, source, formField)
        println("relation = $relation")
        if (relation != null)
            "entityData." + relation.name.relationNameAdjustment() + "." + getPathToManyWithoutFirst(relation, catalogDef)
        else
            "entityData." + formField.getFieldAliasName(dataModelList)
    } else {
        "entityData." + formField.getFieldAliasName(dataModelList)
    }
}

private fun replacePercentPlaceholder(label: String, dataModelList: List<DataModel>, form: Form, field: Field, catalogDef: CatalogDef): String {
    println("replacePercentPlaceholder field [${field.name}] label [$label] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, form.dataModel, field) ?: return ""
    val hasLengthPlaceholder = hasLengthPlaceholder(label, dataModelList, fieldFromDataModel)
    val labelWithLength = if (hasLengthPlaceholder) {

        val labelWithSize = if (field.name.contains(".")) {
            val relation: Relation? = findRelation(dataModelList, form.dataModel.name, field)
            println("relation = $relation")
            if (relation != null)
                "entityData." + relation.name.relationNameAdjustment() + "." + getPathToManyWithoutFirst(relation, catalogDef) + ".size"
            else
                "entityData." + field.getFieldAliasName(dataModelList) + ".size"

        } else {
            "entityData." + field.getFieldAliasName(dataModelList) + ".size"
        }
        label.replace("%length%", "\" + $labelWithSize + \"")
    } else {
        label
    }
    val hasFieldPlaceholder = hasFieldPlaceholder(label, dataModelList, form.dataModel, field)
    return if (hasFieldPlaceholder) {
        replaceFieldPlaceholder(labelWithLength, dataModelList, fieldFromDataModel)
    } else {
        cleanPrefixSuffix("\"$labelWithLength\"")
    }
}

private fun replaceFieldPlaceholder(label: String, dataModelList: List<DataModel>, fieldFromDataModel: Field): String {
    println("replaceFieldPlaceholder field [${fieldFromDataModel.name}] label [$label] ////")
    val labelWithoutRemainingLength = label.replace("%length%", "")
    val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
    val newLabel = regex.replace(labelWithoutRemainingLength) { matchResult ->
        val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
        println("Regexed fieldName: $fieldName")
        val fieldInLabel: Field? = getDataModelFieldFromPath(dataModelList, fieldFromDataModel, fieldName)
        println("fieldInLabel: $fieldInLabel")
        if (fieldInLabel != null)
            "\" + entityData.${fieldFromDataModel.name.fieldAdjustment()}.${fieldName.fieldAdjustment()}.toString() + \""
        else
            ""
    }
    val labelWithField = "\"" + newLabel.removePrefix(" ").removeSuffix(" ") + "\""
    return cleanPrefixSuffix(labelWithField)
}

fun cleanPrefixSuffix(label: String): String {
    return label.removePrefix("\"\" + ").removeSuffix(" + \"\"").removePrefix("\" + ").removeSuffix(" + \"")
}

fun getNavbarTitle(dataModelList: List<DataModel>, form: Form, formField: Field, catalogDef: CatalogDef): String {
    return getNavbarTitle(dataModelList, form.dataModel, formField, catalogDef)
}

fun getNavbarTitle(dataModelList: List<DataModel>, dataModel: DataModel, formField: Field, catalogDef: CatalogDef): String {
    println("getNavbarTitle - getNavbarTitle field [${formField.name}] ////")
    val fieldFromDataModel: Field = getDataModelField(dataModelList, dataModel, formField) ?: return ""
    println("getNavbarTitle - fieldFromDataModel: $fieldFromDataModel")

    fieldFromDataModel.format?.let { format ->
        println("getNavbarTitle - format: $format")

        val regex = ("((?:%[\\w|\\s|\\.]+%)+)").toRegex()
        val formatWithoutRemainingLength = format.replace("%length%", "")
        val navbarTitle = regex.replace(formatWithoutRemainingLength) { matchResult ->
            val fieldName = matchResult.destructured.component1().removePrefix("%").removeSuffix("%")
            // Verify that fieldName exists in source dataModel
            println("Regexed fieldName: $fieldName")
            val path = unAliasPath(fieldName, dataModel.name, catalogDef)
            println("path = $path")
            val endFieldName = path.substringAfterLast(".").fieldAdjustment()
            val destBeforeField = destBeforeField(catalogDef, dataModel.name, path)
            val endField = dataModelList.find { it.name.tableNameAdjustment() == destBeforeField.tableNameAdjustment() }?.fields?.find { it.name.fieldAdjustment() == endFieldName }

            println("endField: $endField")

            if (endField?.isNativeType(dataModelList) == true) {

                val fieldAliasName = endField.getFieldAliasName(dataModelList)
                println("fieldAliasName = $fieldAliasName")

                when {
                    path.contains(".") -> {
                        val relation = findRelationFromPath(dataModelList, dataModel.name, path.substringBeforeLast("."))
                        if (relation != null) {
                            println("Found relation with same path with path $relation")

                            val pathToOneWithoutFirst = getPathToOneWithoutFirst(relation, catalogDef)
                            if (pathToOneWithoutFirst.isNotEmpty())
                                "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.${relation.name.relationNameAdjustment()}?.${pathToOneWithoutFirst}?.$fieldAliasName.toString()}"
                            else
                                "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.${relation.name.relationNameAdjustment()}?.$fieldAliasName.toString()}"
                        } else {
                            println("No relation found with path : ${path.substringBeforeLast(".")}")
                            fieldName
                        }
                    }

                    else -> {
                        "\${(roomEntity as ${dataModel.name.tableNameAdjustment()}RoomEntity?)?.__entity?.${fieldAliasName.fieldAdjustment()}.toString()}"
                    }
                }
            } else {
                fieldName
            }
        }
        println("navbarTitle: $navbarTitle")
        return navbarTitle
    }

    println("fieldFromDataModel.format = null")
    return dataModelList.find { it.name == fieldFromDataModel.relatedEntities }?.label
            ?: dataModelList.find { it.name == fieldFromDataModel.relatedDataClass }?.label
            ?: ""
}

/**
 * fieldFromDataModel is here to get the Field from dataModel instead of list/detail form as some information may be missing.
 * If it's a related field, fieldFromDataModel will be null, therefore check the field variable
*/

fun getShortLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.getShortLabel() ?: ""
}

fun getLabelWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): String {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.getLabel() ?: ""
}

fun isRelationWithFixes(dataModelList: List<DataModel>, form: Form, field: Field): Boolean {
    val fieldFromDataModel: Field? = getDataModelField(dataModelList, form.dataModel, field)
    return fieldFromDataModel?.isRelation(dataModelList) ?: false
}
