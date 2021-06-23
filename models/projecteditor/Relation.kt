data class Relation(
        val source: String,
        val target: String,
        val name: String,
        val relationType: RelationType,
        val subFields: List<Field>
)
