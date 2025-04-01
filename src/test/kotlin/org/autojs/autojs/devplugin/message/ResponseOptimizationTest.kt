package org.autojs.autojs.devplugin.message

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.autojs.autojs.devplugin.message.TestHelper.parseJson

/**
 * 测试优化后的Response结构，特别是重构的工厂方法
 */
class ResponseOptimizationTest {

    @Test
    @DisplayName("测试通用工厂方法生成的JSON结构正确性")
    fun testFactoryMethodsConsistency() {
        // 生成不同类型的命令
        val saveProjectJson = Command.saveProject("dir1", "md5-1")
        val runProjectJson = Command.runProject("dir2", "md5-2")
        val saveFileJson = Command.save("file.js", "code1")
        val runFileJson = Command.run("file.js", "code1")
        val reRunFileJson = Command.reRun("file.js", "code1")
        val stopFileJson = Command.stop("file.js")
        val stopAllJson = Command.stopAll()
        
        // 解析为JSON对象
        val saveProjectObj = parseJson(saveProjectJson)
        val runProjectObj = parseJson(runProjectJson)
        val saveFileObj = parseJson(saveFileJson)
        val runFileObj = parseJson(runFileJson)
        val reRunFileObj = parseJson(reRunFileJson)
        val stopFileObj = parseJson(stopFileJson)
        val stopAllObj = parseJson(stopAllJson)
        
        // 验证所有命令都有必须的字段
        val allJsonObjects = listOf(
            saveProjectObj, runProjectObj, saveFileObj, 
            runFileObj, reRunFileObj, stopFileObj, stopAllObj
        )
        
        for (jsonObj in allJsonObjects) {
            assertTrue(jsonObj.has("type"), "所有命令应该有type字段")
            assertTrue(jsonObj.has("message_id"), "所有命令应该有message_id字段")
            assertTrue(jsonObj.has("data"), "所有命令应该有data字段")
            assertTrue(jsonObj.getAsJsonObject("data").has("command"), "所有data都应该有command字段")
        }
        
        // 验证字节命令有md5字段
        assertTrue(saveProjectObj.has("md5"), "SAVE_PROJECT命令应该有md5字段")
        assertTrue(runProjectObj.has("md5"), "RUN_PROJECT命令应该有md5字段")
        
        // 验证普通命令没有md5字段
        assertFalse(saveFileObj.has("md5"), "SAVE命令不应该有md5字段")
        assertFalse(runFileObj.has("md5"), "RUN命令不应该有md5字段")
        assertFalse(reRunFileObj.has("md5"), "RE_RUN命令不应该有md5字段")
        assertFalse(stopFileObj.has("md5"), "STOP命令不应该有md5字段")
        assertFalse(stopAllObj.has("md5"), "STOP_ALL命令不应该有md5字段")
    }
    
    @Test
    @DisplayName("测试类型安全 - 确保枚举值与JSON字符串值匹配")
    fun testEnumValueConsistency() {
        // 测试TotalType枚举
        assertEquals("bytes_command", TotalType.BYTES_COMMAND.value)
        assertEquals("command", TotalType.COMMAND.value)
        
        // 测试CommandType枚举
        assertEquals("save_project", CommandType.SAVE_PROJECT.value)
        assertEquals("run_project", CommandType.RUN_PROJECT.value)
        assertEquals("save", CommandType.SAVE.value)
        assertEquals("run", CommandType.RUN.value)
        assertEquals("rerun", CommandType.RE_RUN.value)
        assertEquals("stop", CommandType.STOP.value)
        assertEquals("stopAll", CommandType.STOP_ALL.value)
    }
    
    @Test
    @DisplayName("测试命令参数的正确传递")
    fun testCommandParameterPassing() {
        // 测试脚本命令参数传递
        val scriptJson = Command.save("test.js", "script-content")
        val scriptObj = parseJson(scriptJson)
        val scriptData = scriptObj.getAsJsonObject("data")
        
        assertEquals("test.js", scriptData.get("id").asString)
        assertEquals("test.js", scriptData.get("name").asString)
        assertEquals("script-content", scriptData.get("script").asString)
        
        // 测试项目命令参数传递
        val projectJson = Command.saveProject("project-dir", "project-md5")
        val projectObj = parseJson(projectJson)
        val projectData = projectObj.getAsJsonObject("data")
        
        assertEquals("project-dir", projectData.get("id").asString)
        assertEquals("project-dir", projectData.get("name").asString)
        assertEquals("project-md5", projectObj.get("md5").asString)
        
        // 测试只有ID参数的命令
        val stopJson = Command.stop("file-to-stop")
        val stopObj = parseJson(stopJson)
        val stopData = stopObj.getAsJsonObject("data")
        
        assertEquals("file-to-stop", stopData.get("id").asString)
        assertFalse(stopData.has("script"), "STOP命令不应该有script字段")
        
        // 测试无参数命令
        val stopAllJson = Command.stopAll()
        val stopAllObj = parseJson(stopAllJson)
        val stopAllData = stopAllObj.getAsJsonObject("data")
        
        assertFalse(stopAllData.has("id"), "STOP_ALL命令不应该有id字段")
        assertFalse(stopAllData.has("name"), "STOP_ALL命令不应该有name字段")
        assertFalse(stopAllData.has("script"), "STOP_ALL命令不应该有script字段")
    }
    
    @Test
    @DisplayName("测试MsgResponse类的md5过滤功能")
    fun testMsgResponseMd5Filtering() {
        // 创建带有null md5的响应
        val nullMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-id-1",
            data = Command(CommandType.SAVE, "id1", "name1", "script1"),
            md5 = null
        )
        val nullMd5Json = nullMd5Response.toJson()
        val nullMd5Obj = parseJson(nullMd5Json)
        assertFalse(nullMd5Obj.has("md5"), "null md5不应该出现在JSON中")
        
        // 创建带有空字符串md5的响应
        val emptyMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-id-2",
            data = Command(CommandType.SAVE, "id2", "name2", "script2"),
            md5 = ""
        )
        val emptyMd5Json = emptyMd5Response.toJson()
        val emptyMd5Obj = parseJson(emptyMd5Json)
        assertFalse(emptyMd5Obj.has("md5"), "空字符串md5不应该出现在JSON中")
        
        // 创建带有有效md5的响应
        val validMd5Response = MsgResponse(
            type = TotalType.COMMAND,
            messageId = "msg-id-3",
            data = Command(CommandType.SAVE, "id3", "name3", "script3"),
            md5 = "valid-md5"
        )
        val validMd5Json = validMd5Response.toJson()
        val validMd5Obj = parseJson(validMd5Json)
        assertTrue(validMd5Obj.has("md5"), "有效md5应该出现在JSON中")
        assertEquals("valid-md5", validMd5Obj.get("md5").asString)
    }
} 