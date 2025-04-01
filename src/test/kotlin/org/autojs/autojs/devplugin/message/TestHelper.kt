package org.autojs.autojs.devplugin.message

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * 测试辅助类
 */
object TestHelper {
    
    /**
     * 解析JSON字符串为JsonObject
     */
    fun parseJson(json: String): JsonObject {
        return JsonParser.parseString(json).asJsonObject
    }
    
    /**
     * 验证通用的Command字段
     */
    fun assertCommandFields(jsonObject: JsonObject, expectedType: String, expectedCommandType: String) {
        assertEquals(expectedType, jsonObject.get("type").asString, "消息类型应该匹配")
        assertTrue(jsonObject.has("message_id"), "应该包含message_id字段")
        
        val data = jsonObject.getAsJsonObject("data")
        assertEquals(expectedCommandType, data.get("command").asString, "命令类型应该匹配")
    }
    
    /**
     * 验证脚本字段
     */
    fun assertScriptField(data: JsonObject, expectedScript: String) {
        assertTrue(data.has("script"), "应该包含script字段")
        assertEquals(expectedScript, data.get("script").asString, "script内容应该匹配")
    }
    
    /**
     * 验证资源标识字段
     */
    fun assertResourceFields(data: JsonObject, expectedId: String, expectedName: String) {
        assertEquals(expectedId, data.get("id").asString, "id字段应该匹配")
        assertEquals(expectedName, data.get("name").asString, "name字段应该匹配") 
    }
    
    /**
     * 创建用于测试的对象转JSON实例
     */
    fun createTestGson() = GsonConfig.createGson()
} 