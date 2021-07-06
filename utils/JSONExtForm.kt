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

    Log.d("dataModelList = ${dataModelList.joinToString { it.id }}")
    Log.d("navigationTableList = ${navigationTableList.joinToString()}")

    forms?.keys()?.forEach { keyDataModel ->
        dataModelList.filter { it.isSlave == false }.find { it.id == keyDataModel }?.let { dataModel ->
            val form = Form(dataModel = dataModel)
            val newFormJSONObject = forms.getSafeObject(keyDataModel.toString())
            newFormJSONObject?.getSafeString(FORM_KEY)?.let {
                form.name = it
            }
            Log.d("***+ formType : $formType")
            Log.d("newFormJSONObject = $newFormJSONObject")
            Log.d("newFormJSONObject?.getSafeArray(FIELDS_KEY) = ${newFormJSONObject?.getSafeArray(FIELDS_KEY)}")
            val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getObjectListAsString()
            form.fields = getFormFields(fieldList)
            formList.add(form)
        }
    }

    dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
        Log.d("dataModel.id = ${dataModel.id}")
        if (navigationTableList.contains(dataModel.id) && !formList.map { it.dataModel.id }.contains(dataModel.id)) {
            Log.d("adding empty form for dataModel : ${dataModel.name}")
            val form = Form(dataModel = dataModel)
            formList.add(form)
        }
    }

    for (form in formList) {
        Log.d("form (before checking missing forms) : ${form.name}")
        form.fields?.let {
            for (field in it)
                Log.d("> field : ${field.name}")
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
                        Log.d("adding field to default form = $it")
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
        Log.d("form (after checking missing forms) : ${form.name}")
        form.fields?.let {
            for (field in it)
                Log.d("> field : ${field.name}")
        }
    }
    return formList
}

fun JSONObject.getSearchFields(dataModelList: List<DataModel>): HashMap<String, List<String>> {
    val searchFields = HashMap<String, List<String>>()
    this.getSafeObject("project")?.let { project ->
        project.getSafeObject("list")?.let { listForms ->
            Log.i("listForms :: $listForms")
            listForms.keys().forEach eachTableIndex@{ tableIndex ->
                if (tableIndex !is String) return@eachTableIndex
                val tableSearchableFields = mutableListOf<String>()
                val tableName = dataModelList.find { it.id == tableIndex }?.name
                listForms.getSafeObject(tableIndex)?.let { listForm ->
                    // could be an array or an object (object if only one item dropped)
                    val searchableFieldAsArray = listForm.getSafeArray("searchableField")
                    if (searchableFieldAsArray != null) {
                        for (ind in 0 until searchableFieldAsArray.length()) {
                            searchableFieldAsArray.getSafeObject(ind)?.getSafeString("name")?.let { fieldName ->
                                Log.v("$tableName SearchField fieldName.fieldAdjustment() :: ${fieldName.fieldAdjustment()}")
                                tableSearchableFields.add(fieldName.fieldAdjustment())
                            }
                        }
                    } else {
                        val searchableFieldAsObject = listForm.getSafeObject("searchableField")
                        if (searchableFieldAsObject != null) {
                            searchableFieldAsObject.getSafeString("name")?.let { fieldName ->
                                Log.v("$tableName  SearchField fieldName.fieldAdjustment() :: ${fieldName.fieldAdjustment()}")
                                tableSearchableFields.add(fieldName.fieldAdjustment())
                            }
                        } else {
                            Log.w("$tableName searchableField is not available as object or array in $listForm")
                        }
                    }
                }
                if (tableSearchableFields.size != 0) { // if has search field add it
                    if (tableName != null) {
                        searchFields[tableName.tableNameAdjustment()] = tableSearchableFields
                    } else {
                        Log.e("Cannot get tableName for index $tableIndex when filling search fields")
                    }
                }
            }
            Log.i("Computed search fields $searchFields")
        } // else no list form, so no search fields
    }
    return searchFields
}
