data class TemplateFormFieldFiller(
    val name: String,
    val label: String,
    val viewId: Int,
    val isRelation: Boolean,
    val isImage: Boolean,
    val accessor: String,
    val isCustomFormat: Boolean,
    val isImageNamed: Boolean,
    val formatType: String,
    val fieldName: String,
    val imageKeyAccessor: String,
    val fieldTableName: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val isInt: Boolean
)
