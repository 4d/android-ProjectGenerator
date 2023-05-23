//DEPS com.samskivert:jmustache:1.15
//DEPS com.squareup.retrofit2:converter-gson:2.9.0

@file:DependsOnMaven("com.github.ajalt.clikt:clikt-jvm:3.2.0")
@file:DependsOnMaven("org.codehaus.groovy:groovy-sql:3.0.9")
@file:DependsOnMaven("org.xerial:sqlite-jdbc:3.36.0.3")
@file:DependsOnMaven("org.json:json:20210307")
@file:DependsOn("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.21")

//@file:DependsOnMaven("com.google.android:android:4.1.1.4")
//@file:DependsOnMaven("commons-io:commons-io:2.8.0")
//@file:DependsOnMaven("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.5.31")
//@file:DependsOnMaven("org.graalvm.compiler:compiler:21.3.0")

@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
//@file:CompilerOpts("-jvm-target 1.8")

@file:Import("Includes.kt")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import java.io.File

class GenerateCommand : CliktCommand(name = "generate") {

    private lateinit var FILES_TO_COPY: String
    private lateinit var TEMPLATE_FILES: String
    private lateinit var TEMPLATE_FORMS: String
    private lateinit var HOST_DB: String
    private lateinit var projectEditorJson: File
    private lateinit var catalogJson: File

    val projectEditor: String by option(help = projectEditorText).prompt(projectEditorText).validate {
        projectEditorJson = File(it)
        require(projectEditorJson.exists()) { "Can't find project editor json file ${projectEditorJson.name}" }
    }
    val filesToCopy: String by option(help = filesToCopyText).prompt(filesToCopyText).validate {
        val filesToCopyDir = File(it)
        require(filesToCopyDir.exists() && filesToCopyDir.isDirectory) { "Can't find files to copy directory $it" }
        FILES_TO_COPY = filesToCopy.removeSuffix("/")
    }
    val templateFiles: String by option(help = templateFilesText).prompt(templateFilesText).validate {
        val templateFilesDir = File(it)
        require(templateFilesDir.exists() && templateFilesDir.isDirectory) { "Can't find template files directory $it" }
        TEMPLATE_FILES = templateFiles.removeSuffix("/")
    }
    val templateForms: String by option(help = templateFormsText).prompt(templateFormsText).validate {
        val templateFormsDir = File(it)
        require(templateFormsDir.exists() && templateFormsDir.isDirectory) { "Can't find template forms directory $it" }
        TEMPLATE_FORMS = templateForms.removeSuffix("/")
    }
    val hostDb: String by option(help = hostDbText).prompt(hostDbText).validate {
        HOST_DB = hostDb.removeSuffix("/")
    }
    val catalog: String by option(help = catalogText).prompt(catalogText).validate {
        catalogJson = File(it)
        require(catalogJson.exists()) { "Can't find catalog json file ${catalogJson.name}" }
    }

    override fun run() {
        println("Parameters checked.")
        println("Version: ${Version.VALUE}")
        println("Starting procedure...")
        start()
        println("Procedure complete.")
    }

