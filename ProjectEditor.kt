import DefaultValues.DEFAULT_REMOTE_URL
import ExitCodes.PROJECT_EDITOR_JSON_EMPTY
import ProjectEditorConstants.AUTHENTICATION_KEY
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.DEVELOPER_KEY
import ProjectEditorConstants.EMAIL_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.ORGANIZATION_KEY
import ProjectEditorConstants.PATH_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PRODUCTION_KEY
import ProjectEditorConstants.PRODUCT_KEY
import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.SDK_KEY
import ProjectEditorConstants.SERVER_KEY
import ProjectEditorConstants.STRING_TYPE
import ProjectEditorConstants.TEAMID_KEY
import ProjectEditorConstants.TEXT_TYPE
import ProjectEditorConstants.TIME_TYPE
import ProjectEditorConstants.URLS_KEY
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class ProjectEditor(projectEditorFile: File) {

    var dataModelList: List<DataModel>
    var listFormList: List<Form>
    var detailFormList: List<Form>
    var navigationTableList: List<String>

    private var jsonObj: JSONObject

    init {
        val jsonString = projectEditorFile.readFile()

        if (jsonString.isEmpty()) {
            println("Json file ${projectEditorFile.name} is empty")
            exitProcess(PROJECT_EDITOR_JSON_EMPTY)
        }

        jsonObj = retrieveJSONObject(jsonString)

        dataModelList = jsonObj.getDataModelList()
        println("> DataModels list successfully read.")

        listFormList = jsonObj.getListFormList(dataModelList)
        println("> List forms list successfully read.")

        detailFormList = jsonObj.getDetailFormList(dataModelList)
        println("> Detail forms list successfully read.")

        navigationTableList = jsonObj.getNavigationTableList()
        println("> Navigation tables list successfully read.")
    }

    fun findJsonString(key: String): String? {
        return when (key) {
            "author" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DEVELOPER_KEY)?.getSafeString(NAME_KEY)
            "targetDirPath" -> jsonObj.getSafeString(PATH_KEY)
            "androidSdk" -> jsonObj.getSafeString(SDK_KEY)
            "companyWithCaps" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)?.getSafeString(NAME_KEY)
            "appNameWithCaps" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(PRODUCT_KEY)?.getSafeString(NAME_KEY)
            "remoteUrl" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)?.getSafeObject(URLS_KEY)?.getSafeString(PRODUCTION_KEY)
            "teamId" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)?.getSafeString(TEAMID_KEY)
            else -> return null
        }
    }

    fun findJsonBoolean(key: String): Boolean? {
        return when (key) {
            "mailAuth" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)?.getSafeObject(AUTHENTICATION_KEY)?.getSafeBoolean(EMAIL_KEY)
            else -> return null
        }
    }

    fun getAppInfo(): AppInfo {
        val mailAuth = findJsonBoolean("mailAuth") ?: false
        var remoteUrl = findJsonString("remoteUrl")
        if (remoteUrl.isNullOrEmpty())
            remoteUrl = DEFAULT_REMOTE_URL
        val teamId = findJsonString("teamId") ?: ""
        val embeddedData = findJsonBoolean("embeddedData") ?: false
        return AppInfo(
                team = Team(TeamID = teamId, TeamName = ""),
                guestLogin = mailAuth.not(),
                remoteUrl = remoteUrl,
                embeddedData = embeddedData
        )
    }
}

fun typeStringFromTypeInt(type: Int?): String = when (type) {
    0 -> STRING_TYPE
    1 -> FLOAT_TYPE
    2 -> TEXT_TYPE
    3 -> PHOTO_TYPE
    4 -> DATE_TYPE
    5 -> EMPTY_TYPE
    6 -> BOOLEAN_TYPE
    7 -> EMPTY_TYPE
    8 -> INT_TYPE
    9 -> INT_TYPE
    10 -> TIME_TYPE
    12 -> EMPTY_TYPE
    25 -> INT_TYPE
    else -> EMPTY_TYPE
}
