import java.io.File

fun readFileDirectlyAsText(file: File): String
        = file.readText(Charsets.UTF_8)

fun replaceTemplateText(oldFormText: String, formType: FormType): String {

    val variableType: String
    val variableName : String
    val formatPath: String = "<import type=\"com.qmobile.qmobileui.utils.Format\" />\n"
    val typeChoicePath: String = "<import type=\"com.qmobile.qmobileui.utils.TypeChoice\" />\n"

    if (formType == FormType.LIST) {
        variableType = "{{package}}.data.model.entity.{{tableName}}"
        variableName = "entityData"
    } else {
        variableType = "{{package}}.viewmodel.entity.EntityViewModel{{tableName}}"
        variableName = "viewModel"
    }

    var newFormText = oldFormText.replace("<!--FOR_EACH_FIELD-->", "{{#form_fields}}")
        .replace("<!--END_FOR_EACH_FIELD-->", "{{/form_fields}}")
        .replace("<!--IF_IS_RELATION-->", "{{#isRelation}}")
        .replace("<!--END_IF_IS_RELATION-->", "{{/isRelation}}")
        .replace("<!--IF_IS_NOT_RELATION-->", "{{^isRelation}}")
        .replace("<!--END_IF_IS_NOT_RELATION-->", "{{/isRelation}}")
        .replace("<!--IF_IS_IMAGE-->", "{{#isImage}}")
        .replace("<!--END_IF_IS_IMAGE-->", "{{/isImage}}")
        .replace("<!--IF_IS_NOT_IMAGE-->", "{{^isImage}}")
        .replace("<!--END_IF_IS_NOT_IMAGE-->", "{{/isImage}}")
        .replace("__LABEL_ID__", "{{tableName_lowercase}}_field_label_{{viewId}}")
        .replace("__VALUE_ID__", "{{tableName_lowercase}}_field_value_{{viewId}}")
        .replace("__BUTTON_ID__", "{{tableName_lowercase}}_field_button_{{viewId}}")
        .replace("android:text=\"__LABEL__\"", "android:text=\"{{label}}\"")
        .replace("android:text=\"__BUTTON__\"", "android:text=\"{{label}}\"")

    var regex = ("(\\h*)app:imageUrl=\"__IMAGE__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            "${indent}{{#isImage}}\n" +
                    "${indent}app:imageFieldName='@{\"{{imageFieldName}}\"}'\n" +
                    "${indent}app:imageKey=\"@{ {{accessor}}{{imageKeyAccessor}} }\"\n" +
                    "${indent}app:imageTableName='@{\"{{imageTableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{ {{accessor}}{{name}}.__deferred.uri}\"\n" +
                    "${indent}{{/isImage}}"
        else
            "${indent}{{#isImage}}\n" +
                    "${indent}app:imageFieldName='@{\"{{imageFieldName}}\"}'\n" +
                    "${indent}app:imageKey=\"@{ {{accessor}}{{imageKeyAccessor}} }\"\n" +
                    "${indent}app:imageTableName='@{\"{{imageTableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{ {{accessor}}{{name}}.__deferred.uri}\"\n" +
                    "${indent}{{/isImage}}"
    }

    regex = ("(\\h*)android:text=\"__TEXT__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            "${indent}{{^isImage}}\n" +
                    "${indent}{{#isFormatted}}\n" +
                    "${indent}android:text=\"@{Format.{{formatFunction}}({{formatType}},{{accessor}}{{name}}.toString())}\"\n" +
                    "${indent}{{/isFormatted}}\n" +
                    "${indent}{{^isFormatted}}\n" +
                    "${indent}android:text=\"@{ {{accessor}}{{name}}.toString()}\"\n" +
                    "${indent}{{/isFormatted}}\n" +
                    "${indent}{{/isImage}}"
        else
            "${indent}{{^isImage}}\n" +
                    "${indent}{{#isFormatted}}\n" +
                    "${indent}android:text=\"@{Format.{{formatFunction}}({{formatType}},{{accessor}}{{name}}.toString())}\"\n" +
                    "${indent}{{/isFormatted}}\n" +
                    "${indent}{{^isFormatted}}\n" +
                    "${indent}android:text=\"@{ {{accessor}}{{name}}.toString()}\"\n" +
                    "${indent}{{/isFormatted}}\n" +
                    "${indent}{{/isImage}}"
    }

    regex = ("(\\h*)<!--ENTITY_VARIABLE-->").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST)
            "${indent}$formatPath" + "${indent}$typeChoicePath\n" +
                "${indent}<variable\n" +
                "${indent}\tname=\"${variableName}\"\n" +
                "${indent}\ttype=\"${variableType}\"/>\n\n" +
                "${indent}{{#relations}}\n" +
                "${indent}<variable\n" +
                "${indent}\tname=\"{{relation_name}}\"\n" +
                "${indent}\ttype=\"{{package}}.data.model.entity.{{relation_target}}\"/>\n" +
                "${indent}{{/relations}}"
        else
            "${indent}$formatPath" + "${indent}$typeChoicePath\n" +
                    "${indent}<variable\n" +
                    "${indent}\tname=\"${variableName}\"\n" +
                    "${indent}\ttype=\"${variableType}\"/>"
    }

    regex = ("__SPECIFIC_ID_(\\d+)__").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val id = matchResult.destructured.component1()
        "{{tableName_lowercase}}_field_value_${id}"
    }

    regex = ("(\\h*)android:text=\"__BUTTON_(\\d+)__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        "${indent}android:text=\"{{label}}\""
    }

    regex = ("(\\h*)android:text=\"__TEXT_(\\d+)__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        if (formType == FormType.LIST)
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{^field_${id}_is_image}}\n" +
                    "${indent}{{#field_${id}_formatted}}\n" +
                    "${indent}android:text=\"@{Format.{{field_${id}_format_function}}({{field_${id}_format_type}},{{field_${id}_accessor}}{{field_${id}_name}}.toString())}\"\n" +
                    "${indent}{{/field_${id}_formatted}}\n" +
                    "${indent}{{^field_${id}_formatted}}\n" +
                    "${indent}android:text=\"@{ {{field_${id}_accessor}}{{field_${id}_name}}.toString()}\"\n" +
                    "${indent}{{/field_${id}_formatted}}\n" +
                    "${indent}{{/field_${id}_is_image}}\n" +
                    "${indent}{{/field_${id}_defined}}"
        else
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{^field_${id}_is_image}}\n" +
                    "${indent}{{#field_${id}_formatted}}\n" +
                    "${indent}android:text=\"@{Format.{{field_${id}_format_function}}({{field_${id}_format_type}},{{field_${id}_accessor}}{{field_${id}_name}}.toString())}\"\n" +
                    "${indent}{{/field_${id}_formatted}}\n" +
                    "${indent}{{^field_${id}_formatted}}\n" +
                    "${indent}android:text=\"@{ {{field_${id}_accessor}}{{field_${id}_name}}.toString()}\"\n" +
                    "${indent}{{/field_${id}_formatted}}\n" +
                    "${indent}{{/field_${id}_is_image}}\n" +
                    "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)android:progress=\"__PROGRESS_(\\d+)__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        if (formType == FormType.LIST)
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{#field_${id}_is_int}}\n" +
                    "${indent}android:progress=\"@{ {{field_${id}_accessor}}{{field_${id}_name}} != null ? {{field_${id}_accessor}}{{field_${id}_name}} : 0}\"\n" +
                    "${indent}{{/field_${id}_is_int}}\n" +
                    "${indent}{{/field_${id}_defined}}"
        else
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{#field_${id}_is_int}}\n" +
                    "${indent}android:progress=\"@{ {{field_${id}_accessor}}{{field_${id}_name}} != null ? {{field_${id}_accessor}}{{field_${id}_name}} : 0}\"\n" +
                    "${indent}{{/field_${id}_is_int}}\n" +
                    "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)android:text=\"__LABEL_(\\d+)__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}android:text=\"{{field_${id}_label}}\"\n" +
                "${indent}{{/field_${id}_defined}}"

    }

    regex = ("(\\h*)app:imageUrl=\"__IMAGE_(\\d+)__\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        if (formType == FormType.LIST)
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{#field_${id}_is_image}}\n" +
                    "${indent}app:imageFieldName='@{\"{{imageFieldName}}\"}'\n" +
                    "${indent}app:imageKey=\"@{ {{field_${id}_accessor}}{{imageKeyAccessor}} }\"\n" +
                    "${indent}app:imageTableName='@{\"{{imageTableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{ {{field_${id}_accessor}}{{field_${id}_name}}.__deferred.uri}\"\n" +
                    "${indent}{{/field_${id}_is_image}}\n" +
                    "${indent}{{/field_${id}_defined}}\n" +
                    "${indent}{{^field_${id}_defined}}\n" +
                    "${indent}app:imageDrawable=\"@{@drawable/ic_placeholder}\"\n" +
                    "${indent}{{/field_${id}_defined}}"
        else
            "${indent}{{#field_${id}_defined}}\n" +
                    "${indent}{{#field_${id}_is_literal}}\n" +
                    "${indent}app:imageFieldName='@{\"{{imageFieldName}}\"}'\n" +
                    "${indent}app:imageKey=\"@{ {{field_${id}_accessor}}{{imageKeyAccessor}} }\"\n" +
                    "${indent}app:imageTableName='@{\"{{imageTableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{ {{field_${id}_accessor}}{{field_${id}_name}}.__deferred.uri}\"\n" +
                    "${indent}{{/field_${id}_is_image}}\n" +
                    "${indent}{{/field_${id}_defined}}\n" +
                    "${indent}{{^field_${id}_defined}}\n" +
                    "${indent}app:imageDrawable=\"@{@drawable/ic_placeholder}\"\n" +
                    "${indent}{{/field_${id}_defined}}"
    }

    return newFormText
}