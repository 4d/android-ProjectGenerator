//DEPS com.samskivert:jmustache:1.15
//DEPS com.squareup.retrofit2:converter-gson:2.9.0

@file:DependsOnMaven("com.github.ajalt.clikt:clikt-jvm:3.2.0")
@file:DependsOnMaven("org.json:json:20210307")
@file:DependsOnMaven("com.google.android:android:4.1.1.4")

@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:CompilerOpts("-jvm-target 1.8")

//INCLUDE utils/Includes.kt

import DefaultValues.DEFAULT_APPLICATION
import DefaultValues.DEFAULT_COMPANY
import DefaultValues.DEFAULT_PACKAGE
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
    init {
        Log.plantTree(this::class.java.canonicalName)
    }

    val projectEditor: String by option(help = Clikt.projectEditorText).prompt(Clikt.projectEditorText).validate {
        projectEditorJson = File(it)
        require(projectEditorJson.exists()) { "Can't find project editor json file ${projectEditorJson.name}" }
    }
    val filesToCopy: String by option(help = Clikt.filesToCopyText).prompt(Clikt.filesToCopyText).validate {
        val filesToCopyDir = File(it)
        require(filesToCopyDir.exists() && filesToCopyDir.isDirectory) { "Can't find files to copy directory $it" }
        FILES_TO_COPY = filesToCopy.removeSuffix("/")
    }
    val templateFiles: String by option(help = Clikt.templateFilesText).prompt(Clikt.templateFilesText).validate {
        val templateFilesDir = File(it)
        require(templateFilesDir.exists() && templateFilesDir.isDirectory) { "Can't find template files directory $it" }
        TEMPLATE_FILES = templateFiles.removeSuffix("/")
    }
    val templateForms: String by option(help = Clikt.templateFormsText).prompt(Clikt.templateFormsText).validate {
        val templateFormsDir = File(it)
        require(templateFormsDir.exists() && templateFormsDir.isDirectory) { "Can't find template forms directory $it" }
        TEMPLATE_FORMS = templateForms.removeSuffix("/")
    }
    val hostDb: String by option(help = Clikt.hostDbText).prompt(Clikt.hostDbText).validate {
//        val hostDbDir = File(it)
//        require(hostDbDir.exists() && hostDbDir.isDirectory) { "Can't find host database directory $it" }
        HOST_DB = hostDb.removeSuffix("/")
    }

    override fun run() {
        Log.d("Parameters checked.")
        Log.d("Version: ${Version.VALUE}")
        Log.i("Starting procedure...")
        start()
        Log.i("Procedure complete.")
    }

    private fun start() {

        Log.d("e ${projectEditorJson.name}...")

        val projectEditor = ProjectEditor(projectEditorJson)

        Log.d("Reading project editor json file done.")
        Log.v("--------------------------------------")

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

        Log.d("Start gathering Mustache templating data...")

        val mustacheHelper = MustacheHelper(fileHelper, projectEditor)

        Log.d("Gathering Mustache templating data done.")
        Log.v("----------------------------------------")

        fileHelper.copyFiles()

        Log.d("Files successfully copied.")

        fileHelper.createPathDirectories()

        Log.i("Start applying Mustache templating...")

        mustacheHelper.applyListFormTemplate()

        mustacheHelper.applyDetailFormTemplate()

        pathHelper.deleteTemporaryUnzippedDirectories()

        mustacheHelper.processTemplates()

        Log.i("Mustache templating done.")
        Log.v("-------------------------")

        mustacheHelper.makeQueries()

        Log.i("\"queries.json\" file successfully generated.")

        mustacheHelper.makeAppInfo()

        Log.i("\"app_info.json\" file successfully generated.")

        mustacheHelper.makeCustomFormatters()

        Log.i("\"custom_formatters.json\" file successfully generated.")

        mustacheHelper.makeSearchableFields()

        Log.i("\"searchable_fields.json\" file successfully generated.")

        Log.d("Output: ${projectEditor.findJsonString("targetDirPath")}")
    }
}

class Main : CliktCommand() {
    override fun run() {}
}

fun main(args: Array<String>) = Main().subcommands(VersionCommand(), GenerateCommand()).main(args)
