data class DataModel(
        var id: String? = null,
        var name: String,
        var label: String? = null,
        var shortLabel: String? = null,
        var fields: MutableList<Field>? = null,
        var query: String? = null,
        var iconPath: String? = null,
        var isSlave: Boolean? = null,
        var relationList: MutableList<Relation>? = null
)

fun DataModel.getLabel(): String {
        var label = this.name
        this.label?.let {
                label = it
        } ?: kotlin.run {
                this.shortLabel?.let {
                        label = it
                }
        }
        return label
}