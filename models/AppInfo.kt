import org.json.JSONObject

data class AppInfo(
        val team: Team,
        val guestLogin: Boolean,
        val remoteUrl: String,
        val embeddedData: Boolean,
        val initialGlobalStamp: Int,
        val searchableField: Map<String,List<String>>
)
