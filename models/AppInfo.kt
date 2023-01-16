data class AppInfo(
    val appData: AppData,
    val teamId: String,
    val guestLogin: Boolean,
    val remoteUrl: String,
    val initialGlobalStamp: Int,
    val dumpedTables: List<String>,
    val logLevel: Int,
    val relations: Boolean,
    val crashLogs: Boolean,
    val buildInfo: BuildInfo
)