import DefaultValues.DEFAULT_AUTHOR
import DefaultValues.DEFAULT_PREFIX
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
import MustacheConstants.APP_NAME
import MustacheConstants.APP_NAME_WITH_CAPS
import MustacheConstants.AUTHOR
import MustacheConstants.CACHE_4D_SDK_PATH
import MustacheConstants.COMPANY
import MustacheConstants.COMPANY_HEADER
import MustacheConstants.DATE_DAY
import MustacheConstants.DATE_MONTH
import MustacheConstants.DATE_YEAR
import MustacheConstants.ENTITY_CLASSES
import MustacheConstants.FIELDS
import MustacheConstants.FIRST_FIELD
import MustacheConstants.FORM_FIELDS
import MustacheConstants.PREFIX
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
import java.util.Calendar
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

    init {
        data[COMPANY_HEADER] = fileHelper.pathHelper.companyWithCaps
        data[AUTHOR] = projectEditor.findJsonString("author") ?: DEFAULT_AUTHOR
        data[DATE_DAY] = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
        data[DATE_MONTH] = (Calendar.getInstance().get(Calendar.MONTH) + 1).toString()
        data[DATE_YEAR] = Calendar.getInstance().get(Calendar.YEAR).toString()
        data[PREFIX] = DEFAULT_PREFIX
        data[COMPANY] = fileHelper.pathHelper.companyCondensed
        data[APP_NAME] = fileHelper.pathHelper.appNameCondensed
        data[APP_NAME_WITH_CAPS] = fileHelper.pathHelper.appNameWithCaps

        // for network_security_config.xml
        // whitelist production host address if defined, else, server host address, else localhost
        var remoteAddress =  projectEditor.findJsonString("productionUrl")
        println("remoteAddress = $remoteAddress")
        if (remoteAddress.isNullOrEmpty())
            remoteAddress = projectEditor.findJsonString("remoteUrl")
            if (remoteAddress.isNullOrEmpty())
                remoteAddress = DEFAULT_REMOTE_URL
        data[REMOTE_ADDRESS] = remoteAddress.removePrefix("https://").removePrefix("http://").split(":")[0]
        println("data[REMOTE_ADDRESS] = ${data[REMOTE_ADDRESS]}")

        projectEditor.findJsonString("androidSdk")?.let {
            data[ANDROID_SDK_PATH] = it
        } ?: run {
            println("Missing Android SDK path")
            exitProcess(MISSING_ANDROID_SDK_PATH)
        }
        println("> Android SDK = ${data[ANDROID_SDK_PATH]}")

        projectEditor.findJsonString("cache4dSdk")?.let {
            data[CACHE_4D_SDK_PATH] = it
        } ?: run {
            println("Missing 4D Mobile cache SDK path")
            exitProcess(MISSING_ANDROID_CACHE_SDK_PATH)
        }
        println("> Cache 4D SDK = ${data[CACHE_4D_SDK_PATH]}")

        var entityClassesString = ""

        val dataModelRelationList = mutableListOf<TemplateRelationFiller>()

        projectEditor.dataModelList.forEach { dataModel ->

            dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                dataModelRelationList.add(TemplateRelationFiller(relation_source = relation.source, relation_target = relation.target, relation_name = relation.name.condensePropertyName()))
            }

            tableNames.add(TemplateTableFiller(name = dataModel.name))
            tableNames_lowercase.add(
                TemplateLayoutFiller(
                    name = dataModel.name,
                    nameLowerCase = dataModel.name.toLowerCase(),
                    nameCamelCase = dataModel.name.capitalizeWords(),
                    hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                    icon = dataModel.iconPath ?: ""))
            entityClassesString += "${dataModel.name}::class, "
        }

        val tableNames_without_relations = mutableListOf<TemplateTableFiller>()

        tableNames.forEach { tableName ->
            if (!dataModelRelationList.map { it.relation_source }.contains(tableName.name))
                tableNames_without_relations.add(tableName)
        }

        data[TABLENAMES] = tableNames
        data[TABLENAMES_LOWERCASE] = tableNames_lowercase
        data[TABLENAMES_RELATIONS] = dataModelRelationList
        data[TABLENAMES_WITHOUT_RELATIONS] = tableNames_without_relations
        data[TABLENAMES_RELATIONS_DISTINCT] = dataModelRelationList.distinctBy { it.relation_source to it.relation_target }
        data[ENTITY_CLASSES] = entityClassesString.dropLast(2)

        val typesAndTables = mutableListOf<TemplateTableFiller>()
        typesAndTables.addAll(tableNames)
        typesAndTables.add(TemplateTableFiller(name = "Photo"))
        typesAndTables.add(TemplateTableFiller(name = "Date"))
        typesAndTables.add(TemplateTableFiller(name = "Time"))
        data[TYPES_AND_TABLES] = typesAndTables

        var navigationTableCounter = 0 // Counter to limit to 4 navigation tables as it is not possible to have more than 5
        projectEditor.navigationTableList.forEach { navigationTableId ->
            projectEditor.dataModelList.find { it.id == navigationTableId }?.let { dataModel ->
                if (navigationTableCounter > 3)
                    return@forEach
                tableNamesForNavigation.add(
                    TemplateLayoutFiller(
                        name = dataModel.name,
                        nameLowerCase = dataModel.name.toLowerCase(),
                        nameCamelCase = dataModel.name.capitalizeWords(),
                        hasIcon = (dataModel.iconPath != null && dataModel.iconPath != ""),
                        icon = dataModel.iconPath ?: ""))
                navigationTableCounter++
            }
        }
        data[TABLENAMES_NAVIGATION] = tableNamesForNavigation

        // Specifying if list layout is table or collection (LinearLayout or GridLayout)
        tableNamesForNavigation.map { it.name }.forEach { tableName ->
            val listFormName = projectEditor.listFormList.find { listform -> listform.dataModel.name == tableName }?.name
            val formPath = fileHelper.pathHelper.getFormPath(listFormName, FormType.LIST)
            tableNamesForLayoutType.add(TemplateLayoutTypeFiller(name = tableName, layout_manager_type = getLayoutManagerType(formPath)))
        }
