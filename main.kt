//DEPS com.samskivert:jmustache:1.15
//DEPS com.squareup.retrofit2:converter-gson:2.9.0

@file:DependsOnMaven("com.github.ajalt.clikt:clikt-jvm:3.0.1")
@file:DependsOnMaven("org.json:json:20180813")
@file:DependsOnMaven("com.google.android:android:4.1.1.4")

@file:KotlinOpts("-J-Xmx5g")
@file:KotlinOpts("-J-server")
@file:CompilerOpts("-jvm-target 1.8")

//INCLUDE utils/Includes.kt

import DefaultValues.DEFAULT_APPLICATION
import DefaultValues.DEFAULT_COMPANY
import DefaultValues.DEFAULT_PREFIX
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.validate
import java.io.File

class Main : CliktCommand() {

    private lateinit var FILES_TO_COPY: String
    private lateinit var TEMPLATE_FILES: String
    private lateinit var TEMPLATE_FORMS: String
    private lateinit var HOST_DB: String
    private lateinit var projectEditorJson: File

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
        val hostDbDir = File(it)
        require(hostDbDir.exists() && hostDbDir.isDirectory) { "Can't find host database directory $it" }
        HOST_DB = hostDb.removeSuffix("/")
    }

    override fun run() {
        println("Parameters checked.")
        println("Starting procedure...")
        start()
        println("Procedure complete.")
    }

    private fun start() {

        println("Start reading project editor json file ${projectEditorJson.name}...")

        val projectEditor = ProjectEditor(projectEditorJson)

        println("Reading project editor json file done.")
        println("--------------------------------------")

        val pathHelper = PathHelper(
                targetDirPath = projectEditor.findJsonString("targetDirPath")?.removeSuffix("/") ?: "",
                templateFilesPath = TEMPLATE_FILES,
                templateFormsPath = TEMPLATE_FORMS,
                hostDb = HOST_DB,
                filesToCopy = FILES_TO_COPY,
                prefix = DEFAULT_PREFIX,
                companyWithCaps = projectEditor.findJsonString("companyWithCaps") ?: DEFAULT_COMPANY,
                appNameWithCaps = projectEditor.findJsonString("appNameWithCaps") ?: DEFAULT_APPLICATION
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

        mustacheHelper.applyTemplates()

        mustacheHelper.applyListFormTemplate()

        mustacheHelper.applyDetailFormTemplate()

        println("Mustache templating done.")
        println("-------------------------")

        mustacheHelper.makeQueries()

        println("Queries file successfully generated.")

        mustacheHelper.makeAppInfo()

        println("AppInfo file successfully generated.")
    }
}

fun main(args: Array<String>) = Main().main(args)
