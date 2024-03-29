import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

fun JSONObject.getSafeObject(key: String): JSONObject? {
    return try {
        this.getJSONObject(key)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONObject.getSafeString(key: String): String? {
    return try {
        this.getString(key)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONObject.getSafeInt(key: String): Int? {
    return try {
        this.getInt(key)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONObject.getSafeBoolean(key: String): Boolean? {
    return try {
        this.getBoolean(key)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONObject.getSafeArray(key: String): JSONArray? {
    return try {
        this.getJSONArray(key)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONObject.toStringMap(): Map<String, Any> {
    val map: MutableMap<String, Any> = mutableMapOf()

    this.keys().forEach { key ->
        if (key !is String) return@forEach
        this.getSafeString(key)?.let { value ->
            map[key] = value
        }
        this.getSafeInt(key)?.let { value ->
            map[key] = value
        }
    }
    return map
}

fun JSONArray.getSafeString(position: Int): String? {
    return try {
        this.getString(position)
    } catch (e: JSONException) {
        return null
    }
}

fun JSONArray?.getStringList(): List<String> {
    val list = mutableListOf<String>()
    this?.let {
        for (i in 0 until this.length()) {
            list.add(this.getSafeString(i).toString())
        }
    }
    return list
}

fun JSONArray?.getObjectListAsString(): List<String> {
    val list = mutableListOf<String>()
    this?.let {
        for (i in 0 until this.length()) {
            val safeObject = this.getSafeObject(i)
            list.add(safeObject.toString())
        }
    }
    return list
}

fun JSONArray.getSafeObject(position: Int): JSONObject? {
    return try {
        this.getJSONObject(position)
    } catch (e: JSONException) {
        return null
    }
}

fun retrieveJSONObject(jsonString: String): JSONObject? {
    return try {
        JSONObject(jsonString.substring(jsonString.indexOf("{"), jsonString.lastIndexOf("}") + 1))
    } catch (e: StringIndexOutOfBoundsException) {
        null
    }
}
