data class TemplateLayoutTypeFiller(val name: String, val layout_manager_type: String, val isSwipeAllowed: Boolean)

fun getTemplateLayoutTypeFiller(tableName: String, formPath: String): TemplateLayoutTypeFiller =
        TemplateLayoutTypeFiller(name = tableName, layout_manager_type = getLayoutManagerType(formPath), isSwipeAllowed = isSwipeAllowed(formPath))

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

fun isSwipeAllowed(formPath: String): Boolean {
    getManifestJSONContent(formPath)?.let {
        val isSwipeAllowed: Boolean? = it.getSafeObject("tags")?.getSafeBoolean("swipe")
        isSwipeAllowed?.let { isAllowed ->
            return isAllowed
        }
    }
    return when (getLayoutManagerType(formPath)) {
        "Collection" -> false
        "Table" -> true
        else -> true
    }
}