//        projectEditor.listFormList.forEach { listForm ->
//            val formPath = fileHelper.pathHelper.getFormPath(listForm.name, FormType.LIST)
//            tableNamesForLayoutType.add(TemplateLayoutTypeFiller(name = listForm.dataModel.name, layout_manager_type = getLayoutManagerType(formPath)))
//        }
        data[TABLENAMES_LAYOUT] = tableNamesForLayoutType
        println("data[TABLENAMES_LAYOUT] = ${data[TABLENAMES_LAYOUT]}")
    }

    /**
     * TEMPLATING
     */
    fun applyTemplates() {
        File(fileHelper.pathHelper.templateFilesPath).walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }.forEach { currentFolder ->

            compiler = generateCompilerFolder(currentFolder.absolutePath)

            currentFolder.walkTopDown().filter { file -> !file.isHidden && file.isFile && currentFolder.absolutePath.contains(file.parent) }.forEach { currentFile ->

                println(" > Processed file : $currentFile")

                template = compiler.compile("{{>${currentFile.name}}}")

                val newFilePath = fileHelper.pathHelper.getPath(currentFile.absolutePath.replaceXmlTxtSuffix())

                relations.clear()
                projectEditor.dataModelList.forEach { dataModel ->
                    dataModel.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->
                        relations.add(TemplateRelationFiller(relation_source = relation.source, relation_target = relation.target, relation_name = relation.name.condensePropertyName()))
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

                        data[TABLENAME] = tableName.name
                        data[TABLENAME_LOWERCASE] = tableName.name.toLowerCase()
                        projectEditor.dataModelList.find { it.name == tableName.name }?.fields?.let { fields ->
                            val fieldList = mutableListOf<TemplateFieldFiller>()
                            for (field in fields) {
                                field.fieldTypeString?.let { fieldTypeString ->
                                    fieldList.add(
                                            TemplateFieldFiller(
                                                    name = field.name.condensePropertyName(),
                                                    fieldTypeString = fieldTypeString,
                                                    variableType = field.variableType,
                                                    name_original = field.name
                                            )
                                    )

                                } ?: kotlin.run {
                                    println("An error occurred while parsing the fieldType of field : $field")
                                    exitProcess(FIELD_TYPE_ERROR)
                                }
                            }
                            data[FIELDS] = fieldList
                            data[FIRST_FIELD] = fieldList.firstOrNull()?.name ?: ""
                        }

                        relations.clear()
                        val relationsImport = mutableListOf<TemplateRelationFiller>() // need another list, to remove double in import section

                        projectEditor.dataModelList.find { it.name == tableName.name }?.relationList?.filter { it.relationType == RelationType.MANY_TO_ONE }?.forEach { relation ->

                            relations.add(TemplateRelationFiller(relation_source = relation.source, relation_target = relation.target, relation_name = relation.name.condensePropertyName()))

                            var isAlreadyImported = false
                            for (relationImport in relationsImport) {
                                if (relationImport.relation_source == relation.source && relationImport.relation_target == relation.target)
                                    isAlreadyImported = true
                            }
                            if (!isAlreadyImported)
                                relationsImport.add(TemplateRelationFiller(relation_source = relation.source, relation_target = relation.target, relation_name = relation.name.condensePropertyName())) // name is unused
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
                            data[RELATION_NAME] = relation.name.condensePropertyName()
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

                        println(" > Processed template file : $currentFile")

                        template = compiler.compile("{{>${currentFile.name}}}")

                        if (currentFile.name == LAYOUT_FILE) {
                            val oldFormText = readFileDirectlyAsText(currentFile)
                            val newFormText = replaceTemplateText(oldFormText, FormType.LIST)
                            template = compiler.compile(newFormText)

                            data[TABLENAME_LOWERCASE] = listForm.dataModel.name.toLowerCase()
                            data[TABLENAME] = listForm.dataModel.name

                            var i = 0
                            listForm.fields?.forEach { field -> // Could also iter over specificFieldsCount as Detail form
                                i++
                                data["field_${i}_defined"] = field.name.isNotEmpty()
                                data["field_${i}_name"] = field.name.condensePropertyName()
                                data["field_${i}_label"] = field.label ?: ""
                            }

                            val newFilePath = fileHelper.pathHelper.getRecyclerViewItemPath(listForm.dataModel.name)
                            applyTemplate(newFilePath)

                            // cleaning data for other templates
                            for (j in 1 until i + 1) {
                                data.remove("field_${j}_defined")
                                data.remove("field_${j}_name")
                                data.remove("field_${j}_label")
                            }
                        } else { // any file to copy in project
                            val newFile = File(fileHelper.pathHelper.getLayoutTemplatePath(currentFile.absolutePath, formPath))
                            println("File to copy : ${currentFile.absolutePath}; target : ${newFile.absolutePath}")
                            if (!currentFile.copyRecursively(target = newFile, overwrite = true)) {
                                println("An error occurred while copying template files with target : ${newFile.absolutePath}")
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

                       println(" > Processed template file : $currentFile")

                       template = compiler.compile("{{>${currentFile.name}}}")

                       if (currentFile.name == LAYOUT_FILE) {
                           val oldFormText = readFileDirectlyAsText(currentFile)
                           val newFormText = replaceTemplateText(oldFormText, FormType.DETAIL)

                           template = compiler.compile(newFormText)

                           data[TABLENAME_LOWERCASE] = detailForm.dataModel.name.toLowerCase()
                           data[TABLENAME] = detailForm.dataModel.name

                           val formFieldList = mutableListOf<TemplateFormFieldFiller>()

                           detailForm.fields?.let { fieldList ->

                               if (fieldList.isNotEmpty()) {

                                   if (specificFieldsCount == 0) { // template with no specific field
                                       for (i in fieldList.indices) {
                                           formFieldList.add(createFormField(fieldList[i], i + 1))
                                       }
                                   } else { // template with specific fields

                                       for (i in 0 until specificFieldsCount) {
                                           data["field_${i + 1}_defined"] = fieldList[i].name.isNotEmpty()
                                           data["field_${i + 1}_name"] = fieldList[i].name.condensePropertyName()
                                           data["field_${i + 1}_label"] = fieldList[i].label ?: ""
                                       }

                                       if (fieldList.size > specificFieldsCount + 1) {
                                           for (i in specificFieldsCount + 1 until fieldList.size) {
                                               formFieldList.add(createFormField(fieldList[i], i))
                                           }
                                       } else {
                                           // no additional field given
                                       }
                                   }

                               }
                               data[FORM_FIELDS] = formFieldList
                           }

                           val newFilePath = fileHelper.pathHelper.getDetailFormPath(detailForm.dataModel.name)
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
            println("An error occurred while creating new file : $newFile")
            exitProcess(FILE_CREATION_ERROR)
        }
        newFile.writeText(template.execute(data))
    }

    fun makeQueries() {
        val queryList = mutableListOf<Query>()
        projectEditor.dataModelList.forEach { dataModel ->
            dataModel.query?.let { query ->
                queryList.add(Query(dataModel.name, query))
            }
        }
        val queries = Queries(queryList)

        val queriesFile = File(fileHelper.pathHelper.assetsPath(), QUERIES_FILENAME)
        queriesFile.parentFile.mkdirs()
        if (!queriesFile.createNewFile()) {
            println("An error occurred while creating new file : $queriesFile")
            exitProcess(FILE_CREATION_ERROR)
        }
        queriesFile.writeText(gson.toJson(queries))
    }

    fun makeAppInfo() {
        val appInfoFile = File(fileHelper.pathHelper.assetsPath(), APP_INFO_FILENAME)
        appInfoFile.parentFile.mkdirs()
        if (!appInfoFile.createNewFile()) {
            println("An error occurred while creating new file : $appInfoFile")
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
        val variableFieldPath: String
        val variableName: String
        if (formType == FormType.LIST) {
            variableType = "{{prefix}}.{{company}}.{{app_name}}.data.model.entity.{{tableName}}"
            variableFieldPath = "entityData"
            variableName = "entityData"
        } else {
            variableType = "{{prefix}}.{{company}}.{{app_name}}.viewmodel.entity.EntityViewModel{{tableName}}"
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
            .replace("android:text=\"__TEXT__\"", "android:text=\"@{${variableFieldPath}.{{name}}.toString()}\"")

        var regex = ("(\\h*)app:imageUrl=\"__IMAGE__\"").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}app:imageFieldName='@{\"{{name}}\"}'\n" +
            "${indent}app:imageKey=\"@{${variableFieldPath}.__KEY}\"\n" +
            "${indent}app:imageTableName='@{\"{{tableName}}\"}'\n" +
            "${indent}app:imageUrl=\"@{${variableFieldPath}.{{name}}.__deferred.uri}\""
        }

        regex = ("(\\h*)<!--ENTITY_VARIABLE-->").toRegex()
        newFormText = regex.replace(newFormText) { matchResult ->
            val indent = matchResult.destructured.component1()
            "${indent}<variable\n" +
            "${indent}\tname=\"${variableName}\"\n" +
            "${indent}\ttype=\"${variableType}\" />"
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
