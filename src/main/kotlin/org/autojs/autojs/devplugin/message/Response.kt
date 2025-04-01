package org.autojs.autojs.devplugin.message

import com.google.gson.annotations.SerializedName
import org.autojs.autojs.devplugin.service.Helper

/**
 * 消息类型枚举
 */
enum class TotalType(val value: String) {
    BYTES_COMMAND("bytes_command"),  // 字节类命令（通常涉及文件传输）
    COMMAND("command")               // 普通指令命令
}

/**
 * 命令类型枚举
 */
enum class CommandType(val value: String) {
    // 传输文件夹相关命令
    SAVE_PROJECT("save_project"),    // 保存项目(保存文件夹)
    RUN_PROJECT("run_project"),      // 运行项目(保存并运行文件夹,但是文件夹里必须要有project.json文件)

    // 单文件相关命令
    SAVE("save"),                    // 保存文件
    RUN("run"),                      // 运行文件
    RE_RUN("rerun"),                 // 重新运行文件
    STOP("stop"),                    // 停止运行文件
    STOP_ALL("stopAll")              // 停止所有脚本
}

/**
 * 普通的命令信息数据类
 */
data class Command(
    @SerializedName("command")
    var command: CommandType,
    @SerializedName("id")
    val id: String? = null,
    @SerializedName("name")
    val name: String? = null,
    @SerializedName("script")
    val script: String? = null
) {
    companion object {
        /**
         * 创建保存项目的命令
         * @param dirName 目录名称
         * @param md5 文件MD5值
         * @return JSON字符串
         */
        fun saveProject(dirName: String, md5: String): String =
            createBytesCommand(CommandType.SAVE_PROJECT, dirName, dirName, md5)

        /**
         * 创建运行项目的命令
         * @param dirName 目录名称
         * @param md5 文件MD5值
         * @return JSON字符串
         */
        fun runProject(dirName: String, md5: String): String =
            createBytesCommand(CommandType.RUN_PROJECT, dirName, dirName, md5)

        /**
         * 创建保存文件的命令
         * @param fileName 文件名
         * @param script 脚本内容
         * @return JSON字符串
         */
        fun save(fileName: String, script: String): String = 
            createScriptCommand(CommandType.SAVE, fileName, fileName, script)

        /**
         * 创建运行文件的命令
         * @param fileName 文件名
         * @param script 脚本内容
         * @return JSON字符串
         */
        fun run(fileName: String, script: String): String = 
            createScriptCommand(CommandType.RUN, fileName, fileName, script)

        /**
         * 创建重新运行文件的命令
         * @param fileName 文件名
         * @param script 脚本内容
         * @return JSON字符串
         */
        fun reRun(fileName: String, script: String): String = 
            createScriptCommand(CommandType.RE_RUN, fileName, fileName, script)

        /**
         * 创建停止运行文件的命令
         * @param fileName 文件名
         * @return JSON字符串
         */
        fun stop(fileName: String): String = 
            createSimpleCommand(CommandType.STOP, fileName)

        /**
         * 创建停止所有脚本的命令
         * @return JSON字符串
         */
        fun stopAll(): String = 
            createSimpleCommand(CommandType.STOP_ALL)

        /**
         * 创建通用的命令响应
         * @param type 消息类型
         * @param commandType 命令类型
         * @param id 资源ID
         * @param name 资源名称
         * @param script 脚本内容
         * @param md5 文件MD5值
         * @return JSON字符串
         */
        private fun createCommand(
            type: TotalType,
            commandType: CommandType,
            id: String? = null,
            name: String? = null,
            script: String? = null,
            md5: String? = null
        ): String {
            val command = Command(
                command = commandType,
                id = id,
                name = name,
                script = script
            )
            
            return genMsgResponse(type, md5, command).toJson()
        }
        
        /**
         * 创建字节命令（带MD5的命令，通常用于传输文件）
         */
        private fun createBytesCommand(
            commandType: CommandType,
            id: String,
            name: String,
            md5: String
        ): String = createCommand(TotalType.BYTES_COMMAND, commandType, id, name, null, md5)
        
        /**
         * 创建脚本命令（带脚本内容的命令）
         */
        private fun createScriptCommand(
            commandType: CommandType,
            id: String,
            name: String,
            script: String
        ): String = createCommand(TotalType.COMMAND, commandType, id, name, script)
        
        /**
         * 创建简单命令（只带ID或不带任何参数的命令）
         */
        private fun createSimpleCommand(
            commandType: CommandType,
            id: String? = null
        ): String = createCommand(TotalType.COMMAND, commandType, id)

        /**
         * 生成消息响应对象
         */
        private fun genMsgResponse(type: TotalType, md5: String? = null, command: Command): MsgResponse {
            return MsgResponse(
                type = type,
                messageId = Helper.generateMessageId(),
                md5 = md5,
                data = command
            )
        }
    }

    /**
     * 将命令转换为JSON字符串
     * @param type 消息类型
     * @return JSON字符串
     */
    fun toJson(type: TotalType): String {
        val response = MsgResponse(
            type = type,
            messageId = Helper.generateMessageId(),
            data = this,
        )
        return response.toJson()
    }
}

/**
 * 服务端往客户端发送的命令的数据结构
 */
data class MsgResponse(
    @SerializedName("type")
    val type: TotalType,
    @SerializedName("message_id")
    val messageId: String,
    @SerializedName("data")
    val data: Command,
    /**
     * 可选的md5字段，通常用于表示文件数据的MD5校验值
     * 如果该字段为null，在转换为JSON时会被自动过滤掉
     * 如果是空字符串，需要先转为null再序列化
     */
    @SerializedName("md5")
    val md5: String? = null
) {
    /**
     * 将响应对象转换为JSON字符串
     * 如果md5为null或空字符串，则在JSON中省略该字段
     * @return JSON字符串
     */
    fun toJson(): String {
        // 使用默认Gson配置（不序列化null值）
        val gson = GsonConfig.createGson()
        
        // 如果md5是空字符串，将其视为null处理
        return if (md5?.isEmpty() == true) {
            // 创建一个不包含md5字段（md5设为null）的新对象
            val responseWithoutMd5 = MsgResponse(
                type = this.type,
                messageId = this.messageId,
                data = this.data,
                md5 = null
            )
            gson.toJson(responseWithoutMd5)
        } else {
            gson.toJson(this)
        }
    }
}