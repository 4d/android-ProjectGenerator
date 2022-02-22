import java.io.File

fun getCatalog(assetsPath: String, tableName: String, concatFields: String): DataClass? {

    val filePath = getCatalogPath(assetsPath, tableName)

    Log.i("[$tableName] Reading catalog at path $filePath")

    val entityCatalogFile = File(filePath)

    if (entityCatalogFile.exists()) {
        val jsonString = entityCatalogFile.readFile()

        if (jsonString.isNotEmpty()) {
            retrieveJSONObject(jsonString)?.let { jsonObj ->

                val dataClasses = jsonObj.getSafeArray("dataClasses")
                dataClasses?.getJSONObject(0)?.let { jsonDataClass ->
                    jsonDataClass.getSafeString("name")?.let { dataClassName ->

                        jsonDataClass.getSafeArray("attributes")?.let { attributes ->

                            val fields = mutableListOf<FieldData>()

                            for (i in 0 until attributes.length()) {

                                val attribute = attributes.getJSONObject(i)

                                attribute.getSafeString("name")?.let { fieldName ->

                                    if (concatFields.contains(fieldName)) {

                                        val field = FieldData(fieldName.fieldAdjustment())
                                        attribute.getSafeString("type")
                                            ?.let { type -> field.isImage = type == "image" }
                                        attribute.getSafeString("kind")?.let { kind ->
                                            when (kind) {
                                                "relatedEntity" -> field.isManyToOneRelation = true
                                                "relatedEntities" -> field.isOneToManyRelation = true
                                            }
                                            if (field.isManyToOneRelation) {
                                                attribute.getSafeString("type")
                                                    ?.let { relatedOriginalTableName ->
                                                        field.relatedOriginalTableName =
                                                            relatedOriginalTableName
                                                    }
                                            }
                                        }
                                        fields.add(field)
                                    } else {
                                        Log.i("Field is not defined : $fieldName")
                                    }
                                }
                            }

                            Log.i("[$tableName] Catalog successfully read")
                            return DataClass(name = dataClassName, fields = fields)
                        }
                    }
                }
            }
            Log.i("[$tableName] Catalog json is missing name or attributes keys")
        } else {
            Log.i("[$tableName] Empty catalog file")
        }
    } else {
        Log.i("[$tableName] No catalog file found")
    }
    return null
}