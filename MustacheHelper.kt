import DefaultValues.DEFAULT_ADDRESS
import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_REMOTE_URL
import DefaultValues.LAYOUT_FILE
import FileHelperConstants.ACTIONS_FILENAME
import FileHelperConstants.APP_INFO_FILENAME
import FileHelperConstants.CUSTOM_FORMATTERS_FILENAME
import FileHelperConstants.DS_STORE
import FileHelperConstants.TABLE_INFO_FILENAME
import MustacheConstants.ANDROID_SDK_PATH
import MustacheConstants.APP_NAME_WITH_CAPS
import MustacheConstants.AUTHOR
import MustacheConstants.CACHE_4D_SDK_PATH
import MustacheConstants.COMPANY_HEADER
import MustacheConstants.CUSTOM_FORMATTER_IMAGES
import MustacheConstants.DATE_DAY
import MustacheConstants.DATE_MONTH
import MustacheConstants.DATE_YEAR
import MustacheConstants.DEBUG_MODE
import MustacheConstants.ENTITY_CLASSES
import MustacheConstants.FIELDS
import MustacheConstants.FIRST_FIELD
import MustacheConstants.FORM_FIELDS
import MustacheConstants.HAS_ANY_MANY_TO_ONE_RELATION
import MustacheConstants.HAS_ANY_ONE_TO_MANY_RELATION
import MustacheConstants.HAS_ANY_ONE_TO_MANY_RELATION_FOR_LAYOUT
import MustacheConstants.HAS_CUSTOM_FORMATTER_IMAGES
import MustacheConstants.HAS_DATASET
import MustacheConstants.HAS_RELATION
import MustacheConstants.HAS_REMOTE_ADDRESS
import MustacheConstants.KOTLIN_INPUT_CONTROLS
import MustacheConstants.PACKAGE
import MustacheConstants.PERMISSIONS
import MustacheConstants.RELATIONS
import MustacheConstants.RELATIONS_EMBEDDED_RETURN_TYPE
import MustacheConstants.RELATIONS_ID
import MustacheConstants.RELATIONS_MANY_TO_ONE
import MustacheConstants.RELATIONS_MANY_TO_ONE_FOR_DETAIL
import MustacheConstants.RELATIONS_MANY_TO_ONE_FOR_LIST
import MustacheConstants.RELATIONS_ONE_TO_MANY
import MustacheConstants.RELATIONS_ONE_TO_MANY_FOR_DETAIL
import MustacheConstants.RELATIONS_ONE_TO_MANY_FOR_LIST
import MustacheConstants.RELATIONS_WITHOUT_ALIAS
import MustacheConstants.REMOTE_ADDRESS
import MustacheConstants.TABLENAME
import MustacheConstants.TABLENAMES
import MustacheConstants.TABLENAMES_LAYOUT
import MustacheConstants.TABLENAMES_LOWERCASE
import MustacheConstants.TABLENAMES_NAVIGATION
import MustacheConstants.TABLENAMES_WITHOUT_MANY_TO_ONE_RELATION
import MustacheConstants.TABLENAMES_LAYOUT_RELATIONS
import MustacheConstants.TABLENAMES_NAVIGATION_FOR_NAVBAR
import MustacheConstants.TABLENAME_CAMELCASE
import MustacheConstants.TABLENAME_LOWERCASE
import MustacheConstants.TABLENAME_ORIGINAL
import MustacheConstants.TABLE_HAS_ANY_RELATION
import MustacheConstants.TABLE_HAS_DATE_FIELD
import MustacheConstants.TABLE_HAS_ONE_TO_MANY_FIELD
import MustacheConstants.TABLE_HAS_TIME_FIELD
import MustacheConstants.TYPES_AND_TABLES
import PathHelperConstants.TEMPLATE_PLACEHOLDER
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.samskivert.mustache.Mustache
import com.samskivert.mustache.Template
import org.json.JSONObject
import java.io.File
import java.io.FileReader
import java.lang.Integer.toHexString
import java.util.*

class MustacheHelper(private val fileHelper: FileHelper, private val projectEditor: ProjectEditor) {

    private var data = mutableMapOf<String, Any>()

    private val gson: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()

    private val featureChecker = FeatureChecker(projectEditor)

    private lateinit var compiler: Mustache.Compiler
    private lateinit var template: Template

    private val tableNamesForNavigation = mutableListOf<TemplateLayoutFiller>()
    private val tableNamesForNavigationForNavBar = mutableListOf<TemplateLayoutFiller>()
    private val tableNamesForLayoutType = mutableListOf<TemplateLayoutTypeFiller>()
    private var tableNames = mutableListOf<TemplateTableFiller>()
    private var tableNamesLowercase = mutableListOf<TemplateLayoutFiller>()
    private var relationsManyToOne = mutableListOf<TemplateRelationFiller>()
    private var relationsOneToMany = mutableListOf<TemplateRelationFiller>()
    private var relationsEmbeddedReturnType = mutableListOf<TemplateRelationForRoomFiller>()
    private var customFormatterImages = mutableListOf<TemplateFormatterFiller>()
    private val tableNamesWithoutManyToOneRelation = mutableListOf<TemplateTableFiller>()

    private val oneToManyRelationFillerForEachListLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val manyToOneRelationFillerForEachListLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val oneToManyRelationFillerForEachDetailLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val manyToOneRelationFillerForEachDetailLayout = mutableListOf<TemplateRelationFillerForEachLayout>()

    private val permissionFillerList = mutableListOf<TemplatePermissionFiller>()

    // <formatName, <imageName, <resourceName, darkModeResourceName>>
    private val customFormattersImagesMap: MutableMap<String, MutableMap<String, Pair<String, String>>> = mutableMapOf()
    // <tableName, <fieldName, fieldMapping>>
    private val customFormattersFields: MutableMap<String, MutableMap<String, FieldMapping>> = mutableMapOf()

    // <tableName, List<Fields>>
    private val tableFieldsMap = mutableMapOf<String, List<Field>>()

    private val actions = projectEditor.getActions()

    private var kotlinInputControls = mutableListOf<TemplateInputControlFiller>()


    init {
        Log.d("==================================\n" +
                "MustacheHelper init\n" +
                "==================================\n")

        data[COMPANY_HEADER] = fileHelper.pathHelper.companyWithCaps
        data[AUTHOR] = projectEditor.findJsonString("author") ?: DEFAULT_AUTHOR
        data[DATE_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        data[DATE_MONTH] = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        data[DATE_YEAR] = Calendar.getInstance().get(Calendar.YEAR).toString()
        data[PACKAGE] = fileHelper.pathHelper.pkg
        data[APP_NAME_WITH_CAPS] = fileHelper.pathHelper.appNameWithCaps

        data[DEBUG_MODE] = projectEditor.findJsonBoolean("debugMode") ?: false

        // for network_security_config.xml
        // whitelist production host address if defined, else, server host address, else localhost
        var remoteAddress = projectEditor.findJsonString("productionUrl")
        Log.d("remoteAddress : $remoteAddress")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = projectEditor.findJsonString("remoteUrl")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = DEFAULT_REMOTE_URL
        val cleanRemoteAddress = remoteAddress.removePrefix("https://").removePrefix("http://").split(":")[0]
        if (cleanRemoteAddress != DEFAULT_ADDRESS) {
            data[HAS_REMOTE_ADDRESS] = true
            data[REMOTE_ADDRESS] = cleanRemoteAddress
            Log.d("cleanRemoteAddress = $cleanRemoteAddress")
        } else {
            data[HAS_REMOTE_ADDRESS] = false
            data[REMOTE_ADDRESS] = ""
            Log.d("\"$DEFAULT_ADDRESS\" is already added in network_security_config.xml")
        }

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
                    throw Exception("Missing Android SDK path")
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
                throw Exception("Missing 4D Mobile cache SDK path. Define `cache_4d_sdk` in json file or `QMOBILE_HOME` env variable")
            }
        }
        Log.d("> Cache 4D SDK = ${data[CACHE_4D_SDK_PATH]}")
        if (!File("${data[CACHE_4D_SDK_PATH]}").exists()) {
            throw Exception("Cache 4D SDK path do not exists. Define it correctly.")
        }

