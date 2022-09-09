import org.json.JSONObject

data class TemplatePermissionFiller(
    val permission: String
)

object Permissions {
    const val WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE"
    const val CAMERA = "android.permission.CAMERA"
    const val READ_CONTACTS = "android.permission.READ_CONTACTS"
}

/**
 * Special case for WRITE_EXTERNAL_STORAGE
 * For Camera, adds <uses-feature /> line
 */
fun getTemplatePermissionFiller(name: String): TemplatePermissionFiller {
    val line = when (name) {
        Permissions.WRITE_EXTERNAL_STORAGE -> "<uses-permission\n" +
                "        android:name=\"$name\"\n" +
                "        android:maxSdkVersion=\"28\" />"
        Permissions.CAMERA -> "<uses-feature android:name=\"android.hardware.camera\"/>\n" +
                "<uses-permission android:name=\"$name\" />"
        else -> "<uses-permission android:name=\"$name\" />"
    }
    return TemplatePermissionFiller(permission = line)
}

fun JSONObject?.checkCapabilities(): List<String> {
    return when {
        this?.getSafeArray("android") != null -> this.getSafeArray("android").getStringList()
        this?.getSafeBoolean("contacts") == true -> listOf(Permissions.READ_CONTACTS)
        this?.getSafeBoolean("location") == true -> listOf() // TODO
        else -> listOf()
    }
}