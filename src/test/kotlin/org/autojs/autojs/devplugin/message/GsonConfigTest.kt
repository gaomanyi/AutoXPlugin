package org.autojs.autojs.devplugin.message

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 测试GsonConfig类的自定义枚举适配器
 */
class GsonConfigTest {

    @Test
    @DisplayName("测试自定义Gson适配器 - 确保枚举被序列化为value值")
    fun testEnumSerialization() {
        // 准备测试数据
        val command = Command(
            command = CommandType.SAVE,
            id = "test.js",
            name = "test.js",
            script = "console.log('test')"
        )
        
        // 使用自定义Gson序列化
        val customGson = GsonConfig.createGson()
        val customJson = customGson.toJson(command)
        
        // 使用标准Gson序列化
        val standardGson = Gson()
        val standardJson = standardGson.toJson(command)
        
        // 解析结果
        val customJsonObj = JsonParser.parseString(customJson).asJsonObject
        val standardJsonObj = JsonParser.parseString(standardJson).asJsonObject
        
        // 验证结果
        assertEquals("save", customJsonObj.get("command").asString, 
            "自定义Gson应将CommandType.SAVE序列化为'save'")
        assertNotEquals("save", standardJsonObj.get("command").asString, 
            "标准Gson不会将CommandType.SAVE序列化为'save'")
    }
    
    @Test
    @DisplayName("测试MsgResponse序列化 - 确保枚举被序列化为value值")
    fun testMsgResponseSerialization() {
        // 准备测试数据
        val command = Command(CommandType.RUN, "test.js", "test.js", "console.log('test')")
        val response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-123",
            data = command,
            md5 = "md5-value"
        )
        
        // 序列化并解析
        val json = response.toJson()
        val jsonObj = JsonParser.parseString(json).asJsonObject
        
        // 验证结果
        assertEquals("command", jsonObj.get("type").asString, 
            "type字段应被序列化为'command'而不是'COMMAND'")
        
        val dataObj = jsonObj.getAsJsonObject("data")
        assertEquals("run", dataObj.get("command").asString, 
            "command字段应被序列化为'run'而不是'RUN'")
    }
    
    @Test
    @DisplayName("测试Command工厂方法 - 确保所有命令类型正确序列化")
    fun testCommandFactoryMethods() {
        // 测试所有工厂方法生成的命令
        val saveProjectJson = Command.saveProject("dir1", "md5-1")
        val runProjectJson = Command.runProject("dir2", "md5-2")
        val saveJson = Command.save("file.js", "code1")
        val runJson = Command.run("file.js", "code2")
        val reRunJson = Command.reRun("file.js", "code3")
        val stopJson = Command.stop("file.js")
        val stopAllJson = Command.stopAll()
        
        // 解析所有JSON
        val jsonObjects = listOf(
            JsonParser.parseString(saveProjectJson).asJsonObject,
            JsonParser.parseString(runProjectJson).asJsonObject,
            JsonParser.parseString(saveJson).asJsonObject,
            JsonParser.parseString(runJson).asJsonObject,
            JsonParser.parseString(reRunJson).asJsonObject,
            JsonParser.parseString(stopJson).asJsonObject,
            JsonParser.parseString(stopAllJson).asJsonObject
        )
        
        // 验证命令类型值
        val dataObjects = jsonObjects.map { it.getAsJsonObject("data") }
        val commandValues = listOf(
            "save_project", "run_project", "save", "run", "rerun", "stop", "stopAll"
        )
        
        for (i in dataObjects.indices) {
            assertEquals(commandValues[i], dataObjects[i].get("command").asString,
                "命令类型应该被序列化为正确的value值")
        }
        
        // 验证消息类型值
        val typeValues = listOf(
            "bytes_command", "bytes_command", "command", "command", "command", "command", "command"
        )
        
        for (i in jsonObjects.indices) {
            assertEquals(typeValues[i], jsonObjects[i].get("type").asString,
                "消息类型应该被序列化为正确的value值")
        }
    }
} 