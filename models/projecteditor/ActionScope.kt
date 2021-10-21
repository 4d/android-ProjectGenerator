import FileHelperConstants.ACTIONS_DETAILS_FILENAME
import FileHelperConstants.ACTIONS_LIST_FILENAME

enum class ActionScope(val fileName: String, val nameInJson: String) {
    CURRENT_RECORD(ACTIONS_DETAILS_FILENAME, "currentRecord"),
    LIST(ACTIONS_LIST_FILENAME, "table"),
}