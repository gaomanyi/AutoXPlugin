package org.autojs.autojs.devplugin.message

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

/**
 * 测试检查JSON的实际输出
 */
class JsonOutputTest {

    @Test
    @DisplayName("输出枚举序列化结果")
    fun printEnumSerializationOutput() {
        // 创建测试命令
        val command = Command(CommandType.RUN, "test.js", "test.js", "console.log('hello')")
        val response = MsgResponse(TotalType.COMMAND, "msg-123", command)
        
        // 使用自定义Gson序列化并输出
        val json = response.toJson()
        println("使用自定义Gson序列化的结果:")
        println(json)
        
        // 测试所有工厂方法
        val saveProjectJson = Command.saveProject("dir1", "md5-1")
        val runProjectJson = Command.runProject("dir2", "md5-2")
        val saveJson = Command.save("file.js", "code1")
        val runJson = Command.run("file.js", "code2")
        val reRunJson = Command.reRun("file.js", "code3")
        val stopJson = Command.stop("file.js")
        val stopAllJson = Command.stopAll()
        
        println("\n使用工厂方法生成的JSON:")
        println("saveProject: $saveProjectJson")
        println("runProject: $runProjectJson")
        println("save: $saveJson")
        println("run: $runJson")
        println("reRun: $reRunJson")
        println("stop: $stopJson")
        println("stopAll: $stopAllJson")
    }
} 