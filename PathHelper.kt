import DefaultValues.DEFAULT_DETAIL_FORM
import DefaultValues.DEFAULT_LIST_FORM
import ExitCodes.MISSING_TARGET_DIR
import PathHelperConstants.APP_PATH_KEY
import PathHelperConstants.ASSETS_PATH_KEY
import PathHelperConstants.DETAIL_FORMS_KEY
import PathHelperConstants.DETAIL_FORM_PREFIX
import PathHelperConstants.HOST_FORMS
import PathHelperConstants.JAVA_PATH_KEY
import PathHelperConstants.LAYOUT_PATH_KEY
import PathHelperConstants.LIST_FORMS_KEY
import PathHelperConstants.MAIN_PATH_KEY
import PathHelperConstants.NAVIGATION_PATH_KEY
import PathHelperConstants.PACKAGE_JOINED_PH
import PathHelperConstants.PACKAGE_PH
import PathHelperConstants.RECYCLER_VIEW_ITEM_PREFIX
import PathHelperConstants.RES_PATH_KEY
import PathHelperConstants.SRC_PATH_KEY
import java.io.File
import java.util.zip.ZipFile
import kotlin.system.exitProcess

class PathHelper(
        val targetDirPath: String,
        val templateFilesPath: String,
        val templateFormsPath: String,
        val hostDb: String,
        val filesToCopy: String,
        val companyWithCaps: String,
        val appNameWithCaps: String,
        val pkg: String
) {

//    val companyCondensed = companyWithCaps.condense()
//    val appNameCondensed = appNameWithCaps.condense()

    fun getPath(currentPath: String): String {
        val path = targetDirPath + replacePath(currentPath)
        return path.replaceIfWindowsPath()
    }

    fun getLayoutTemplatePath(currentPath: String, formPath: String): String {
        val path = targetDirPath + replaceLayoutTemplatePath(currentPath, formPath)
        return path.replaceIfWindowsPath()
    }

    fun replaceDirectoriesPath(path: String): String {
        return path.replace(PACKAGE_PH, pkg.replace(".", File.separator))
            .replace(PACKAGE_JOINED_PH, pkg) // for buildSrc
    }

    private fun replacePath(currentPath: String): String {
        val paths = currentPath.replaceIfWindowsPath().split(Regex(templateFilesPath))
        if (paths.size < 2) {
            println("Couldn't find target directory with path : $currentPath")
            exitProcess(MISSING_TARGET_DIR)
        }
        return replaceDirectoriesPath(paths[1])
    }

    private fun replaceLayoutTemplatePath(currentPath: String, formPath: String): String {
        val paths = currentPath.replaceIfWindowsPath().split(Regex(formPath))
        if (paths.size < 2) {
            println("Couldn't find target directory with path : $currentPath")
            exitProcess(MISSING_TARGET_DIR)
        }
        return replaceDirectoriesPath(paths[1])
    }

    val listFormTemplatesPath = templateFormsPath + File.separator + LIST_FORMS_KEY

    val detailFormTemplatesPath = templateFormsPath + File.separator + DETAIL_FORMS_KEY

    val hostFormTemplatesPath = hostDb + File.separator + HOST_FORMS

    val hostListFormTemplatesPath = hostFormTemplatesPath + File.separator + LIST_FORMS_KEY

    val hostDetailFormTemplatesPath = hostFormTemplatesPath + File.separator + DETAIL_FORMS_KEY

    private val srcPath = targetDirPath + File.separator +
            APP_PATH_KEY + File.separator +
            SRC_PATH_KEY

    fun resPath(): String {
        val resPath = srcPath + File.separator +
                MAIN_PATH_KEY + File.separator +
                RES_PATH_KEY
        return resPath.replaceIfWindowsPath()
    }

    fun navigationPath(): String {
        val navPath = resPath() + File.separator + NAVIGATION_PATH_KEY
        return navPath.replaceIfWindowsPath()
    }

    private val layoutPath = resPath() + File.separator + LAYOUT_PATH_KEY

    fun assetsPath(): String {
        val assetsPath = srcPath + File.separator +
            MAIN_PATH_KEY + File.separator +
            ASSETS_PATH_KEY
        return assetsPath.replaceIfWindowsPath()
    }

    fun getRecyclerViewItemPath(tableName: String) = layoutPath + File.separator + RECYCLER_VIEW_ITEM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getDetailFormPath(tableName: String) = layoutPath + File.separator + DETAIL_FORM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getTargetPath(dir: String): String {
        val path = srcPath + File.separator +
                dir + File.separator +
                JAVA_PATH_KEY + File.separator +
                PACKAGE_PH
        return replaceDirectoriesPath(path)
    }

    private fun isWindowsOS(): Boolean = System.getProperty("os.name").contains("Windows")

    private fun String.replaceIfWindowsPath(): String {
        return if (isWindowsOS())
            this.replace("\\", "/")
        else
            this
    }


    fun getFormPath(formName: String?, formType: FormType): String {
        return if (formName.isNullOrEmpty()) {
            if (formType == FormType.LIST) getDefaultTemplateListFormPath() else getDefaultTemplateDetailFormPath()
        } else {
            if (formType == FormType.LIST) getTemplateListFormPath(formName) else getTemplateDetailFormPath(formName)
        }
    }

    fun appFolderExistsInTemplate(formPath: String): Boolean = File(formPath + File.separator + APP_PATH_KEY).exists()

    fun getAppFolderInTemplate(formPath: String): String {
        return formPath + File.separator + APP_PATH_KEY
    }

    fun getDefaultTemplateListFormPath() = listFormTemplatesPath + File.separator + DEFAULT_LIST_FORM
    fun getDefaultTemplateDetailFormPath() = detailFormTemplatesPath + File.separator + DEFAULT_DETAIL_FORM

    fun getTemplateListFormPath(formName: String): String {
        var templatePath = ""
        if (formName.startsWith("/")) {
            templatePath = hostListFormTemplatesPath

            /*if (formName.endsWith(".zip")) {
                val zipFile = File(templatePath + File.separator + formName.removePrefix("/"))
                unzipTemplate(zipFile)
            }*/

        } else {
            templatePath = listFormTemplatesPath
        }
        return templatePath + File.separator + formName.removePrefix("/")
    }

    fun getTemplateDetailFormPath(formName: String): String {
        var templatePath = ""
        if (formName.startsWith("/")) {
            templatePath = hostDetailFormTemplatesPath

            /*if (formName.endsWith(".zip")) {
                val zipFile = File(templatePath + File.separator + formName.removePrefix("/"))
                unzipTemplate(zipFile)
            }*/

        } else {
            templatePath = detailFormTemplatesPath
        }
        return templatePath + File.separator + formName.removePrefix("/")
    }

    private fun unzipTemplate(zipFile: File) {
        ZipFile(zipFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->

                println("zip entry is : $entry")
                zip.getInputStream(entry).use { input ->

                    File(entry.name).outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}
