import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.SCOPE_KEY
import org.json.JSONArray
import org.json.JSONObject


fun JSONObject?.getActionsList(dataModelList: List<DataModel>, nameInJson: String): JSONObject {
    val jsonArray = this?.getSafeObject(PROJECT_KEY)?.getSafeArray("actions")
    val allActions = mutableListOf<JSONObject>()
    val jsonObject = JSONObject()

    if (jsonArray == null)
    // Return empty jsonObject
        return jsonObject

    // get All actions
    for (i in 0 until jsonArray.length()) {
        val action = jsonArray.getSafeObject(i)
        if (action?.getSafeString(SCOPE_KEY) == nameInJson) {
            allActions.add(action)
        }
    }

    val actionsGroupedByTableNumber = HashMap(allActions.groupBy { it.getSafeInt("tableNumber") })
    dataModelList.forEach { keyDataModel ->
        actionsGroupedByTableNumber.values.forEach {
            val tableNumber = it.firstOrNull()?.getSafeInt("tableNumber")
            if (tableNumber.toString() == keyDataModel.id)
                jsonObject.put(keyDataModel.name.tableNameAdjustment(), it)
        }
    }

    return jsonObject
}
