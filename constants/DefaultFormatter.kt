
object DefaultFormatter{
    private var defaultForamt = HashMap<String,String>()
    // checkbyFielsType
    fun getKeys():  HashMap<String,String>{
        defaultForamt.put("6","falseOrTrue") // bool
        defaultForamt.put("4","mediumDate") // date
        defaultForamt.put("11","mediumTime") // Time
        defaultForamt.put("9","integer") // integer
        return defaultForamt
    }

}
