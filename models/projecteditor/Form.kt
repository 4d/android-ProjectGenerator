data class Form(
        var dataModel: DataModel,
        var name: String? = null,
        var fields: List<Field>? = null
)

fun createFormField(field: Field, i: Int, isFormatted: Boolean, formatFunction: String = "", formatType: String = ""): TemplateFormFieldFiller{
    return TemplateFormFieldFiller(name = field.name.condenseSpaces(), label = field.label
        ?: field.name, viewId = i, isRelation = field.inverseName != null, isImage = field.fieldType == 3,
        layout_variable_accessor = if (field.name.contains(".")) "" else ".entity", isFormatted = isFormatted,
        formatFunction = formatFunction, formatType = formatType)
}