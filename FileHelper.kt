import ExitCodes.COPY_FILE_ERROR
import FileHelperConstants.kotlinProjectDirs
import PathHelperConstants.TEMPLATE_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_DAO_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_ENTITY_PLACEHOLDER
import PathHelperConstants.XML_TXT_EXT
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class FileHelper(val pathHelper: PathHelper) {

    /**
     * CREATE COM.COMPANY.EXAMPLE PATH DIRECTORIES
     */
    fun createPathDirectories() {
        for (dir in kotlinProjectDirs) {
            val path = pathHelper.getTargetPath(dir)
            val directories = File(pathHelper.replaceDirectoriesPath(path))
            directories.mkdirs()
        }
    }

    /**
     *  COPY NON MODIFIED FILES
     */
    fun copyFiles() {
        val sourceFolder = File(pathHelper.filesToCopy)
        val targetFolder = File(pathHelper.targetDirPath)
        if (targetFolder.exists()) {
            println("SDK files already exist in target path, will try to delete previous build files.")
            if (targetFolder.deleteRecursively()) {
                println("Old project files successfully deleted.")
            } else {
                println("Could not delete old project files.")
            }
        }
        targetFolder.mkdir()
        if (!sourceFolder.copyRecursively(target = targetFolder, overwrite = true)) {
            println("An error occurred while copying files with target folder : ${targetFolder.absolutePath}")
            exitProcess(COPY_FILE_ERROR)
        }

        renameTxtXmlFiles(targetFolder)
    }

    private fun renameTxtXmlFiles(targetFolder: File) {
        targetFolder.walkTopDown().filter { folder -> !folder.isHidden && folder.isDirectory }
            .forEach { currentFolder ->

                currentFolder.walkTopDown()
                    .filter { file -> !file.isHidden && file.isFile && file.absolutePath.endsWith(XML_TXT_EXT) }
                    .forEach { currentTxtXmlFile ->
                        val newFile = File(currentTxtXmlFile.absolutePath.replaceXmlTxtSuffix())
                        currentTxtXmlFile.renameTo(newFile)
                    }
            }
    }
}

fun File.isWithTemplateName() = this.name.contains(TEMPLATE_PLACEHOLDER)

fun File.isWithRelationDaoTemplateName() = this.name.contains(TEMPLATE_RELATION_DAO_PLACEHOLDER)

fun File.isWithRelationEntityTemplateName() = this.name.contains(TEMPLATE_RELATION_ENTITY_PLACEHOLDER)

fun File.readFile(): String {
    return this.bufferedReader().use {
        it.readText()
    }
}

fun getTemplateManifest(formPath: String): File = File(formPath + File.separator + "manifest.json")

fun getTemplateManifestJSONContent(formPath: String): JSONObject? {
    val manifest = getTemplateManifest(formPath)
    return if (manifest.exists()) {
        val jsonString = manifest.readFile()
        retrieveJSONObject(jsonString)
    } else
        null
}