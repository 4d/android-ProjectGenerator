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
    imageHeight: Int,
    wholeFormHasIcons: Boolean
): TemplateFormFieldFiller {
    Log.d("createDetailFormField : field = $field")
    Log.d("createDetailFormField : field.fieldName() = ${field.getFieldName()}")

    val templateFormFieldFiller = TemplateFormFieldFiller(
        name = field.name.fieldAdjustment(),
        label = getLabelWithFixes(dataModelList, form, field),
        shortLabel = getShortLabelWithFixes(dataModelList, form, field),
        viewId = i,
        isRelation = field.inverseName != null,
        isImage = field.isImage(),
        sourceTableName = field.getSourceTableName(dataModelList, form),
        accessor = field.getLayoutVariableAccessor(FormType.DETAIL),
        isCustomFormat = formatType.startsWith("/"),
        formatFieldName = field.name,
        isImageNamed = isImageNamed,
        formatType = formatType,
        fieldName = field.getFieldName(),
        imageKeyAccessor = field.getFieldKeyAccessor(FormType.DETAIL),
        fieldTableName = form.dataModel.name,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        hasIcon = if (field.inverseName != null) getIconWithFixes(dataModelList, form, field) != "" else wholeFormHasIcons,
        iconPath = getIconWithFixes(dataModelList, form, field)
    )
    Log.d("createDetailFormField : templateFormFieldFiller = $templateFormFieldFiller")
    return templateFormFieldFiller
}