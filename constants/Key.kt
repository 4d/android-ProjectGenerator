
object Key{
    private var typeChoice = HashMap<String,String>()
    private var formatTypeFunctionName = HashMap<String,String>()
   fun getKeys():  HashMap<String,String>{
       typeChoice.put("currencyEuro","TypeChoice.EuroCurrency.key.toString()")
       typeChoice.put("currencyYen","TypeChoice.JapanCurrency.key.toString()")
       typeChoice.put("currencyDollar","TypeChoice.USCurrency.key.toString()")
       typeChoice.put("percent","TypeChoice.Percentage.key.toString()")
       typeChoice.put("ordinal","TypeChoice.Ordinal.key.toString()")
       typeChoice.put("spellOut","TypeChoice.SpellOut.key.toString()")
       typeChoice.put("integer","TypeChoice.Number.Key.toString()")
       typeChoice.put("real","TypeChoice.Real.key.toString()")
       typeChoice.put("decimal","TypeChoice.Decimal.key.toString()")
       typeChoice.put("noOrYes","TypeChoice.YesNo.key.toString()")
       typeChoice.put("falseOrTrue","TypeChoice.TrueFalse.key.toString()")
       typeChoice.put("shortTime","TypeChoice.ShortTime.key.toString()")
       typeChoice.put("mediumTime","TypeChoice.Time.key.toString()")
       typeChoice.put("duration","TypeChoice.Duration.key.toString()")
       typeChoice.put("fullDate","TypeChoice.FullDate.key.toString()")
       typeChoice.put("longDate","TypeChoice.LongDate.key.toString()")
       typeChoice.put("mediumDate","TypeChoice.MediumDate.key.toString()")
       typeChoice.put("shortDate","TypeChoice.ShortDate.key.toString()")
       return typeChoice
   }
    fun getFormatTypeFunctionName():HashMap<String,String>{
        formatTypeFunctionName.put("noOrYes","formatBoolean")
        formatTypeFunctionName.put("falseOrTrue","formatBoolean")
        formatTypeFunctionName.put("shortTime","time")
        formatTypeFunctionName.put("mediumTime","time")
        formatTypeFunctionName.put("duration","time")
        formatTypeFunctionName.put("duration","time")
        formatTypeFunctionName.put("fullDate","date")
        formatTypeFunctionName.put("longDate","date")
        formatTypeFunctionName.put("mediumDate","date")
        formatTypeFunctionName.put("shortDate","date")
        formatTypeFunctionName.put("currencyEuro","number")
        formatTypeFunctionName.put("currencyYen","number")
        formatTypeFunctionName.put("currencyDollar","number")
        formatTypeFunctionName.put("percent","number")
        formatTypeFunctionName.put("ordinal","number")
        formatTypeFunctionName.put("spellOut","number")
        formatTypeFunctionName.put("integer","number")
        formatTypeFunctionName.put("real","number")
        formatTypeFunctionName.put("decimal","number")
        return formatTypeFunctionName
    }
}
