package org.autojs.autojs.devplugin.message

import org.autojs.autojs.devplugin.message.TestHelper.assertCommandFields
import org.autojs.autojs.devplugin.message.TestHelper.assertResourceFields
import org.autojs.autojs.devplugin.message.TestHelper.assertScriptField
import org.autojs.autojs.devplugin.message.TestHelper.parseJson
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Response类的测试用例
 */
class ResponseTest {

    @Test
    @DisplayName("测试MsgResponse的toJson方法 - 当md5为null时")
    fun testMsgResponseToJsonWithNullMd5() {
        // 创建测试数据
        val command = Command(CommandType.SAVE, "testId", "testName", "testScript")
        val response = MsgResponse(TotalType.COMMAND, "testMessageId", command)
        
        // 执行测试方法
        val json = response.toJson()
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertEquals("command", jsonObject.get("type").asString)
        assertEquals("testMessageId", jsonObject.get("message_id").asString)
        assertFalse(jsonObject.has("md5"), "JSON不应该包含md5字段")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertEquals("save", data.get("command").asString)
        assertResourceFields(data, "testId", "testName")
        assertScriptField(data, "testScript")
    }
    
    @Test
    @DisplayName("测试MsgResponse的toJson方法 - 当md5为空字符串时")
    fun testMsgResponseToJsonWithEmptyMd5() {
        // 创建测试数据
        val command = Command(CommandType.SAVE, "testId", "testName", "testScript")
        val response = MsgResponse(TotalType.COMMAND, "testMessageId", command, "")
        
        // 执行测试方法
        val json = response.toJson()
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertFalse(jsonObject.has("md5"), "JSON不应该包含md5字段")
    }
    
    @Test
    @DisplayName("测试MsgResponse的toJson方法 - 当md5有值时")
    fun testMsgResponseToJsonWithMd5() {
        // 创建测试数据
        val command = Command(CommandType.SAVE, "testId", "testName", "testScript")
        val response = MsgResponse(TotalType.COMMAND, "testMessageId", command, "testMd5")
        
        // 执行测试方法
        val json = response.toJson()
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertTrue(jsonObject.has("md5"), "JSON应该包含md5字段")
        assertEquals("testMd5", jsonObject.get("md5").asString)
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - saveProject")
    fun testSaveProjectCommand() {
        // 执行测试方法
        val json = Command.saveProject("testDir", "testMd5")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "bytes_command", "save_project")
        assertEquals("testMd5", jsonObject.get("md5").asString)
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "testDir", "testDir")
        assertFalse(data.has("script"), "不应该包含script字段")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - runProject")
    fun testRunProjectCommand() {
        // 执行测试方法
        val json = Command.runProject("testDir", "testMd5")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "bytes_command", "run_project")
        assertEquals("testMd5", jsonObject.get("md5").asString)
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "testDir", "testDir")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - save")
    fun testSaveCommand() {
        // 执行测试方法
        val json = Command.save("test.js", "console.log('test')")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "save")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "test.js", "test.js")
        assertScriptField(data, "console.log('test')")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - run")
    fun testRunCommand() {
        // 执行测试方法
        val json = Command.run("test.js", "console.log('test')")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "run")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "test.js", "test.js")
        assertScriptField(data, "console.log('test')")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - reRun")
    fun testReRunCommand() {
        // 执行测试方法
        val json = Command.reRun("test.js", "console.log('test')")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "rerun")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "test.js", "test.js")
        assertScriptField(data, "console.log('test')")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - stop")
    fun testStopCommand() {
        // 执行测试方法
        val json = Command.stop("test.js")
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "stop")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertEquals("test.js", data.get("id").asString)
        assertFalse(data.has("script"), "不应该包含script字段")
    }
    
    @Test
    @DisplayName("测试Command的工厂方法 - stopAll")
    fun testStopAllCommand() {
        // 执行测试方法
        val json = Command.stopAll()
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "stopAll")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertFalse(data.has("id"), "不应该包含id字段")
        assertFalse(data.has("script"), "不应该包含script字段")
    }
    
    @Test
    @DisplayName("测试Command的toJson方法")
    fun testCommandToJson() {
        // 创建测试数据
        val command = Command(CommandType.RUN, "testId", "testName", "testScript")
        
        // 执行测试方法
        val json = command.toJson(TotalType.COMMAND)
        
        // 验证结果
        val jsonObject = parseJson(json)
        assertCommandFields(jsonObject, "command", "run")
        
        // 验证data部分
        val data = jsonObject.getAsJsonObject("data")
        assertResourceFields(data, "testId", "testName")
        assertScriptField(data, "testScript")
    }
} 