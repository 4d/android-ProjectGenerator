import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_REMOTE_URL
import DefaultValues.LAYOUT_FILE
import ExitCodes.COPY_TEMPLATE_FILE_ERROR
import ExitCodes.FIELD_TYPE_ERROR
import ExitCodes.FILE_CREATION_ERROR
import ExitCodes.MISSING_ANDROID_CACHE_SDK_PATH
import ExitCodes.MISSING_ANDROID_SDK_PATH
import FileHelperConstants.APP_INFO_FILENAME
import FileHelperConstants.QUERIES_FILENAME
import MustacheConstants.ANDROID_SDK_PATH
import MustacheConstants.APP_NAME_WITH_CAPS
import MustacheConstants.AUTHOR
import MustacheConstants.CACHE_4D_SDK_PATH
import MustacheConstants.COMPANY_HEADER
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
import MustacheConstants.TABLENAME_LOWERCASE
import MustacheConstants.TABLENAME_ORIGINAL
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
import java.lang.IllegalArgumentException
import java.lang.Integer.toHexString
import java.util.*
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.system.exitProcess
import java.nio.file.Paths





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
    private lateinit var variableName: String
    private lateinit var variableFieldPath: String

    init {
        Log.plantTree(this::class.java.canonicalName)
        data[COMPANY_HEADER] = fileHelper.pathHelper.companyWithCaps
        data[AUTHOR] = projectEditor.findJsonString("author") ?: DEFAULT_AUTHOR
        data[DATE_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        data[DATE_MONTH] = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        data[DATE_YEAR] = Calendar.getInstance().get(Calendar.YEAR).toString()
//        data[PREFIX] = DEFAULT_PREFIX
//        data[COMPANY] = fileHelper.pathHelper.companyCondensed
//        data[APP_NAME] = fileHelper.pathHelper.appNameCondensed
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
            Log.e("Missing Android SDK path")
            exitProcess(MISSING_ANDROID_SDK_PATH)
        }
        Log.d("> Android SDK = ${data[ANDROID_SDK_PATH]}")

        projectEditor.findJsonString("cache4dSdk")?.let {
            data[CACHE_4D_SDK_PATH] = it
        } ?: run {
            Log.e("Missing 4D Mobile cache SDK path")
            exitProcess(MISSING_ANDROID_CACHE_SDK_PATH)
        }
        Log.d("> Cache 4D SDK = ${data[CACHE_4D_SDK_PATH]}")

        projectEditor.findJsonString("backgroundColor")?.let {
            // WIP
            /*println("backgroundColor = $it")
            val backgroundColor: Int = Color.parseColor(it)
            println("backgroundColor = $backgroundColor")
            val darker08 = toHexString(manipulateColor(backgroundColor, 0.8f))
            val darker04 = toHexString(manipulateColor(backgroundColor, 0.4f))
            val lighter12 = toHexString(manipulateColor(backgroundColor, 1.2f))
            val lighter16 = toHexString(manipulateColor(backgroundColor, 1.6f))
            println("darker08 = $darker08")
            println("darker04 = $darker04")
            println("lighter12 = $lighter12")
            println("lighter16 = $lighter16")*/
        }

        projectEditor.findJsonString("foregroundColor")?.let {
            // TO BE IMPLEMENTED
        }

        var entityClassesString = ""

        val dataModelRelationList = mutableListOf<TemplateRelationFiller>()

        //println("ProjectDataModelLis :: ${projectEditor.dataModelList}" )
        projectEditor.dataModelList.forEach { dataModel ->

            dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                dataModelRelationList.add(TemplateRelationFiller(relation_source = relation.source.condenseSpaces(),
                    relation_target = relation.target.condenseSpaces(),
                    relation_name = relation.name.condenseSpaces()))
            }

            tableNames.add(TemplateTableFiller(name = dataModel.name.condenseSpaces(), name_original = dataModel.name))

            tableNames_lowercase.add(
                TemplateLayoutFiller(
                    name = dataModel.name.condenseSpaces(),
                    name_original = dataModel.name,
                    nameLowerCase = dataModel.name.toLowerCase().condenseSpaces(),
                    nameCamelCase = dataModel.name.capitalizeWords().condenseSpaces(),
                    hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                    icon = dataModel.iconPath ?: ""))

            entityClassesString += "${dataModel.name.condenseSpaces()}::class, "
            Log.d("ProjectDataModelLis  ${entityClassesString}:: ${dataModel.name.condenseSpaces()}")
        }

        val tableNames_without_relations = mutableListOf<TemplateTableFiller>()

        tableNames.forEach { tableName ->
            if (!dataModelRelationList.map { it.relation_source.condenseSpaces() }.contains(tableName.name))
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
        typesAndTables.add(TemplateTableFiller(name = "Photo", name_original = "Photo"))
        typesAndTables.add(TemplateTableFiller(name = "Date", name_original = "Date"))
        typesAndTables.add(TemplateTableFiller(name = "Time", name_original = "Time"))
        data[TYPES_AND_TABLES] = typesAndTables


        var navigationTableCounter =
            0 // Counter to limit to 4 navigation tables as it is not possible to have more than 5
        projectEditor.navigationTableList.forEach { navigationTableId ->
            projectEditor.dataModelList.find { it.id == navigationTableId }?.let { dataModel ->
                if (navigationTableCounter > 3)
                    return@forEach

                tableNamesForNavigation.add(
                    TemplateLayoutFiller(
                        name = dataModel.name.condenseSpaces(),
                        name_original = dataModel.name,
                        nameLowerCase = dataModel.name.toLowerCase().condenseSpaces(),
                        nameCamelCase = dataModel.name.capitalizeWords().condenseSpaces(),
                        hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                        icon = dataModel.iconPath ?: ""))

                navigationTableCounter++
            }
        }
        data[TABLENAMES_NAVIGATION] = tableNamesForNavigation

        // Specifying if list layout is table or collection (LinearLayout or GridLayout)
        tableNamesForNavigation.map { it.name }.forEach { tableName ->
            val listFormName = projectEditor.listFormList.find { listform -> listform.dataModel.name.condenseSpaces() == tableName.condenseSpaces() }?.name
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
                .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) }
                .forEach { currentFile ->

                    Log.d("Processed file"  ,"$currentFile")

                    template = compiler.compile("{{>${currentFile.name}}}")

                    val newFilePath = fileHelper.pathHelper.getPath(currentFile.absolutePath.replaceXmlTxtSuffix())

                    relations.clear()
                    projectEditor.dataModelList.forEach { dataModel ->
                        dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }
                            ?.forEach { relation ->
                                relations.add(TemplateRelationFiller(relation_source = relation.source.condenseSpaces(),
                                    relation_target = relation.target.condenseSpaces(),
                                    relation_name = relation.name.condenseSpaces()))
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

                            data[TABLENAME] = tableName.name.condenseSpaces()
                            data[TABLENAME_ORIGINAL] = tableName.name
                            data[TABLENAME_LOWERCASE] = tableName.name.toLowerCase().condenseSpaces()
                            projectEditor.dataModelList.find { it.name.condenseSpaces() == tableName.name.condenseSpaces() }?.fields?.let { fields ->
                                val fieldList = mutableListOf<TemplateFieldFiller>()
                                for (field in fields) {
                                    field.fieldTypeString?.let { fieldTypeString ->
                                        fieldList.add(
                                            TemplateFieldFiller(
                                                name = field.name.condenseSpaces(),
                                                fieldTypeString = fieldTypeString,
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
                                data[FIRST_FIELD] = fieldList.firstOrNull()?.name ?: ""
                                //println("FIRST_FIELD :: ${data[FIRST_FIELD]}")
                            }

                        relations.clear()
                        val relationsImport = mutableListOf<TemplateRelationFiller>() // need another list, to remove double in import section

                        projectEditor.dataModelList.find { it.name.condenseSpaces() == tableName.name.condenseSpaces() }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            relations.add(TemplateRelationFiller(
                                relation_source = relation.source.condenseSpaces(),
                                relation_target = relation.target.condenseSpaces(),
                                relation_name = relation.name.condenseSpaces()))

                            var isAlreadyImported = false
                            for (relationImport in relationsImport) {
                                if (relationImport.relation_source == relation.source.condenseSpaces() && relationImport.relation_target == relation.target.condenseSpaces())
                                    isAlreadyImported = true
                            }
                            if (!isAlreadyImported)
                                relationsImport.add(TemplateRelationFiller(
                                    relation_source = relation.source.condenseSpaces(),
                                    relation_target = relation.target.condenseSpaces(),
                                    relation_name = relation.name.condenseSpaces())) // name is unused
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

                            data[RELATION_SOURCE] = relation.source
                            data[RELATION_TARGET] = relation.target

                            val replacedPath = newFilePath.replace(TEMPLATE_RELATION_DAO_PLACEHOLDER, "${relation.source}Has${relation.target}")

                            applyTemplate(replacedPath)

                            //cleaning
                            data.remove(RELATION_SOURCE)
                            data.remove(RELATION_TARGET)
                        }
                    }

                } else if (currentFile.isWithRelationEntityTemplateName()) {

                    projectEditor.dataModelList.forEach { dataModel ->
                        dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            data[RELATION_SOURCE] = relation.source
                            data[RELATION_TARGET] = relation.target
                            data[RELATION_NAME] = relation.name.condenseSpaces()
                            data[RELATION_SAME_TYPE] = relation.source == relation.target

                            val replacedPath = newFilePath.replace(TEMPLATE_RELATION_ENTITY_PLACEHOLDER, "${relation.source}And${relation.target}")

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

            println("MustacheHelper : listform.name = ${listForm.name}")
            println("MustacheHelper : listform.datamodel.name = ${listForm.dataModel.name}")
            println("MustacheHelper : listform.datamodel.name.condenseSpaces() = ${listForm.dataModel.name.condenseSpaces()}")
            println("MustacheHelper : listform.fields size = ${listForm.fields?.size}")

            var formPath = fileHelper.pathHelper.getFormPath(listForm.name, FormType.LIST)

            if (File(formPath).exists()) {
                if (!fileHelper.pathHelper.appFolderExistsInTemplate(formPath)) {
                    println("WARNING : AN IOS TEMPLATE WAS GIVEN FOR THE LIST FORM $formPath")
                    formPath = fileHelper.pathHelper.getDefaultTemplateListFormPath()
                }
            } else {
                println("WARNING : MISSING LIST FORM TEMPLATE $formPath")
                formPath = fileHelper.pathHelper.getDefaultTemplateListFormPath()
            }

            val appFolderInTemplate = fileHelper.pathHelper.getAppFolderInTemplate(formPath)

            File(appFolderInTemplate).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->

                compiler = generateCompilerFolder(currentFolder.absolutePath)

                currentFolder.walkTopDown()
                    .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) }
                    .forEach { currentFile ->

                        Log.i(" > Processed template file : $currentFile")

                        template = compiler.compile("{{>${currentFile.name}}}")

                        if (currentFile.name == LAYOUT_FILE) {
                            val oldFormText = readFileDirectlyAsText(currentFile)
                            val newFormText = replaceTemplateText(oldFormText, FormType.LIST)
                            template = compiler.compile(newFormText)

                            data[TABLENAME_LOWERCASE] = listForm.dataModel.name.toLowerCase().condenseSpaces()
                            data[TABLENAME] = listForm.dataModel.name.condenseSpaces()

                            var i = 0
                            listForm.fields?.forEach { field -> // Could also iter over specificFieldsCount as Detail form
                                i++
                               // data["field_${i}_defined"] = field.name.isNotEmpty()
                               // data["field_${i}_name"] = field.name.condensePropertyName()
                               // data["field_${i}_label"] = field.label ?: ""

                                if ((field.inverseName != null) || (fileHelper.pathHelper.isDefaultTemplateListFormPath(formPath) && field.fieldType == 3)) { // is relation or image in default template

                                    data["field_${i}_defined"] = false
                                    data["field_${i}_label"] = field.label ?: ""
                                    data["field_${i}_name"] = ""

                                } else { // not a relation

                                    val key = formatFields[field.name.condenseSpaces()]
                                    if (formatFields[field.name.condenseSpaces()] != null) {
                                        data["field_${i}_name"] =
                                            "@{Format.${formatTypeFunctionName[key]}(${typeChoice[key]},$variableName.${field.name.condenseSpaces()}.toString())}"
                                    } else {
                                        data["field_${i}_name"] = "${field.name.condenseSpaces()}"
                                    }
                                    if (field.name.condenseSpaces().equals("Photo")) {
                                        Log.d("Fieldname :: ${field.name.condenseSpaces()}")
                                        data["field_${i}_name"] = "${field.name.condenseSpaces()}"
                                    }

                                    data["field_${i}_defined"] = field.name.isNotEmpty()
                                    data["field_${i}_label"] = field.label ?: ""
                                }
                            }

                            val newFilePath = fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name.condenseSpaces())
                            applyTemplate(newFilePath)

                            // cleaning data for other templates
                            for (j in 1 until i + 1) {
                                data.remove("field_${j}_defined")
                                data.remove("field_${j}_name")
                                data.remove("field_${j}_label")
                            }
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
                   println("WARNING : AN IOS TEMPLATE WAS GIVEN FOR THE DETAIL FORM $formPath")
                   formPath = fileHelper.pathHelper.getDefaultTemplateDetailFormPath()
               }
           } else {
               println("WARNING : MISSING DETAIL FORM TEMPLATE $formPath")
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
                   .filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) }
                   .forEach { currentFile ->

                       Log.i(" > Processed template file : $currentFile")

                       template = compiler.compile("{{>${currentFile.name}}}")

                       if (currentFile.name == LAYOUT_FILE) {
                           val oldFormText = readFileDirectlyAsText(currentFile)
                           val newFormText = replaceTemplateText(oldFormText, FormType.DETAIL)

                           template = compiler.compile(newFormText)

                           data[TABLENAME_LOWERCASE] = detailForm.dataModel.name.toLowerCase().condenseSpaces()
                           data[TABLENAME] = detailForm.dataModel.name.condenseSpaces()

                           val formFieldList = mutableListOf<TemplateFormFieldFiller>()

                           detailForm.fields?.let { fieldList ->

                               fieldList.forEach {
                                   println("${detailForm.name} / field = $it")
                               }

                               if (fieldList.isNotEmpty()) {

                                   if (specificFieldsCount == 0) { // template with no specific field
                                       for (i in fieldList.indices) {
                                           if (fieldList[i].name.isNotEmpty()) {
                                               // formFieldList.add(createFormField(fieldList[i], i + 1))

                                                val key = fieldList[i].name.condenseSpaces()
                                                if (formatFields[key] != null) {
                                                    var customFormat =
                                                        "@{Format.${formatTypeFunctionName[formatFields[key]]}(${typeChoice[formatFields[key]]},$variableFieldPath.${key}.toString()).toString()}"
                                                    formFieldList.add(createFormField(customFormat, fieldList[i], i + 1))
                                                } else {
                                                    formFieldList.add(createFormField(fieldList[i],i + 1))
                                                }
                                           } else {
                                               // you can get null fields in json file
                                               // occurs when you select a form, and then select back Blank form
                                           }
                                       }
                                   } else { // template with specific fields

                                       for (i in 0 until specificFieldsCount) {
                                           println("fieldList[i] = ${fieldList[i]}")
                                           data["field_${i + 1}_defined"] = fieldList[i].name.isNotEmpty()
                                           data["field_${i + 1}_name"] = fieldList[i].name.condenseSpaces()
                                           data["field_${i + 1}_label"] = fieldList[i].label ?: ""
                                       }

                                       println("fieldList.size = ${fieldList.size}")
                                       println("specificFieldsCount = $specificFieldsCount")

                                       if (fieldList.size > specificFieldsCount) {
                                           var k = specificFieldsCount // another counter to avoid null field
                                           for (i in specificFieldsCount until fieldList.size) {
                                               println("in for loop, i = $i")
                                               println("in for loop, k = $k")
                                               println("fieldList[i] = ${fieldList[i]}")
                                               if (fieldList[i].name.isNotEmpty()) {
                                                   formFieldList.add(createFormField(fieldList[i], k + 1))
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

                           val newFilePath = fileHelper.pathHelper.getDetailFormPath(detailForm.dataModel.name.condenseSpaces())
                           applyTemplate(newFilePath)

                           // cleaning data for other templates
                           data.remove(FORM_FIELDS)
                           for (i in 1 until specificFieldsCount) {
                               data.remove("field_${i}_defined")
                               data.remove("field_${i}_name")
                               data.remove("field_${i}_label")
                           }

                       } else { // any file to copy in project
                           val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath, formPath))
                           if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                               println("An error occurred while copying template files with target : ${newFile.absolutePath}")
                               exitProcess(COPY_TEMPLATE_FILE_ERROR)
                           }
                       }
                   }
           }
       }
   }

    private fun applyTemplate(newPath: String) {
        val newFile = File(newPath.replaceXmlTxtSuffix())
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
                queryList.add(Query(dataModel.name.condenseSpaces(), query))
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

    fun readFileDirectlyAsText(file: File): String
            = file.readText(Charsets.UTF_8)

    fun replaceTemplateText(oldFormText: String, formType: FormType): String {

        val variableType: String
        val formatPath: String = "<import type=\"com.qmobile.qmobileui.utils.Format\" />\n"
        val typeChoicePath: String = "<import type=\"com.qmobile.qmobileui.utils.TypeChoice\" />\n"

        if (formType == FormType.LIST) {
            variableType = "{{package}}.data.model.entity.{{tableName}}"
            variableFieldPath = "entityData"
            variableName = "entityData"
        } else {
            variableType = "{{package}}.viewmodel.entity.EntityViewModel{{tableName}}"
            variableFieldPath = "viewModel.entity"
            variableName = "viewModel"
        }

        var newFormText = oldFormText.replace("<!--FOR_EACH_FIELD-->", "{{#form_fields}}")
            .replace("<!--END_FOR_EACH_FIELD-->", "{{/form_fields}}")
            .replace("<!--IF_IS_RELATION-->", "{{#isRelation}}")
            .replace("<!--END_IF_IS_RELATION-->", "{{/isRelation}}")
            .replace("<!--IF_IS_NOT_RELATION-->", "{{^isRelation}}")
            .replace("<!--END_IF_IS_NOT_RELATION-->", "{{/isRelation}}")
            .replace("<!--IF_IS_IMAGE-->", "{{#isImage}}")
            .replace("<!--END_IF_IS_IMAGE-->", "{{/isImage}}")
            .replace("<!--IF_IS_NOT_IMAGE-->", "{{^isImage}}")
            .replace("<!--END_IF_IS_NOT_IMAGE-->", "{{/isImage}}")
            .replace("__LABEL_ID__", "{{tableName_lowercase}}_field_label_{{viewId}}")
            .replace("__VALUE_ID__", "{{tableName_lowercase}}_field_value_{{viewId}}")
            .replace("__BUTTON_ID__", "{{tableName_lowercase}}_field_button_{{viewId}}")
            .replace("android:text=\"__LABEL__\"", "android:text=\"{{label}}\"")
            .replace("android:text=\"__BUTTON__\"", "android:text=\"{{label}}\"")

        var regex = ("(\\h*)app:imageUrl=\"__IMAGE__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}app:imageFieldName='@{\"{{name}}\"}'\n" +
                    "${indent}app:imageKey=\"@{${variableFieldPath}.__KEY}\"\n" +
                    "${indent}app:imageTableName='@{\"{{tableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{${variableFieldPath}.{{name}}.__deferred.uri}\""
        }

        regex = ("(\\h*)android:text=\"__TEXT__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}android:text=\"@{${variableFieldPath}.{{name}}.toString()}\""
        }

        regex = ("(\\h*)<!--ENTITY_VARIABLE-->").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}$formatPath" + "${indent}$typeChoicePath" +
                    "${indent}<variable\n" +
                    "${indent}\tname=\"${variableName}\"\n" +
                    "${indent}\ttype=\"${variableType}\"/>"
        }

        regex = ("__SPECIFIC_ID_(\\d+)__").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val id = matchResult.destructured.component1()
            "{{tableName_lowercase}}_field_value_${id}"
        }

        regex = ("(\\h*)android:text=\"__BUTTON_(\\d+)__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}android:text=\"{{label}}\""
        }

        regex = ("(\\h*)android:text=\"__TEXT_(\\d+)__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            val id = matchResult.destructured.component2()
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}android:text=\"@{${variableFieldPath}.{{field_${id}_name}}.toString()}\"\n" +
                    "${indent}{{/field_${id}_defined}}"
        }

        regex = ("(\\h*)android:progress=\"__PROGRESS_(\\d+)__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            val id = matchResult.destructured.component2()
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}android:progress=\"@{${variableFieldPath}.{{field_${id}_name}} != null ? ${variableFieldPath}.{{field_${id}_name}} : 0}\"\n" +
                    "${indent}{{/field_${id}_defined}}"
        }

        regex = ("(\\h*)android:text=\"__LABEL_(\\d+)__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            val id = matchResult.destructured.component2()
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}android:text=\"{{field_${id}_label}}\"\n" +
                    "${indent}{{/field_${id}_defined}}"

        }

        regex = ("(\\h*)app:imageUrl=\"__IMAGE_(\\d+)__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            val id = matchResult.destructured.component2()
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}app:imageFieldName='@{\"{{field_${id}_name}}\"}'\n" +
                    "${indent}app:imageKey=\"@{${variableFieldPath}.__KEY}\"\n" +
                    "${indent}app:imageTableName='@{\"{{tableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{${variableFieldPath}.{{field_${id}_name}}.__deferred.uri}\"\n" +
                    "${indent}{{/field_${id}_defined}}\n" +
                    "${indent}{{^field_${id}_defined}}\n" +
                    "${indent}app:imageDrawable=\"@{@drawable/ic_placeholder}\"\n" +
                    "${indent}{{/field_${id}_defined}}"
        }

        return newFormText
    }

    fun getLayoutManagerType(formPath: String): String {
        println("getLayoutManagerType: $formPath")

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