import org.json.JSONArray
import org.json.JSONObject

class SqlQueryBuilder(entry: Any, private val fields: List<FieldData>) {
    /**
     * inputEntities can be a JSONArray, or a JSONObject.
     * It is a JSONArray when it receives a list of entities
     * It is a JSONObject when it's a related entity decoded from another table entities
     */

    val outputEntities = arrayListOf<Array<Any?>>()
    val hashMap = mutableMapOf<String, Any?>()
    val relatedEntitiesMap = mutableMapOf<String, MutableList<JSONObject>>()

    init {
        Log.d("SqlQueryBuilder : fields = ${fields.joinToString { it.name }}")

        when (entry) {
            is JSONArray -> {
                for (i in 0 until entry.length()) {

                    hashMap["__KEY"] = null
                    hashMap["__TIMESTAMP"] = null
                    hashMap["__STAMP"] = null
                    fields.forEach { field ->
                        hashMap[field.name.fieldAdjustment()] = null
                        if (field.isManyToOneRelation)
                            hashMap["__${field.name.fieldAdjustment()}Key"] = null
//                        when {
//                            field.isManyToOneRelation -> hashMap["__${field.name.fieldAdjustment()}Key"] = null
//                            field.isOneToManyRelation -> hashMap["__${field.name.fieldAdjustment()}Size"] = null
//                            else -> hashMap[field.name.fieldAdjustment()] = null
//                        }
                    }

                    val inputEntity = entry.getJSONObject(i)
                    val outputEntity = extractEntity(inputEntity)
                    outputEntities.add(outputEntity)
                }
            }
            is JSONObject -> {
                hashMap["__KEY"] = null
                hashMap["__TIMESTAMP"] = null
                hashMap["__STAMP"] = null
                fields.forEach { field ->
                    hashMap[field.name.fieldAdjustment()] = null
                    if (field.isManyToOneRelation)
                        hashMap["__${field.name.fieldAdjustment()}Key"] = null
//                    when {
//                        field.isManyToOneRelation -> hashMap["__${field.name.fieldAdjustment()}Key"] = null
//                        field.isOneToManyRelation -> hashMap["__${field.name.fieldAdjustment()}Size"] = null
//                        else -> hashMap[field.name.fieldAdjustment()] = null
//                    }
                }

                val outputEntity = extractEntity(entry)
                outputEntities.add(outputEntity)
            }
        }
    }

    private fun extractEntity(inputEntity: JSONObject): Array<Any?> {

        val outputEntity = arrayOfNulls<Any>(hashMap.keys.size)

        inputEntity.keys().forEach {
            val key: String = it.toString()
            if (hashMap.containsKey(key.fieldAdjustment())) {
                hashMap[key.fieldAdjustment()] = inputEntity[key]
                Log.d("inputEntity de Key is :")
                Log.d("${inputEntity[key]}")

                fields.find { it.name.fieldAdjustment() == key.fieldAdjustment() }?.let { field ->
//                fields.find { f -> f.name.fieldAdjustment() == key.fieldAdjustment() }?.let { field ->
                    when {
                        field.isImage -> {
                            // Nothing to do
                        }
                        field.isManyToOneRelation -> {
                            val neededObject = hashMap[key.fieldAdjustment()]
//                            val neededObject = inputEntity[key]
                            if (neededObject is JSONObject) {
                                hashMap["__${key.fieldAdjustment()}Key"] =
                                    neededObject.getSafeString("__KEY")
                                hashMap[key.fieldAdjustment()] = null

//                                hashMap["__${key.fieldAdjustment()}Key"] = neededObject.getSafeString("__KEY")

                                // add the relation in a new SqlQuery
                                field.relatedOriginalTableName?.let { originalTableName ->
                                    if (!relatedEntitiesMap.containsKey(originalTableName)) {
                                        relatedEntitiesMap[originalTableName] = mutableListOf()
                                    }
                                    relatedEntitiesMap[originalTableName]?.add(neededObject)
                                }
                            }
                        }
                        field.isOneToManyRelation -> {
                            // TODO
                        }
//                        else -> hashMap[key.fieldAdjustment()] = inputEntity[key]
                    }
                }
            }
        }

        val sortedMap = hashMap.toSortedMap()
        var k = 0
        for ((_, value) in sortedMap) {
            outputEntity[k] = value
            k++
        }
        return outputEntity
    }
}