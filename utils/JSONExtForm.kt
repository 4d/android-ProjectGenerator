import ProjectEditorConstants.DETAIL_KEY
import ProjectEditorConstants.FIELDS_KEY
import ProjectEditorConstants.FORM_KEY
import ProjectEditorConstants.LIST_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PROJECT_KEY
import org.json.JSONObject

fun JSONObject.getFormList(dataModelList: List<DataModel>, formType: FormType, navigationTableList: List<String>): List<Form> {
    val formList = mutableListOf<Form>()
    val formTypeKey = if (formType == FormType.LIST) LIST_KEY else DETAIL_KEY
    val forms = this.getSafeObject(PROJECT_KEY)?.getSafeObject(formTypeKey)

    forms?.names()?.let {
        for (i in 0 until forms.names().length()) {
            val keyDataModel = forms.names().getString(i)
            dataModelList.find { it.id == keyDataModel }?.let { dataModel ->
                val form = Form(dataModel = dataModel)
                val newFormJSONObject = forms.getSafeObject(keyDataModel.toString())
                newFormJSONObject?.getSafeString(FORM_KEY)?.let {
                    form.name = it
                }
                println("***+ formType : $formType")
                println("newFormJSONObject = $newFormJSONObject")
                println("newFormJSONObject?.getSafeArray(FIELDS_KEY) = ${ newFormJSONObject?.getSafeArray(FIELDS_KEY)}")
                val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getObjectListAsString()
                form.fields = getFormFields(fieldList)
                formList.add(form)
            }
        }
    } ?: kotlin.run {

        dataModelList.forEach { dataModel ->
            if (navigationTableList.contains(dataModel.id)) {
                println("adding empty form for dataModel : ${dataModel.name}")
                val form = Form(dataModel = dataModel)
                formList.add(form)
            }
        }
    }

    for (form in formList) {
        println("form (before checking missing forms) : ${form.name}")
        form.fields?.let {
            for (field in it)
                println("> field : ${field.name}")
        }
    }

    for (form in formList) {
        if (form.name.isNullOrEmpty()) {
            // Set default forms
            val fields = mutableListOf<Field>()
            dataModelList.find { it.name == form.dataModel.name }?.fields?.forEach {
                if (!isPrivateRelationField(it.name) && it.isSlave == false) {
                    // if Simple Table (default list form, avoid photo and relations)
                    if (formType == FormType.LIST && (it.inverseName != null || it.fieldTypeString == PHOTO_TYPE)) {
                        // don't add this field
                    } else {
                        println("adding field to default form = $it")
                        fields.add(it)
                    }
                }
            }
            form.fields = fields

        } else {
            // a form was specified
        }
    }

    for (form in formList) {
        println("form (after checking missing forms) : ${form.name}")
        form.fields?.let {
            for (field in it)
                println("> field : ${field.name}")
        }
    }
    return formList
}