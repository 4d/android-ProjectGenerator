import ExitCodes.MISSING_TARGET_DIR
import PathHelperConstants.APP_PATH_KEY
import PathHelperConstants.ASSETS_PATH_KEY
import PathHelperConstants.COMPANY_PH
import PathHelperConstants.DETAIL_FORMS_KEY
import PathHelperConstants.DETAIL_FORM_PREFIX
import PathHelperConstants.DRAWABLE_PATH_KEY
import PathHelperConstants.JAVA_PATH_KEY
import PathHelperConstants.LAYOUT_PATH_KEY
import PathHelperConstants.LIST_FORMS_KEY
import PathHelperConstants.MAIN_PATH_KEY
import PathHelperConstants.NAVIGATION_PATH_KEY
import PathHelperConstants.PACKAGE_PH
import PathHelperConstants.PREFIX_PH
import PathHelperConstants.RECYCLER_VIEW_ITEM_PREFIX
import PathHelperConstants.RES_PATH_KEY
import PathHelperConstants.SRC_PATH_KEY
import java.io.File
import kotlin.system.exitProcess

class PathHelper(
        val targetDirPath: String,
        val templateFilesPath: String,
        val templateFormsPath: String,
        val filesToCopy: String,
        val prefix: String,
        val companyWithCaps: String,
        val appNameWithCaps: String
) {

    val companyCondensed = companyWithCaps.condense()
    val appNameCondensed = appNameWithCaps.condense()

    fun getPath(currentPath: String): String {
        return targetDirPath + replacePath(currentPath)
    }

    fun replaceDirectoriesPath(path: String): String {
        return path.replace(PREFIX_PH, prefix).replace(COMPANY_PH, companyCondensed).replace(PACKAGE_PH, appNameCondensed)
    }

    private fun replacePath(currentPath: String): String {
        val paths = currentPath.split(Regex(templateFilesPath))

        if (paths.size < 2) {
            println("Couldn't find target directory with path : $currentPath")
            exitProcess(MISSING_TARGET_DIR)
        }
        return replaceDirectoriesPath(paths[1])
    }

    val listFormTemplatesPath = templateFormsPath + File.separator + LIST_FORMS_KEY

    val detailFormTemplatesPath = templateFormsPath + File.separator + DETAIL_FORMS_KEY

    val srcPath = targetDirPath + File.separator +
            APP_PATH_KEY + File.separator +
            SRC_PATH_KEY

    val resPath = srcPath + File.separator +
            MAIN_PATH_KEY + File.separator +
            RES_PATH_KEY

    val navigationPath = resPath + File.separator +
            NAVIGATION_PATH_KEY

    val layoutPath = resPath + File.separator + LAYOUT_PATH_KEY

    val drawablePath = resPath + File.separator + DRAWABLE_PATH_KEY

    val assetsPath = srcPath + File.separator +
            MAIN_PATH_KEY + File.separator +
            ASSETS_PATH_KEY

    fun getRecyclerViewItemPath(tableName: String) = layoutPath + File.separator + RECYCLER_VIEW_ITEM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getDetailFormPath(tableName: String) = layoutPath + File.separator + DETAIL_FORM_PREFIX + tableName.toLowerCase().addXmlSuffix()

    fun getTargetPath(dir: String) = srcPath + File.separator +
            dir + File.separator +
            JAVA_PATH_KEY + File.separator +
            PREFIX_PH + File.separator +
            COMPANY_PH + File.separator +
            PACKAGE_PH
}
