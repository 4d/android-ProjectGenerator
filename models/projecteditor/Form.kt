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
    formatType: String,
    isImageNamed: Boolean,
    imageWidth: Int,
    imageHeight: Int
): TemplateFormFieldFiller {

    return TemplateFormFieldFiller(
        name = field.name.fieldAdjustment(),
        label = field.getLabel(),
        viewId = i,
        isRelation = field.inverseName != null,
        isImage = field.isImage(),
        accessor = field.getLayoutVariableAccessor(FormType.DETAIL),
        isCustomFormat = formatType.startsWith("/"),
        isImageNamed = isImageNamed,
        formatType = formatType,
        fieldName = field.getFieldName(),
        imageKeyAccessor = field.getFieldKeyAccessor(FormType.DETAIL),
        fieldTableName = field.getFieldTableName(dataModelList, form),
        imageWidth = imageWidth,
        imageHeight = imageHeight
    )
}