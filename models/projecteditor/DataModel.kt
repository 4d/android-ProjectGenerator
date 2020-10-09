data class DataModel(
        var id: String? = null,
        var name: String,
        var label: String? = null,
        var shortLabel: String? = null,
        var fields: List<Field>? = null,
        var query: String? = null,
        var iconPath: String? = null,
        var isSlave: Boolean? = null,
        var relationList: List<Relation>? = null
)
