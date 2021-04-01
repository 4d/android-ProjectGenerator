import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_REMOTE_URL
import DefaultValues.LAYOUT_FILE
import ExitCodes.COPY_TEMPLATE_FILE_ERROR
import ExitCodes.FIELD_TYPE_ERROR
import ExitCodes.FILE_CREATION_ERROR
import ExitCodes.MISSING_ANDROID_CACHE_SDK_PATH
import ExitCodes.MISSING_ANDROID_SDK_PATH
import FileHelperConstants.APP_INFO_FILENAME
import FileHelperConstants.DS_STORE
import FileHelperConstants.QUERIES_FILENAME
import MustacheConstants.ANDROID_SDK_PATH
import MustacheConstants.APP_NAME_WITH_CAPS
import MustacheConstants.AUTHOR
import MustacheConstants.CACHE_4D_SDK_PATH
import MustacheConstants.COLORS_DEFINED
import MustacheConstants.COLOR_PRIMARY_DARKER
import MustacheConstants.COLOR_PRIMARY_DARKER_PLUS
import MustacheConstants.COLOR_PRIMARY_DARKER_PLUS_PLUS
import MustacheConstants.COLOR_PRIMARY_LIGHTER
import MustacheConstants.COLOR_PRIMARY_LIGHTER_PLUS
import MustacheConstants.COLOR_PRIMARY_LIGHTER_PLUS_PLUS
import MustacheConstants.COLOR_PRIMARY_NEUTRAL
import MustacheConstants.COMPANY_HEADER
import MustacheConstants.DATE_DAY
import MustacheConstants.DATE_MONTH
import MustacheConstants.DATE_YEAR
import MustacheConstants.ENTITY_CLASSES
import MustacheConstants.FIELDS
import MustacheConstants.FIRST_FIELD
import MustacheConstants.FORM_FIELDS
import MustacheConstants.IMAGE_FIELD_NAME
import MustacheConstants.IMAGE_KEY_ACCESSOR
import MustacheConstants.IMAGE_TABLE_NAME
import MustacheConstants.PACKAGE
import MustacheConstants.RELATIONS
import MustacheConstants.RELATIONS_IMPORT
import MustacheConstants.RELATION_NAME
import MustacheConstants.RELATION_SAME_TYPE
import MustacheConstants.RELATION_SOURCE
import MustacheConstants.RELATION_TARGET
import MustacheConstants.REMOTE_ADDRESS
import MustacheConstants.TABLENAME
import MustacheConstants.TABLENAMES
import MustacheConstants.TABLENAMES_LAYOUT
import MustacheConstants.TABLENAMES_LOWERCASE
import MustacheConstants.TABLENAMES_NAVIGATION
import MustacheConstants.TABLENAMES_RELATIONS
import MustacheConstants.TABLENAMES_RELATIONS_DISTINCT
import MustacheConstants.TABLENAMES_WITHOUT_RELATIONS
import MustacheConstants.TABLENAMES__LAYOUT_RELATIONS
import MustacheConstants.TABLENAME_LOWERCASE
import MustacheConstants.TABLENAME_ORIGINAL
import MustacheConstants.THEME_COLOR_ON_PRIMARY
import MustacheConstants.THEME_COLOR_PRIMARY
import MustacheConstants.THEME_COLOR_PRIMARY_DARKER
import MustacheConstants.THEME_COLOR_PRIMARY_LIGHTER
import MustacheConstants.TYPES_AND_TABLES
import PathHelperConstants.TEMPLATE_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_DAO_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_ENTITY_PLACEHOLDER
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import java.io.File
import java.io.FileReader
import java.lang.Integer.toHexString
import java.util.*
import kotlin.system.exitProcess

class MustacheHelper(private val fileHelper: FileHelper, private val projectEditor: ProjectEditor) {

    private var data = mutableMapOf<String, Any>()

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    private lateinit var compiler: Mustache.Compiler
    private lateinit var template: Template

    private val tableNamesForNavigation = mutableListOf<TemplateLayoutFiller>()
    private val tableNamesForLayoutType = mutableListOf<TemplateLayoutTypeFiller>()
    private var tableNames = mutableListOf<TemplateTableFiller>()
    private var tableNames_lowercase = mutableListOf<TemplateLayoutFiller>()
    private var relations = mutableListOf<TemplateRelationFiller>()
    private var formatFields: HashMap<String, String>

    //TypeChoice Key
    private val typeChoice = Key.getKeys()
    private val formatTypeFunctionName = Key.getFormatTypeFunctionName()

