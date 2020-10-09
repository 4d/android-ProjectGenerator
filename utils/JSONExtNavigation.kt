import ProjectEditorConstants.MAIN_KEY
import ProjectEditorConstants.ORDER_KEY
import ProjectEditorConstants.PROJECT_KEY
import org.json.JSONObject

fun JSONObject.getNavigationTableList(): List<String> {
    val array = this.getSafeObject(PROJECT_KEY)?.getSafeObject(MAIN_KEY)?.getSafeArray(ORDER_KEY)
    return array.getStringList()
}
