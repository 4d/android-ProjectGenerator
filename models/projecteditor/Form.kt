data class Form(
    var dataModel: DataModel,
    var name: String? = null,
    var fields: List<Field>? = null
)

fun createDetailFormField(
    field: Field,
    i: Int,
    dataModelList: List<DataModel>,
    form: Form,
    isFormatted: Boolean,
    isCustom:Boolean,
    formatFunction: String = "",
    formatType: String = ""
): TemplateFormFieldFiller {

    return TemplateFormFieldFiller(
        name = field.name.fieldAdjustment(),
        label = field.getLabel(),
        viewId = i,
        isRelation = field.inverseName != null,
        isImage = field.isImage(),
        accessor = field.getLayoutVariableAccessor(FormType.DETAIL),
        isFormatted = isFormatted,
        isCustom= isCustom,
        formatFunction = formatFunction,
        formatType = formatType,
        imageFieldName = field.getImageFieldName(),
        imageKeyAccessor = field.getImageKeyAccessor(FormType.DETAIL),
        imageTableName = field.getImageTableName(dataModelList, form),
        isInt = field.isInt()
    )
}