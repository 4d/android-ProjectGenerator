data class TemplateFormFieldFiller(
    val name: String,
    val label: String,
    val viewId: Int,
    val isRelation: Boolean,
    val isImage: Boolean,
    val layout_variable_accessor: String,
    val isFormatted: Boolean,
    val formatFunction: String,
    val formatType: String
)
