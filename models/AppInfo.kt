data class AppInfo(
    val team: Team,
    val guestLogin: Boolean,
    val remoteUrl: String,
    val initialGlobalStamp: Int,
    val dumpedTables: List<String>,
    val logLevel: Int,
    val relations: Boolean
)