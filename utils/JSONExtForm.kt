import ProjectEditorConstants.DETAIL_KEY
import ProjectEditorConstants.EMPTY_KEY
import ProjectEditorConstants.FIELDS_KEY
import ProjectEditorConstants.FORM_KEY
import ProjectEditorConstants.LIST_KEY
import ProjectEditorConstants.PHOTO_TYPE
import ProjectEditorConstants.PROJECT_KEY
import org.json.JSONObject

fun JSONObject.getFormList(dataModelList: List<DataModel>, formType: FormType, navigationTableList: List<String>, catalogDef: CatalogDef, aliasToAddCallback: (aliasToAdd: Relation) -> Unit): List<Form> {
    val formList = mutableListOf<Form>()
    val formTypeKey = if (formType == FormType.LIST) LIST_KEY else DETAIL_KEY
    val forms = this.getSafeObject(PROJECT_KEY)?.getSafeObject(formTypeKey)

    Log.d("navigationTableList = ${navigationTableList.joinToString()}")

    forms?.keys()?.forEach { keyDataModel ->
        dataModelList.filter { it.isSlave == false }.find { it.id == keyDataModel }?.let { dataModel ->
            val form = Form(dataModel = dataModel)
            val newFormJSONObject = forms.getSafeObject(keyDataModel.toString())
            newFormJSONObject?.getSafeString(FORM_KEY)?.let {
                form.name = it
            }
            val fieldList = newFormJSONObject?.getSafeArray(FIELDS_KEY).getObjectListAsString()
            form.fields = getFormFields(fieldList, dataModel.name, catalogDef)
            formList.add(form)
        }
    }

    dataModelList.filter { it.isSlave == false }.forEach { dataModel ->
        if (navigationTableList.contains(dataModel.id) && !formList.map { it.dataModel.id }.contains(dataModel.id)) {
            val form = Form(dataModel = dataModel)
            formList.add(form)
        }
    }

    for (form in formList) {
        if (form.name.isNullOrEmpty()) {
            // Set default forms
            val fields = mutableListOf<Field>()
            dataModelList.find { it.name == form.dataModel.name }?.fields?.forEach {
                if (!isPrivateRelationField(it.name) && it.isSlave == false) {
                    // if Simple Table (default list form, avoid photo and relation)
                    if (formType == FormType.LIST && (it.fieldTypeString == PHOTO_TYPE || it.inverseName != null)) {
                        // don't add this field
                        Log.d("Not adding field to default form = ${it.name}")
                    } else {
                        Log.d("Adding field to default form = ${it.name}")
                        fields.add(it)
                    }
                }
            }
            form.fields = fields
        } else {
            // a form was specified
        }
    }

    // Add any relation for unaliased path
    formList.forEach { form ->
        form.fields?.forEach { field ->
            if (field.isFieldAlias(form.dataModel.name, dataModelList)) {
                field.path?.let { path ->
                    Log.d("Going to add first part of a fieldAlias")
                    Log.d("field : $field")
                    val aliasRelation = Relation(
                        source = form.dataModel.name,
                        target = destBeforeField(catalogDef, form.dataModel.name, path),
                        name = path.substringBeforeLast(".").replace(".", "_").fieldAdjustment(),
                        type = RelationType.MANY_TO_ONE,
                        subFields = listOf(),
                        inverseName = "",
                        path = path.substringBeforeLast(".")
                    )
                    Log.d("new Alias to be added : $aliasRelation")
                    aliasToAddCallback(aliasRelation)
                }
            }
        }
    }

    formList.find { it.dataModel.name == "Emp" }?.fields?.forEach {
        Log.d("XX: Name: ${it.name}, path: ${it.path}")
    }

    return formList
}

fun JSONObject.getSearchFields(dataModelList: List<DataModel>): HashMap<String, List<String>> {
    val searchFields = HashMap<String, List<String>>()
    this.getSafeObject("project")?.let { project ->
        project.getSafeObject("list")?.let { listForms ->
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
                                Log.v("Adding searchField ${fieldName.fieldAdjustment()} for table $tableName")
                                tableSearchableFields.add(fieldName.fieldAdjustment())
                            }
                        }
                    } else {
                        val searchableFieldAsObject = listForm.getSafeObject("searchableField")
                        if (searchableFieldAsObject != null) {
                            searchableFieldAsObject.getSafeString("name")?.let { fieldName ->
                                Log.v("Adding searchField ${fieldName.fieldAdjustment()} for table $tableName")
                                tableSearchableFields.add(fieldName.fieldAdjustment())
                            }
                        } else {
                            Log.w("searchField definition error for table $tableName")
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
        } // else no list form, so no search fields
    }
    return searchFields
}
