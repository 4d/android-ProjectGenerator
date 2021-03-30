import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.TIME_TYPE

object DefaultFormatter{
    private var defaultFormat = HashMap<String,String>()
    // checkbyFielsType
    fun getKeys():  HashMap<String,String>{
        defaultFormat.put(BOOLEAN_TYPE,"falseOrTrue") // bool
        defaultFormat.put(DATE_TYPE,"mediumDate") // date
        defaultFormat.put(TIME_TYPE,"mediumTime") // Time
        defaultFormat.put(INT_TYPE,"integer") // integer
        return defaultFormat
    }

}