    init {
        Log.plantTree(this::class.java.canonicalName)
        data[COMPANY_HEADER] = fileHelper.pathHelper.companyWithCaps
        data[AUTHOR] = projectEditor.findJsonString("author") ?: DEFAULT_AUTHOR
        data[DATE_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        data[DATE_MONTH] = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        data[DATE_YEAR] = Calendar.getInstance().get(Calendar.YEAR).toString()
        data[PACKAGE] = fileHelper.pathHelper.pkg
        data[APP_NAME_WITH_CAPS] = fileHelper.pathHelper.appNameWithCaps
        formatFields = projectEditor.formatFields

        // for network_security_config.xml
        // whitelist production host address if defined, else, server host address, else localhost
        var remoteAddress =  projectEditor.findJsonString("productionUrl")
        Log.d("remoteAddress = $remoteAddress")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = projectEditor.findJsonString("remoteUrl")
            if (remoteAddress.isNullOrEmpty())
                remoteAddress = DEFAULT_REMOTE_URL
        data[REMOTE_ADDRESS] = remoteAddress.removePrefix("https://").removePrefix("http://").split(":")[0]
        Log.d("data[REMOTE_ADDRESS] = ${data[REMOTE_ADDRESS]}")

        projectEditor.findJsonString("androidSdk")?.let {
            data[ANDROID_SDK_PATH] = it
        } ?: run {
            val androidHome = System.getenv("ANDROID_HOME")
            if (!androidHome.isNullOrEmpty()) {
                if (File(androidHome).exists()) {
                    data[ANDROID_SDK_PATH] = androidHome
                }
            }

            if (data[ANDROID_SDK_PATH] == null) {
                val commonSdkPath = "${System.getProperty("user.home")}/Library/Android/sdk"
                if (File(commonSdkPath).exists()) {
                    data[ANDROID_SDK_PATH] = commonSdkPath
                } else {
                    Log.e("Missing Android SDK path")
                    exitProcess(MISSING_ANDROID_SDK_PATH)
                }
            } else {
                // already defined
            }
        }
        Log.d("> Android SDK = ${data[ANDROID_SDK_PATH]}")

        projectEditor.findJsonString("cache4dSdk")?.let {
            data[CACHE_4D_SDK_PATH] = it
        } ?: run {
            val qmobileHome = System.getenv("QMOBILE_HOME")
            if (!qmobileHome.isNullOrEmpty()) {
                if (File(qmobileHome).exists()) {
                    data[CACHE_4D_SDK_PATH] = qmobileHome
                }
            }

            if (data[CACHE_4D_SDK_PATH] == null) {
                Log.e("Missing 4D Mobile cache SDK path. Define `cache_4d_sdk` in json file or `QMOBILE_HOME` env variable")
                exitProcess(MISSING_ANDROID_CACHE_SDK_PATH)
            }
        }
        Log.d("> Cache 4D SDK = ${data[CACHE_4D_SDK_PATH]}")
        if (!File("${data[CACHE_4D_SDK_PATH]}").exists()) {
            Log.e("Cache 4D SDK path do not exists. Define it correctly.")
            exitProcess(MISSING_ANDROID_SDK_PATH)
        }

        projectEditor.findJsonString("backgroundColor")?.let {
            Log.d("backgroundColor = $it")
            data[COLORS_DEFINED] = true
            data[COLOR_PRIMARY_NEUTRAL] = it

            val backgroundColor: Int = Color.parseColor(it)
            data[COLOR_PRIMARY_DARKER] =  "#" + toHexString(manipulateColor(backgroundColor, 0.8f)).toUpperCase() // darker +
            data[COLOR_PRIMARY_DARKER_PLUS] = "#" + toHexString(manipulateColor(backgroundColor, 0.6f)).toUpperCase() // darker ++
            data[COLOR_PRIMARY_DARKER_PLUS_PLUS] = "#" + toHexString(manipulateColor(backgroundColor, 0.4f)).toUpperCase() // darker +++
            data[COLOR_PRIMARY_LIGHTER] = "#" + toHexString(manipulateColor(backgroundColor, 1.2f)).toUpperCase() // lighter +
            data[COLOR_PRIMARY_LIGHTER_PLUS] = "#" + toHexString(manipulateColor(backgroundColor, 1.4f)).toUpperCase() // lighter ++
            data[COLOR_PRIMARY_LIGHTER_PLUS_PLUS] = "#" + toHexString(manipulateColor(backgroundColor, 1.6f)).toUpperCase() // lighter +++

            data[THEME_COLOR_PRIMARY] = "@color/primary_neutral"
            data[THEME_COLOR_PRIMARY_DARKER] = "@color/primary_darker_3"
            data[THEME_COLOR_PRIMARY_LIGHTER] = "@color/primary_lighter_3"
        } ?: run {
            data[COLORS_DEFINED] = false
            data[THEME_COLOR_PRIMARY] = "@color/cyan_900"
            data[THEME_COLOR_PRIMARY_DARKER] = "@color/cyan_dark"
            data[THEME_COLOR_PRIMARY_LIGHTER] = "@color/cyan_light"
        }

        projectEditor.findJsonString("foregroundColor")?.let {
            if (data[COLORS_DEFINED] == true) {
                data[THEME_COLOR_ON_PRIMARY] = it
            } else {
                data[THEME_COLOR_ON_PRIMARY] = "@color/white"
            }
        } ?: run {
            data[THEME_COLOR_ON_PRIMARY] = "@color/white"
        }

        var entityClassesString = ""

        val dataModelRelationList = mutableListOf<TemplateRelationFiller>()
        val layoutRelationList = mutableListOf<TemplateRelationFiller>()

        projectEditor.dataModelList.forEach { dataModel ->

            dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                dataModelRelationList.add(TemplateRelationFiller(relation_source = relation.source.tableNameAdjustment(),
                    relation_target = relation.target.tableNameAdjustment(),
                    relation_name = relation.name.fieldAdjustment()
                ))
            }

            tableNames.add(TemplateTableFiller(
                name = dataModel.name.tableNameAdjustment(),
                name_original = dataModel.name,
                nameCamelCase = dataModel.name.dataBindingAdjustment(),
                concat_fields = dataModel.fields?.joinToString { "\"${it.name}\"" } ?: ""))

            tableNames_lowercase.add(
                TemplateLayoutFiller(
                    name = dataModel.name.tableNameAdjustment(),
                    name_original = dataModel.name,
                    nameLowerCase = dataModel.name.toLowerCase().fieldAdjustment(),
                    nameCamelCase = dataModel.name.dataBindingAdjustment(),
                    label = dataModel.getLabel(),
                    hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                    icon = dataModel.iconPath ?: ""))

            entityClassesString += "${dataModel.name.tableNameAdjustment()}::class, "
            Log.d("ProjectDataModelList  ${entityClassesString}:: ${dataModel.name.tableNameAdjustment()}")
        }

        val tableNames_without_relations = mutableListOf<TemplateTableFiller>()

        tableNames.forEach { tableName ->
            if (!dataModelRelationList.map { it.relation_source.tableNameAdjustment() }.contains(tableName.name))
                tableNames_without_relations.add(tableName)
        }

        data[TABLENAMES] = tableNames
        data[TABLENAMES_LOWERCASE] = tableNames_lowercase
        data[TABLENAMES_RELATIONS] = dataModelRelationList
        data[TABLENAMES_WITHOUT_RELATIONS] = tableNames_without_relations
        data[TABLENAMES_RELATIONS_DISTINCT] =
            dataModelRelationList.distinctBy { it.relation_source to it.relation_target }
        data[ENTITY_CLASSES] = entityClassesString.dropLast(2)

        val typesAndTables = mutableListOf<TemplateTableFiller>()
        typesAndTables.addAll(tableNames)
        typesAndTables.add(TemplateTableFiller(name = "Photo", name_original = "Photo", nameCamelCase = "photo", concat_fields = ""))
        typesAndTables.add(TemplateTableFiller(name = "Date", name_original = "Date", nameCamelCase = "date", concat_fields = ""))
        typesAndTables.add(TemplateTableFiller(name = "Time", name_original = "Time", nameCamelCase = "time", concat_fields = ""))
        data[TYPES_AND_TABLES] = typesAndTables


        var navigationTableCounter =
            0 // Counter to limit to 4 navigation tables as it is not possible to have more than 5
        projectEditor.navigationTableList.forEach { navigationTableId ->
            projectEditor.dataModelList.find { it.id == navigationTableId }?.let { dataModel ->
                if (navigationTableCounter <= 3) {
                    Log.w("Adding [${dataModel.name}] in navigation table list")

                    tableNamesForNavigation.add(
                        TemplateLayoutFiller(
                            name = dataModel.name.tableNameAdjustment(),
                            name_original = dataModel.name,
                            nameLowerCase = dataModel.name.toLowerCase().fieldAdjustment(),
                            nameCamelCase = dataModel.name.dataBindingAdjustment(),
                            label = dataModel.getLabel(),
                            hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                            icon = dataModel.iconPath ?: ""))

                    navigationTableCounter++

                    dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                        layoutRelationList.add(
                            TemplateRelationFiller(
                                relation_source = relation.source.dataBindingAdjustment(),
                                relation_target = relation.target.tableNameAdjustment(),
                                relation_name = relation.name.fieldAdjustment()
                        ))
                    }
                } else {
                    Log.w("Not adding [${dataModel.name}] in navigation table list")
                }
            }
        }
        data[TABLENAMES_NAVIGATION] = tableNamesForNavigation
        data[TABLENAMES__LAYOUT_RELATIONS] = layoutRelationList

