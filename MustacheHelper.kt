import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_REMOTE_URL
import DefaultValues.LAYOUT_FILE
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
import MustacheConstants.CUSTOM_FORMATTERS_IMAGES
import MustacheConstants.DATE_DAY
import MustacheConstants.DATE_MONTH
import MustacheConstants.DATE_YEAR
import MustacheConstants.ENTITY_CLASSES
import MustacheConstants.FIELDS
import MustacheConstants.FIRST_FIELD
import MustacheConstants.FORM_FIELDS
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
import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.TIME_TYPE
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
    private val tableNamesForLayoutType = mutableListOf<TemplateLayoutTypeFiller>()
    private var tableNames = mutableListOf<TemplateTableFiller>()
    private var tableNames_lowercase = mutableListOf<TemplateLayoutFiller>()
    private var relations = mutableListOf<TemplateRelationFiller>()
    private var customFormatterImages = mutableListOf<TemplateFormatterFiller>()

    // <formatName, <imageName, resourceName>>
    private lateinit var customFormattersImagesMap: Map<String, Map<String, String>>
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

        projectEditor.findJsonString("backgroundColor")?.let {

            Log.i("backgroundColor = $it")

            data[COLORS_DEFINED] = true
            data[COLOR_PRIMARY_NEUTRAL] = it

            val backgroundColor: Int = Color.parseColor(it)
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
        } ?: run {
            data[COLORS_DEFINED] = false
            data[THEME_COLOR_PRIMARY] = "@color/cyan_900"
            data[THEME_COLOR_PRIMARY_DARKER] = "@color/cyan_dark"
            data[THEME_COLOR_PRIMARY_LIGHTER] = "@color/cyan_light"
        }

        projectEditor.findJsonString("foregroundColor")?.let {

            Log.i("foregroundColor = $it")

            if (data[COLORS_DEFINED] == true) {
                data[THEME_COLOR_ON_PRIMARY] = if (it == "#00") "@color/black" else "@color/white"
            } else {
                data[THEME_COLOR_ON_PRIMARY] = "@color/white"
            }
        } ?: run {
            data[THEME_COLOR_ON_PRIMARY] = "@color/white"
        }

        Log.i("data[THEME_COLOR_ON_PRIMARY] = ${data[THEME_COLOR_ON_PRIMARY]}")

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
        typesAndTables.add(TemplateTableFiller(name = "Photo",
            name_original = "Photo",
            nameCamelCase = "photo",
            concat_fields = ""))
        typesAndTables.add(TemplateTableFiller(name = "Date",
            name_original = "Date",
            nameCamelCase = "date",
            concat_fields = ""))
        typesAndTables.add(TemplateTableFiller(name = "Time",
            name_original = "Time",
            nameCamelCase = "time",
            concat_fields = ""))
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
                            nameLowerCase = dataModel.name.tableNameAdjustment().toLowerCase(),
                            nameCamelCase = dataModel.name.dataBindingAdjustment(),
                            label = dataModel.getLabel(),
                            hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                            icon = dataModel.iconPath ?: ""))

                    navigationTableCounter++

                    dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                        ?.forEach { relation ->
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
            val listFormName =
                projectEditor.listFormList.find { listform -> listform.dataModel.name.tableNameAdjustment() == tableName.tableNameAdjustment() }?.name
            var formPath = fileHelper.pathHelper.getFormPath(listFormName, FormType.LIST)
            formPath = fileHelper.pathHelper.verifyFormPath(formPath, FormType.LIST)
            tableNamesForLayoutType.add(TemplateLayoutTypeFiller(name = tableName, layout_manager_type = getLayoutManagerType(formPath)))
        }

        data[TABLENAMES_LAYOUT] = tableNamesForLayoutType
        Log.d("TABLENAMES", "${data[TABLENAMES]}")

        customFormatterImages = mutableListOf()

        for ((formatterName, imageMap) in customFormattersImagesMap) {
            for ((imageName, resourceName) in imageMap) {
                customFormatterImages.add(TemplateFormatterFiller(
                    formatterName = formatterName,
                    imageName = imageName,
                    resourceName = resourceName
                ))
            }
        }
        data[CUSTOM_FORMATTERS_IMAGES] = customFormatterImages
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

                        Log.d("Processed file", "$currentFile")

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
                                projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }
                                    ?.let { datamodel ->
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
                                            throw Exception("An error occurred while parsing the fieldType of field : $field")
                                        }
                                    }

                                    data[FIELDS] = fieldList
                                    Log.d("FIELDS", "${data[FIELDS]}")
                                    val firstField: String = fieldList.firstOrNull()?.name ?: ""
                                    data[FIRST_FIELD] = firstField
                                }

                                relations.clear()
                                val relationsImport =
                                    mutableListOf<TemplateRelationFiller>() // need another list, to remove double in import section

                                projectEditor.dataModelList.find { it.name.tableNameAdjustment() == tableName.name.tableNameAdjustment() }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                                    ?.forEach { relation ->

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
                                dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                                    ?.forEach { relation ->

                                        data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                                        data[RELATION_TARGET] = relation.target.tableNameAdjustment()

                                        val replacedPath = newFilePath.replace(TEMPLATE_RELATION_DAO_PLACEHOLDER,
                                            "${relation.source.tableNameAdjustment()}Has${relation.target.tableNameAdjustment()}")

                                        applyTemplate(replacedPath)

                                        //cleaning
                                        data.remove(RELATION_SOURCE)
                                        data.remove(RELATION_TARGET)
                                    }
                            }

                        } else if (currentFile.isWithRelationEntityTemplateName()) {

                            projectEditor.dataModelList.forEach { dataModel ->
                                dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                                    ?.forEach { relation ->

                                        data[RELATION_SOURCE] = relation.source.tableNameAdjustment()
                                        data[RELATION_TARGET] = relation.target.tableNameAdjustment()
                                        data[RELATION_NAME] = relation.name.fieldAdjustment()
                                        data[RELATION_SAME_TYPE] = relation.source == relation.target

                                        val replacedPath = newFilePath.replace(TEMPLATE_RELATION_ENTITY_PLACEHOLDER,
                                            "${relation.source.tableNameAdjustment()}And${relation.target.tableNameAdjustment()}")

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
                                relations.clear()
                                projectEditor.dataModelList.find { it.name.tableNameAdjustment() == listForm.dataModel.name.tableNameAdjustment() }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                                    ?.forEach { relation ->
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

                                    if ((field.inverseName != null) || (fileHelper.pathHelper.isDefaultTemplateListFormPath(
                                            formPath) && field.isImage())
                                    ) { // is relation or image in default template

                                        data["field_${i}_defined"] = false
                                        data["field_${i}_custom_formatted"] = false
                                        data["field_${i}_custom_formatted_imageNamed"] = false
                                        data["field_${i}_label"] = ""
                                        data["field_${i}_name"] = ""
                                        data["field_${i}_accessor"] = ""
                                        data["field_${i}_format_type"] = ""
                                        data["field_${i}_field_name"] = ""
                                        data["field_${i}_field_table_name"] = ""
                                        data["field_${i}_field_image_width"] = 0
                                        data["field_${i}_field_image_height"] = 0

                                    } else { // not a relation

                                        data["field_${i}_name"] = field.name.fieldAdjustment()
                                        data["field_${i}_defined"] = field.name.isNotEmpty()
                                        data["field_${i}_is_image"] = field.isImage()
                                        data["field_${i}_label"] = field.getLabel()
                                        data["field_${i}_is_int"] = field.isInt()
                                        data["field_${i}_custom_formatted"] = false
                                        data["field_${i}_custom_formatted_imageNamed"] = false
                                        data["field_${i}_format_type"] = ""
                                        data["field_${i}_accessor"] = field.getLayoutVariableAccessor(FormType.LIST)
                                        data["field_${i}_field_name"] = field.getFieldName()
                                        data["field_${i}_field_table_name"] = field.getFieldTableName(projectEditor.dataModelList, listForm)
                                        if (field.isImage()) {
                                            data["field_${i}_image_key_accessor"] = field.getFieldKeyAccessor(FormType.LIST)
                                        }

                                        val format = getFormatNameForType(field.fieldType, field.format)
                                        data["field_${i}_format_type"] = format

                                        if (format.startsWith("/")) {
                                            data["field_${i}_custom_formatted"] = true
                                            if (isImageNamedBinding(listForm, field.name)) {
                                                data["field_${i}_custom_formatted_imageNamed"] = true
                                                data["field_${i}_field_image_width"] = getImageSize(listForm, field.name, "width")
                                                data["field_${i}_field_image_height"] = getImageSize(listForm, field.name, "height")
                                            }
                                        }
                                    }
                                }

                                val newFilePath =
                                    fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name.tableNameAdjustment())
                                applyTemplate(newFilePath)

                                // cleaning data for other templates
                                for (j in 1 until i + 1) {
                                    data.remove("field_${j}_defined")
                                    data.remove("field_${j}_is_image")
                                    data.remove("field_${j}_is_int")
                                    data.remove("field_${j}_name")
                                    data.remove("field_${j}_label")
                                    data.remove("field_${j}_custom_formatted")
                                    data.remove("field_${j}_custom_formatted_imageNamed")
                                    data.remove("field_${j}_format_type")
                                    data.remove("field_${j}_accessor")
                                    data.remove("field_${j}_field_name")
                                    data.remove("field_${j}_image_key_accessor")
                                    data.remove("field_${j}_field_table_name")
                                    data.remove("field_${j}_field_image_width")
                                    data.remove("field_${j}_field_image_height")
                                }
                                data.remove(RELATIONS)
                            } else { // any file to copy in project
                                val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath,
                                    formPath))
                                Log.i("File to copy : ${currentFile.absolutePath}; target : ${newFile.absolutePath}")
                                if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                                    throw Exception("An error occurred while copying template files with target : ${newFile.absolutePath}")
                                }
                            }
                        }
                }
        }
    }


    private fun getFormatNameForType(fieldType: Int?, format: String?): String {
        if (format.equals("integer")) {
            return when (fieldType) {
                6 -> "boolInteger" // Boolean
                11 -> "timeInteger" // Time
                else -> "integer"
            }
        }
        if (format.isNullOrEmpty()) {
            return when (typeFromTypeInt(fieldType)) {
                BOOLEAN_TYPE -> "falseOrTrue"
                DATE_TYPE -> "mediumDate"
                TIME_TYPE -> "mediumTime"
                INT_TYPE -> "integer"
                FLOAT_TYPE -> "decimal"
                else -> ""
            }
        } else {
            return format
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

                                    fieldList.forEach {
                                        Log.d("DETAIL ${detailForm.name} / field = $it")
                                    }

                                    if (fieldList.isNotEmpty()) {

                                        if (specificFieldsCount == 0) { // template with no specific field
                                            for (i in fieldList.indices) {
                                                val field = fieldList[i]
                                                if (field.name.isNotEmpty()) {

                                                    val format = getFormatNameForType(field.fieldType, field.format)
                                                    val formField = createDetailFormField(
                                                        field = field,
                                                        i = i + 1,
                                                        dataModelList = projectEditor.dataModelList,
                                                        form = detailForm,
                                                        formatType = format,
                                                        isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                        imageWidth = getImageSize(detailForm, field.name, "width"),
                                                        imageHeight = getImageSize(detailForm, field.name, "height")
                                                    )

                                                    formFieldList.add(formField)
                                                } else {
                                                    // you can get null fields in json file
                                                    // occurs when you select a form, and then select back Blank form
                                                }
                                            }
                                        } else { // template with specific fields

                                            for (i in 0 until specificFieldsCount) {

                                                if (i < fieldList.size) {
                                                    val field = fieldList[i]

                                                    if (field.inverseName == null) { // is not relation
                                                        Log.d("Adding specific Field $field")
                                                        Log.d("fieldList[i].getLayoutVariableAccessor() = ${
                                                            field.getLayoutVariableAccessor(FormType.DETAIL)
                                                        }")

                                                        data["field_${i + 1}_defined"] = field.name.isNotEmpty()
                                                        data["field_${i + 1}_is_image"] = field.isImage()
                                                        data["field_${i + 1}_is_int"] = field.isInt()
                                                        data["field_${i + 1}_name"] = field.name.fieldAdjustment()
                                                        data["field_${i + 1}_label"] = field.getLabel()
                                                        data["field_${i + 1}_custom_formatted"] = false
                                                        data["field_${i + 1}_custom_formatted_imageNamed"] = false
                                                        data["field_${i + 1}_format_type"] = ""
                                                        data["field_${i + 1}_accessor"] =
                                                            field.getLayoutVariableAccessor(FormType.DETAIL)
                                                        data["field_${i + 1}_field_name"] = field.getFieldName()
                                                        data["field_${i + 1}_field_table_name"] = field.getFieldTableName(projectEditor.dataModelList, detailForm)
                                                        if (field.isImage()) {
                                                            data["field_${i + 1}_image_key_accessor"] = field.getFieldKeyAccessor(FormType.DETAIL)
                                                        }

                                                        val format = getFormatNameForType(field.fieldType, field.format)
                                                        data["field_${i + 1}_format_type"] = format
                                                        if (format.startsWith("/")) { // custom format
                                                            data["field_${i + 1}_custom_formatted"] = true

                                                            if (isImageNamedBinding(detailForm, field.name)) {
                                                                data["field_${i + 1}_custom_formatted_imageNamed"] = true
                                                                data["field_${i + 1}_field_image_width"] = getImageSize(detailForm, field.name, "width")
                                                                data["field_${i + 1}_field_image_height"] = getImageSize(detailForm, field.name, "height")
                                                            }
                                                        }

                                                        Log.v("format :: $format")
                                                        Log.i("applyDetailFormTemplate fieldName :: ${field.name.fieldAdjustment()}")

                                                    } else {
                                                        Log.d("Field [${field.name}] not added in specifc field because it is a relation")
                                                    }

                                                } else {
                                                    Log.d("Field list shorter than specific fields count")
                                                    data["field_${i + 1}_defined"] = false
                                                    data["field_${i + 1}_is_image"] = false
                                                    data["field_${i + 1}_is_int"] = false
                                                    data["field_${i + 1}_name"] = ""
                                                    data["field_${i + 1}_label"] = ""
                                                    data["field_${i + 1}_custom_formatted"] = false
                                                    data["field_${i + 1}_custom_formatted_imageNamed"] = false
                                                    data["field_${i + 1}_format_type"] = ""
                                                    data["field_${i + 1}_accessor"] = ""
                                                    data["field_${i + 1}_field_name"] = ""
                                                    data["field_${i + 1}_image_key_accessor"] = ""
                                                    data["field_${i + 1}_field_table_name"] = ""
                                                    data["field_${i + 1}_field_image_width"] = 0
                                                    data["field_${i + 1}_field_image_height"] = 0
                                                }
                                            }

                                            Log.i("fieldList.size = ${fieldList.size}")
                                            Log.i("specificFieldsCount = $specificFieldsCount")

                                            if (fieldList.size > specificFieldsCount) {
                                                var k = specificFieldsCount // another counter to avoid null field
                                                for (i in specificFieldsCount until fieldList.size) {
                                                    Log.d("in for loop, i = $i")
                                                    Log.d("in for loop, k = $k")
                                                    val field = fieldList[i]
                                                    Log.d("fieldList[i] = $field")
                                                    if (field.name.isNotEmpty()) {
                                                        Log.d("Adding free Field in specific template $field")

                                                        val format = getFormatNameForType(field.fieldType, field.format)
                                                        val formField = createDetailFormField(
                                                            field = field,
                                                            i = k + 1,
                                                            dataModelList = projectEditor.dataModelList,
                                                            form = detailForm,
                                                            formatType = format,
                                                            isImageNamed = isImageNamedBinding(detailForm, field.name),
                                                            imageWidth = getImageSize(detailForm, field.name, "width"),
                                                            imageHeight = getImageSize(detailForm, field.name, "height")
                                                        )

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
                                    data.remove("field_${i}_defined")
                                    data.remove("field_${i}_is_image")
                                    data.remove("field_${i}_is_int")
                                    data.remove("field_${i}_name")
                                    data.remove("field_${i}_label")
                                    data.remove("field_${i}_custom_formatted")
                                    data.remove("field_${i}_custom_formatted_imageNamed")
                                    data.remove("field_${i}_format_type")
                                    data.remove("field_${i}_accessor")
                                    data.remove("field_${i}_field_name")
                                    data.remove("field_${i}_image_key_accessor")
                                    data.remove("field_${i}_field_table_name")
                                }

                            } else { // any file to copy in project
                                val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath,
                                    formPath))
                                Log.i("File to copy : ${currentFile.absolutePath}; target : ${newFile.absolutePath}")
                                if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                                    throw Exception("An error occurred while copying template files with target : ${newFile.absolutePath}")
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
            newFile = File(newFile.parent.removeSuffix("/")
                .removeSuffix("\\") + File.separator + fileName.validateWord() + "." + newFile.extension)
            println("===== newFile = ${newFile.absolutePath}")
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

        val appInfoFile = File(fileHelper.pathHelper.assetsPath(), APP_INFO_FILENAME)
        appInfoFile.parentFile.mkdirs()
        if (!appInfoFile.createNewFile()) {
            throw Exception("An error occurred while creating new file : $appInfoFile")
        }
        appInfoFile.writeText(gson.toJson(projectEditor.getAppInfo(customFormattersFields)))
    }

    private fun generateCompilerFolder(templateFileFolder: String): Mustache.Compiler {
        return Mustache.compiler().escapeHTML(false).withLoader { name ->
            FileReader(File(templateFileFolder, name))
        }
    }

    private fun getLayoutManagerType(formPath: String): String {
        Log.i("getLayoutManagerType: $formPath")
        var type = "Collection"
        getManifestJSONContent(formPath)?.let {
            type = it.getSafeObject("tags")?.getSafeString("___LISTFORMTYPE___") ?: "Collection"
        }
        return when (type) {
            "Collection" -> "GRID"
            "Table" -> "LINEAR"
            else -> "LINEAR"
        }
    }

    // <tableName, <fieldName, fieldMapping>>
    private fun getCustomFormatterFields(): Map<String, Map<String, FieldMapping>> {

        val customFormatMap = mutableMapOf<String, Map<String, FieldMapping>>()
        val customFormattersImagesMap = mutableMapOf<String, Map<String, String>>() // <formatName, <imageName, resourceName>>

        projectEditor.dataModelList.forEach { dataModel ->
            val map = mutableMapOf<String, FieldMapping>()
            dataModel.fields?.forEach{ field ->
                field.format?.let { format ->
                    Log.d("ProjectEditor.kt  /  Format = $format")
                    if (format.startsWith("/")) {

                        val isSearchable = isCustomFormatterSearchable(dataModel.name, field.name, projectEditor.searchableFields)
                        val formatPath = fileHelper.pathHelper.getCustomFormatterPath(format)
                        getManifestJSONContent(formatPath)?.let {
                            val fieldMapping = getFieldMapping(it, format, isSearchable)
                            Log.d("fieldMapping = $fieldMapping")
                            if (fieldMapping.binding == "imageNamed" || fieldMapping.binding == "localizedText") {
                                map[field.name] = fieldMapping

                                if (isImageNamed(fieldMapping)) {

                                    val imageMap = mutableMapOf<String, String>()

                                    // choiceList can be Map<String, String> (JSONObject in appinfo.json)
                                    // or a List<String> (JSONArray in appinfo.json)
                                    when (fieldMapping.choiceList) {
                                        is Map<*, *> -> {
                                            fieldMapping.choiceList.values.forEach { imageName ->
                                                if (imageName is String) {
                                                    if (imageName.contains(".")) {
                                                        imageMap[imageName] = getResourceName(format, imageName)
                                                    }
                                                }
                                            }
                                        }
                                        is List<*> -> {
                                            fieldMapping.choiceList.forEach { imageName ->
                                                if (imageName is String) {
                                                    if (imageName.contains(".")) {
                                                        imageMap[imageName] = getResourceName(format, imageName)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    customFormattersImagesMap.putIfAbsent(format, imageMap)
                                }
                            } else {
                                Log.d("Not adding this custom formatter as its binding is neither localizedText nor imageNamed")
                            }
                        }

                    } else {
                        customFormatMap.put(dataModel.name, map)
                    }
                } ?: kotlin.run {
                    customFormatMap.put(dataModel.name, map)
                }
            }
        }
        this.customFormattersImagesMap = customFormattersImagesMap
        return customFormatMap
    }

    private fun getResourceName(format: String, imageName: String): String {
        val correctedFormatName = format
            .removePrefix("/")
            .toLowerCase()
            .replace("[^a-z0-9]+".toRegex(), "_")

        Log.d("correctedFormatName = $correctedFormatName")

        val correctedImageName = imageName
            .substring(0, imageName.lastIndexOf('.')) // removes extension
            .replace(".+/".toRegex(), "")
            .removePrefix(File.separator)
            .toLowerCase()
            .replace("[^a-z0-9]+".toRegex(), "_")

        Log.d("correctedImageName = $correctedImageName")

        return "${correctedFormatName}_${correctedImageName}"
    }

    private fun getImageSize(form: Form, fieldName: String, type: String): Int {
        customFormattersFields[form.dataModel.name]?.get(fieldName)?.let{ fieldMapping ->
            return when (type) {
                "width" -> fieldMapping.imageWidth ?: 0
                "height" -> fieldMapping.imageHeight ?: 0
                else -> 0
            }
        }
        return 0
    }

    private fun isImageNamedBinding(form: Form, fieldName: String): Boolean {
        customFormattersFields[form.dataModel.name]?.get(fieldName)?.let{ fieldMapping ->
            return isImageNamed(fieldMapping)
        }
        return false
    }

    private fun isImageNamed(fieldMapping: FieldMapping) = fieldMapping.binding == "imageNamed"
}
