data class TemplatePermissionFiller(
    val permission: String
)

fun getTemplatePermissionFiller(name: String): TemplatePermissionFiller {
    val line = when (name) {
        "android.permission.WRITE_EXTERNAL_STORAGE" -> "<uses-permission\n" +
                "        android:name=\"android.permission.WRITE_EXTERNAL_STORAGE\"\n" +
                "        android:maxSdkVersion=\"28\" />"
        else -> "<uses-permission\n" +
                "        android:name=\"$name\" />"
    }
    return TemplatePermissionFiller(permission = line)
}