        val defaultSeed = "rgb(103,80,164)" // #6750A4

        val seed = projectEditor.findJsonString("dominantColor") ?: defaultSeed

        Log.i("seed = $seed")

        val rgbString = seed.removePrefix("rgb(").removeSuffix(")") // 103,80,164
        val red = rgbString.split(",")[0].toInt()
        val green = rgbString.split(",")[1].toInt()
        val blue = rgbString.split(",")[2].toInt()

        val primaryContrast = getContrast(red, green, blue)
        val hexStringSeedColor = getHexStringColor(red, green, blue)
        val seedColor: Int = Color.parseColor(hexStringSeedColor)
        Log.i("backgroundColor = $hexStringSeedColor")
        data["seed"] = hexStringSeedColor

        if (primaryContrast < 0.5) { // LIGHT COLOR
            fillLightColorLightTheme(seedColor)
            fillLightColorDarkTheme(seedColor)
        } else { // DARK COLOR
            fillDarkColorLightTheme(seedColor)
            fillDarkColorDarkTheme(seedColor)
        }


        val layoutRelationList = mutableListOf<TemplateRelationFiller>()
        var entityClassesString = ""

        projectEditor.dataModelList.forEach { dataModel ->

            dataModel.relations?.filter { it.isNotNativeType(projectEditor.dataModelList) }?.forEach { relation ->
                val filler = relation.getTemplateRelationFiller(projectEditor.catalogDef)
                if (relation.type == RelationType.MANY_TO_ONE) {
                    relationsManyToOne.add(filler)
                } else {
                    relationsOneToMany.add(filler)
                }
            }

            tableNames.add(dataModel.getTemplateTableFiller())

            tableNamesLowercase.add(dataModel.getTemplateLayoutFiller())

            entityClassesString += "${dataModel.name.tableNameAdjustment()}::class, "
        }

        tableNames.forEach { tableName ->
            if (!relationsManyToOne.map { it.relation_source.tableNameAdjustment() }.contains(tableName.name))
                tableNamesWithoutManyToOneRelation.add(tableName)
        }

        data[TABLENAMES] = tableNames
        data[TABLENAMES_LOWERCASE] = tableNamesLowercase

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        val relations = mutableListOf<TemplateRelationDefFiller>()
        val relationsId = mutableListOf<TemplateRelationDefFiller>()

        Log.d("Hiya relations many to one")
        relationsManyToOne.forEach {
            Log.d("filler: $it")
            val relationDefFiller = it.getTemplateRelationDefFiller(RelationType.MANY_TO_ONE)
            Log.d("relation filler : $relationDefFiller")
            relations.add(relationDefFiller)
            if (!it.isAlias)
                relationsId.add(it.getTemplateRelationDefFillerForRelationId())
        }
        Log.d("Hiya relations one to many")
        relationsOneToMany.forEach {
            Log.d("filler: $it")
            Log.d("relation filler : ${it.getTemplateRelationDefFiller(RelationType.ONE_TO_MANY)}")
            relations.add(it.getTemplateRelationDefFiller(RelationType.ONE_TO_MANY))
        }
        data[RELATIONS] = relations.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        data[RELATIONS_WITHOUT_ALIAS] = relations.distinctBy { it.relation_source to it.relation_target to it.relation_name }.filter { it.path.isNullOrEmpty() }

        data[RELATIONS_ID] = relationsId.distinctBy { it.relation_source to it.relation_target to it.relation_name }

        data[HAS_ANY_MANY_TO_ONE_RELATION] = relationsManyToOne.isNotEmpty()
        data[HAS_ANY_ONE_TO_MANY_RELATION] = relationsOneToMany.isNotEmpty()

        data[TABLENAMES_WITHOUT_MANY_TO_ONE_RELATION] = tableNamesWithoutManyToOneRelation
        data[ENTITY_CLASSES] = entityClassesString.dropLast(2)

        val typesAndTables = mutableListOf<TemplateTableFiller>()
        typesAndTables.addAll(tableNames)
        typesAndTables.add(getTemplateTableFiller("Photo"))
        typesAndTables.add(getTemplateTableFiller("Date"))
        typesAndTables.add(getTemplateTableFiller("Time"))
        typesAndTables.add(getTemplateTableFiller("Map"))
        data[TYPES_AND_TABLES] = typesAndTables

        var navigationTableCounter = 0 // Counter to limit to 4 navigation tables as it is not possible to have more than 5
        projectEditor.navigationTableList.forEach { dataModelId ->
            projectEditor.dataModelList.find { it.id == dataModelId }?.let { dataModel ->
                if (navigationTableCounter <= 3) {

                    navigationTableCounter++
                    Log.w("Adding [${dataModel.name}] in navigation table list for navbar")
                    tableNamesForNavigationForNavBar.add(dataModel.getTemplateLayoutFillerForNavigation())
                } else {
                    Log.w("Not adding [${dataModel.name}] in navigation table list for navbar")
                }
            }
        }

        projectEditor.dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
            Log.w("Adding [${dataModel.name}] in navigation table list")

            tableNamesForNavigation.add(dataModel.getTemplateLayoutFillerForNavigation())

