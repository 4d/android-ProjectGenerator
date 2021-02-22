import DefaultValues.DEFAULT_REMOTE_URL
import ExitCodes.PROJECT_EDITOR_JSON_EMPTY
import ProjectEditorConstants.AUTHENTICATION_KEY
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.CACHE_4D_SDK_KEY
import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.DATASOURCE_KEY
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.DEVELOPER_KEY
import ProjectEditorConstants.EMAIL_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.LIST_KEY
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.ORGANIZATION_KEY
import ProjectEditorConstants.PATH_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PRODUCTION_KEY
import ProjectEditorConstants.PRODUCT_KEY
import ProjectEditorConstants.PROJECT_KEY
import ProjectEditorConstants.SDK_KEY
import ProjectEditorConstants.SEARCHABLE_KEY
import ProjectEditorConstants.SERVER_KEY
import ProjectEditorConstants.SOURCE_KEY
import ProjectEditorConstants.STRING_TYPE
import ProjectEditorConstants.TEAMID_KEY
import ProjectEditorConstants.TIME_TYPE
import ProjectEditorConstants.URLS_KEY
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class ProjectEditor(projectEditorFile: File) {

    lateinit var dataModelList: List<DataModel>
    lateinit var listFormList: List<Form>
    lateinit var detailFormList: List<Form>
    lateinit var navigationTableList: List<String>


    private val searchableFields = HashMap<String,List<String>>()


    private lateinit var jsonObj: JSONObject

    init {
        val jsonString = projectEditorFile.readFile()

        if (jsonString.isEmpty()) {
            println("Json file ${projectEditorFile.name} is empty")
            exitProcess(PROJECT_EDITOR_JSON_EMPTY)
        }



        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            dataModelList = jsonObj.getDataModelList()
            getColumnSearchable(jsonObj.getJSONObject(PROJECT_KEY).getJSONObject(LIST_KEY))


            listFormList = jsonObj.getFormList(dataModelList, FormType.LIST)
            println("> List forms list successfully read.")

            detailFormList = jsonObj.getFormList(dataModelList, FormType.DETAIL)
            println("> Detail forms list successfully read.")

            println("deatiledListForm :: ${detailFormList}")

            navigationTableList = jsonObj.getNavigationTableList()
            println("> Navigation tables list successfully read.")

        } ?: kotlin.run {
            println("Could not read global json object from file ${projectEditorFile.name}")
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
            "remoteUrl" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)?.getSafeObject(URLS_KEY)
                ?.getSafeString(PRODUCTION_KEY)
            "teamId" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)?.getSafeString(TEAMID_KEY)
            "embeddedData" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DATASOURCE_KEY)
                ?.getSafeString(SOURCE_KEY)
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
        var remoteUrl = findJsonString("remoteUrl")
        if (remoteUrl.isNullOrEmpty())
            remoteUrl = DEFAULT_REMOTE_URL
        val teamId = findJsonString("teamId") ?: ""
        val embeddedData = findJsonString("embeddedData") != null
        return AppInfo(
            team = Team(TeamID = teamId, TeamName = ""),
            guestLogin = mailAuth.not(),
            remoteUrl = remoteUrl,
            embeddedData = embeddedData,
            initialGlobalStamp = 0,
            searchableField = searchableFields
        )
    }

    //Fun Fetch Column
    private fun getColumnSearchable(datarecv: JSONObject?) {
        datarecv.let {

            for (i in 0 until it?.names()?.length()!!) {
                var columns = mutableListOf<String>()
                val searchableField = datarecv?.getSafeObject(it.names()[i] as String)
                    ?.getSafeArray(ProjectEditorConstants.SEARCHABLE_KEY)
                if (searchableField != null) {
                    for (index in 0 until searchableField.length()) {
                       columns.add(searchableField.getSafeObject(index)?.get(NAME_KEY) as String)
                    }
                } else {
                    columns.add(datarecv?.getSafeObject(it.names()[i] as String)
                        ?.getSafeObject(SEARCHABLE_KEY)?.get(NAME_KEY) as String)
                }
               searchableFields.put(getTableName(it.names()[i].toString()),columns)
            }

        }
    }


    private fun getTableName(index: String): String {
        val dataModel = jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DATAMODEL_KEY)?.getJSONObject(index)
        val newDataModelJSONObject = dataModel?.getSafeObject(ProjectEditorConstants.EMPTY_KEY)
        return newDataModelJSONObject?.get(NAME_KEY) as String
    }
}

fun typeStringFromTypeInt(type: Int?): String = when (type) {
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
