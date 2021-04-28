import com.google.gson.JsonObject
import org.json.JSONObject

data class AppInfo(
        val team: Team,
        val guestLogin: Boolean,
        val remoteUrl: String,
        val initialGlobalStamp: Int,
        val dumpedTables: List<String>,
        val searchableField: Map<String, List<String>>,
        val logLevel: Int,
       // val customFormatter:  HashMap<String,HashMap<String,JsonObject>>,
       val customFormatters : HashMap<String,HashMap<String,FieldMapping>>
)

data class FieldMapping(val binding:String?,val formatchoice: JSONObject?,val isSearchable: Boolean?,val formatType: String?)