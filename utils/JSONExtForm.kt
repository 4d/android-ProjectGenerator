import ProjectEditorConstants.DETAIL_KEY
import ProjectEditorConstants.FIELDS_KEY
import ProjectEditorConstants.FORM_KEY
import ProjectEditorConstants.LIST_KEY
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
                val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getStringList()
                form.fields = getFormFields(fieldList, formType)
                formList.add(form)
            }
        }
    }
    // Check for missing detailForms
    dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
        val dataModelHasAnAssociatedForm = formList.find { it.dataModel.name == dataModel.name }

        if (dataModelHasAnAssociatedForm == null) { // no form was given for this dataModel
            val form = Form(dataModel = dataModel)
            val fields = mutableListOf<Field>()
            dataModel.fields?.forEach {
                if (!isPrivateRelationField(it.name)) {
                    fields.add(it)
                }
            }
            form.fields = fields
            formList.add(form)
        } else if (dataModelHasAnAssociatedForm.fields.isNullOrEmpty()) { // no field was given in the form
            val fields = mutableListOf<Field>()
            dataModel.fields?.forEach {
                if (!isPrivateRelationField(it.name)) {
                    fields.add(it)
                }
            }
            dataModelHasAnAssociatedForm.fields = fields
        }
    }
    return formList
}
