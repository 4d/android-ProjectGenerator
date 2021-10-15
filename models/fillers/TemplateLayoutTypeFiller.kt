data class TemplateLayoutTypeFiller(val name: String, val layout_manager_type: String)

fun getTemplateLayoutTypeFiller(tableName: String, formPath: String): TemplateLayoutTypeFiller =
    TemplateLayoutTypeFiller(name = tableName, layout_manager_type = getLayoutManagerType(formPath))

fun getLayoutManagerType(formPath: String): String {
    Log.i("getLayoutManagerType: $formPath")
    var type = "Collection"
    getManifestJSONContent(formPath)?.let {
        type = it.getSafeObject("tags")?.getSafeString("___LISTFORMTYPE___") ?: "Collection"
    }
    return when (type) {
        "Collection" -> "GRID"
        "Table" -> "LINEAR"
        else -> "LINEAR"
    }
}