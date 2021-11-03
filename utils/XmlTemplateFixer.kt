import java.io.File

fun readFileDirectlyAsText(file: File): String
        = file.readText(Charsets.UTF_8)

fun replaceTemplateText(oldFormText: String, formType: FormType): String {

    val variableType: String
    val variableName : String

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
        .replace("___LABEL_ID___", "{{tableName_lowercase}}_field_label_{{viewId}}")
        .replace("___VALUE_ID___", "{{tableName_lowercase}}_field_value_{{viewId}}")
        .replace("___BUTTON_ID___", "{{tableName_lowercase}}_field_value_{{viewId}}")

    var regex = ("(\\h*)app:imageUrl=\"___IMAGE___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            throw Exception("Not Yet Implemented (free field in list form)")
        else
            "${indent}{{#isImage}}\n" +
                    "${indent}app:imageFieldName='@{\"{{fieldName}}\"}'\n" +
                    "${indent}app:imageKey=\"@{ {{accessor}}{{imageKeyAccessor}} }\"\n" +
                    "${indent}app:imageTableName='@{\"{{sourceTableName}}\"}'\n" +
                    "${indent}app:imageUrl=\"@{ {{accessor}}{{name}}.__deferred.uri}\"\n" +
                    "${indent}{{/isImage}}"
    }

    regex = ("(\\h*)android:text=\"___TEXT___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            throw Exception("Not Yet Implemented (free field in list form)")
        else
            "${indent}{{^isImage}}\n" +
                    "${indent}{{#isCustomFormat}}\n" + //If is custom
                    "${indent}app:tableName='@{\"{{fieldTableName}}\"}'\n" +
                    "${indent}app:fieldName='@{\"{{formatFieldName}}\"}'\n" +
                    "${indent}{{#isImageNamed}}\n" + //If is imageNamed
                    "${indent}app:imageWidth=\"@{ {{imageWidth}} }\"\n" +
                    "${indent}app:imageHeight=\"@{ {{imageHeight}} }\"\n" +
                    "${indent}{{/isImageNamed}}\n" + // End is imageNamed
                    "${indent}{{/isCustomFormat}}\n" + //End If Custom
                    "${indent}app:text=\"@{ {{accessor}}{{name}} }\"\n" +
                    "${indent}app:format='@{\"{{formatType}}\"}'\n" +
                    "${indent}{{/isImage}}"
    }

    regex = ("(\\h*)android:text=\"___BUTTON___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        "${indent}android:text=\"{{label}}\"\n" +
                "${indent}{{#hasIcon}}\n" +
                "${indent}app:icon='@{\"{{iconPath}}\"}'\n" +
                "${indent}{{/hasIcon}}"
    }

    regex = ("(\\h*)android:text=\"___FIELD_LABEL___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            throw Exception("Not Yet Implemented (free field in list form)")
        else
            "${indent}android:text=\"{{label}}\"\n" +
                    "${indent}{{#hasIcon}}\n" +
                    "${indent}app:icon='@{\"{{iconPath}}\"}'\n" +
                    "${indent}{{/hasIcon}}"
    }

    regex = ("(\\h*)android:text=\"___FIELD_SHORT_LABEL___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST) // should never come here (free field in list form)
            throw Exception("Not Yet Implemented (free field in list form)")
        else
            "${indent}android:text=\"{{shortLabel}}\"\n" +
                    "${indent}{{#hasIcon}}\n" +
                    "${indent}app:icon='@{\"{{iconPath}}\"}'\n" +
                    "${indent}{{/hasIcon}}"
    }

    regex = ("(\\h*)<!--ENTITY_VARIABLE-->").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        if (formType == FormType.LIST)
            "${indent}<variable\n" +
                    "${indent}\tname=\"${variableName}\"\n" +
                    "${indent}\ttype=\"${variableType}\"/>\n\n" +
                    "${indent}{{#relations_many_to_one}}\n" +
                    "${indent}<variable\n" +
                    "${indent}\tname=\"{{relation_name}}\"\n" +
                    "${indent}\ttype=\"{{package}}.data.model.entity.{{relation_target}}\"/>\n" +
                    "${indent}{{/relations_many_to_one}}\n" +
                    "${indent}{{#has_any_one_to_many_relation}}\n" +
                    "${indent}<import type=\"java.util.List\" />\n" +
                    "${indent}{{#relations_one_to_many}}\n" +
                    "${indent}<variable\n" +
                    "${indent}\tname=\"{{relation_name}}\"\n" +
                    "${indent}\ttype=\"List&lt;{{package}}.data.model.entity.{{relation_target}}>\"/>\n" +
                    "${indent}{{/relations_one_to_many}}\n" +
                    "${indent}{{/has_any_one_to_many_relation}}"
        else
            "${indent}<variable\n" +
                    "${indent}\tname=\"${variableName}\"\n" +
                    "${indent}\ttype=\"${variableType}\"/>"
    }

    regex = ("___SPECIFIC_ID_(\\d+)___").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val id = matchResult.destructured.component1()
        "{{tableName_lowercase}}_field_value_${id}"
    }

    regex = ("(\\h*)android:text=\"___BUTTON_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}android:text=\"{{field_${id}_label}}\"\n" +
                "${indent}{{#field_${id}_hasIcon}}\n" +
                "${indent}app:icon='@{\"{{field_${id}_iconPath}}\"}'\n" +
                "${indent}{{/field_${id}_hasIcon}}"
    }

    regex = ("(\\h*)android:text=\"___TEXT_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}{{^field_${id}_is_image}}\n" +
                "${indent}{{#field_${id}_is_relation}}\n" +
                "${indent}android:text=\"{{field_${id}_label}}\"\n" +
                "${indent}app:linkColor=\"@{true}\"\n" +
                "${indent}{{/field_${id}_is_relation}}\n" +
                "${indent}{{^field_${id}_is_relation}}\n" +
                "${indent}{{#field_${id}_custom_formatted}}\n" +
                "${indent}app:tableName='@{\"{{{field_${id}_field_table_name}}}\"}'\n" +
                "${indent}app:fieldName='@{\"{{{field_${id}_format_field_name}}}\"}'\n" +
                "${indent}{{#field_${id}_custom_formatted_imageNamed}}\n" +
                "${indent}app:imageWidth=\"@{ {{field_${id}_field_image_width}} }\"\n" +
                "${indent}app:imageHeight=\"@{ {{field_${id}_field_image_height}} }\"\n" +
                "${indent}{{/field_${id}_custom_formatted_imageNamed}}\n" +
                "${indent}{{/field_${id}_custom_formatted}}\n" +
                "${indent}app:text=\"@{ {{field_${id}_accessor}}{{field_${id}_name}} }\"\n" +
                "${indent}app:format='@{\"{{field_${id}_format_type}}\"}'\n" +
                "${indent}{{/field_${id}_is_relation}}\n" +
                "${indent}{{/field_${id}_is_image}}\n" +
                "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)android:progress=\"___PROGRESS_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}app:progress=\"@{ {{field_${id}_accessor}}{{field_${id}_name}} }\"\n" +
                "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)android:text=\"___FIELD_LABEL_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}android:text=\"{{field_${id}_label}}\"\n" +
                "${indent}{{#field_${id}_hasIcon}}\n" +
                "${indent}app:icon='@{\"{{field_${id}_iconPath}}\"}'\n" +
                "${indent}{{/field_${id}_hasIcon}}\n" +
                "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)android:text=\"___FIELD_SHORT_LABEL_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}android:text=\"{{field_${id}_shortLabel}}\"\n" +
                "${indent}{{#field_${id}_hasIcon}}\n" +
                "${indent}app:icon='@{\"{{field_${id}_iconPath}}\"}'\n" +
                "${indent}{{/field_${id}_hasIcon}}\n" +
                "${indent}{{/field_${id}_defined}}"
    }

    regex = ("(\\h*)app:imageUrl=\"___IMAGE_(\\d+)___\"").toRegex()
    newFormText = regex.replace(newFormText) { matchResult ->
        val indent = matchResult.destructured.component1()
        val id = matchResult.destructured.component2()
        "${indent}{{#field_${id}_defined}}\n" +
                "${indent}{{#field_${id}_is_image}}\n" +
                "${indent}app:imageFieldName='@{\"{{field_${id}_field_name}}\"}'\n" +
                "${indent}app:imageKey=\"@{ {{field_${id}_accessor}}{{field_${id}_image_key_accessor}} }\"\n" +
                "${indent}app:imageTableName='@{\"{{field_${id}_source_table_name}}\"}'\n" +
                "${indent}app:imageUrl=\"@{ {{field_${id}_accessor}}{{field_${id}_name}}.__deferred.uri}\"\n" +
                "${indent}{{/field_${id}_is_image}}\n" +
                "${indent}{{/field_${id}_defined}}\n" +
                "${indent}{{^field_${id}_defined}}\n" +
                "${indent}app:imageDrawable=\"@{@drawable/ic_placeholder}\"\n" +
                "${indent}{{/field_${id}_defined}}"
    }

    return newFormText
}