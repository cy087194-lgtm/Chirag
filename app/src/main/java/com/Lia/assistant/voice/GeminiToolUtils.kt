package com.Lia.assistant.voice

import org.json.JSONArray
import org.json.JSONObject

object GeminiToolUtils {
    /**
     * Builds a Gemini function declaration JSONObject:
     * {
     *   "name": name,
     *   "description": description,
     *   "parameters": {
     *     "type": "OBJECT",
     *     "properties": {
     *       "param1": {"type": "STRING", "description": "..."},
     *       ...
     *     },
     *     "required": ["param1", ...]
     *   }
     * }
     */
    fun functionDecl(
        name: String,
        description: String,
        params: Map<String, String> = emptyMap(),
        required: List<String> = emptyList()
    ): JSONObject {
        val decl = JSONObject()
        decl.put("name", name)
        decl.put("description", description)

        val parameters = JSONObject()
        parameters.put("type", "OBJECT")

        val properties = JSONObject()
        for ((paramName, paramDesc) in params) {
            val prop = JSONObject()
            prop.put("type", "STRING")
            prop.put("description", paramDesc)
            properties.put(paramName, prop)
        }
        parameters.put("properties", properties)

        val reqArray = JSONArray()
        for (r in required) {
            reqArray.put(r)
        }
        parameters.put("required", reqArray)

        decl.put("parameters", parameters)
        return decl
    }
}
