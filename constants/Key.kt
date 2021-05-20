object Key {

    fun getKey(type: String?): String? {
        return when (type) {
            "currencyEuro" -> "TypeChoice.JapanCurrency.key.toString()"
            "currencyYen" -> "TypeChoice.EuroCurrency.key.toString()"
            "currencyDollar" -> "TypeChoice.USCurrency.key.toString()"
            "percent" -> "TypeChoice.Percentage.key.toString()"
            "ordinal" -> "TypeChoice.Ordinal.key.toString()"
            "spellOut" -> "TypeChoice.SpellOut.key.toString()"
            "integer" -> "TypeChoice.Number.Key.toString()"
            "real" -> "TypeChoice.Real.key.toString()"
            "decimal" -> "TypeChoice.Decimal.key.toString()"
            "noOrYes" -> "TypeChoice.YesNo.key.toString()"
            "falseOrTrue" -> "TypeChoice.TrueFalse.key.toString()"
            "boolInteger" -> "TypeChoice.Number.Key.toString()"
            "shortTime" -> "TypeChoice.ShortTime.key.toString()"
            "mediumTime" -> "TypeChoice.Time.key.toString()"
            "duration" -> "TypeChoice.Duration.key.toString()"
            "fullDate" -> "TypeChoice.FullDate.key.toString()"
            "longDate" -> "TypeChoice.LongDate.key.toString()"
            "mediumDate" -> "TypeChoice.MediumDate.key.toString()"
            "shortDate" -> "TypeChoice.ShortDate.key.toString()"
            "custom" -> "custom"
            else -> null
        }
    }

    fun getFormatTypeFunctionName(type: String?): String? {
        return when (type) {
            "noOrYes" -> "formatBoolean"
            "falseOrTrue" -> "formatBoolean"
            "boolInteger" -> "formatBoolean"
            "shortTime" -> "time"
            "mediumTime" -> "time"
            "duration" -> "time"
            "fullDate" -> "date"
            "longDate" -> "date"
            "mediumDate" -> "date"
            "shortDate" -> "date"
            "currencyEuro" -> "number"
            "currencyYen" -> "number"
            "currencyDollar" -> "number"
            "percent" -> "number"
            "ordinal" -> "number"
            "spellOut" -> "number"
            "integer" -> "number"
            "real" -> "number"
            "decimal" -> "number"
            "custom" -> "custom"
            else -> null
        }
    }
}
