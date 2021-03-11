object Log {
    private const val TEXT_ERROR = "\u001B[31m"
    private const val TEXT_DEBUG = "\u001b[34m"
    private  const val TEXT_INFO = "\u001b[35m"
    private const val TEXT_WARNING ="\u001b[36m"
    private const val TEXT_RESET= "\u001B[0m"
    private const val TEXT_STYLE ="\u001B[1m"
    private lateinit var className: String
    fun e(tag :String,message:String) = println("${TEXT_STYLE}${TEXT_ERROR}${className} E/$tag: $message $TEXT_RESET")
    fun d(tag :String,message:String) = println("${TEXT_DEBUG}${className} D/$tag: $message $TEXT_RESET")
    fun i(tag :String,message:String) = println("${TEXT_INFO}${className} I/$tag: $message $TEXT_RESET")
    fun w(tag :String,message:String) =println("${TEXT_WARNING}${className} W/$tag: $message $TEXT_RESET")
    fun v(tag :String,message:String) = println("${className} V/$tag: $message $TEXT_RESET")
    fun v(message:String) = println("${className} V/ : $message $TEXT_RESET")
    fun w(message:String) =println("${TEXT_WARNING}${className} W/ : $message $TEXT_RESET")
    fun i(message:String) = println("${TEXT_INFO}${className} I/ : $message $TEXT_RESET")
    fun d(message:String) = println("${TEXT_DEBUG}${className} D/ : $message $TEXT_RESET")
    fun e(message:String) = println("${TEXT_STYLE}${TEXT_ERROR}${className} E/: $message $TEXT_RESET")
    fun plantTree(className: String){this.className = className}
}