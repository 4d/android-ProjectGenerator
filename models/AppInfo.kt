import org.json.JSONObject

data class AppInfo(
        val team: Team,
        val guestLogin: Boolean,
        val remoteUrl: String,
        val initialGlobalStamp: Int,
        val dumpedTables: List<String>,
        val searchableField: Map<String, List<String>>,
        val logLevel: Int,
        val customFormatterJson:  HashMap<String,JSONObject>,
)
