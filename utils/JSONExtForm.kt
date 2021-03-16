import ProjectEditorConstants.DETAIL_KEY
import ProjectEditorConstants.FIELDS_KEY
import ProjectEditorConstants.FORM_KEY
import ProjectEditorConstants.LIST_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PROJECT_KEY
import org.json.JSONObject

fun JSONObject.getFormList(dataModelList: List<DataModel>, formType: FormType): List<Form> {
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
        dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
            println("adding empty form for dataModel : ${dataModel.name}")
            val form = Form(dataModel = dataModel)
            formList.add(form)
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


    // Check for missing forms
    /*dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
        println("datamodel . name = ${dataModel.name}")
        val dataModelHasAnAssociatedForm = formList.find { it.dataModel.name == dataModel.name }

        println("dataModelHasAnAssociatedForm.name = ${dataModelHasAnAssociatedForm?.name}")
        if (dataModelHasAnAssociatedForm?.name == null) { // no form was given for this dataModel
            println("dataModelHasAnAssociatedForm = null")
            val form = Form(dataModel = dataModel)
            val fields = mutableListOf<Field>()
            dataModel.fields?.forEach {
//                println("field = $it")
                if (!isPrivateRelationField(it.name) && it.isSlave == false) {
                    // if Simple Table (default list form, avoid photo and relations)
                    if (formType == FormType.LIST && (it.inverseName != null || it.fieldTypeString == PHOTO_TYPE)) {
                        // don't add this field
                    } else {
                        println("adding field to an undeclared form name = $it")
                        fields.add(it)
                    }
                }
            }
            form.fields = fields
            println("adding form : $form")
            formList.add(form)
        } else if (dataModelHasAnAssociatedForm.fields.isNullOrEmpty()) { // no field was given in the form, but form already created - wrong, instead :
            // default forms
            println("dataModelHasAnAssociatedForm fields nullOrEmpty")
            val fields = mutableListOf<Field>()
            dataModel.fields?.forEach {
//                println("field = $it")
                if (!isPrivateRelationField(it.name) && it.isSlave == false) {
                    // if Simple Table (default list form, avoid photo and relations)
                    if (formType == FormType.LIST && (it.inverseName != null || it.fieldTypeString == PHOTO_TYPE)) {
                        // don't add this field
                    } else {
                        println("adding field to a declared form name = $it")
//                        fields.add(it)
                    }
                }
            }
            dataModelHasAnAssociatedForm.fields = fields
        }
    }*/
    for (form in formList) {
        println("form (after checking missing forms) : ${form.name}")
        form.fields?.let {
            for (field in it)
                println("> field : ${field.name}")
        }
    }
    return formList
}

fun setDefaultForm() {

}
