data class TemplateFormFieldFiller(
    val name: String,
    val label: String,
    val viewId: Int,
    val isRelation: Boolean,
    val isImage: Boolean,
    val accessor: String,
//    val isFormatted: Boolean,
    val isCustomFormat: Boolean,
//    val formatFunction: String,
    val formatType: String,
    val imageFieldName: String,
    val imageKeyAccessor: String,
    val imageTableName: String,
    val isInt: Boolean
)
