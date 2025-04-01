package org.autojs.autojs.devplugin.message

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 测试Gson处理null值的功能
 */
class GsonNullHandlingTest {

    @Test
    @DisplayName("测试Gson默认不序列化null值")
    fun testGsonNullHandling() {
        // 测试数据类
        data class TestData(val id: String, val nullValue: String? = null, val emptyValue: String = "")
        
        val testData = TestData("test-id", null, "")
        
        // 使用序列化null值的Gson
        val gsonWithNulls = GsonBuilder().serializeNulls().create()
        val jsonWithNulls = gsonWithNulls.toJson(testData)
        val jsonWithNullsObj = JsonParser.parseString(jsonWithNulls).asJsonObject
        
        // 验证结果
        assertTrue(jsonWithNullsObj.has("nullValue"), "serializeNulls()设置应该包含null值字段")
        assertEquals("null", jsonWithNullsObj.get("nullValue").toString(), "null值字段应该被序列化为null")
        
        // 使用默认Gson配置（不序列化null值）
        val defaultGson = Gson()
        val defaultJson = defaultGson.toJson(testData)
        val defaultJsonObj = JsonParser.parseString(defaultJson).asJsonObject
        
        // 验证结果
        assertFalse(defaultJsonObj.has("nullValue"), "默认Gson配置应该排除null值字段")
        assertTrue(defaultJsonObj.has("emptyValue"), "默认Gson配置不应排除空字符串字段")
    }
    
    @Test
    @DisplayName("测试MsgResponse.toJson方法")
    fun testMsgResponseToJson() {
        // 创建测试数据 - null md5
        val nullMd5Command = Command(CommandType.SAVE, "test1.js", "test1.js", "script1")
        val nullMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-1",
            data = nullMd5Command,
            md5 = null
        )
        
        // 创建测试数据 - 空字符串md5
        val emptyMd5Command = Command(CommandType.RUN, "test2.js", "test2.js", "script2")
        val emptyMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-2",
            data = emptyMd5Command,
            md5 = ""
        )
        
        // 创建测试数据 - 有值的md5
        val valueMd5Command = Command(CommandType.STOP, "test3.js", "test3.js")
        val valueMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-3",
            data = valueMd5Command,
            md5 = "valid-md5"
        )
        
        // 转换为JSON
        val nullMd5Json = nullMd5Response.toJson()
        val emptyMd5Json = emptyMd5Response.toJson()
        val valueMd5Json = valueMd5Response.toJson()
        
        // 解析JSON
        val nullMd5JsonObj = JsonParser.parseString(nullMd5Json).asJsonObject
        val emptyMd5JsonObj = JsonParser.parseString(emptyMd5Json).asJsonObject
        val valueMd5JsonObj = JsonParser.parseString(valueMd5Json).asJsonObject
        
        // 验证结果
        assertFalse(nullMd5JsonObj.has("md5"), "null md5字段应该被过滤掉")
        assertFalse(emptyMd5JsonObj.has("md5"), "空字符串md5字段应该被过滤掉")
        assertTrue(valueMd5JsonObj.has("md5"), "有效md5字段应该被保留")
        assertEquals("valid-md5", valueMd5JsonObj.get("md5").asString, "md5值应该正确")
    }
} 