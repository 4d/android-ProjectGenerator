data class Relation(
    val source: String,
    val target: String,
    val name: String,
    val relationType: RelationType,
    var subFields: List<Field>,
    val inverseName: String,
    var associatedViewId: String
)