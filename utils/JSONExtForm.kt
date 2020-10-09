import ProjectEditorConstants.DETAIL_KEY
import ProjectEditorConstants.FIELDS_KEY
import ProjectEditorConstants.FORM_KEY
import ProjectEditorConstants.LIST_KEY
import ProjectEditorConstants.PROJECT_KEY
import org.json.JSONObject

fun JSONObject.getListFormList(dataModelList: List<DataModel>): List<Form> {
    val listFormList = mutableListOf<Form>()
    val listForms = this.getSafeObject(PROJECT_KEY)?.getSafeObject(LIST_KEY)

    listForms?.names()?.let {
        for (i in 0 until listForms.names().length()) {
            val keyDataModel = listForms.names().getString(i)
            dataModelList.find { it.id == keyDataModel }?.let { dataModel ->
                val listForm = Form(dataModel = dataModel)
                val newFormJSONObject = listForms.getSafeObject(keyDataModel.toString())
                newFormJSONObject?.getSafeString(FORM_KEY)?.let { listForm.name = it }
                val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getStringList()
                listForm.fields = getFormFields(fieldList, FormType.LIST)
                listFormList.add(listForm)
            }
        }
    }
    return listFormList
}

fun JSONObject.getDetailFormList(dataModelList: List<DataModel>): List<Form> {
    val detailFormList = mutableListOf<Form>()
    val detailForms = this.getSafeObject(PROJECT_KEY)?.getSafeObject(DETAIL_KEY)

    detailForms?.names()?.let {
        for (i in 0 until detailForms.names().length()) {
            val keyDataModel = detailForms.names().getString(i)
            dataModelList.find { it.id == keyDataModel }?.let { dataModel ->
                val detailForm = Form(dataModel = dataModel)
                val newFormJSONObject = detailForms.getSafeObject(keyDataModel.toString())
                newFormJSONObject?.getSafeString(FORM_KEY)?.let { detailForm.name = it }
                val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getStringList()
                detailForm.fields = getFormFields(fieldList, FormType.DETAIL)
                detailFormList.add(detailForm)
            }
        }
    }
    return detailFormList
}