            dataModel.relations?.filter { it.isNotNativeType(projectEditor.dataModelList) }?.forEach { relation ->
                layoutRelationList.add(relation.getTemplateRelationFiller(projectEditor.catalogDef))
            }
        }
        data[TABLENAMES_NAVIGATION] = tableNamesForNavigation
        data[TABLENAMES_NAVIGATION_FOR_NAVBAR] = tableNamesForNavigationForNavBar
        data[TABLENAMES_LAYOUT_RELATIONS] = layoutRelationList.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        data[HAS_RELATION] = layoutRelationList.isNotEmpty()

        // Specifying if list layout is table or collection (LinearLayout or GridLayout)
        tableNamesForNavigation.map { it.name }.forEach { tableName ->
            val listFormName =
                projectEditor.listFormList.find { listForm -> listForm.dataModel.name.tableNameAdjustment() == tableName.tableNameAdjustment() }?.name
            var formPath = fileHelper.pathHelper.getFormPath(listFormName, FormType.LIST)
            formPath = fileHelper.pathHelper.verifyFormPath(formPath, FormType.LIST)
            tableNamesForLayoutType.add(getTemplateLayoutTypeFiller(tableName, formPath))
        }

        data[TABLENAMES_LAYOUT] = tableNamesForLayoutType

        getCustomFormatterFields()

        customFormatterImages = mutableListOf()

        for ((formatterName, imageMap) in customFormattersImagesMap) {
            for ((imageName, pair) in imageMap) { // <imageName, <resourceName, darkModeResourceName>
                customFormatterImages.add(
                    getTemplateFormatterFiller(
                        formatterName = formatterName,
                        imageName = imageName,
                        pair = pair
                    )
                )
            }
        }
        data[CUSTOM_FORMATTER_IMAGES] = customFormatterImages
        data[HAS_CUSTOM_FORMATTER_IMAGES] = customFormatterImages.isNotEmpty()

        data[HAS_DATASET] = projectEditor.findJsonBoolean(FeatureFlagConstants.HAS_DATASET_KEY) ?: true

        getAllInputControls()
        data[KOTLIN_INPUT_CONTROLS] = kotlinInputControls.distinct()

        getAllActionPermissions()
        data[PERMISSIONS] = permissionFillerList.distinct()
    }

    /**
     * TEMPLATING
     */
    fun processTemplates() {
        Log.d("processTemplates")
        File(fileHelper.pathHelper.templateFilesPath).walkTopDown()
            .filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->
                processFolder(currentFolder)
            }
    }

    private fun processFolder(currentFolder: File) {

        compiler = generateCompilerFolder(currentFolder.absolutePath)

        currentFolder.walkTopDown()
            .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) && file.name != DS_STORE }
            .forEach { currentFile ->
                featureChecker.checkFeaturesAndProcess(currentFile) {
                    processFile(currentFile)
                }
            }
    }

    private fun processFile(currentFile: File) {
        Log.d("processFile : $currentFile")

        template = compiler.compile("{{>${currentFile.name}}}")

        val newFilePath = fileHelper.pathHelper.getPath(currentFile.absolutePath.replaceXmlTxtSuffix())

        relationsManyToOne.clear()
        relationsOneToMany.clear()

        projectEditor.dataModelList.forEach { dataModel ->
            dataModel.relations?.forEach { relation ->
                Log.d("HH: relation: $relation")
                val filler = relation.getTemplateRelationFiller(projectEditor.catalogDef)
                Log.d("HH: filler: $filler")
                if (relation.type == RelationType.MANY_TO_ONE) {
                    if (relation.path.isEmpty())
                        relationsManyToOne.add(filler)
                } else {
                    relationsOneToMany.add(filler)
                }
            }
        }

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne.distinctBy { it.relation_source to it.relation_target to it.relation_name }

        Log.d("RELATIONS_MANY_TO_ONE --------------------")
        relationsManyToOne.distinctBy { it.relation_source to it.relation_target to it.relation_name to it.path }.forEach {
            Log.d("Source [${it.relation_source}] Target [${it.relation_target}] Name [${it.relation_name}] Path [${it.path}]")
        }
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany.distinctBy { it.relation_source to it.relation_target to it.relation_name }

        Log.d("RELATIONS_ONE_TO_MANY --------------------")
        relationsOneToMany.distinctBy { it.relation_source to it.relation_target to it.relation_name to it.path }.forEach {
            Log.d("Source [${it.relation_source}] Target [${it.relation_target}] Name [${it.relation_name}] Path [${it.path}]")
        }

        projectEditor.dataModelList.forEach { dataModel ->
            dataModel.relations?.filter { it.isNotNativeType(projectEditor.dataModelList) }?.forEach { relation ->
                Log.d("relationsEmbeddedReturnType relation : $relation")
                relation.getTemplateRelationForRoomFiller(projectEditor.catalogDef)?.let { filler ->
                    Log.d("relationsEmbeddedReturnType Add filler : $filler")
                    relationsEmbeddedReturnType.add(filler)
                }
            }
        }

        data[RELATIONS_EMBEDDED_RETURN_TYPE] = relationsEmbeddedReturnType.distinctBy { it.className }

        data[RELATIONS_ONE_TO_MANY_FOR_LIST] = oneToManyRelationFillerForEachListLayout
        data[RELATIONS_MANY_TO_ONE_FOR_LIST] = manyToOneRelationFillerForEachListLayout
        data[RELATIONS_ONE_TO_MANY_FOR_DETAIL] = oneToManyRelationFillerForEachDetailLayout
        data[RELATIONS_MANY_TO_ONE_FOR_DETAIL] = manyToOneRelationFillerForEachDetailLayout

        if (currentFile.isWithTemplateName()) {
            Log.d("currentFile isWithTemplateName")
            Log.d("currentFile isWithTemplateName, tableNames: $tableNames")

            for (tableName in tableNames) { // file will be duplicated

                Log.d("currentFile isWithTemplateName, tableName: $tableName")


                if (newFilePath.contains(fileHelper.pathHelper.navigationPath()) ||
                    newFilePath.contains(fileHelper.pathHelper.formPath("list")) ||
                    newFilePath.contains(fileHelper.pathHelper.formPath("detail"))) {
                    if (tableNamesForNavigation.firstOrNull { it.name == tableName.name } == null)
                        continue
                }

                fillFileWithTemplateName(tableName)

                val replacedPath = if (newFilePath.contains(fileHelper.pathHelper.resPath()))
                    newFilePath.replace(TEMPLATE_PLACEHOLDER, tableName.name.toLowerCase())
                else
                    newFilePath.replace(TEMPLATE_PLACEHOLDER, tableName.name)

                applyTemplate(replacedPath)

                //cleaning
                data.remove(FIELDS)
                data.remove(TABLE_HAS_DATE_FIELD)
                data.remove(TABLE_HAS_TIME_FIELD)
                data.remove(TABLE_HAS_ONE_TO_MANY_FIELD)
                data.remove(RELATIONS_MANY_TO_ONE)
                data.remove(FIRST_FIELD)
            }

        } else {
            Log.d("currentFile applying default templating")
            applyTemplate(newFilePath)
        }
    }

    private fun fillFileWithTemplateName(tableName: TemplateTableFiller) {

        data[TABLENAME] = tableName.name.tableNameAdjustment()

        data[TABLENAME_ORIGINAL] = tableName.name_original.encode()
        projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }
            ?.let { dataModel ->
                data[TABLENAME_ORIGINAL] = dataModel.getLabel().encode()
            }

        data[TABLENAME_LOWERCASE] = tableName.name.toLowerCase().fieldAdjustment()
        data[TABLENAME_CAMELCASE] = tableName.name.dataBindingAdjustment()
        projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.fields?.let { fields ->

            val fieldList = mutableListOf<TemplateFieldFiller>()
            fields.filter { it.kind != "alias" }.forEach { field ->
                Log.d("> Field [${field.name}] : $field")
                field.fieldTypeString?.let { fieldTypeString ->
                    fieldList.add(field.getTemplateFieldFiller(fieldTypeString))
                } ?: kotlin.run {
                    throw Exception("An error occurred while parsing the fieldType of field : $field")
                }
            }

            tableFieldsMap[tableName.name_original] = fields

            data[FIELDS] = fieldList
            data[TABLE_HAS_DATE_FIELD] = fieldList.map { it.fieldTypeString }.contains("Date")
            data[TABLE_HAS_TIME_FIELD] = fieldList.map { it.fieldTypeString }.contains("Time")
            data[TABLE_HAS_ONE_TO_MANY_FIELD] = fieldList.map { it.fieldTypeString }.firstOrNull { it.startsWith("Entities<") } != null
            val firstField: String = fieldList.firstOrNull()?.name ?: ""
            data[FIRST_FIELD] = firstField
        }

        relationsManyToOne.clear()
        relationsOneToMany.clear()
        data[TABLE_HAS_ANY_RELATION] = false

        projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.relations?.filter { it.isNotNativeType(projectEditor.dataModelList) }?.forEach { relation ->
            val filler = relation.getTemplateRelationFiller(projectEditor.catalogDef)
            if (relation.type == RelationType.MANY_TO_ONE) {
                Log.d("XXX Add Many to one filler = $filler")
                Log.d("XXX Add Many to one, relation was $relation")
                relationsManyToOne.add(filler)
            } else {
                Log.d("XXX Add One to many filler = $filler")
                relationsOneToMany.add(filler)

            }
        }

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany.distinctBy { it.relation_source to it.relation_target to it.relation_name }
        if (relationsManyToOne.size > 0 || relationsOneToMany.size >0)
            data[TABLE_HAS_ANY_RELATION] = true
    }

    fun applyListFormTemplate() {
        projectEditor.listFormList.forEach { listForm ->

            Log.d("applyListFormTemplate : listForm.name = ${listForm.name} for table ${listForm.dataModel.name}. FieldSize : ${listForm.fields?.size}")

            var formPath = fileHelper.pathHelper.getFormPath(listForm.name, FormType.LIST)
            formPath = fileHelper.pathHelper.verifyFormPath(formPath, FormType.LIST)

            val appFolderInTemplate = fileHelper.pathHelper.getAppFolderInTemplate(formPath)

            File(appFolderInTemplate).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
                .forEach { currentFolder ->

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
                                relationsManyToOne.clear()
                                relationsOneToMany.clear()
                                projectEditor.dataModelList.find { it.name.tableNameAdjustment() == listForm.dataModel.name.tableNameAdjustment() }?.relations?.filter { it.isNotNativeType(projectEditor.dataModelList) }?.forEach { relation ->
                                    val filler = relation.getTemplateRelationFiller(projectEditor.catalogDef)
                                    if (relation.type == RelationType.MANY_TO_ONE) {
                                        relationsManyToOne.add(filler)
                                    } else {
                                        relationsOneToMany.add(filler)
                                    }
                                }
                                data[RELATIONS_MANY_TO_ONE] = relationsManyToOne.distinctBy { it.relation_source to it.relation_target to it.relation_name }
                                data[RELATIONS_ONE_TO_MANY] = relationsOneToMany.distinctBy { it.relation_source to it.relation_target to it.relation_name }
                                data[HAS_ANY_ONE_TO_MANY_RELATION_FOR_LAYOUT] = relationsOneToMany.isNotEmpty()

                                var wholeFormHasIcons = false

                                listForm.fields?.forEach { field ->
                                    val fieldFromDataModel: Field? = getDataModelField(projectEditor.dataModelList, listForm, field)
                                    if (!fieldFromDataModel?.icon.isNullOrEmpty())
                                        wholeFormHasIcons = true
                                }

                                Log.d("wholeFormHasIcons = $wholeFormHasIcons")

                                var i = 0
                                listForm.fields?.forEach { field -> // Could also iterate over specificFieldsCount as Detail form
                                    i++

                                    Log.d("[${listForm.dataModel.name}][${field.name}] - $field")

                                    if (fileHelper.pathHelper.isDefaultTemplateListFormPath(formPath) && field.isImage()) { // is image in default template
                                        resetIndexedEntries(i)
                                    } else { // not a relation
                                        fillIndexedFormData(i, field, listForm, wholeFormHasIcons)
                                        if (isRelationWithFixes(projectEditor.dataModelList, listForm, field)) {
                                            fillRelationFillerForEachLayout(field, listForm, FormType.LIST, i)
                                        }
                                    }
                                }

                                val newFilePath = fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name.tableNameAdjustment())
                                applyTemplate(newFilePath)

                                // cleaning data for other templates
                                for (j in 1 until i + 1) {
                                    removeIndexedEntries(j)
                                }
                                data.remove(RELATIONS_MANY_TO_ONE)
                                data.remove(RELATIONS_ONE_TO_MANY)
                            } else { // any file to copy in project

                                copyOtherTemplateFiles(currentFile, formPath, listForm.dataModel.name.tableNameAdjustment())
                            }
                        }
                }
        }
    }

    fun applyDetailFormTemplate() {
        projectEditor.detailFormList.forEach { detailForm ->

            Log.d("applyDetailFormTemplate : detailForm.name = ${detailForm.name} for table ${detailForm.dataModel.name}. FieldSize : ${detailForm.fields?.size}")

            var formPath = fileHelper.pathHelper.getFormPath(detailForm.name, FormType.DETAIL)
            formPath = fileHelper.pathHelper.verifyFormPath(formPath, FormType.DETAIL)

            // not used in list form
            var specificFieldsCount = 0
            var maxFields = 0
            getManifestJSONContent(formPath)?.let {
                specificFieldsCount = it.getSafeObject("fields")?.getSafeInt("count") ?: 0
                val manifestMaxFields = it.getSafeObject("fields")?.getSafeInt("max") ?: 0
                val defMaxFields = detailForm.fields?.size ?: 0
                maxFields = if (manifestMaxFields > defMaxFields) defMaxFields else manifestMaxFields
                if (maxFields == 0) maxFields = detailForm.fields?.size ?: 0
            }

            val appFolderInTemplate = fileHelper.pathHelper.getAppFolderInTemplate(formPath)

            File(appFolderInTemplate).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
                .forEach { currentFolder ->

                    compiler = generateCompilerFolder(currentFolder.absolutePath)

                    currentFolder.walkTopDown()
                        .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) && file.name != DS_STORE }
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

                                    Log.d("My detailForm field list :")
                                    fieldList.forEach {
                                        Log.d(it.name)
                                    }

                                    var wholeFormHasIcons = false

                                    detailForm.fields?.forEach { field ->
                                        val fieldFromDataModel: Field? = getDataModelField(projectEditor.dataModelList, detailForm, field)
                                        if (!fieldFromDataModel?.icon.isNullOrEmpty())
                                            wholeFormHasIcons = true
                                    }

                                    Log.d("wholeFormHasIcons = $wholeFormHasIcons")

                                    if (fieldList.isNotEmpty()) {

                                        if (specificFieldsCount == 0) { // template with no specific field
                                            for (i in fieldList.indices) {
                                                val field = fieldList[i]
                                                if (field.name.isNotEmpty()) {

                                                    val format = getFormatNameForType(fileHelper.pathHelper, projectEditor.dataModelList, detailForm, field)
                                                    val formField = field.getTemplateFormFieldFiller(
                                                        i = i + 1,
                                                        dataModelList = projectEditor.dataModelList,
                                                        form = detailForm,
                                                        formatType = format,
                                                        isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                        imageWidth = getImageSize(detailForm, field.name, "width"),
                                                        imageHeight = getImageSize(detailForm, field.name, "height"),
                                                        wholeFormHasIcons = wholeFormHasIcons,
                                                        pathHelper = fileHelper.pathHelper,
                                                        catalogDef = projectEditor.catalogDef
                                                    )

                                                    formFieldList.add(formField)

                                                    fillRelationFillerForEachLayout(field, detailForm, FormType.DETAIL, i + 1)

                                                } else {
                                                    // you can get null fields in json file
                                                    // occurs when you select a form, and then select back Blank form
                                                }
                                            }
                                        } else { // template with specific fields

                                            for (i in 0 until specificFieldsCount) {

                                                if (i < fieldList.size) {
                                                    val field = fieldList[i]
                                                    fillIndexedFormData(i + 1, field, detailForm, wholeFormHasIcons)
                                                    fillRelationFillerForEachLayout(field, detailForm, FormType.DETAIL, i + 1)

                                                } else {
                                                    Log.d("Field list shorter than specific fields count")
                                                    resetIndexedEntries(i + 1)
                                                }
                                            }

                                            if (fieldList.size > specificFieldsCount && specificFieldsCount < maxFields) {

                                                var k = specificFieldsCount // another counter to avoid null field

                                                for (i in specificFieldsCount until maxFields) {
                                                    Log.d("index i is $i, specificFieldsCount $specificFieldsCount, maxFields $maxFields")
                                                    val field = fieldList[i]

                                                    if (field.name.isNotEmpty()) {

                                                        Log.d("Adding free Field in specific template ${field.name}")

                                                        val format = getFormatNameForType(fileHelper.pathHelper, projectEditor.dataModelList, detailForm, field)
                                                        val formField = field.getTemplateFormFieldFiller(
                                                            i = k + 1,
                                                            dataModelList = projectEditor.dataModelList,
                                                            form = detailForm,
                                                            formatType = format,
                                                            isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                            imageWidth = getImageSize(detailForm, field.name, "width"),
                                                            imageHeight = getImageSize(detailForm, field.name, "height"),
                                                            wholeFormHasIcons = wholeFormHasIcons,
                                                            pathHelper = fileHelper.pathHelper,
                                                            catalogDef = projectEditor.catalogDef
                                                        )

                                                        fillRelationFillerForEachLayout(field, detailForm, FormType.DETAIL, k + 1)

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

                                val newFilePath =
                                    fileHelper.pathHelper.getDetailFormPath(detailForm.dataModel.name.tableNameAdjustment())
                                applyTemplate(newFilePath)

                                // cleaning data for other templates
                                data.remove(FORM_FIELDS)
                                for (i in 1 until specificFieldsCount) {
                                    removeIndexedEntries(i)
                                }

                            } else { // any file to copy in project
                                copyOtherTemplateFiles(currentFile, formPath, detailForm.dataModel.name.tableNameAdjustment())
                            }
                        }
                }
        }
    }

    private fun fillRelationFillerForEachLayout(field: Field, form: Form, formType: FormType, index: Int) {
        Log.d("XX: fillRelationFillerForEachLayout, $field")
        val source: String = form.dataModel.name
        val navbarTitle = getNavbarTitle(projectEditor.dataModelList, form, field, projectEditor.catalogDef)

        findRelation(projectEditor.dataModelList, source, field)?.let { relation ->
            fillRelationFillerForEachRelation(source, index, formType, relation, navbarTitle)
        }
    }

    private fun fillRelationFillerForEachRelation(source: String, index: Int, formType: FormType, relation: Relation, navbarTitle: String) {
        val filler = getTemplateRelationFillerForLayout(source, index, navbarTitle, relation, projectEditor.catalogDef)
        when {
            formType == FormType.LIST && relation.type == RelationType.ONE_TO_MANY ->
                oneToManyRelationFillerForEachListLayout.add(filler)
            formType == FormType.LIST && relation.type == RelationType.MANY_TO_ONE ->
                manyToOneRelationFillerForEachListLayout.add(filler)
            formType == FormType.DETAIL && relation.type == RelationType.ONE_TO_MANY ->
                oneToManyRelationFillerForEachDetailLayout.add(filler)
            formType == FormType.DETAIL && relation.type == RelationType.MANY_TO_ONE ->
                manyToOneRelationFillerForEachDetailLayout.add(filler)
        }
        Log.d("Adding fillRelationFillerForEachRelation : $filler")
    }

    private fun removeIndexedEntries(i: Int) {
        data.remove("field_${i}_name")
        data.remove("field_${i}_defined")
        data.remove("field_${i}_is_relation")
        data.remove("field_${i}_is_image")
        data.remove("field_${i}_label")
        data.remove("field_${i}_shortLabel")
        data.remove("field_${i}_iconPath")
        data.remove("field_${i}_hasIcon")
        data.remove("field_${i}_custom_formatted")
        data.remove("field_${i}_is_kotlin_custom_formatted")
        data.remove("field_${i}_kotlin_custom_format_binding")
        data.remove("field_${i}_custom_formatted_imageNamed")
        data.remove("field_${i}_format_type")
        data.remove("field_${i}_accessor")
        data.remove("field_${i}_image_field_name")
        data.remove("field_${i}_image_source_table_name")
        data.remove("field_${i}_image_key_accessor")
        data.remove("field_${i}_format_field_name")
        data.remove("field_${i}_field_table_name")
        data.remove("field_${i}_field_image_width")
        data.remove("field_${i}_field_image_height")
        data.remove("field_${i}_label_has_percent_placeholder")
        data.remove("field_${i}_label_with_percent_placeholder")
        data.remove("field_${i}_shortLabel_has_percent_placeholder")
        data.remove("field_${i}_shortLabel_with_percent_placeholder")
    }

    private fun resetIndexedEntries(i: Int) {
        data["field_${i}_name"] = ""
        data["field_${i}_defined"] = false
        data["field_${i}_is_relation"] = false
        data["field_${i}_is_image"] = false
        data["field_${i}_label"] = ""
        data["field_${i}_shortLabel"] = ""
        data["field_${i}_iconPath"] = ""
        data["field_${i}_hasIcon"] = false
        data["field_${i}_custom_formatted"] = false
        data["field_${i}_is_kotlin_custom_formatted"] = false
        data["field_${i}_kotlin_custom_format_binding"] = ""
        data["field_${i}_custom_formatted_imageNamed"] = false
        data["field_${i}_format_type"] = ""
        data["field_${i}_accessor"] = ""
        data["field_${i}_image_field_name"] = ""
        data["field_${i}_image_source_table_name"] = ""
        data["field_${i}_image_key_accessor"] = ""
        data["field_${i}_format_field_name"] = ""
        data["field_${i}_field_table_name"] = ""
        data["field_${i}_field_image_width"] = 0
        data["field_${i}_field_image_height"] = 0
        data["field_${i}_label_has_percent_placeholder"] = false
        data["field_${i}_label_with_percent_placeholder"] = ""
        data["field_${i}_shortLabel_has_percent_placeholder"] = false
        data["field_${i}_shortLabel_with_percent_placeholder"] = ""
    }

    private fun fillIndexedFormData(i: Int, field: Field, form: Form, wholeFormHasIcons: Boolean) {
        Log.d("index is $i")
        Log.d("fillIndexedFormData, field = $field")
        data["field_${i}_name"] = field.getFieldAliasName(projectEditor.dataModelList)
        data["field_${i}_defined"] = field.name.isNotEmpty()
        data["field_${i}_is_image"] = field.isImage()
        data["field_${i}_label"] = getLabelWithFixes(projectEditor.dataModelList, form, field)
        data["field_${i}_shortLabel"] = getShortLabelWithFixes(projectEditor.dataModelList, form, field)
        data["field_${i}_iconPath"] = ""
        data["field_${i}_hasIcon"] = false
        data["field_${i}_custom_formatted"] = false
        data["field_${i}_custom_formatted_imageNamed"] = false
        data["field_${i}_format_type"] = ""
        data["field_${i}_accessor"] = field.getLayoutVariableAccessor()
        data["field_${i}_image_field_name"] = field.getImageFieldName()
        data["field_${i}_image_source_table_name"] = destBeforeField(projectEditor.catalogDef, form.dataModel.name, field.path)

        val isRelation = isRelationWithFixes(projectEditor.dataModelList, form, field)
        Log.d("field ${field.name}, isRelation ? : $isRelation")
        if (isRelation) {
            data["field_${i}_is_relation"] = true
            val labelHasPercentPlaceholder = hasLabelPercentPlaceholder(projectEditor.dataModelList, form, field)
            if (labelHasPercentPlaceholder) {
                data["field_${i}_label_has_percent_placeholder"] = true
                data["field_${i}_label_with_percent_placeholder"] = getLabelWithPercentPlaceholder(projectEditor.dataModelList, form, field, projectEditor.catalogDef)
            }

            val shortLabelHasPercentPlaceholder = hasShortLabelPercentPlaceholder(projectEditor.dataModelList, form, field)
            if (shortLabelHasPercentPlaceholder) {
                data["field_${i}_shortLabel_has_percent_placeholder"] = true
                data["field_${i}_shortLabel_with_percent_placeholder"] = getShortLabelWithPercentPlaceholder(projectEditor.dataModelList, form, field, projectEditor.catalogDef)
            }
        }

        if (field.isImage()) {
            data["field_${i}_image_key_accessor"] = field.getFieldKeyAccessor(projectEditor.dataModelList)
        }

        if (wholeFormHasIcons) {
            data["field_${i}_iconPath"] = getIcon(projectEditor.dataModelList, form, field)
            data["field_${i}_hasIcon"] = true
        }

        val format = getFormatNameForType(fileHelper.pathHelper, projectEditor.dataModelList, form, field)
        data["field_${i}_format_type"] = format

        if (fileHelper.pathHelper.isValidFormatter(format)) {
            Log.d("isValidFormatter true")
            data["field_${i}_custom_formatted"] = true
            data["field_${i}_format_field_name"] = field.name
            data["field_${i}_field_table_name"] = form.dataModel.name

            if (isImageNamedBinding(form, field.name)) {

                Log.d("Field : ${field.name}, table : ${form.dataModel.name}, is imageNamed binding")

                data["field_${i}_custom_formatted_imageNamed"] = true
                data["field_${i}_field_image_width"] = getImageSize(form, field.name, "width")
                data["field_${i}_field_image_height"] = getImageSize(form, field.name, "height")
            } else {
                Log.d("Field : ${field.name}, table : ${form.dataModel.name}, is not imageNamed binding")
            }

        } else if (fileHelper.pathHelper.isValidKotlinCustomFormatter(format)) {
            Log.d("isValidKotlinCustomFormatter true")
            data["field_${i}_is_kotlin_custom_formatted"] = true
            data["field_${i}_kotlin_custom_format_binding"] = fileHelper.pathHelper.getKotlinCustomFormatterBinding(format)
        } else {
            Log.d("Both kotlin and basic custom formatters false")
        }
    }

    private fun copyOtherTemplateFiles(currentFile: File, formPath: String, tableName: String) {
        val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath,
            formPath))

        if (currentFile.isWithTemplateName()) {
            tableNames.find { it.name == tableName }?.let { templateTableFiller ->
                fillFileWithTemplateName(templateTableFiller)
                val replacedPath = newFile.absolutePath.replace(TEMPLATE_PLACEHOLDER, templateTableFiller.name)
                applyTemplate(newPath = replacedPath, overwrite = true)
                //cleaning
                data.remove(FIELDS)
                data.remove(RELATIONS_MANY_TO_ONE)
                data.remove(FIRST_FIELD)
            }
        } else {
            Log.i("File to copy : ${currentFile.absolutePath}; target : ${newFile.absolutePath}")
            if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                throw Exception("An error occurred while copying template files with target : ${newFile.absolutePath}")
            }
        }
    }

    private fun applyTemplate(newPath: String, overwrite: Boolean = false) {
        var newFile = File(newPath.replaceXmlTxtSuffix())
        val fileName = newFile.nameWithoutExtension

        if (reservedKeywords.contains(fileName)) {
            newFile = File(newFile.parent.removeSuffix("/")
                .removeSuffix("\\") + File.separator + fileName.validateWord() + "." + newFile.extension)
        }
        if (newFile.exists() && overwrite) {
            newFile.delete()
        }
        if (newFile.exists()) {
            return
        }
        newFile.parentFile.mkdirs()
        if (!newFile.createNewFile()) {
            throw Exception("An error occurred while creating new file : $newFile")
        }
        newFile.writeText(template.execute(data))
    }

    fun makeTableInfo() {
        makeJsonFile(TABLE_INFO_FILENAME, projectEditor.buildTableInfo(tableFieldsMap))
    }

    fun makeAppInfo() {
        makeJsonFile(APP_INFO_FILENAME, projectEditor.getAppInfo())
    }

    fun makeCustomFormatters() {
        makeJsonFile(CUSTOM_FORMATTERS_FILENAME, customFormattersFields)
    }

    fun makeActions() {
        val hasActionsFeatureFlag = projectEditor.findJsonBoolean(FeatureFlagConstants.HAS_ACTIONS_KEY) ?: true
        Log.d("hasActionsFeatureFlag = $hasActionsFeatureFlag")
        if (hasActionsFeatureFlag) {
            makeJsonFile(ACTIONS_FILENAME, actions)
        }
    }

    private fun makeJsonFile(fileName: String, content: Any) {
        val file = File(fileHelper.pathHelper.assetsPath(), fileName)
        file.parentFile.mkdirs()
        if (!file.createNewFile()) {
            throw Exception("An error occurred while creating new file : $file")
        }
        if (content is JSONObject) {
            file.writeText(content.toString())
        } else {
            file.writeText(gson.toJson(content))
        }
    }

    private fun generateCompilerFolder(templateFileFolder: String): Mustache.Compiler {
        return Mustache.compiler().escapeHTML(false).withLoader { name ->
            FileReader(File(templateFileFolder, name))
        }
    }

    private fun getAllActionPermissions() {
        getActionPermissions(actions.table.values)
        getActionPermissions(actions.currentRecord.values)
    }

    private fun getActionPermissions(actionListPerTable: Collection<List<Action>>) {
        actionListPerTable.forEach { actionList ->
            actionList.forEach { action ->
                action.parameters?.forEach { actionParameter ->
                    if (actionParameter.type == "string" && actionParameter.format == "barcode") {
                        permissionFillerList.add(getTemplatePermissionFiller(Permissions.CAMERA))
                    }
                }
            }
        }
    }

    private fun getAllInputControls() {
        val hasInputControlsFeatureFlag = projectEditor.findJsonBoolean(FeatureFlagConstants.HAS_KOTLIN_INPUT_CONTROLS) ?: true
        if (hasInputControlsFeatureFlag) {
            getInputControls(actions.table.values)
            getInputControls(actions.currentRecord.values)
        }
    }

    private fun getInputControls(actionListPerTable: Collection<List<Action>>) {
        actionListPerTable.forEach { actions ->
            actions.forEach { action ->
                action.parameters?.forEach { actionParameter ->
                    actionParameter.format?.let { format ->
                        if (format.startsWith("/")) {
                            val inputControlPath = fileHelper.pathHelper.getInputControlPath(format)
                            getManifestJSONContent(inputControlPath)?.let {

                                val kotlinInputControlClass = fileHelper.pathHelper.findMatchingKotlinInputControlClass(inputControlPath)
                                if (kotlinInputControlClass != null) {

                                    val templateInputControlFiller = TemplateInputControlFiller(
                                        name = format.removePrefix("/"),
                                        class_name = kotlinInputControlClass
                                    )
                                    kotlinInputControls.add(templateInputControlFiller)

                                    val fieldMapping = getFieldMapping(it, format)
                                    Log.d("fieldMapping for input control :  $fieldMapping")

                                    if (fieldMapping.isValidKotlinInputControl()) {
                                        // Saving any permission for input controls
                                        fieldMapping.capabilities.forEach { permissionName ->
                                            permissionFillerList.add(getTemplatePermissionFiller(permissionName))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // <tableName, <fieldName, fieldMapping>>
    private fun getCustomFormatterFields() {

        Log.d("getCustomFormatterFields checking list forms")
        projectEditor.listFormList.forEach { listForm ->
            getCustomFormatterField(listForm)
        }
        Log.d("getCustomFormatterFields customFormatMap: $customFormattersFields")

        Log.d("\ngetCustomFormatterFields checking detail forms")
        projectEditor.detailFormList.forEach { detailForm ->
            getCustomFormatterField(detailForm)
        }
    }

    private fun getCustomFormatterField(form: Form) {
        Log.d("form for ${form.dataModel.name}")
        form.fields?.forEach { field ->
            Log.d("field = $field")
            getDataModelField(projectEditor.dataModelList, form, field)?.let { fieldFromDataModel ->
                Log.d("fieldFromDataModel = $fieldFromDataModel")
                val map: MutableMap<String, FieldMapping> = customFormattersFields[form.dataModel.name.tableNameAdjustment()] ?: mutableMapOf()
                if (map[field.name] == null) {

                    if (fieldFromDataModel.format != null) {
                        val format = fieldFromDataModel.format as String

                        if (format.startsWith("/")) {

                            val formatPath = fileHelper.pathHelper.getCustomFormatterPath(format)
                            getManifestJSONContent(formatPath)?.let {

                                val fieldMapping = getFieldMapping(it, format)
                                Log.d("fieldMapping :  $fieldMapping")

                                if (fieldMapping.isValidFormatter()) {
                                    // Saving any permission for kotlin custom formatters
                                    fieldMapping.capabilities.forEach { permissionName ->
                                        permissionFillerList.add(getTemplatePermissionFiller(permissionName))
                                    }

                                    extractFormatter(fieldMapping, formatPath, format)
                                    map[field.name] = fieldMapping
                                }
                            }
                        }
                    }
                }
                customFormattersFields[form.dataModel.name.tableNameAdjustment()] = map
            }
        }
    }

    private fun extractFormatter(fieldMapping: FieldMapping, formatPath: String, format: String) {

        if (fieldMapping.isImageNamed()) {
            val imageMap = mutableMapOf<String, Pair<String, String>>()

            // choiceList can be Map<String, String> (JSONObject in app_info.json)
            // or a List<String> (JSONArray in app_info.json)
            when (fieldMapping.choiceList) {
                is Map<*, *> -> {
                    fieldMapping.choiceList.values.forEach eachImageName@ { imageName ->
                        if (imageName !is String) return@eachImageName
                        if (imageName.contains(".") && imageExistsInFormatter(formatPath, imageName)) {
                            val darkModeExists = imageExistsInFormatterInDarkMode(formatPath, imageName)
                            imageMap[imageName] = getResourceName(format, imageName, darkModeExists)
                        }
                    }
                }
                is List<*> -> {
                    fieldMapping.choiceList.forEach eachImageName@ { imageName ->
                        if (imageName !is String) return@eachImageName
                        if (imageName.contains(".") && imageExistsInFormatter(formatPath, imageName)) {
                            val darkModeExists = imageExistsInFormatterInDarkMode(formatPath, imageName)
                            imageMap[imageName] = getResourceName(format, imageName, darkModeExists)
                        }
                    }
                }
            }
            customFormattersImagesMap[format] = imageMap
        }
    }

    private fun getResourceName(format: String, imageName: String, darkModeExists: Boolean): Pair<String, String> {
        val correctedFormatName = format
            .removePrefix("/")
            .substringBefore(".") // removes extension for .zip file
            .toLowerCase()
            .replace("[^a-z0-9]+".toRegex(), "_")

        Log.d("getResourceName, correctedFormatName : $correctedFormatName")

        val correctedImageName = correctIconPath(imageName)

        return if (darkModeExists)
            Pair("${correctedFormatName}_${correctedImageName}", "${correctedFormatName}_${correctedImageName}_dark")
        else
            Pair("${correctedFormatName}_${correctedImageName}", "")
    }

    private fun getImageSize(form: Form, fieldName: String, type: String): Int {
        customFormattersFields[form.dataModel.name.tableNameAdjustment()]?.get(fieldName)?.let{ fieldMapping ->
            return when (type) {
                "width" -> fieldMapping.imageWidth ?: 0
                "height" -> fieldMapping.imageHeight ?: 0
                else -> 0
            }
        }
        return 0
    }

    private fun isImageNamedBinding(form: Form, fieldName: String): Boolean {
        customFormattersFields[form.dataModel.name.tableNameAdjustment()]?.get(fieldName)?.let{ fieldMapping ->
            return fieldMapping.isImageNamed()
        }
        return false
    }

    private fun getHexStringColor(red: Int, green: Int, blue: Int): String {
        var redHexString = toHexString(red)
        var greenHexString = toHexString(green)
        var blueHexString = toHexString(blue)

        if (redHexString.length == 1) redHexString = "0$redHexString"
        if (greenHexString.length == 1) greenHexString = "0$greenHexString"
        if (blueHexString.length == 1) blueHexString = "0$blueHexString"

        return "#$redHexString$greenHexString$blueHexString"
    }

    private fun fillLightColorLightTheme(seedColor: Int) {
        val hexStringPrimaryColor = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.33)).toUpperCase()
        data["theme_light_primary"] = hexStringPrimaryColor
        val primaryColor: Int = Color.parseColor(hexStringPrimaryColor)
        data["theme_light_onPrimary"] = "@android:color/white"
        data["theme_light_primaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.93)).toUpperCase()
        data["theme_light_onPrimaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.12)).toUpperCase()

        val hexStringSecondaryColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.19, 0.37)).toUpperCase()
        data["theme_light_secondary"] = hexStringSecondaryColor
        val secondaryColor: Int = Color.parseColor(hexStringSecondaryColor)
        data["theme_light_onSecondary"] = "@android:color/white"
        data["theme_light_secondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 1.0, 0.93)).toUpperCase()
        data["theme_light_onSecondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 1.0, 0.12)).toUpperCase()

        val blendColors = ColorUtils.blendARGB(primaryColor, secondaryColor, 0.5f)
        val analogousRightColor = analogousRightColor(blendColors, 50.0f)
        val hexStringTertiaryColor = "#" + toHexString(manipulate(analogousRightColor, l = 0.40)).toUpperCase()
        data["theme_light_tertiary"] = hexStringTertiaryColor
        val tertiaryColor: Int = Color.parseColor(hexStringTertiaryColor)
        data["theme_light_onTertiary"] = "@android:color/white"
        data["theme_light_tertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 1.0, 0.93)).toUpperCase()
        data["theme_light_onTertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 1.0, 0.12)).toUpperCase()

        val hexStringErrorColor = "#BA1A1A"
        data["theme_light_error"] = hexStringErrorColor
        val errorColor: Int = Color.parseColor(hexStringErrorColor)
        data["theme_light_onError"] = "@android:color/white"
        data["theme_light_errorContainer"] = "#" + toHexString(manipulate(errorColor, 1.0f, 1.0, 0.93)).toUpperCase()
        data["theme_light_onErrorContainer"] = "#" + toHexString(manipulate(errorColor, 1.0f, 1.0, 0.12)).toUpperCase()

        data["theme_light_background"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.99)).toUpperCase()
        data["theme_light_onBackground"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        val hexStringSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.99)).toUpperCase()
        data["theme_light_surface"] = hexStringSurfaceColor
        val hexStringOnSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_light_onSurface"] = hexStringOnSurfaceColor
        data["theme_light_surfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.45, 0.91)).toUpperCase()
        data["theme_light_onSurfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.29)).toUpperCase()
        data["theme_light_outline"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.07, 0.48)).toUpperCase()

        val onSurfaceColor: Int = Color.parseColor(hexStringOnSurfaceColor)
        data["theme_light_inverseOnSurface"] = "#" + toHexString(oppositeColor(onSurfaceColor)).toUpperCase()
        val surfaceColor: Int = Color.parseColor(hexStringSurfaceColor)
        data["theme_light_inverseSurface"] = "#" + toHexString(oppositeColor(surfaceColor)).toUpperCase()
        data["theme_light_inversePrimary"] = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.85)).toUpperCase()

        data["theme_light_shadow"] = "@android:color/black"
        data["theme_light_surfaceTint"] = hexStringPrimaryColor
        data["theme_light_surfaceTintColor"] = hexStringPrimaryColor
    }

    private fun fillLightColorDarkTheme(seedColor: Int) {
        val hexStringPrimaryColor = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.68)).toUpperCase()
        data["theme_dark_primary"] = hexStringPrimaryColor
        val primaryColor: Int = Color.parseColor(hexStringPrimaryColor)
        data["theme_dark_onPrimary"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.2)).toUpperCase()
        data["theme_dark_primaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.27)).toUpperCase()
        data["theme_dark_onPrimaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.93)).toUpperCase()

        val hexStringSecondaryColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.35, 0.77)).toUpperCase()
        data["theme_dark_secondary"] = hexStringSecondaryColor
        val secondaryColor: Int = Color.parseColor(hexStringSecondaryColor)
        data["theme_dark_onSecondary"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.20, 0.21)).toUpperCase()
        data["theme_dark_secondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.13, 0.31)).toUpperCase()
        data["theme_dark_onSecondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.65, 0.92)).toUpperCase()

        val blendColors = ColorUtils.blendARGB(primaryColor, secondaryColor, 0.5f)
        val analogousRightColor = analogousRightColor(blendColors, 50.0f)
        val hexStringTertiaryColor = "#" + toHexString(manipulate(analogousRightColor, l = 0.80)).toUpperCase()
        data["theme_dark_tertiary"] = hexStringTertiaryColor
        val tertiaryColor: Int = Color.parseColor(hexStringTertiaryColor)
        data["theme_dark_onTertiary"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.3, 0.21)).toUpperCase()
        data["theme_dark_tertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.23, 0.3)).toUpperCase()
        data["theme_dark_onTertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 1.0, 0.92)).toUpperCase()

        val hexStringErrorColor = "#FFB4AB"
        data["theme_dark_error"] = hexStringErrorColor
        data["theme_dark_onError"] = "#690005"
        data["theme_dark_errorContainer"] = "#93000A"
        data["theme_dark_onErrorContainer"] = "#FFDAD6"

        data["theme_dark_background"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_onBackground"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        val hexStringSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_surface"] = hexStringSurfaceColor
        val hexStringOnSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        data["theme_dark_onSurface"] = hexStringOnSurfaceColor
        data["theme_dark_surfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.29)).toUpperCase()
        data["theme_dark_onSurfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.79)).toUpperCase()
        data["theme_dark_outline"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.05, 0.58)).toUpperCase()

        data["theme_dark_inverseOnSurface"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_inverseSurface"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        data["theme_dark_inversePrimary"] = "#" + toHexString(manipulate(seedColor, 1.0f, 0.39, 0.42)).toUpperCase()

        data["theme_dark_shadow"] = "@android:color/black"
        data["theme_dark_surfaceTint"] = hexStringPrimaryColor
        data["theme_dark_surfaceTintColor"] = hexStringPrimaryColor
    }

    private fun fillDarkColorLightTheme(seedColor: Int) {
        val hexStringPrimaryColor = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.33)).toUpperCase()
        data["theme_light_primary"] = hexStringPrimaryColor
        val primaryColor: Int = Color.parseColor(hexStringPrimaryColor)
        data["theme_light_onPrimary"] = "@android:color/white"
        data["theme_light_primaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.87)).toUpperCase()
        data["theme_light_onPrimaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.09)).toUpperCase()

        val hexStringSecondaryColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.19, 0.37)).toUpperCase()
        data["theme_light_secondary"] = hexStringSecondaryColor
        val secondaryColor: Int = Color.parseColor(hexStringSecondaryColor)
        data["theme_light_onSecondary"] = "@android:color/white"
        data["theme_light_secondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.65, 0.87)).toUpperCase()
        data["theme_light_onSecondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.56, 0.09)).toUpperCase()

        val blendColors = ColorUtils.blendARGB(primaryColor, secondaryColor, 0.5f)
        val analogousRightColor = analogousRightColor(blendColors, 50.0f)
        val hexStringTertiaryColor = "#" + toHexString(manipulate(analogousRightColor, l = 0.40)).toUpperCase()
        data["theme_light_tertiary"] = hexStringTertiaryColor
        val tertiaryColor: Int = Color.parseColor(hexStringTertiaryColor)
        data["theme_light_onTertiary"] = "@android:color/white"
        data["theme_light_tertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.91, 0.87)).toUpperCase()
        data["theme_light_onTertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 1.0, 0.08)).toUpperCase()

        val hexStringErrorColor = "#BA1A1A"
        data["theme_light_error"] = hexStringErrorColor
        val errorColor: Int = Color.parseColor(hexStringErrorColor)
        data["theme_light_onError"] = "@android:color/white"
        data["theme_light_errorContainer"] = "#" + toHexString(manipulate(errorColor, 1.0f, 1.0, 0.93)).toUpperCase()
        data["theme_light_onErrorContainer"] = "#" + toHexString(manipulate(errorColor, 1.0f, 1.0, 0.12)).toUpperCase()

        data["theme_light_background"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.99)).toUpperCase()
        data["theme_light_onBackground"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        val hexStringSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.99)).toUpperCase()
        data["theme_light_surface"] = hexStringSurfaceColor
        val hexStringOnSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_light_onSurface"] = hexStringOnSurfaceColor
        data["theme_light_surfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.45, 0.91)).toUpperCase()
        data["theme_light_onSurfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.29)).toUpperCase()
        data["theme_light_outline"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.07, 0.48)).toUpperCase()

        val onSurfaceColor: Int = Color.parseColor(hexStringOnSurfaceColor)
        data["theme_light_inverseOnSurface"] = "#" + toHexString(oppositeColor(onSurfaceColor)).toUpperCase()
        val surfaceColor: Int = Color.parseColor(hexStringSurfaceColor)
        data["theme_light_inverseSurface"] = "#" + toHexString(oppositeColor(surfaceColor)).toUpperCase()
        data["theme_light_inversePrimary"] = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.73)).toUpperCase()

        data["theme_light_shadow"] = "@android:color/black"
        data["theme_light_surfaceTint"] = hexStringPrimaryColor
        data["theme_light_surfaceTintColor"] = hexStringPrimaryColor
    }

    private fun fillDarkColorDarkTheme(seedColor: Int) {
        val hexStringPrimaryColor = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.68)).toUpperCase()
        data["theme_dark_primary"] = hexStringPrimaryColor
        val primaryColor: Int = Color.parseColor(hexStringPrimaryColor)
        data["theme_dark_onPrimary"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.15)).toUpperCase()
        data["theme_dark_primaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.21)).toUpperCase()
        data["theme_dark_onPrimaryContainer"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 1.0, 0.87)).toUpperCase()

        val hexStringSecondaryColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.35, 0.77)).toUpperCase()
        data["theme_dark_secondary"] = hexStringSecondaryColor
        val secondaryColor: Int = Color.parseColor(hexStringSecondaryColor)
        data["theme_dark_onSecondary"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.26, 0.17)).toUpperCase()
        data["theme_dark_secondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.17, 0.26)).toUpperCase()
        data["theme_dark_onSecondaryContainer"] = "#" + toHexString(manipulate(secondaryColor, 1.0f, 0.65, 0.87)).toUpperCase()

        val blendColors = ColorUtils.blendARGB(primaryColor, secondaryColor, 0.5f)
        val analogousRightColor = analogousRightColor(blendColors, 50.0f)
        val hexStringTertiaryColor = "#" + toHexString(manipulate(analogousRightColor, l = 0.80)).toUpperCase()
        data["theme_dark_tertiary"] = hexStringTertiaryColor
        val tertiaryColor: Int = Color.parseColor(hexStringTertiaryColor)
        data["theme_dark_onTertiary"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.55, 0.15)).toUpperCase()
        data["theme_dark_tertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.35, 0.25)).toUpperCase()
        data["theme_dark_onTertiaryContainer"] = "#" + toHexString(manipulate(tertiaryColor, 1.0f, 0.91, 0.87)).toUpperCase()

        val hexStringErrorColor = "#FFB4AB"
        data["theme_dark_error"] = hexStringErrorColor
        data["theme_dark_onError"] = "#690005"
        data["theme_dark_errorContainer"] = "#93000A"
        data["theme_dark_onErrorContainer"] = "#FFDAD6"

        data["theme_dark_background"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_onBackground"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        val hexStringSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_surface"] = hexStringSurfaceColor
        val hexStringOnSurfaceColor = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        data["theme_dark_onSurface"] = hexStringOnSurfaceColor
        data["theme_dark_surfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.29)).toUpperCase()
        data["theme_dark_onSurfaceVariant"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.79)).toUpperCase()
        data["theme_dark_outline"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.05, 0.58)).toUpperCase()

        data["theme_dark_inverseOnSurface"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.1, 0.11)).toUpperCase()
        data["theme_dark_inverseSurface"] = "#" + toHexString(manipulate(primaryColor, 1.0f, 0.09, 0.89)).toUpperCase()
        data["theme_dark_inversePrimary"] = "#" + toHexString(manipulate(seedColor, 1.0f, 1.0, 0.27)).toUpperCase()

        data["theme_dark_shadow"] = "@android:color/black"
        data["theme_dark_surfaceTint"] = hexStringPrimaryColor
        data["theme_dark_surfaceTintColor"] = hexStringPrimaryColor
    }
}