    private fun start() {

        println("file: ${projectEditorJson.path}...")

        val catalogDef = CatalogDef(catalogJson)

        println("Reading catalog json file done.")
        println("--------------------------------------")

        val projectEditor = ProjectEditor(projectEditorFile = projectEditorJson, catalogDef = catalogDef)

        println("Reading project editor json file done.")
        println("--------------------------------------")

        var targetDirPath = ""
        projectEditor.findJsonString("targetDirPath")?.let {
            targetDirPath = it
        } ?: run {
            val targetDirPathFromEnv = System.getenv("TARGET_PATH")
            if (!targetDirPathFromEnv.isNullOrEmpty())
                targetDirPath = targetDirPathFromEnv
        }
        if (targetDirPath.isEmpty()) {
            throw Exception("No target directory. Define env var `TARGET_PATH` or pass it in project JSON editor ")
        }

        val pathHelper = PathHelper(
                targetDirPath = targetDirPath.removeSuffix("/"),
                templateFilesPath = TEMPLATE_FILES,
                templateFormsPath = TEMPLATE_FORMS,
                hostDb = HOST_DB,
                filesToCopy = FILES_TO_COPY,
                companyWithCaps = projectEditor.findJsonString("companyWithCaps") ?: DEFAULT_COMPANY,
                appNameWithCaps = projectEditor.findJsonString("appNameWithCaps") ?: DEFAULT_APPLICATION,
                pkg = projectEditor.findJsonString("package") ?: DEFAULT_PACKAGE
        )

        val fileHelper = FileHelper(pathHelper)

        println("Start gathering Mustache templating data...")

        val mustacheHelper = MustacheHelper(fileHelper, projectEditor)

        println("Gathering Mustache templating data done.")
        println("----------------------------------------")

        fileHelper.copyFiles()

        println("Files successfully copied.")

        fileHelper.createPathDirectories()

        println("Start applying Mustache templating...")

        mustacheHelper.applyListFormTemplate()

        println("Applied List Form Templates")

        mustacheHelper.applyDetailFormTemplate()

        println("Applied Detail Form Templates")

        pathHelper.deleteTemporaryUnzippedDirectories()

        println("Deleted Temporary Unzipped Directories")

        mustacheHelper.processTemplates()

        println("Mustache templating done.")

        mustacheHelper.copyFilesAfterGlobalTemplating()

        println("Copied remaining files after templating")

        println("-------------------------")

        mustacheHelper.makeTableInfo()

        println("\"$TABLE_INFO_FILENAME\" file successfully generated.")

        mustacheHelper.makeAppInfo()

        println("\"$APP_INFO_FILENAME\" file successfully generated.")

        mustacheHelper.makeCustomFormatters()

        println("\"$CUSTOM_FORMATTERS_FILENAME\" file successfully generated.")

        mustacheHelper.makeInputControls()

        println("\"$INPUT_CONTROLS_FILENAME\" file successfully generated.")

        mustacheHelper.makeActions()

        println("\"$ACTIONS_FILENAME\" file successfully generated.")

        println("Output: ${projectEditor.findJsonString("targetDirPath")}")

        println("data:")
        logData(mustacheHelper.data)
    }
}

class CreateDatabaseCommand : CliktCommand(name = "createDatabase") {

    private lateinit var ASSETS: String
    private lateinit var DBFILEPATH: String
    private lateinit var projectEditorJson: File
    private lateinit var catalogJson: File

    val projectEditor: String by option(help = projectEditorText).prompt(projectEditorText).validate {
        projectEditorJson = File(it)
        require(projectEditorJson.exists()) { "Can't find project editor json file ${projectEditorJson.name}" }
    }
    val assets: String by option(help = assetsText).prompt(assetsText).validate {
        val assetsDir = File(it)
        require(assetsDir.exists() && assetsDir.isDirectory) { "Can't find assets directory $it" }
        ASSETS = assets.removeSuffix("/")
    }
    val dbFile: String by option(help = dbText).prompt(dbText).validate {
        val dbParentDir = File(it).parentFile
        dbParentDir.mkdirs()
        require(dbParentDir.exists() && dbParentDir.isDirectory) { "Can't find db's parent directory directory $dbParentDir" }
        DBFILEPATH = dbFile.removeSuffix("/")
    }
    val catalog: String by option(help = catalogText).prompt(catalogText).validate {
        catalogJson = File(it)
        require(catalogJson.exists()) { "Can't find catalog json file ${catalogJson.name}" }
    }

    override fun run() {
        println("Parameters checked.")
        println("Version: ${Version.VALUE}")
        println("Starting procedure...")
        start()
        println("Procedure complete.")
    }

    private fun start() {

        println("file: ${projectEditorJson.path}...")

        val catalogDef = CatalogDef(catalogJson)

        println("Reading catalog json file done.")
        println("--------------------------------------")

        val projectEditor = ProjectEditor(projectEditorFile = projectEditorJson, catalogDef = catalogDef, isCreateDatabaseCommand = true)

        println("Reading project editor json file done.")
        println("--------------------------------------")

        CreateDatabaseTask(projectEditor.dataModelList, ASSETS, DBFILEPATH)

        println("Output: $DBFILEPATH}")
    }
}

class Main : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) = Main().subcommands(VersionCommand(), GenerateCommand(), CreateDatabaseCommand()).main(args)
