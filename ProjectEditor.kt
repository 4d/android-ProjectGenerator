import DefaultValues.DEFAULT_LOG_LEVEL
import DefaultValues.DEFAULT_REMOTE_URL
import ProjectEditorConstants.AUTHENTICATION_KEY
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.CACHE_4D_SDK_KEY
import ProjectEditorConstants.DATASOURCE_KEY
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.DEVELOPER_KEY
import ProjectEditorConstants.DOMINANT_COLOR_KEY
import ProjectEditorConstants.EMAIL_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.ORGANIZATION_KEY
import ProjectEditorConstants.PACKAGE_KEY
import ProjectEditorConstants.PATH_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PRODUCTION_KEY
import ProjectEditorConstants.PRODUCT_KEY
import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.REMOTE_URL_KEY
import ProjectEditorConstants.SDK_KEY
import ProjectEditorConstants.SERVER_KEY
import ProjectEditorConstants.SOURCE_KEY
import ProjectEditorConstants.STRING_TYPE
import ProjectEditorConstants.TEAMID_KEY
import ProjectEditorConstants.TIME_TYPE
import ProjectEditorConstants.UI_KEY
import ProjectEditorConstants.URLS_KEY
import models.action.ActionsListContent
import org.json.JSONObject
import java.io.File

class ProjectEditor(projectEditorFile: File) {

    lateinit var dataModelList: List<DataModel>
    lateinit var listFormList: List<Form>
    lateinit var detailFormList: List<Form>
    lateinit var navigationTableList: List<String>
    lateinit var searchableFields: HashMap<String, List<String>>
    lateinit var actionsList: ActionsListContent

    lateinit var jsonObj: JSONObject

    init {
        val jsonString = projectEditorFile.readFile()
        Log.plantTree(this::class.java.canonicalName)

        if (jsonString.isEmpty()) {
            throw Exception("Json file ${projectEditorFile.name} is empty")
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            navigationTableList = jsonObj.getNavigationTableList()
            Log.d("> Navigation tables list successfully read.")

            dataModelList = jsonObj.getDataModelList()
            Log.d("> DataModels list successfully read.")

            searchableFields = jsonObj.getSearchFields(dataModelList)
            Log.d("> Searchable fields successfully read.")

            listFormList = jsonObj.getFormList(dataModelList, FormType.LIST, navigationTableList)
            Log.d("> List forms list successfully read.")

            detailFormList = jsonObj.getFormList(dataModelList, FormType.DETAIL, navigationTableList)
            Log.d("> Detail forms list successfully read.")

            actionsList = jsonObj.getActionsList()
            Log.d("> Actions  list successfully read.")

        } ?: kotlin.run {
            Log.e("Could not read global json object from file ${projectEditorFile.name}")
        }
    }

    fun findJsonString(key: String): String? {
        return when (key) {
            "author" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DEVELOPER_KEY)?.getSafeString(NAME_KEY)
            "targetDirPath" -> jsonObj.getSafeString(PATH_KEY)
            "androidSdk" -> jsonObj.getSafeString(SDK_KEY)
            "cache4dSdk" -> jsonObj.getSafeString(CACHE_4D_SDK_KEY)
            "companyWithCaps" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)
                ?.getSafeString(NAME_KEY)
            "appNameWithCaps" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(PRODUCT_KEY)?.getSafeString(NAME_KEY)
            "package" -> jsonObj.getSafeString(PACKAGE_KEY)
            "productionUrl" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)?.getSafeObject(URLS_KEY)
                ?.getSafeString(PRODUCTION_KEY)
            "remoteUrl" -> jsonObj.getSafeString(REMOTE_URL_KEY)
            "teamId" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)?.getSafeString(TEAMID_KEY)
            "embeddedData" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DATASOURCE_KEY)
                ?.getSafeString(SOURCE_KEY)
            "dominantColor" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(UI_KEY)
                ?.getSafeString(DOMINANT_COLOR_KEY)
            else -> return null
        }
    }

    fun findJsonBoolean(key: String): Boolean? {
        return when (key) {
            "mailAuth" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)
                ?.getSafeObject(AUTHENTICATION_KEY)?.getSafeBoolean(EMAIL_KEY)
            else -> return null
        }
    }

    fun getAppInfo(): AppInfo {
        val mailAuth = findJsonBoolean("mailAuth") ?: false
        var remoteUrl = findJsonString("productionUrl")
        if (remoteUrl.isNullOrEmpty())
            remoteUrl = findJsonString("remoteUrl")
        if (remoteUrl.isNullOrEmpty())
            remoteUrl = DEFAULT_REMOTE_URL
        val teamId = findJsonString("teamId") ?: ""
        return AppInfo(
            team = Team(TeamID = teamId, TeamName = ""),
            guestLogin = mailAuth.not(),
            remoteUrl = remoteUrl,
            initialGlobalStamp = 0,
            dumpedTables = mutableListOf(),
            logLevel = DEFAULT_LOG_LEVEL,
            relations = true
        )
    }
}


fun typeStringFromTypeInt(type: Int?): String = when (type) {
    0 -> STRING_TYPE
    1 -> FLOAT_TYPE
    2 -> STRING_TYPE
    3 -> PHOTO_TYPE
    4 -> STRING_TYPE
    5 -> EMPTY_TYPE
    6 -> BOOLEAN_TYPE
    7 -> EMPTY_TYPE
    8 -> INT_TYPE
    9 -> INT_TYPE
    11 -> STRING_TYPE
    12 -> EMPTY_TYPE
    25 -> INT_TYPE
    else -> EMPTY_TYPE
}

fun typeFromTypeInt(type: Int?): String = when (type) {
    0 -> STRING_TYPE
    1 -> FLOAT_TYPE
    2 -> STRING_TYPE
    3 -> PHOTO_TYPE
    4 -> DATE_TYPE
    5 -> EMPTY_TYPE
    6 -> BOOLEAN_TYPE
    7 -> EMPTY_TYPE
    8 -> INT_TYPE
    9 -> INT_TYPE
    11 -> TIME_TYPE
    12 -> EMPTY_TYPE
    25 -> INT_TYPE
    else -> EMPTY_TYPE
}
