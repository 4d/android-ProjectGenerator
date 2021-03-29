import DefaultValues.DEFAULT_REMOTE_URL
import ExitCodes.PROJECT_EDITOR_JSON_EMPTY
import ProjectEditorConstants.AUTHENTICATION_KEY
import ProjectEditorConstants.BACKGROUND_COLOR
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.CACHE_4D_SDK_KEY
import ProjectEditorConstants.COLORS
import ProjectEditorConstants.DATAMODEL_KEY
import ProjectEditorConstants.DATASOURCE_KEY
import ProjectEditorConstants.DEVELOPER_KEY
import ProjectEditorConstants.EMAIL_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.FOREGROUND_COLOR
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
import ProjectEditorConstants.URLS_KEY
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class ProjectEditor(projectEditorFile: File) {

    lateinit var dataModelList: List<DataModel>
    lateinit var listFormList: List<Form>
    lateinit var detailFormList: List<Form>
    lateinit var navigationTableList: List<String>
    private val searchableFields = HashMap<String, List<String>>()

    lateinit var jsonObj: JSONObject
    // Hold sort Filed
    val formatFields = HashMap<String,String>()
    var defaultFormatLFiledType = HashMap<String,String>()

    init {
        val jsonString = projectEditorFile.readFile()
        Log.plantTree(this::class.java.canonicalName)

        if (jsonString.isEmpty()) {
            Log.d("Json file ${projectEditorFile.name} is empty")
            exitProcess(PROJECT_EDITOR_JSON_EMPTY)
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            navigationTableList = jsonObj.getNavigationTableList()
            Log.d("> Navigation tables list successfully read.")

            dataModelList = jsonObj.getDataModelList()
            Log.d("> DataModels list successfully read.")

            getSearchableColums(jsonObj)
            Log.d("> Searchable fields successfully read.")

            setFormatFields()
            Log.d("> Format fields successfully read.")

            listFormList = jsonObj.getFormList(dataModelList, FormType.LIST, navigationTableList)
            Log.d("> List forms list successfully read.")

            detailFormList = jsonObj.getFormList(dataModelList, FormType.DETAIL, navigationTableList)
            Log.d("> Detail forms list successfully read.")

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
            "package" ->  jsonObj.getSafeString(PACKAGE_KEY)
            "productionUrl" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)?.getSafeObject(URLS_KEY)?.getSafeString(PRODUCTION_KEY)
            "remoteUrl" -> jsonObj.getSafeString(REMOTE_URL_KEY)
            "teamId" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(ORGANIZATION_KEY)?.getSafeString(TEAMID_KEY)
            "embeddedData" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DATASOURCE_KEY)
                ?.getSafeString(SOURCE_KEY)
            "backgroundColor" -> jsonObj.getSafeObject(COLORS)?.getSafeString(BACKGROUND_COLOR)
            "foregroundColor" -> jsonObj.getSafeObject(COLORS)?.getSafeString(FOREGROUND_COLOR)
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

    private fun getTableName(index: String): String? { // CLEAN there is already a DataModel object decoded
        val dataModel = jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(DATAMODEL_KEY)?.getSafeObject(index)
        val newDataModelJSONObject = dataModel?.getSafeObject(ProjectEditorConstants.EMPTY_KEY)
        return newDataModelJSONObject?.get(NAME_KEY) as? String
    }

    private fun getSearchableColums(datarecv: JSONObject?) {

        datarecv.let {
            if (datarecv!!.has("project")) {
                if (datarecv.getJSONObject("project").has("list")) {
                    val jsonrecv = datarecv.getJSONObject("project").getJSONObject("list")
                    val jsonKeys = jsonrecv.names()
                    Log.i("JSONArray :: $jsonrecv")
                    for (index in 0 until jsonKeys.length()) {
                        var columns = mutableListOf<String>()
                        val jsonObject = jsonrecv.getJSONObject(jsonKeys.getString(index))
                        if (jsonObject.has("searchableField")) {
                            val dat = jsonObject.getSafeArray("searchableField")
                            if (dat != null) {
                                for (ind in 0 until dat.length()) {
                                    columns.add(dat.getJSONObject(ind).get("name") as String)
                                }
                            } else {
                                if (!(jsonObject.get("searchableField")).equals(null)) {
                                    columns.add(jsonObject.getJSONObject("searchableField").get("name") as String)
                                } else {
                                    Log.w("searchableField is not available")
                                }
                            }
                        } else {
                            Log.w("No searchable Field Found")
                        }
                        if (columns.size != 0) {
                            getTableName(jsonrecv.names()[index].toString())?.let {
                                searchableFields.put(it, columns)
                            }
                        }
                    }

                }
            }
        }
    }

    private fun setFormatFields(){
        if (jsonObj.has("project")) {
            var project = jsonObj.getJSONObject("project")
            if (project.has("dataModel")) { // CLEAN there is already a DataModel object decoded
                //println("Data Model Present")
                val dataModel = project.getJSONObject("dataModel")
                val dataModeArray = dataModel.names()
                for (index in 0 until dataModeArray.length()) {
                    val tableKey = dataModeArray[index] as String
                    val fieldJSONObject = dataModel.getJSONObject(tableKey)
                    val fieldJSONArray = fieldJSONObject.names()
                    //if ()
                    for (ind in 0 until fieldJSONArray.length()) {
                        var fieldKey = fieldJSONArray[ind] as String
                        val jsonColumnObject = fieldJSONObject.getSafeObject(fieldKey)
                        // if (fieldKey.isNumber()) { // number key is associate to an object (but not all keys, could be trable property even if table property are not in key "" )
                        // else if relation
                        // else ignore

                        if (jsonColumnObject !=null && jsonColumnObject.has("format")){
                            if (jsonColumnObject.has("name")) {
                                formatFields.put(jsonColumnObject["name"].toString(),jsonColumnObject["format"].toString())
                           }
                        }else{
                            if(jsonColumnObject !=null ){
                                if (jsonColumnObject.has("name") && jsonColumnObject.has("fieldType")) {
                                    defaultFormatLFiledType.put(jsonColumnObject["name"].toString(),jsonColumnObject["fieldType"].toString())
                                }
                            }
                        }
                    }
                }
                println(dataModeArray)
            }
        }
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
