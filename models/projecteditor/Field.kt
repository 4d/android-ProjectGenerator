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
)

fun isPrivateRelationField(fieldName: String): Boolean = fieldName.startsWith("__") && fieldName.endsWith("Key")

fun Field.isImage() = this.fieldType == 3

fun Field.getImageFieldName() =
    if (this.name.fieldAdjustment().contains("."))
        this.name.fieldAdjustment().split(".")[1]
    else
        this.name.fieldAdjustment()

fun Field.getImageKeyAccessor(formType: FormType) =
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

fun Field.getImageTableName(dataModelList: List<DataModel>, form: Form): String {
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
    var label = this.name
    this.label?.let {
        label = it
    } ?: kotlin.run {
        this.shortLabel?.let {
            label = it
        }
    }
    return label
}

fun Field.isInt(): Boolean = this.fieldType == 8 || this.fieldType == 9 || this.fieldType == 25