import DefaultValues.DEBUG_LOG_LEVEL
import DefaultValues.DEFAULT_LOG_LEVEL
import DefaultValues.DEFAULT_REMOTE_URL
import ProjectEditorConstants.ACTIONS_KEY
import ProjectEditorConstants.AUTHENTICATION_KEY
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.CACHE_4D_SDK_KEY
import ProjectEditorConstants.DATASOURCE_KEY
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.DEBUG_MODE_KEY
import ProjectEditorConstants.DEVELOPER_KEY
import ProjectEditorConstants.DOMINANT_COLOR_KEY
import ProjectEditorConstants.DUMPED_STAMP_KEY
import ProjectEditorConstants.DUMPED_TABLES_KEY
import ProjectEditorConstants.EMAIL_KEY
import ProjectEditorConstants.EMPTY_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.NAME_KEY
import ProjectEditorConstants.OBJECT_TYPE
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProjectEditor(projectEditorFile: File, val catalogDef: CatalogDef, isCreateDatabaseCommand: Boolean = false) {

    lateinit var dataModelList: List<DataModel>
    lateinit var listFormList: List<Form>
    lateinit var detailFormList: List<Form>
    lateinit var navigationTableList: List<String>
    lateinit var searchableFields: HashMap<String, List<String>>

    lateinit var jsonObj: JSONObject

    init {
        val jsonString = projectEditorFile.readFile()
        Log.d("==================================\n" +
                "ProjectEditor init\n" +
                "==================================\n")

        if (jsonString.isEmpty()) {
            throw Exception("Json file ${projectEditorFile.name} is empty")
        }

        retrieveJSONObject(jsonString)?.let {
            jsonObj = it

            if (isCreateDatabaseCommand) {
                dataModelList = jsonObj.getDataModelList(catalogDef, isCreateDatabaseCommand = true)
                Log.d("> DataModels list successfully read.")

            } else {

                navigationTableList = jsonObj.getNavigationTableList()
                Log.d("> Navigation tables list successfully read.")

                dataModelList = jsonObj.getDataModelList(catalogDef)
                Log.d("> DataModels list successfully read.")

                searchableFields = jsonObj.getSearchFields(dataModelList)
                Log.d("> Searchable fields successfully read.")

                listFormList = jsonObj.getFormList(dataModelList, FormType.LIST, navigationTableList, catalogDef) { aliasToAdd ->
                    addAliasToDataModel(dataModelList, aliasToAdd)
                }
                Log.d("> List forms list successfully read.")

                detailFormList = jsonObj.getFormList(dataModelList, FormType.DETAIL, navigationTableList, catalogDef) { aliasToAdd ->
                    addAliasToDataModel(dataModelList, aliasToAdd)
                }
                Log.d("> Detail forms list successfully read.")
            }

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
            else -> null
        }
    }

    fun findJsonInt(key: String): Int? {
        return when (key) {
            "dumpedStamp" -> jsonObj.getSafeInt(DUMPED_STAMP_KEY)
            else -> null
        }
    }

    fun findJsonArray(key: String): JSONArray? {
        return when (key) {
            "dumpedTables" -> jsonObj.getSafeArray(DUMPED_TABLES_KEY)
            else -> null
        }
    }

    fun findJsonBoolean(key: String): Boolean? {
        return when (key) {
            "mailAuth" -> jsonObj.getSafeObject(PROJECT_KEY)?.getSafeObject(SERVER_KEY)
                    ?.getSafeObject(AUTHENTICATION_KEY)?.getSafeBoolean(EMAIL_KEY)
            FeatureFlagConstants.HAS_RELATIONS_KEY -> jsonObj.getSafeBoolean(FeatureFlagConstants.HAS_RELATIONS_KEY)
            FeatureFlagConstants.HAS_ACTIONS_KEY -> jsonObj.getSafeBoolean(FeatureFlagConstants.HAS_ACTIONS_KEY)
            FeatureFlagConstants.HAS_DATASET_KEY -> jsonObj.getSafeBoolean(FeatureFlagConstants.HAS_DATASET_KEY)
            "debugMode" -> jsonObj.getSafeBoolean(DEBUG_MODE_KEY)
            else -> null
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
        val debugMode = findJsonBoolean("debugMode") ?: false
        return AppInfo(
                team = Team(TeamID = teamId, TeamName = ""),
                guestLogin = mailAuth.not(),
                remoteUrl = remoteUrl,
                initialGlobalStamp = if (debugMode) 0 else findJsonInt("dumpedStamp") ?: 0,
                dumpedTables = findJsonArray("dumpedTables")?.getStringList() ?: mutableListOf(),
                logLevel = if (debugMode) DEBUG_LOG_LEVEL else DEFAULT_LOG_LEVEL,
                relations = findJsonBoolean(FeatureFlagConstants.HAS_RELATIONS_KEY) ?: true
        )
    }

    fun getQueries(queriesFile: File): Map<String, String> {
        val queryMap = mutableMapOf<String, String>()
        dataModelList.forEach { dataModel ->
            dataModel.query?.let { query ->
                queryMap[dataModel.name.tableNameAdjustment()] = query
            }
        }
        queriesFile.parentFile.mkdirs()
        if (!queriesFile.createNewFile()) {
            throw Exception("An error occurred while creating new file : $queriesFile")
        }
        return queryMap
    }

    fun getActions(): Actions {
        val actionList = mutableListOf<Action>()
        jsonObj.getSafeObject(PROJECT_KEY)?.getSafeArray(ACTIONS_KEY)?.let { actionsArray ->
            for (i in 0 until actionsArray.length()) {
                actionsArray.getSafeObject(i)?.let { actionObject ->
                    actionObject.getSafeString("name")?.let { actionName ->
                        val newAction = Action(actionName)
                        actionObject.getSafeString("shortLabel")?.let { newAction.shortLabel = it }
                        actionObject.getSafeString("label")?.let { newAction.label = it }
                        actionObject.getSafeString("scope")?.let { newAction.scope = it }
                        actionObject.getSafeInt("tableNumber")?.let { newAction.tableNumber = it }
                        actionObject.getSafeString("icon")?.let { newAction.icon = it }
                        actionObject.getSafeString("preset")?.let { newAction.preset = it }
                        actionObject.getSafeString("style")?.let { newAction.style = it }
                        actionObject.getSafeArray("parameters")?.let { parametersArray ->
                            val parameterList = mutableListOf<ActionParameter>()
                            for (j in 0 until parametersArray.length()) {
                                parametersArray.getSafeObject(j)?.let { parameter ->
                                    parameter.getSafeString("name")?.let { parameterName ->
                                        val newParameter = ActionParameter(parameterName)
                                        parameter.getSafeString("label")?.let { newParameter.label = it }
                                        parameter.getSafeString("shortLabel")?.let { newParameter.shortLabel = it }
                                        parameter.getSafeString("type")?.let { newParameter.type = it }
                                        parameter.getSafeString("default")?.let { newParameter.default = it }
                                        parameter.getSafeString("placeholder")?.let { newParameter.placeholder = it }
                                        parameter.getSafeString("format")?.let { newParameter.format = it }
                                        actionObject.getSafeInt("fieldNumber")?.let { newParameter.fieldNumber = it }
                                        actionObject.getSafeString("defaultField")?.let { newParameter.defaultField = it }
                                        parameter.getSafeArray("rules")?.let { rulesArray ->
                                            val rulesList = mutableListOf<Any>()
                                            for (k in 0 until rulesArray.length()) {
                                                // can be a string or an object
                                                rulesArray.getSafeString(k)?.let { rulesList.add(it) }
                                                rulesArray.getSafeObject(k)?.let { rulesList.add(it.toStringMap()) }
                                            }
                                            newParameter.rules = rulesList
                                        }
                                        parameterList.add(newParameter)
                                    }
                                }
                            }
                            newAction.parameters = parameterList
                        }
                        actionList.add(newAction)
                    }
                }
            }
        }
        val scopedActions: Map<String?, List<Action>> = actionList.groupBy { it.scope }
        val currentRecordActions = formatMap(scopedActions, "currentRecord")
        val tableActions = formatMap(scopedActions, "table")
        return Actions(table = tableActions, currentRecord = currentRecordActions)
    }

    private fun formatMap(scopedActions:  Map<String?, List<Action>>, scope: String):  Map<String, List<Action>> {
        val newMap = mutableMapOf<String, List<Action>>()
        scopedActions[scope]?.groupBy { it.tableNumber }?.let { actionsWithTableNumber :  Map<Int?, List<Action>> ->
            for ((tableNumber, actions) in actionsWithTableNumber) {
                dataModelList.find { it.id == tableNumber.toString() }?.name?.let { tableName ->
                    newMap[tableName.tableNameAdjustment()] = actions
                }
            }
        }
        return newMap
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
    38 -> OBJECT_TYPE
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
    38 -> OBJECT_TYPE
    else -> EMPTY_TYPE
}