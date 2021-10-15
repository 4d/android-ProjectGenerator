data class TemplateLayoutFiller(
    val name: String,
    val name_original: String,
    val nameLowerCase: String,
    val nameCamelCase: String,
    val label: String,
    val hasIcon: Boolean,
    val icon: String
)

fun DataModel.getTemplateLayoutFillerForNavigation(): TemplateLayoutFiller =
    TemplateLayoutFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameLowerCase = this.name.tableNameAdjustment().toLowerCase(),
        nameCamelCase = this.name.dataBindingAdjustment(),
        label = this.getLabel(),
        hasIcon = (this.iconPath != null && this.iconPath != ""),
        icon = this.iconPath ?: ""
    )

fun DataModel.getTemplateLayoutFiller(): TemplateLayoutFiller =
    TemplateLayoutFiller(
        name = this.name.tableNameAdjustment(),
        name_original = this.name,
        nameLowerCase = this.name.toLowerCase().fieldAdjustment(),
        nameCamelCase = this.name.dataBindingAdjustment(),
        label = this.getLabel(),
        hasIcon = (this.iconPath != null && this.iconPath != ""),
        icon = this.iconPath ?: ""
    )