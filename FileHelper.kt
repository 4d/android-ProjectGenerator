import ExitCodes.COPY_FILE_ERROR
import FileHelperConstants.kotlinProjectDirs
import PathHelperConstants.TEMPLATE_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_DAO_PLACEHOLDER
import PathHelperConstants.TEMPLATE_RELATION_ENTITY_PLACEHOLDER
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
