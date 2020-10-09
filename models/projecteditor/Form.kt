enum class FormType {
    LIST, DETAIL
}

data class Form(
        var dataModel: DataModel,
        var name: String? = null,
        var fields: List<Field>? = null
)

fun createFormField(field: Field, i: Int): TemplateFormFieldFiller {
    return TemplateFormFieldFiller(name = field.name, label = field.label
            ?: field.name, viewId = i, isRelation = field.inverseName != null, isImage = field.fieldType == 3)
}
