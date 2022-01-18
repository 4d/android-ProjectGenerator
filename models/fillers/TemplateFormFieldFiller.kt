data class TemplateFormFieldFiller(
    val name: String,
    val label: String,
    val shortLabel: String,
    val viewId: Int,
    val isRelation: Boolean,
    val isImage: Boolean,
    val sourceTableName: String,
    val accessor: String,
    val isCustomFormat: Boolean,
    val formatFieldName: String,
    val isImageNamed: Boolean,
    val formatType: String,
    val fieldName: String,
    val imageKeyAccessor: String,
    val fieldTableName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val hasIcon: Boolean,
    val iconPath: String,
    val labelHasPercentPlaceholder: Boolean,
    val labelWithPercentPlaceholder: String,
    val shortLabelHasPercentPlaceholder: Boolean,
    val shortLabelWithPercentPlaceholder: String,
    val entryRelation: String,
    val altButtonText: String,
    val isKotlinCustomFormat: Boolean,
    val kotlinCustomFormatBinding: String
)

fun Field.getTemplateFormFieldFiller(
    i: Int,
    dataModelList: List<DataModel>,
    form: Form,
    formatType: String,
    isImageNamed: Boolean,
    imageWidth: Int,
    imageHeight: Int,
    wholeFormHasIcons: Boolean,
    pathHelper: PathHelper
): TemplateFormFieldFiller {
    Log.d("createDetailFormField : field = $this")
    Log.d("createDetailFormField : field.fieldName() = ${this.getFieldName()}")

    val templateFormFieldFiller = TemplateFormFieldFiller(
        name = this.name.fieldAdjustment(),
        label = getLabelWithFixes(dataModelList, form, this),
        shortLabel = getShortLabelWithFixes(dataModelList, form, this),
        viewId = i,
        isRelation = isRelationWithFixes(dataModelList, form, this),
        isImage = this.isImage(),
        sourceTableName = this.getSourceTableName(dataModelList, form),
        accessor = this.getLayoutVariableAccessor(FormType.DETAIL),
        isCustomFormat = pathHelper.isValidFormatter(formatType),
        formatFieldName = this.name,
        isImageNamed = isImageNamed,
        formatType = formatType,
        fieldName = this.getFieldName(),
        imageKeyAccessor = this.getFieldKeyAccessor(FormType.DETAIL),
        fieldTableName = form.dataModel.name,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        hasIcon = wholeFormHasIcons,
        iconPath = getIconWithFixes(dataModelList, form, this),
        labelHasPercentPlaceholder = hasLabelPercentPlaceholder(dataModelList, form, this),
        labelWithPercentPlaceholder = getLabelWithPercentPlaceholder(dataModelList, form, this, FormType.DETAIL),
        shortLabelHasPercentPlaceholder = hasShortLabelPercentPlaceholder(dataModelList, form, this),
        shortLabelWithPercentPlaceholder = getShortLabelWithPercentPlaceholder(dataModelList, form, this, FormType.DETAIL),
        entryRelation = this.name.fieldAdjustment(),
        altButtonText = if (hasFieldPlaceholder(getShortLabelWithFixes(dataModelList, form, this), dataModelList, form, this)) "" else getShortLabelWithFixes(dataModelList, form, this),
        isKotlinCustomFormat = !pathHelper.isValidFormatter(formatType) && pathHelper.isValidKotlinCustomFormatter(formatType),
        kotlinCustomFormatBinding = if (!pathHelper.isValidFormatter(formatType) && pathHelper.isValidKotlinCustomFormatter(formatType)) pathHelper.getKotlinCustomFormatterBinding(formatType) else ""
    )
    Log.d("createDetailFormField : templateFormFieldFiller = $templateFormFieldFiller")
    return templateFormFieldFiller
}