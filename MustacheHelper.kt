import DefaultValues.DEFAULT_ADDRESS
import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_REMOTE_URL
import DefaultValues.LAYOUT_FILE
import FileHelperConstants.APP_INFO_FILENAME
import FileHelperConstants.CUSTOM_FORMATTERS_FILENAME
import FileHelperConstants.DS_STORE
import FileHelperConstants.QUERIES_FILENAME
import FileHelperConstants.SEARCHABLE_FIELDS_FILENAME
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
import MustacheConstants.CUSTOM_FORMATTER_IMAGES
import MustacheConstants.DATE_DAY
import MustacheConstants.DATE_MONTH
import MustacheConstants.DATE_YEAR
import MustacheConstants.ENTITY_CLASSES
import MustacheConstants.FIELDS
import MustacheConstants.FIRST_FIELD
import MustacheConstants.FORM_FIELDS
import MustacheConstants.HAS_ANY_MANY_TO_ONE_RELATION
import MustacheConstants.HAS_ANY_ONE_TO_MANY_RELATION
import MustacheConstants.HAS_ANY_ONE_TO_MANY_RELATION_FOR_LAYOUT
import MustacheConstants.HAS_ANY_RELATIONS_MANY_TO_ONE_FOR_DETAIL
import MustacheConstants.HAS_ANY_RELATIONS_MANY_TO_ONE_FOR_LIST
import MustacheConstants.HAS_ANY_RELATIONS_ONE_TO_MANY_FOR_DETAIL
import MustacheConstants.HAS_ANY_RELATIONS_ONE_TO_MANY_FOR_LIST
import MustacheConstants.HAS_CUSTOM_FORMATTER_IMAGES
import MustacheConstants.HAS_RELATION
import MustacheConstants.TABLE_HAS_ANY_MANY_TO_ONE_RELATION
import MustacheConstants.HAS_REMOTE_ADDRESS
import MustacheConstants.PACKAGE
import MustacheConstants.RELATIONS_MANY_TO_ONE
import MustacheConstants.RELATIONS_IMPORT
import MustacheConstants.RELATIONS_MANY_TO_ONE_FOR_DETAIL
import MustacheConstants.RELATIONS_MANY_TO_ONE_FOR_LIST
import MustacheConstants.RELATIONS_ONE_TO_MANY
import MustacheConstants.RELATIONS_ONE_TO_MANY_FOR_DETAIL
import MustacheConstants.RELATIONS_ONE_TO_MANY_FOR_LIST
import MustacheConstants.RELATION_NAME
import MustacheConstants.RELATION_NAME_CAP
import MustacheConstants.RELATION_SAME_TYPE
import MustacheConstants.RELATION_SOURCE
import MustacheConstants.RELATION_TARGET
import MustacheConstants.REMOTE_ADDRESS
import MustacheConstants.TABLENAME
import MustacheConstants.TABLENAMES
import MustacheConstants.TABLENAMES_LAYOUT
import MustacheConstants.TABLENAMES_LOWERCASE
import MustacheConstants.TABLENAMES_NAVIGATION
import MustacheConstants.TABLENAMES_WITHOUT_MANY_TO_ONE_RELATION
import MustacheConstants.TABLENAMES_LAYOUT_RELATIONS
import MustacheConstants.TABLENAMES_NAVIGATION_FOR_NAVBAR
import MustacheConstants.TABLENAMES_WITH_MANY_TO_ONE_RELATIONS
import MustacheConstants.TABLENAME_CAMELCASE
import MustacheConstants.TABLENAME_LOWERCASE
import MustacheConstants.TABLENAME_ORIGINAL
import MustacheConstants.TABLE_HAS_ANY_RELATION
import MustacheConstants.TABLE_HAS_DATE_FIELD
import MustacheConstants.TABLE_HAS_ONE_TO_MANY_FIELD
import MustacheConstants.TABLE_HAS_TIME_FIELD
import MustacheConstants.THEME_COLOR_ON_PRIMARY
import MustacheConstants.THEME_COLOR_PRIMARY
import MustacheConstants.THEME_COLOR_PRIMARY_DARKER
import MustacheConstants.THEME_COLOR_PRIMARY_LIGHTER
import MustacheConstants.TYPES_AND_TABLES
import PathHelperConstants.TEMPLATE_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_DAO_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_ENTITY_PLACEHOLDER
import ProjectEditorConstants.HAS_ACTIONS_KEY
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

    private lateinit var compiler: Mustache.Compiler
    private lateinit var template: Template

    private val tableNamesForNavigation = mutableListOf<TemplateLayoutFiller>()
    private val tableNamesForNavigationForNavBar = mutableListOf<TemplateLayoutFiller>()
    private val tableNamesForLayoutType = mutableListOf<TemplateLayoutTypeFiller>()
    private var tableNames = mutableListOf<TemplateTableFiller>()
    private var tableNamesLowercase = mutableListOf<TemplateLayoutFiller>()
    private var relationsManyToOne = mutableListOf<TemplateRelationFiller>()
    private var relationsOneToMany = mutableListOf<TemplateRelationFiller>()
    private var customFormatterImages = mutableListOf<TemplateFormatterFiller>()
    private var tableNamesWithManyToOneRelation = mutableListOf<TemplateTableWithRelationFiller>()
    private val tableNamesWithoutManyToOneRelation = mutableListOf<TemplateTableFiller>()

    private val oneToManyRelationFillerForEachListLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val manyToOneRelationFillerForEachListLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val oneToManyRelationFillerForEachDetailLayout = mutableListOf<TemplateRelationFillerForEachLayout>()
    private val manyToOneRelationFillerForEachDetailLayout = mutableListOf<TemplateRelationFillerForEachLayout>()

    // <formatName, <imageName, <resourceName, darkModeResourceName>>
    private lateinit var customFormattersImagesMap: Map<String, Map<String, Pair<String, String>>>
    // <tableName, <fieldName, fieldMapping>>
    private val customFormattersFields: Map<String, Map<String, FieldMapping>> = getCustomFormatterFields()

    init {
        Log.plantTree(this::class.java.canonicalName)
        data[COMPANY_HEADER] = fileHelper.pathHelper.companyWithCaps
        data[AUTHOR] = projectEditor.findJsonString("author") ?: DEFAULT_AUTHOR
        data[DATE_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        data[DATE_MONTH] = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        data[DATE_YEAR] = Calendar.getInstance().get(Calendar.YEAR).toString()
        data[PACKAGE] = fileHelper.pathHelper.pkg
        data[APP_NAME_WITH_CAPS] = fileHelper.pathHelper.appNameWithCaps

        // for network_security_config.xml
        // whitelist production host address if defined, else, server host address, else localhost
        var remoteAddress = projectEditor.findJsonString("productionUrl")
        Log.d("remoteAddress = $remoteAddress")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = projectEditor.findJsonString("remoteUrl")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = DEFAULT_REMOTE_URL
        val cleanRemoteAddress = remoteAddress.removePrefix("https://").removePrefix("http://").split(":")[0]
        if (cleanRemoteAddress != DEFAULT_ADDRESS) {
            data[HAS_REMOTE_ADDRESS] = true
            data[REMOTE_ADDRESS] = cleanRemoteAddress
            Log.d("data[REMOTE_ADDRESS] = ${data[REMOTE_ADDRESS]}")
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

        data[COLORS_DEFINED] = false
        data[THEME_COLOR_PRIMARY] = "@color/cyan_900"
        data[THEME_COLOR_PRIMARY_DARKER] = "@color/cyan_dark"
        data[THEME_COLOR_PRIMARY_LIGHTER] = "@color/cyan_light"
        data[THEME_COLOR_ON_PRIMARY] = "@android:color/white"

        projectEditor.findJsonString("dominantColor")?.let {  // "rgb(0,75,145)"

            Log.i("dominantColor = $it")

            val rgbString = it.removePrefix("rgb(").removeSuffix(")") // 0,75,145
            val red = rgbString.split(",").getOrNull(0)?.toIntOrNull()
            val green = rgbString.split(",").getOrNull(1)?.toIntOrNull()
            val blue = rgbString.split(",").getOrNull(2)?.toIntOrNull()

            if (red != null && green != null && blue != null) {

                val hexStringBackgroundColor = getHexStringColor(red, green, blue)

                data[COLORS_DEFINED] = true
                data[COLOR_PRIMARY_NEUTRAL] = hexStringBackgroundColor

                Log.i("backgroundColor = $hexStringBackgroundColor")

                val backgroundColor: Int = Color.parseColor(hexStringBackgroundColor)
                data[COLOR_PRIMARY_DARKER] =
                    "#" + toHexString(manipulateColor(backgroundColor, 0.8f)).toUpperCase() // darker +
                data[COLOR_PRIMARY_DARKER_PLUS] =
                    "#" + toHexString(manipulateColor(backgroundColor, 0.6f)).toUpperCase() // darker ++
                data[COLOR_PRIMARY_DARKER_PLUS_PLUS] =
                    "#" + toHexString(manipulateColor(backgroundColor, 0.4f)).toUpperCase() // darker +++
                data[COLOR_PRIMARY_LIGHTER] =
                    "#" + toHexString(manipulateColor(backgroundColor, 1.2f)).toUpperCase() // lighter +
                data[COLOR_PRIMARY_LIGHTER_PLUS] =
                    "#" + toHexString(manipulateColor(backgroundColor, 1.4f)).toUpperCase() // lighter ++
                data[COLOR_PRIMARY_LIGHTER_PLUS_PLUS] =
                    "#" + toHexString(manipulateColor(backgroundColor, 1.6f)).toUpperCase() // lighter +++

                data[THEME_COLOR_PRIMARY] = "@color/primary_neutral"
                data[THEME_COLOR_PRIMARY_DARKER] = "@color/primary_darker_3"
                data[THEME_COLOR_PRIMARY_LIGHTER] = "@color/primary_lighter_3"

                val contrast = 1 - (((0.299 * red) + (0.587 * green) + (0.114 * blue)) / 255)
                data[THEME_COLOR_ON_PRIMARY] = if (contrast < 0.5) "@android:color/black" else "@android:color/white"
            }
        }

        Log.i("data[THEME_COLOR_ON_PRIMARY] = ${data[THEME_COLOR_ON_PRIMARY]}")


        var entityClassesString = ""

        val layoutRelationList = mutableListOf<TemplateRelationFiller>()

        projectEditor.dataModelList.forEach { dataModel ->

            dataModel.relationList?.forEach { relation ->
                val filler = relation.getTemplateRelationFiller()
                if (relation.relationType == RelationType.MANY_TO_ONE) {
                    relationsManyToOne.add(filler)
                    // Check for sub 1-N relations
                    val subTemplateRelationFillerList = relation.checkSubRelations(projectEditor.dataModelList)
                    relationsOneToMany.addAll(subTemplateRelationFillerList)
                } else {
                    relationsOneToMany.add(filler)
                }
            }

            tableNames.add(dataModel.getTemplateTableFiller(projectEditor.dataModelList))

            tableNamesLowercase.add(dataModel.getTemplateLayoutFiller())

            entityClassesString += "${dataModel.name.tableNameAdjustment()}::class, "
            Log.d("ProjectDataModelList  ${entityClassesString}:: ${dataModel.name.tableNameAdjustment()}")
        }

        tableNames.forEach { tableName ->
            if (!relationsManyToOne.map { it.relation_source.tableNameAdjustment() }.contains(tableName.name))
                tableNamesWithoutManyToOneRelation.add(tableName)
        }


        data[TABLENAMES] = tableNames
        data[TABLENAMES_LOWERCASE] = tableNamesLowercase

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany
        data[HAS_ANY_MANY_TO_ONE_RELATION] = relationsManyToOne.isNotEmpty()
        data[HAS_ANY_ONE_TO_MANY_RELATION] = relationsOneToMany.isNotEmpty()

        val inverseRelationsOneToMany = relationsOneToMany.getInverseRelationsOneToMany()
        data[RELATIONS_IMPORT] = relationsManyToOne.plus(inverseRelationsOneToMany).distinctBy { it.relation_source to it.relation_target /*to it.relation_name*/ }
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
        projectEditor.dataModelList.forEach { dataModel ->
            if (projectEditor.navigationTableList.contains(dataModel.id)) {
                if (navigationTableCounter <= 3) {

                    navigationTableCounter++
                    Log.w("Adding [${dataModel.name}] in navigation table list for navbar")
                    tableNamesForNavigationForNavBar.add(dataModel.getTemplateLayoutFillerForNavigation())
                } else {
                    Log.w("Not adding [${dataModel.name}] in navigation table list for navbar")
                }
            }
            Log.w("Adding [${dataModel.name}] in navigation table list")

            tableNamesForNavigation.add(dataModel.getTemplateLayoutFillerForNavigation())


            dataModel.relationList?.forEach { relation ->
                layoutRelationList.add(relation.getTemplateRelationFiller())
                // Check for sub 1-N relations
                if (relation.relationType == RelationType.MANY_TO_ONE) {
                    val subTemplateRelationFillerList = relation.checkSubRelations(projectEditor.dataModelList)
                    layoutRelationList.addAll(subTemplateRelationFillerList)
                }
            }
        }
        data[TABLENAMES_NAVIGATION] = tableNamesForNavigation
        data[TABLENAMES_NAVIGATION_FOR_NAVBAR] = tableNamesForNavigationForNavBar
        data[TABLENAMES_LAYOUT_RELATIONS] = layoutRelationList
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
        Log.d("TABLENAMES", "${data[TABLENAMES]}")

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


        projectEditor.dataModelList.forEach { dataModel ->
            if (dataModel.relationList?.firstOrNull { it.relationType == RelationType.MANY_TO_ONE } != null) {
                tableNamesWithManyToOneRelation.add(dataModel.getTemplateTableWithRelationFiller())
            }
        }

        data[TABLENAMES_WITH_MANY_TO_ONE_RELATIONS] = tableNamesWithManyToOneRelation
    }

    /**
     * TEMPLATING
     */
    fun processTemplates() {
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
                processFile(currentFile)
            }
    }

    private fun processFile(currentFile: File) {
        Log.d("Processed file", "$currentFile")

        template = compiler.compile("{{>${currentFile.name}}}")

        val newFilePath = fileHelper.pathHelper.getPath(currentFile.absolutePath.replaceXmlTxtSuffix())

        relationsManyToOne.clear()
        relationsOneToMany.clear()

        projectEditor.dataModelList.forEach { dataModel ->
            dataModel.relationList?.forEach { relation ->
                val filler = relation.getTemplateRelationFiller()
                if (relation.relationType == RelationType.MANY_TO_ONE) {
                    relationsManyToOne.add(filler)
                    // Check for sub 1-N relations
                    val subTemplateRelationFillerList = relation.checkSubRelations(projectEditor.dataModelList)
                    relationsOneToMany.addAll(subTemplateRelationFillerList)
                } else {
                    relationsOneToMany.add(filler)
                }
            }
        }

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany

        val inverseRelationsOneToMany = relationsOneToMany.getInverseRelationsOneToMany()
        data[RELATIONS_IMPORT] = relationsManyToOne.plus(inverseRelationsOneToMany).distinctBy { it.relation_source to it.relation_target to it.relation_name }

        data[RELATIONS_ONE_TO_MANY_FOR_LIST] = oneToManyRelationFillerForEachListLayout
        data[RELATIONS_MANY_TO_ONE_FOR_LIST] = manyToOneRelationFillerForEachListLayout
        data[RELATIONS_ONE_TO_MANY_FOR_DETAIL] = oneToManyRelationFillerForEachDetailLayout
        data[RELATIONS_MANY_TO_ONE_FOR_DETAIL] = manyToOneRelationFillerForEachDetailLayout
        data[HAS_ANY_RELATIONS_ONE_TO_MANY_FOR_LIST] = oneToManyRelationFillerForEachListLayout.isNotEmpty()
        data[HAS_ANY_RELATIONS_MANY_TO_ONE_FOR_LIST] = manyToOneRelationFillerForEachListLayout.isNotEmpty()
        data[HAS_ANY_RELATIONS_ONE_TO_MANY_FOR_DETAIL] = oneToManyRelationFillerForEachDetailLayout.isNotEmpty()
        data[HAS_ANY_RELATIONS_MANY_TO_ONE_FOR_DETAIL] = manyToOneRelationFillerForEachDetailLayout.isNotEmpty()

        if (currentFile.isWithTemplateName()) {

            for (tableName in tableNames) { // file will be duplicated

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

        } else if (currentFile.isWithRelationDaoTemplateName()) {

            projectEditor.dataModelList.forEach { dataModel ->
                dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                    ?.forEach { relation ->

                        data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                        data[RELATION_TARGET] = relation.target.tableNameAdjustment()
                        data[RELATION_NAME_CAP] = relation.name.fieldAdjustment().capitalize()

                        val replacedPath = newFilePath.replace(TEMPLATE_RELATION_DAO_PLACEHOLDER,
                            "${relation.source.tableNameAdjustment()}Has${relation.target.tableNameAdjustment()}With${relation.name.fieldAdjustment().capitalize()}Key")

                        applyTemplate(replacedPath)

                        //cleaning
                        data.remove(RELATION_SOURCE)
                        data.remove(RELATION_TARGET)
                        data.remove(RELATION_NAME_CAP)
                    }
            }

        } else if (currentFile.isWithRelationEntityTemplateName()) {

            projectEditor.dataModelList.forEach { dataModel ->
                dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                    ?.forEach { relation ->

                        data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                        data[RELATION_TARGET] = relation.target.tableNameAdjustment()
                        data[RELATION_NAME] = relation.name.fieldAdjustment()
                        data[RELATION_NAME_CAP] = relation.name.fieldAdjustment().capitalize()
                        data[RELATION_SAME_TYPE] = relation.source == relation.target

                        val replacedPath = newFilePath.replace(TEMPLATE_RELATION_ENTITY_PLACEHOLDER,
                            "${relation.source.tableNameAdjustment()}And${relation.target.tableNameAdjustment()}With${relation.name.fieldAdjustment().capitalize()}Key")

                        applyTemplate(replacedPath)

                        //cleaning
                        data.remove(RELATION_NAME)
                        data.remove(RELATION_NAME_CAP)
                        data.remove(RELATION_SOURCE)
                        data.remove(RELATION_TARGET)
                        data.remove(RELATION_SAME_TYPE)
                    }
            }

        } else {
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
            for (field in fields) {
                field.fieldTypeString?.let { fieldTypeString ->
                    fieldList.add(field.getTemplateFieldFiller(fieldTypeString))

                } ?: kotlin.run {
                    throw Exception("An error occurred while parsing the fieldType of field : $field")
                }
            }

            data[FIELDS] = fieldList
            data[TABLE_HAS_DATE_FIELD] = fieldList.map { it.fieldTypeString }.contains("Date")
            data[TABLE_HAS_TIME_FIELD] = fieldList.map { it.fieldTypeString }.contains("Time")
            data[TABLE_HAS_ONE_TO_MANY_FIELD] = fieldList.map { it.fieldTypeString }.firstOrNull { it.startsWith("Entities<") } != null
            Log.d("FIELDS", "${data[FIELDS]}")
            val firstField: String = fieldList.firstOrNull()?.name ?: ""
            data[FIRST_FIELD] = firstField
        }

        relationsManyToOne.clear()
        relationsOneToMany.clear()
        data.remove(RELATIONS_IMPORT)
        data[TABLE_HAS_ANY_MANY_TO_ONE_RELATION] = false
        data[TABLE_HAS_ANY_RELATION] = false

        projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.relationList?.forEach { relation ->
            val filler = relation.getTemplateRelationFiller()
            if (relation.relationType == RelationType.MANY_TO_ONE) {
                relationsManyToOne.add(filler)
                // Check for sub 1-N relations
                val subTemplateRelationFillerList = relation.checkSubRelations(projectEditor.dataModelList)
                relationsOneToMany.addAll(subTemplateRelationFillerList)
            } else {
                relationsOneToMany.add(filler)
            }
        }

        data[RELATIONS_MANY_TO_ONE] = relationsManyToOne
        data[RELATIONS_ONE_TO_MANY] = relationsOneToMany
        if (relationsManyToOne.size > 0)
            data[TABLE_HAS_ANY_MANY_TO_ONE_RELATION] = true
        if (relationsManyToOne.size > 0 || relationsOneToMany.size >0)
            data[TABLE_HAS_ANY_RELATION] = true

        val inverseRelationsOneToMany = relationsOneToMany.getInverseRelationsOneToMany()
        data[RELATIONS_IMPORT] = relationsManyToOne.plus(inverseRelationsOneToMany).distinctBy { it.relation_source to it.relation_target to it.relation_name }
    }

    fun applyListFormTemplate() {
        projectEditor.listFormList.forEach { listForm ->

            Log.d("MustacheHelper : listform.name = ${listForm.name}")
            Log.d("MustacheHelper : listform.datamodel.name = ${listForm.dataModel.name}")
            Log.d("MustacheHelper : listform.datamodel.name.tableNameAdjustment() = ${listForm.dataModel.name.tableNameAdjustment()}")
            Log.d("MustacheHelper : listform.fields size = ${listForm.fields?.size}")

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
                                projectEditor.dataModelList.find { it.name.tableNameAdjustment() == listForm.dataModel.name.tableNameAdjustment() }?.relationList?.forEach { relation ->
                                    Log.d("RRR relation = $relation")
                                    val filler = relation.getTemplateRelationFiller()
                                    if (relation.relationType == RelationType.MANY_TO_ONE) {
                                        relationsManyToOne.add(filler)
                                        // Check for sub 1-N relations
                                        val subTemplateRelationFillerList = relation.checkSubRelations(projectEditor.dataModelList)
                                        relationsOneToMany.addAll(subTemplateRelationFillerList)
                                    } else {
                                        relationsOneToMany.add(filler)
                                    }
                                }
                                data[RELATIONS_MANY_TO_ONE] = relationsManyToOne
                                data[RELATIONS_ONE_TO_MANY] = relationsOneToMany
                                data[HAS_ANY_ONE_TO_MANY_RELATION_FOR_LAYOUT] = relationsOneToMany.isNotEmpty()

                                var wholeFormHasIcons = false

                                projectEditor.dataModelList.find { it.id == listForm.dataModel.id }?.fields?.forEach {
                                    Log.d("LIST ${listForm.name} / field = $it")
                                    if (!it.icon.isNullOrEmpty())
                                        wholeFormHasIcons = true
                                }
                                projectEditor.dataModelList.find { it.id  == listForm.dataModel.id }?.relationList?.forEach { relation ->
                                    relation.subFields.forEach {
                                        Log.d("LIST ${listForm.name} / related field = $it")
                                        if (!it.icon.isNullOrEmpty())
                                            wholeFormHasIcons = true
                                    }
                                }

                                Log.d("ListForm - ${listForm.dataModel.name} - wholeFormHasIcons = $wholeFormHasIcons")

                                var i = 0
                                listForm.fields?.forEach { field -> // Could also iterate over specificFieldsCount as Detail form
                                    i++

                                    if (fileHelper.pathHelper.isDefaultTemplateListFormPath(formPath) && field.isImage()) { // is image in default template
                                        resetIndexedEntries(i)
                                    } else { // not a relation
                                        fillIndexedFormData(i, field, FormType.LIST, listForm, wholeFormHasIcons)
                                        if (isRelationWithFixes(projectEditor.dataModelList, listForm, field)) {

                                            Log.d("XXX : field = $field")
                                            Log.d("XXX : form = $listForm")
                                            fillRelationFillerForEachLayout(field, listForm, FormType.LIST, i)
                                        }
                                    }
                                }

                                val newFilePath =
                                    fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name.tableNameAdjustment())
                                applyTemplate(newFilePath)

                                // cleaning data for other templates
                                for (j in 1 until i + 1) {
                                    removeIndexedEntries(j)
                                }
                                data.remove(RELATIONS_MANY_TO_ONE)
                                data.remove(RELATIONS_ONE_TO_MANY)
                            } else { // any file to copy in project

                                copyOtherTemplateFiles(currentFile, formPath)
                            }
                        }
                }
        }
    }

    fun applyDetailFormTemplate() {

        projectEditor.detailFormList.forEach { detailForm ->

            var formPath = fileHelper.pathHelper.getFormPath(detailForm.name, FormType.DETAIL)
            formPath = fileHelper.pathHelper.verifyFormPath(formPath, FormType.DETAIL)

            // not used in list form
            var specificFieldsCount = 0
            getManifestJSONContent(formPath)?.let {
                specificFieldsCount = it.getSafeObject("fields")?.getSafeInt("count") ?: 0
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

                                    var wholeFormHasIcons = false

                                    projectEditor.dataModelList.find { it.id == detailForm.dataModel.id }?.fields?.forEach {
                                        Log.d("DETAIL ${detailForm.name} / field = $it")
                                        if (!it.icon.isNullOrEmpty())
                                            wholeFormHasIcons = true
                                    }
                                    projectEditor.dataModelList.find { it.id  == detailForm.dataModel.id }?.relationList?.forEach { relation ->
                                        relation.subFields.forEach {
                                            Log.d("DETAIL ${detailForm.name} / related field = $it")
                                            if (!it.icon.isNullOrEmpty())
                                                wholeFormHasIcons = true
                                        }
                                    }

                                    Log.d("DetailForm - ${detailForm.dataModel.name} - wholeFormHasIcons = $wholeFormHasIcons")

                                    if (fieldList.isNotEmpty()) {

                                        if (specificFieldsCount == 0) { // template with no specific field
                                            for (i in fieldList.indices) {
                                                val field = fieldList[i]
                                                if (field.name.isNotEmpty()) {

                                                    val format = getFormatWithFixes(projectEditor.dataModelList, detailForm, field, fileHelper.pathHelper)
                                                    val formField = field.getTemplateFormFieldFiller(
                                                        i = i + 1,
                                                        dataModelList = projectEditor.dataModelList,
                                                        form = detailForm,
                                                        formatType = format,
                                                        isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                        imageWidth = getImageSize(detailForm, field.name, "width"),
                                                        imageHeight = getImageSize(detailForm, field.name, "height"),
                                                        wholeFormHasIcons = wholeFormHasIcons
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

                                                    Log.d("Adding specific Field $field")
                                                    Log.d("fieldList[i].getLayoutVariableAccessor() = ${
                                                        field.getLayoutVariableAccessor(FormType.DETAIL)
                                                    }")

                                                    fillIndexedFormData(i + 1, field, FormType.DETAIL, detailForm, wholeFormHasIcons)

                                                    Log.i("applyDetailFormTemplate fieldName :: ${field.name.fieldAdjustment()}")

                                                    fillRelationFillerForEachLayout(field, detailForm, FormType.DETAIL, i)

                                                } else {
                                                    Log.d("Field list shorter than specific fields count")
                                                    resetIndexedEntries(i + 1)
                                                }
                                            }

                                            Log.i("fieldList.size = ${fieldList.size}")
                                            Log.i("specificFieldsCount = $specificFieldsCount")
                                            Log.d("fieldList = $fieldList")
                                            if (fieldList.size > specificFieldsCount) {
                                                var k = specificFieldsCount // another counter to avoid null field
                                                for (i in specificFieldsCount until fieldList.size) {
                                                    Log.d("in for loop, i = $i")
                                                    Log.d("in for loop, k = $k")
                                                    val field = fieldList[i]
                                                    Log.d("fieldList[i] = $field")
                                                    if (field.name.isNotEmpty()) {
                                                        Log.d("Adding free Field in specific template $field")

                                                        Log.d("field.name = ${field.name}")
                                                        Log.d("field.name fieldAdjusted = ${field.name.fieldAdjustment()}")
                                                        Log.d("form's dataModel = ${projectEditor.dataModelList.find { it.id == detailForm.dataModel.id }?.name}")
                                                        val format = getFormatWithFixes(projectEditor.dataModelList, detailForm, field, fileHelper.pathHelper)
                                                        val formField = field.getTemplateFormFieldFiller(
                                                            i = k + 1,
                                                            dataModelList = projectEditor.dataModelList,
                                                            form = detailForm,
                                                            formatType = format,
                                                            isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                            imageWidth = getImageSize(detailForm, field.name, "width"),
                                                            imageHeight = getImageSize(detailForm, field.name, "height"),
                                                            wholeFormHasIcons = wholeFormHasIcons
                                                        )

                                                        fillRelationFillerForEachLayout(field, detailForm, FormType.DETAIL, k + 1)

                                                        Log.v("format :: $format")

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
                                copyOtherTemplateFiles(currentFile, formPath)
                            }
                        }
                }
        }
    }

    private fun fillRelationFillerForEachLayout(field: Field, form: Form, formType: FormType, index: Int) {

        val source: String = form.dataModel.name
        val target: String? = projectEditor.dataModelList.find { it.id == field.relatedTableNumber.toString() }?.name
        val inverseName: String? = getInverseNameWithFixes(projectEditor.dataModelList, form, field)

        if (inverseName != null && target != null) {

            Log.d(">>>> field : $field")
            projectEditor.dataModelList.find { it.id  == form.dataModel.id }?.relationList?.find { it.name == field.name }?.let { relation ->
                Log.d(">>>> relation : $relation")

                val navbarTitle = getNavbarTitleWithFixes(projectEditor.dataModelList, form, field, source)
                fillRelationFillerForEachRelation(source, target, field.name, inverseName, index, formType, relation, navbarTitle)

            } ?: kotlin.run {
                Log.d(">>>> no relation found with this name, here is relation list :")
                Log.d(">>>> ${projectEditor.dataModelList.find { it.id  == form.dataModel.id }?.relationList}")
                val relationSplit = field.name.split(".")
                if (relationSplit.size > 1) {
                    val relationBaseName: String = relationSplit[0]
                    val relationEndName: String = relationSplit[1]

                    projectEditor.dataModelList.find { it.id  == form.dataModel.id }?.fields?.find { it.name == relationBaseName }?.relatedTableNumber?.let { relationBaseTableNumber ->
                        Log.d(">>>> relationBaseTableNumber = $relationBaseTableNumber")
                        projectEditor.dataModelList.find { it.id  == relationBaseTableNumber.toString() }?.relationList?.find { it.name == relationEndName }?.let { subRelation ->
                            Log.d(">>>> subRelation = $subRelation")

                            val navbarTitle = getNavbarTitleWithFixes(projectEditor.dataModelList, form, field, source)
                            fillRelationFillerForEachRelation(source, subRelation.target, field.name, inverseName, index, formType, subRelation, navbarTitle)

                        }
                    }
                }
            }
        }
    }

    private fun fillRelationFillerForEachRelation(source: String, target: String, relationName: String, inverseName: String, index: Int, formType: FormType, relation: Relation, navbarTitle: String) {
        val filler = getTemplateRelationFillerForLayout(source, target, relationName, inverseName, index, navbarTitle)
        // if source table or target table is not in navigation, return
        Log.d("source = ${source.tableNameAdjustment()}")
        Log.d("target = ${target.tableNameAdjustment()}")
//        if (tableNamesForNavigation.find { it.name.tableNameAdjustment() == source.tableNameAdjustment() } == null)
//            return
//        if (tableNamesForNavigation.find { it.name.tableNameAdjustment() == target.tableNameAdjustment() } == null)
//            return
        when {
            formType == FormType.LIST && relation.relationType == RelationType.ONE_TO_MANY ->
                oneToManyRelationFillerForEachListLayout.add(filler)
            formType == FormType.LIST && relation.relationType == RelationType.MANY_TO_ONE ->
                manyToOneRelationFillerForEachListLayout.add(filler)
            formType == FormType.DETAIL && relation.relationType == RelationType.ONE_TO_MANY ->
                oneToManyRelationFillerForEachDetailLayout.add(filler)
            formType == FormType.DETAIL && relation.relationType == RelationType.MANY_TO_ONE ->
                manyToOneRelationFillerForEachDetailLayout.add(filler)
        }
        Log.d("Adding filler : $filler")
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
        data.remove("field_${i}_custom_formatted_imageNamed")
        data.remove("field_${i}_format_type")
        data.remove("field_${i}_accessor")
        data.remove("field_${i}_field_name")
        data.remove("field_${i}_source_table_name")
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
        data["field_${i}_custom_formatted_imageNamed"] = false
        data["field_${i}_format_type"] = ""
        data["field_${i}_accessor"] = ""
        data["field_${i}_field_name"] = ""
        data["field_${i}_source_table_name"] = ""
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

    private fun fillIndexedFormData(i: Int, field: Field, formType: FormType, form: Form, wholeFormHasIcons: Boolean) {
        Log.d("fillIndexedFormData, field = $field")
        data["field_${i}_name"] = field.name.fieldAdjustment()
        data["field_${i}_defined"] = field.name.isNotEmpty()
        data["field_${i}_is_image"] = field.isImage()
        data["field_${i}_label"] = getLabelWithFixes(projectEditor.dataModelList, form, field)
        data["field_${i}_shortLabel"] = getShortLabelWithFixes(projectEditor.dataModelList, form, field)
        data["field_${i}_iconPath"] = ""
        data["field_${i}_hasIcon"] = false
        data["field_${i}_custom_formatted"] = false
        data["field_${i}_custom_formatted_imageNamed"] = false
        data["field_${i}_format_type"] = ""
        data["field_${i}_accessor"] = field.getLayoutVariableAccessor(formType)
        data["field_${i}_field_name"] = field.getFieldName()
        data["field_${i}_source_table_name"] = field.getSourceTableName(projectEditor.dataModelList, form)
        val isRelation = isRelationWithFixes(projectEditor.dataModelList, form, field)
        Log.d("field ${field.name}, isRelation ? : $isRelation")
        if (isRelation) {
            data["field_${i}_is_relation"] = true
            val labelHasPercentPlaceholder = hasLabelPercentPlaceholder(projectEditor.dataModelList, form, field)
            if (labelHasPercentPlaceholder) {
                data["field_${i}_label_has_percent_placeholder"] = true
                data["field_${i}_label_with_percent_placeholder"] = getLabelWithPercentPlaceholder(projectEditor.dataModelList, form, field, formType)
            }

            val shortLabelHasPercentPlaceholder = hasShortLabelPercentPlaceholder(projectEditor.dataModelList, form, field)
            if (shortLabelHasPercentPlaceholder) {
                data["field_${i}_shortLabel_has_percent_placeholder"] = true
                data["field_${i}_shortLabel_with_percent_placeholder"] = getShortLabelWithPercentPlaceholder(projectEditor.dataModelList, form, field, formType)
            }
        }

        if (field.isImage()) {
            data["field_${i}_image_key_accessor"] = field.getFieldKeyAccessor(formType)
        }

        if (wholeFormHasIcons) {
            data["field_${i}_iconPath"] = getIconWithFixes(projectEditor.dataModelList, form, field)
            data["field_${i}_hasIcon"] = true
        }

        val format = getFormatWithFixes(projectEditor.dataModelList, form, field, fileHelper.pathHelper)
        data["field_${i}_format_type"] = format

        if (format.startsWith("/")) {
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
        }
    }

    private fun copyOtherTemplateFiles(currentFile: File, formPath: String) {
        val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath,
            formPath))

        if (currentFile.isWithTemplateName()) {
            for (tableName in tableNames) { // file will be duplicated
                fillFileWithTemplateName(tableName)
                val replacedPath = newFile.absolutePath.replace(TEMPLATE_PLACEHOLDER, tableName.name)
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
        println("fileName= $fileName")
        if (reservedKeywords.contains(fileName)) {
            newFile = File(newFile.parent.removeSuffix("/")
                .removeSuffix("\\") + File.separator + fileName.validateWord() + "." + newFile.extension)
            println("===== newFile = ${newFile.absolutePath}")
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
            throw Exception("An error occurred while creating new file : $queriesFile")
        }
        queriesFile.writeText(gson.toJson(queries))
    }

    fun makeAppInfo() {
        makeJsonFile(APP_INFO_FILENAME, projectEditor.getAppInfo())
    }

    fun makeCustomFormatters() {
        makeJsonFile(CUSTOM_FORMATTERS_FILENAME, customFormattersFields)
    }

    fun makeSearchableFields() {
        makeJsonFile(SEARCHABLE_FIELDS_FILENAME, projectEditor.searchableFields)
    }

    fun makeActionsList() {
        val hasActions = projectEditor.findJsonBoolean(HAS_ACTIONS_KEY) ?: false
        if (hasActions) {
            projectEditor.actions.forEach {
                makeJsonFile(it.key.fileName, it.value)
                Log.i("${it.key.fileName} file successfully generated.")
            }
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

    // <tableName, <fieldName, fieldMapping>>
    private fun getCustomFormatterFields(): Map<String, Map<String, FieldMapping>> {

        val customFormatMap = mutableMapOf<String, Map<String, FieldMapping>>()
        val customFormattersImagesMap = mutableMapOf<String, Map<String, Pair<String, String>>>() // <formatName, <imageName, <resourceName, darkModeResourceName>>

        projectEditor.dataModelList.forEach { dataModel ->
            val map = mutableMapOf<String, FieldMapping>()
            dataModel.fields?.forEach { field ->
                getCustomFormatterField(dataModel, field, null, customFormatMap, map, customFormattersImagesMap)
            }
            dataModel.relationList?.forEach { relation ->
                relation.subFields.forEach { field ->
                    getCustomFormatterField(dataModel, field, relation.name, customFormatMap, map, customFormattersImagesMap)
                }
            }
        }
        this.customFormattersImagesMap = customFormattersImagesMap
        return customFormatMap
    }

    private fun getCustomFormatterField(
        dataModel: DataModel,
        field: Field,
        relationName: String?,
        customFormatMap: MutableMap<String, Map<String, FieldMapping>>,
        map: MutableMap<String, FieldMapping>,
        customFormattersImagesMap: MutableMap<String, Map<String, Pair<String, String>>>
    ) {
        if (field.format != null) {
            val format = field.format as String
            Log.d("getCustomFormatterFields()  /  Format = $format")
            if (format.startsWith("/")) {

                val formatPath = fileHelper.pathHelper.getCustomFormatterPath(format)
                getManifestJSONContent(formatPath)?.let {

                    val fieldMapping = getFieldMapping(it, format)
                    Log.d("extractFormatter : relationName = $relationName")
                    Log.d("fieldMapping = $fieldMapping")
                    if (isValidFormatter(fieldMapping)) {
                        extractFormatter(fieldMapping, field, relationName, formatPath, format, map, customFormattersImagesMap)
                        customFormatMap.put(dataModel.name.tableNameAdjustment(), map)
                    } else {
                        Log.d("Not adding this custom formatter as it's not valid")
                    }
                }

            } else {
                customFormatMap[dataModel.name.tableNameAdjustment()] = map
            }
        } else {
            customFormatMap[dataModel.name.tableNameAdjustment()] = map
        }
    }

    private fun extractFormatter(
        fieldMapping: FieldMapping,
        field: Field,
        relationName: String?,
        formatPath: String,
        format: String,
        map: MutableMap<String, FieldMapping>,
        customFormattersImagesMap: MutableMap<String, Map<String, Pair<String, String>>>
    ) {
        if (relationName.isNullOrEmpty())
            map[field.name.fieldAdjustment()] = fieldMapping
        else
            map[relationName.fieldAdjustment() + "." + field.name.fieldAdjustment()] = fieldMapping

        Log.d("map = $map")

        if (isImageNamed(fieldMapping)) {

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
            customFormattersImagesMap.putIfAbsent(format, imageMap)
        }
    }

    private fun getResourceName(format: String, imageName: String, darkModeExists: Boolean): Pair<String, String> {
        val correctedFormatName = format
            .removePrefix("/")
            .toLowerCase()
            .replace("[^a-z0-9]+".toRegex(), "_")

        Log.d("correctedFormatName = $correctedFormatName")

        val correctedImageName = correctIconPath(imageName)

        return if (darkModeExists)
            Pair("${correctedFormatName}_${correctedImageName}", "${correctedFormatName}_${correctedImageName}_dark")
        else
            Pair("${correctedFormatName}_${correctedImageName}", "")
    }

    private fun getImageSize(form: Form, fieldName: String, type: String): Int {
        customFormattersFields[form.dataModel.name.tableNameAdjustment()]?.get(fieldName.fieldAdjustment())?.let{ fieldMapping ->
            return when (type) {
                "width" -> fieldMapping.imageWidth ?: 0
                "height" -> fieldMapping.imageHeight ?: 0
                else -> 0
            }
        }
        return 0
    }

    private fun isImageNamedBinding(form: Form, fieldName: String): Boolean {
        customFormattersFields[form.dataModel.name.tableNameAdjustment()]?.get(fieldName.fieldAdjustment())?.let{ fieldMapping ->
            return isImageNamed(fieldMapping)
        }
        return false
    }

    private fun isImageNamed(fieldMapping: FieldMapping) = fieldMapping.binding == "imageNamed"

    private fun getHexStringColor(red: Int, green: Int, blue: Int): String {
        var redHexString = toHexString(red)
        var greenHexString = toHexString(green)
        var blueHexString = toHexString(blue)

        if (redHexString.length == 1) redHexString = "0$redHexString"
        if (greenHexString.length == 1) greenHexString = "0$greenHexString"
        if (blueHexString.length == 1) blueHexString = "0$blueHexString"

        return "#$redHexString$greenHexString$blueHexString"
    }
}