        // Specifying if list layout is table or collection (LinearLayout or GridLayout)
        tableNamesForNavigation.map { it.name }.forEach { tableName ->
            val listFormName = projectEditor.listFormList.find { listform -> listform.dataModel.name.tableNameAdjustment() == tableName.tableNameAdjustment() }?.name
            val formPath = fileHelper.pathHelper.getFormPath(listFormName, FormType.LIST)
            tableNamesForLayoutType.add(TemplateLayoutTypeFiller(name = tableName, layout_manager_type = getLayoutManagerType(formPath)))
        }

        data[TABLENAMES_LAYOUT] = tableNamesForLayoutType
        Log.d("TABLENAMES" ,"${data[TABLENAMES]}")
    }

    /**
     * TEMPLATING
     */
    fun applyTemplates() {
        File(fileHelper.pathHelper.templateFilesPath).walkTopDown()
            .filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->

            compiler = generateCompilerFolder(currentFolder.absolutePath)

            currentFolder.walkTopDown()
                .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) && file.name != DS_STORE }
                .forEach { currentFile ->

                    Log.d("Processed file"  ,"$currentFile")

                    template = compiler.compile("{{>${currentFile.name}}}")

                    val newFilePath = fileHelper.pathHelper.getPath(currentFile.absolutePath.replaceXmlTxtSuffix())

                    relations.clear()
                    projectEditor.dataModelList.forEach { dataModel ->
                        dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                            ?.forEach { relation ->
                                relations.add(TemplateRelationFiller(relation_source = relation.source.tableNameAdjustment(),
                                    relation_target = relation.target.tableNameAdjustment(),
                                    relation_name = relation.name.fieldAdjustment()))
                            }
                    }
                    data[RELATIONS] = relations

                    if (currentFile.isWithTemplateName()) {

                        for (tableName in tableNames) { // file will be duplicated

                            if (newFilePath.contains(fileHelper.pathHelper.navigationPath())) {
                                val isTableInNavigation = tableNamesForNavigation.find { it.name == tableName.name }
                                if (isTableInNavigation == null) {
                                    continue
                                }
                            }

                            data[TABLENAME] = tableName.name.tableNameAdjustment()

                            data[TABLENAME_ORIGINAL] = tableName.name_original
                            projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.let { datamodel ->
                                data[TABLENAME_ORIGINAL] = datamodel.getLabel()
                            }

                            data[TABLENAME_LOWERCASE] = tableName.name.toLowerCase().fieldAdjustment()
                            projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.fields?.let { fields ->
                                val fieldList = mutableListOf<TemplateFieldFiller>()
                                for (field in fields) {
                                    field.fieldTypeString?.let { fieldTypeString ->
                                        fieldList.add(
                                            TemplateFieldFiller(
                                                name = field.name.fieldAdjustment(),
                                                fieldTypeString = fieldTypeString.tableNameAdjustment(),
                                                variableType = field.variableType,
                                                name_original = field.name
                                            )
                                        )

                                    } ?: kotlin.run {
                                        Log.e("An error occurred while parsing the fieldType of field : $field")
                                        exitProcess(FIELD_TYPE_ERROR)
                                    }
                                }

                                data[FIELDS] = fieldList
                                Log.d("FIELDS","${data[FIELDS]}")
                                val firstField: String = fieldList.firstOrNull()?.name ?: ""
                                Log.d("firstField = $firstField")
                                data[FIRST_FIELD] = firstField
                            }

                        relations.clear()
                        val relationsImport = mutableListOf<TemplateRelationFiller>() // need another list, to remove double in import section

                        projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            relations.add(TemplateRelationFiller(
                                relation_source = relation.source.tableNameAdjustment(),
                                relation_target = relation.target.tableNameAdjustment(),
                                relation_name = relation.name.fieldAdjustment()))

                            var isAlreadyImported = false
                            for (relationImport in relationsImport) {
                                if (relationImport.relation_source == relation.source.tableNameAdjustment() && relationImport.relation_target == relation.target.tableNameAdjustment())
                                    isAlreadyImported = true
                            }
                            if (!isAlreadyImported)
                                relationsImport.add(TemplateRelationFiller(
                                    relation_source = relation.source.tableNameAdjustment(),
                                    relation_target = relation.target.tableNameAdjustment(),
                                    relation_name = relation.name.fieldAdjustment())) // name is unused
                        }

                        data[RELATIONS] = relations
                        data[RELATIONS_IMPORT] = relationsImport

                        val replacedPath = if (newFilePath.contains(fileHelper.pathHelper.resPath()))
                            newFilePath.replace(TEMPLATE_PLACEHOLDER, tableName.name.toLowerCase())
                        else
                            newFilePath.replace(TEMPLATE_PLACEHOLDER, tableName.name)

                        applyTemplate(replacedPath)

                        //cleaning
                        data.remove(FIELDS)
                        data.remove(RELATIONS)
                        data.remove(FIRST_FIELD)
                    }

                } else if (currentFile.isWithRelationDaoTemplateName()) {

                    projectEditor.dataModelList.forEach { dataModel ->
                        dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                            data[RELATION_TARGET] = relation.target.tableNameAdjustment()

                            val replacedPath = newFilePath.replace(TEMPLATE_RELATION_DAO_PLACEHOLDER, "${relation.source.tableNameAdjustment()}Has${relation.target.tableNameAdjustment()}")

                            applyTemplate(replacedPath)

                            //cleaning
                            data.remove(RELATION_SOURCE)
                            data.remove(RELATION_TARGET)
                        }
                    }

                } else if (currentFile.isWithRelationEntityTemplateName()) {

                    projectEditor.dataModelList.forEach { dataModel ->
                        dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                            data[RELATION_TARGET] = relation.target.tableNameAdjustment()
                            data[RELATION_NAME] = relation.name.fieldAdjustment()
                            data[RELATION_SAME_TYPE] = relation.source == relation.target

                            val replacedPath = newFilePath.replace(TEMPLATE_RELATION_ENTITY_PLACEHOLDER, "${relation.source.tableNameAdjustment()}And${relation.target.tableNameAdjustment()}")

                            applyTemplate(replacedPath)

                            //cleaning
                            data.remove(RELATION_NAME)
                            data.remove(RELATION_SOURCE)
                            data.remove(RELATION_TARGET)
                            data.remove(RELATION_SAME_TYPE)
                        }
                    }

                } else {
                    applyTemplate(newFilePath)
                }
            }
        }
    }

    fun applyListFormTemplate() {
        projectEditor.listFormList.forEach { listForm ->

            Log.d("MustacheHelper : listform.name = ${listForm.name}")
            Log.d("MustacheHelper : listform.datamodel.name = ${listForm.dataModel.name}")
            Log.d("MustacheHelper : listform.datamodel.name.tableNameAdjustment() = ${listForm.dataModel.name.tableNameAdjustment()}")
            Log.d("MustacheHelper : listform.fields size = ${listForm.fields?.size}")

            var formPath = fileHelper.pathHelper.getFormPath(listForm.name, FormType.LIST)

            if (File(formPath).exists()) {
                if (!fileHelper.pathHelper.appFolderExistsInTemplate(formPath)) {
                    Log.w("WARNING : AN IOS TEMPLATE WAS GIVEN FOR THE LIST FORM $formPath")
                    formPath = fileHelper.pathHelper.getDefaultTemplateListFormPath()
                }
            } else {
                Log.w("WARNING : MISSING LIST FORM TEMPLATE $formPath")
                formPath = fileHelper.pathHelper.getDefaultTemplateListFormPath()
            }

            val appFolderInTemplate = fileHelper.pathHelper.getAppFolderInTemplate(formPath)

            File(appFolderInTemplate).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->

                compiler = generateCompilerFolder(currentFolder.absolutePath)

                currentFolder.walkTopDown()
                    .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) && file.name != DS_STORE }
                    .forEach { currentFile ->

                        Log.i(" > Processed template file : $currentFile")

                        template = compiler.compile("{{>${currentFile.name}}}")

                        if (currentFile.name == LAYOUT_FILE) {
                            val oldFormText = readFileDirectlyAsText(currentFile)
                            val newFormText = replaceTemplateText(oldFormText, FormType.LIST)
                            template = compiler.compile(newFormText)

                            data[TABLENAME_LOWERCASE] = listForm.dataModel.name.toLowerCase().fieldAdjustment()
                            data[TABLENAME] = listForm.dataModel.name.tableNameAdjustment()
                            relations.clear()
                            projectEditor.dataModelList.find { it.name.tableNameAdjustment() == listForm.dataModel.name.tableNameAdjustment() }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                                relations.add(TemplateRelationFiller(
                                    relation_source = relation.source.tableNameAdjustment(),
                                    relation_target = relation.target.tableNameAdjustment(),
                                    relation_name = relation.name.fieldAdjustment()))
                            }
                            data[RELATIONS] = relations

                            listForm.fields?.forEach {
                                Log.d("LIST : ${listForm.name} / field = $it")
                            }

                            var i = 0
                            listForm.fields?.forEach { field -> // Could also iter over specificFieldsCount as Detail form
                                i++

                                if ((field.inverseName != null) || (fileHelper.pathHelper.isDefaultTemplateListFormPath(formPath) && field.isImage())) { // is relation or image in default template

                                    data["field_${i}_defined"] = false
                                    data["field_${i}_formatted"] = false
                                    data["field_${i}_label"] = ""
                                    data["field_${i}_name"] = ""
                                    data["field_${i}_accessor"] = ""

                                } else { // not a relation

                                    data["field_${i}_name"] = field.name.fieldAdjustment()
                                    data["field_${i}_defined"] = field.name.isNotEmpty()
                                    data["field_${i}_is_image"] = field.isImage()
                                    data["field_${i}_label"] = field.getLabel()
                                    data["field_${i}_is_int"] = field.isInt()
                                    data["field_${i}_formatted"] = false
                                    data["field_${i}_accessor"] = field.getLayoutVariableAccessor(FormType.LIST)
                                    if (field.isImage()) {
                                        data[IMAGE_FIELD_NAME] = field.getImageFieldName()
                                        data[IMAGE_KEY_ACCESSOR] = field.getImageKeyAccessor(FormType.LIST)
                                        data[IMAGE_TABLE_NAME] = field.getImageTableName(projectEditor.dataModelList, listForm)
                                    }

                                    val key = formatFields[field.name.fieldAdjustment()]
                                    if (key != null) {
                                        formatTypeFunctionName[key]?.let { functionName ->
                                            typeChoice[key]?.let { type ->
                                                data["field_${i}_formatted"] = true
                                                data["field_${i}_format_function"] = functionName
                                                data["field_${i}_format_type"] = type
                                            }
                                        }

                                    } else {
                                        // already defined
                                    }
                                }
                            }

                            val newFilePath = fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name.tableNameAdjustment())
                            applyTemplate(newFilePath)

                            // cleaning data for other templates
                            for (j in 1 until i + 1) {
                                data.remove("field_${j}_defined")
                                data.remove("field_${j}_is_image")
                                data.remove("field_${j}_is_int")
                                data.remove("field_${j}_name")
                                data.remove("field_${j}_label")
                                data.remove("field_${j}_formatted")
                                data.remove("field_${j}_format_function")
                                data.remove("field_${j}_format_type")
                                data.remove("field_${j}_accessor")
                            }
                            data.remove(RELATIONS)
                            data.remove(IMAGE_FIELD_NAME)
                            data.remove(IMAGE_KEY_ACCESSOR)
                            data.remove(IMAGE_TABLE_NAME)
                        } else { // any file to copy in project
                            val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath, formPath))
                            Log.i("File to copy : ${currentFile.absolutePath}; target : ${newFile.absolutePath}")
                            if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                                Log.e("An error occurred while copying template files with target : ${newFile.absolutePath}")
                                exitProcess(COPY_TEMPLATE_FILE_ERROR)
                            }
                        }
                    }
            }
        }
    }

   fun applyDetailFormTemplate() {

       projectEditor.detailFormList.forEach { detailForm ->

           var formPath = fileHelper.pathHelper.getFormPath(detailForm.name, FormType.DETAIL)

           if (File(formPath).exists()) {
               if (!fileHelper.pathHelper.appFolderExistsInTemplate(formPath)) {
                   Log.w("WARNING : AN IOS TEMPLATE WAS GIVEN FOR THE DETAIL FORM $formPath")
                   formPath = fileHelper.pathHelper.getDefaultTemplateDetailFormPath()
               }
           } else {
               Log.w("WARNING : MISSING DETAIL FORM TEMPLATE $formPath")
               formPath = fileHelper.pathHelper.getDefaultTemplateDetailFormPath()
           }

           // not used in list form
           var specificFieldsCount = 0
           getTemplateManifestJSONContent(formPath)?.let {
               specificFieldsCount = it.getSafeObject("fields")?.getSafeInt("count") ?: 0
           }

           val appFolderInTemplate = fileHelper.pathHelper.getAppFolderInTemplate(formPath)

           File(appFolderInTemplate).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->

               compiler = generateCompilerFolder(currentFolder.absolutePath)

               currentFolder.walkTopDown()
                   .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent)  && file.name != DS_STORE }
                   .forEach { currentFile ->

                       Log.i(" > Processed template file : $currentFile")

                       template = compiler.compile("{{>${currentFile.name}}}")

                       if (currentFile.name == LAYOUT_FILE) {
                           val oldFormText = readFileDirectlyAsText(currentFile)
                           val newFormText = replaceTemplateText(oldFormText, FormType.DETAIL)

                           template = compiler.compile(newFormText)

                           data[TABLENAME_LOWERCASE] = detailForm.dataModel.name.toLowerCase().fieldAdjustment()
                           data[TABLENAME] = detailForm.dataModel.name.tableNameAdjustment()

                           val formFieldList = mutableListOf<TemplateFormFieldFiller>()

                           detailForm.fields?.let { fieldList ->

                               fieldList.forEach {
                                   Log.d("DETAIL ${detailForm.name} / field = $it")
                               }

                               if (fieldList.isNotEmpty()) {

                                   if (specificFieldsCount == 0) { // template with no specific field
                                       for (i in fieldList.indices) {
                                           if (fieldList[i].name.isNotEmpty()) {

                                               var formField = createDetailFormField(fieldList[i], i + 1, projectEditor.dataModelList, detailForm, false)

                                               val key = formatFields[fieldList[i].name.fieldAdjustment()]
                                               if (key != null) {
                                                   formatTypeFunctionName[key]?.let { functionName ->
                                                       typeChoice[key]?.let { type ->
                                                           Log.d("Adding free Field with format ${fieldList[i]}")
                                                           formField =  createDetailFormField(fieldList[i], i + 1, projectEditor.dataModelList, detailForm,true, functionName, type)
                                                       }
                                                   }
                                               } else {
                                                   // already defined
                                               }
                                               formFieldList.add(formField)
                                           } else {
                                               // you can get null fields in json file
                                               // occurs when you select a form, and then select back Blank form
                                           }
                                       }
                                   } else { // template with specific fields

                                       for (i in 0 until specificFieldsCount) {

                                           if (i < fieldList.size) {

                                               if (fieldList[i].inverseName == null) { // is not relation
                                                   Log.d("Adding specific Field ${fieldList[i]}")
                                                   Log.d("fieldList[i].getLayoutVariableAccessor() = ${fieldList[i].getLayoutVariableAccessor(FormType.DETAIL)}")

                                                   data["field_${i + 1}_defined"] = fieldList[i].name.isNotEmpty()
                                                   data["field_${i + 1}_is_image"] = fieldList[i].isImage()
                                                   data["field_${i + 1}_is_int"] = fieldList[i].isInt()
                                                   data["field_${i + 1}_name"] = fieldList[i].name.fieldAdjustment()
                                                   data["field_${i + 1}_label"] = fieldList[i].getLabel()
                                                   data["field_${i + 1}_formatted"] = false
                                                   data["field_${i + 1}_accessor"] = fieldList[i].getLayoutVariableAccessor(FormType.DETAIL)
                                                   if (fieldList[i].isImage()) {
                                                       data[IMAGE_FIELD_NAME] = fieldList[i].getImageFieldName()
                                                       data[IMAGE_KEY_ACCESSOR] = fieldList[i].getImageKeyAccessor(FormType.DETAIL)
                                                       data[IMAGE_TABLE_NAME] = fieldList[i].getImageTableName(projectEditor.dataModelList, detailForm)
                                                   }

                                                   val key = formatFields[fieldList[i].name.fieldAdjustment()]
                                                   if (key != null) {
                                                       formatTypeFunctionName[key]?.let { functionName ->
                                                           typeChoice[key]?.let { type ->
                                                               data["field_${i + 1}_formatted"] = true
                                                               data["field_${i + 1}_format_function"] = functionName
                                                               data["field_${i + 1}_format_type"] = type
                                                           }
                                                       }
                                                   } else {
                                                       // already defined
                                                   }
                                               } else {
                                                   Log.d("Field [${fieldList[i].name}] not added in specifc field because it is a relation")
                                               }

                                           } else {
                                               Log.d("Field list shorter than specific fields count")
                                               data["field_${i + 1}_defined"] = false
                                               data["field_${i + 1}_is_image"] = false
                                               data["field_${i + 1}_is_int"] = false
                                               data["field_${i + 1}_name"] = ""
                                               data["field_${i + 1}_label"] = ""
                                               data["field_${i + 1}_formatted"] = false
                                               data["field_${i + 1}_accessor"] = ""
                                               data[IMAGE_FIELD_NAME] = ""
                                               data[IMAGE_KEY_ACCESSOR] = ""
                                               data[IMAGE_TABLE_NAME] = ""
                                           }
                                       }

                                       Log.d("fieldList.size = ${fieldList.size}")
                                       Log.d("specificFieldsCount = $specificFieldsCount")

                                       if (fieldList.size > specificFieldsCount) {
                                           var k = specificFieldsCount // another counter to avoid null field
                                           for (i in specificFieldsCount until fieldList.size) {
                                               Log.d("in for loop, i = $i")
                                               Log.d("in for loop, k = $k")
                                               Log.d("fieldList[i] = ${fieldList[i]}")
                                               if (fieldList[i].name.isNotEmpty()) {
                                                   Log.d("Adding free Field in specific template ${fieldList[i]}")

                                                   val key = formatFields[fieldList[i].name.fieldAdjustment()]
                                                   var formField = createDetailFormField(fieldList[i], k + 1, projectEditor.dataModelList, detailForm,false)
                                                   if (key != null) {
                                                       formatTypeFunctionName[key]?.let { functionName ->
                                                           typeChoice[key]?.let { type ->
                                                               formField =  createDetailFormField(fieldList[i], k + 1, projectEditor.dataModelList, detailForm,true, functionName, type)
                                                           }
                                                       }
                                                   } else {
                                                       // already defined
                                                   }
                                                   formFieldList.add(formField)
                                                   k++
                                               } else {
                                                   // don't add null field
                                               }
                                           }
                                       } else {
                                           // no additional field given
                                       }
                                   }

                               }
                               data[FORM_FIELDS] = formFieldList
                           }

                           val newFilePath = fileHelper.pathHelper.getDetailFormPath(detailForm.dataModel.name.tableNameAdjustment())
                           applyTemplate(newFilePath)

                           // cleaning data for other templates
                           data.remove(FORM_FIELDS)
                           data.remove(IMAGE_FIELD_NAME)
                           data.remove(IMAGE_KEY_ACCESSOR)
                           data.remove(IMAGE_TABLE_NAME)
                           for (i in 1 until specificFieldsCount) {
                               data.remove("field_${i}_defined")
                               data.remove("field_${i}_is_image")
                               data.remove("field_${i}_is_int")
                               data.remove("field_${i}_name")
                               data.remove("field_${i}_label")
                               data.remove("field_${i}_formatted")
                               data.remove("field_${i}_format_function")
                               data.remove("field_${i}_format_type")
                               data.remove("field_${i}_accessor")
                           }

                       } else { // any file to copy in project
                           val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath, formPath))
                           if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                               Log.e("An error occurred while copying template files with target : ${newFile.absolutePath}")
                               exitProcess(COPY_TEMPLATE_FILE_ERROR)
                           }
                       }
                   }
           }
       }
   }

    private fun applyTemplate(newPath: String) {
        var newFile = File(newPath.replaceXmlTxtSuffix())
        val fileName = newFile.nameWithoutExtension
        println("fileName= $fileName")
        if (reservedKeywords.contains(fileName)) {
            newFile =  File(newFile.parent.removeSuffix("/").removeSuffix("\\") + File.separator + fileName.validateWord() + "." + newFile.extension)
            println("===== newFile = ${newFile.absolutePath}")
        }
        if (newFile.exists()) {
            return
        }
        newFile.parentFile.mkdirs()
        if (!newFile.createNewFile()) {
            Log.e("An error occurred while creating new file : $newFile")
            exitProcess(FILE_CREATION_ERROR)
        }
        newFile.writeText(template.execute(data))
    }

    fun makeQueries() {
        val queryList = mutableListOf<Query>()
        projectEditor.dataModelList.forEach { dataModel ->
            dataModel.query?.let { query ->
                queryList.add(Query(dataModel.name.tableNameAdjustment(), query))
            }
        }
        val queries = Queries(queryList)

        val queriesFile = File(fileHelper.pathHelper.assetsPath(), QUERIES_FILENAME)
        queriesFile.parentFile.mkdirs()
        if (!queriesFile.createNewFile()) {
            Log.e("An error occurred while creating new file : $queriesFile")
            exitProcess(FILE_CREATION_ERROR)
        }
        queriesFile.writeText(gson.toJson(queries))
    }

    fun makeAppInfo() {
        val appInfoFile = File(fileHelper.pathHelper.assetsPath(), APP_INFO_FILENAME)
        appInfoFile.parentFile.mkdirs()
        if (!appInfoFile.createNewFile()) {
            Log.e("An error occurred while creating new file : $appInfoFile")
            exitProcess(FILE_CREATION_ERROR)
        }
        appInfoFile.writeText(gson.toJson(projectEditor.getAppInfo()))
    }

    private fun generateCompilerFolder(templateFileFolder: String): Mustache.Compiler {
        return Mustache.compiler().withLoader { name ->
            FileReader(File(templateFileFolder, name))
        }
    }

    fun getLayoutManagerType(formPath: String): String {
        Log.d("getLayoutManagerType: $formPath")

        var type = "Collection"
        getTemplateManifestJSONContent(formPath)?.let {
            type = it.getSafeObject("tags")?.getSafeString("___LISTFORMTYPE___") ?: "Collection"
        }

        return when (type) {
            "Collection" -> "GRID"
            "Table" -> "LINEAR"
            else -> "LINEAR"
        }
    }
}