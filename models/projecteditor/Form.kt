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
//    isFormatted: Boolean,
//    isCustomFormat: Boolean,
//    formatFunction: String = "",
    formatType: String
): TemplateFormFieldFiller {

    return TemplateFormFieldFiller(
        name = field.name.fieldAdjustment(),
        label = field.getLabel(),
        viewId = i,
        isRelation = field.inverseName != null,
        isImage = field.isImage(),
        accessor = field.getLayoutVariableAccessor(FormType.DETAIL),
//        isFormatted = isFormatted,
        isCustomFormat = formatType.startsWith("/"),
//        formatFunction = formatFunction,
        formatType = formatType,
        imageFieldName = field.getFieldName(),
        imageKeyAccessor = field.getFieldKeyAccessor(FormType.DETAIL),
        imageTableName = field.getFieldTableName(dataModelList, form),
        isInt = field.isInt()
    )
}