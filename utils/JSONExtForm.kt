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
    dataModelList.forEach { dataModel ->
        if (!formList.map { it.dataModel.name }.contains(dataModel.name)) { // no form was given for this dataModel
            val form = Form(dataModel = dataModel)
            form.fields = dataModel.fields
            formList.add(form)
        }
    }
    return formList
}
