import ProjectEditorConstants.BOOLEAN_TYPE
import ProjectEditorConstants.DATE_TYPE
import ProjectEditorConstants.FLOAT_TYPE
import ProjectEditorConstants.INT_TYPE
import ProjectEditorConstants.TIME_TYPE

object DefaultFormatter{
    private var defaultFormat = HashMap<String,String>()
    // check by FieldsType
    fun getKeys():  HashMap<String,String>{
        defaultFormat.put(BOOLEAN_TYPE,"falseOrTrue")
        defaultFormat.put(DATE_TYPE,"mediumDate")
        defaultFormat.put(TIME_TYPE,"mediumTime")
        defaultFormat.put(INT_TYPE,"integer")
        defaultFormat.put(FLOAT_TYPE,"decimal")
        return defaultFormat
    }

}
