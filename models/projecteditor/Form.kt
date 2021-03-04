data class Form(
        var dataModel: DataModel,
        var name: String? = null,
        var fields: List<Field>? = null
)

fun createFormField(field: Field, i: Int): TemplateFormFieldFiller {
    return TemplateFormFieldFiller(name = field.name.condensePropertyName(), label = field.label
            ?: field.name, viewId = i, isRelation = field.inverseName != null, isImage = field.fieldType == 3)
}

fun createFormField(customNameWithFormatTemplate: String ,field: Field, i: Int): TemplateFormFieldFiller{
    return TemplateFormFieldFiller(name = customNameWithFormatTemplate, label = field.label
        ?: field.name, viewId = i, isRelation = field.inverseName != null, isImage = field.fieldType == 3)
}