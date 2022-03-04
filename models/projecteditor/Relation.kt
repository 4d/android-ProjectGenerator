data class Relation(
    val source: String,
    val target: String,
    val name: String,
    val type: RelationType,
    var subFields: List<Field>,
    val inverseName: String,
    val path: String
)