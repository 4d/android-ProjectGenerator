import org.json.JSONArray
import org.json.JSONObject

class SqlQueryBuilder(entry: Any, private val fields: List<Field>) {
    /**
     * inputEntities can be a JSONArray, or a JSONObject.
     * It is a JSONArray when it receives a list of entities
     * It is a JSONObject when it's a related entity decoded from another table entities
     */

    val outputEntities = arrayListOf<Array<Any?>>()
    val hashMap = mutableMapOf<String, Any?>()
    val relatedEntitiesMap = mutableMapOf<String, MutableList<JSONObject>>()
    val updateQueryHolderList = mutableListOf<UpdateQueryHolder>()

    init {
        println("SqlQueryBuilder : fields = ${fields.joinToString { it.name }}")

        when (entry) {
            is JSONArray -> {
                for (i in 0 until entry.length()) {

                    hashMap["__KEY"] = null
                    hashMap["__TIMESTAMP"] = null
                    hashMap["__STAMP"] = null
                    fields.forEach { field ->
                        hashMap[field.name.fieldAdjustment()] = null
                        if (field.kind == "relatedEntity")
                            hashMap["__${field.name.fieldAdjustment()}Key"] = null
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
                    if (field.kind == "relatedEntity")
                        hashMap["__${field.name.fieldAdjustment()}Key"] = null
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
                println("inputEntity de Key is :")
                println("${inputEntity[key]}")
                fields.find { f -> f.name.fieldAdjustment() == key.fieldAdjustment() }?.let { field ->
                    println("field is $field")
                    when {
//                        field.isImage -> {
//                            // Nothing to do
//                        }
                        field.kind == "relatedEntity" -> {
                            val neededObject = hashMap[key.fieldAdjustment()]

                            if (neededObject is JSONObject) {
                                hashMap["__${key.fieldAdjustment()}Key"] = neededObject.getSafeString("__KEY")
                                hashMap[key.fieldAdjustment()] = null

                                // add the relation in a new SqlQuery
                                field.fieldTypeString?.let { originalTableName ->
                                    if (!relatedEntitiesMap.containsKey(originalTableName)) {
                                        relatedEntitiesMap[originalTableName] = mutableListOf()
                                    }
                                    relatedEntitiesMap[originalTableName]?.add(neededObject)
                                    println("relatedEntitiesMap[originalTableName] = ${relatedEntitiesMap[originalTableName]}")
                                }
                            }
                        }
                        field.kind == "relatedEntities" -> {
                            val neededObject = hashMap[key.fieldAdjustment()]
                            if (neededObject is JSONObject) {

                                hashMap[key.fieldAdjustment()] = null

                                neededObject.getSafeArray("__ENTITIES")?.let { entities ->

                                    for (i in 0 until entities.length()) {

                                        entities.getJSONObject(i)?.getSafeString("__KEY")?.let { entityKey ->
                                            inputEntity.getSafeString("__KEY")?.let { thisKey ->
                                                neededObject.getSafeString("__DATACLASS")?.let { relatedDataClass ->

                                                    val updateQueryHolder = UpdateQueryHolder(
                                                        relatedDataClass = relatedDataClass.tableNameAdjustment(),
                                                        oneToManyRelationName = field.name.fieldAdjustment(),
                                                        entityKey = entityKey,
                                                        relationKey = thisKey
                                                    )
                                                    updateQueryHolderList.add(updateQueryHolder)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
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

data class UpdateQueryHolder(
    val relatedDataClass: String,
    val oneToManyRelationName: String,
    val entityKey: String,
    val relationKey: String
)