data class FieldData(
    var name: String,
    var isImage: Boolean = false,
    var isOneToManyRelation: Boolean = false,
    var isManyToOneRelation: Boolean = false,
    var relatedOriginalTableName: String? = null
)