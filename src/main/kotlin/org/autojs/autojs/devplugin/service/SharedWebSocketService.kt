package org.autojs.autojs.devplugin.service

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.autojs.autojs.devplugin.message.*
import org.autojs.autojs.devplugin.util.NetworkUtil
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * 应用级别的WebSocket服务
 * 用于多个IDEA实例之间共享连接状态
 */
@Service(Service.Level.APP)
class SharedWebSocketService : Disposable {
    private val DEBUG = false
    private val logger = Logger.getInstance(SharedWebSocketService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()

    private var server: ApplicationEngine? = null
    private val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()

    // 存储连接设备的详细信息
    private val connectedDevices = ConcurrentHashMap<String, ConnectedDevice>()
    private var serverPort = 0
    private var isRunning = false

    // 添加连接状态监听器 - 每个Project实例注册自己的监听器
    private val connectionListeners = ConcurrentHashMap<String, MutableList<ConnectionListener>>()

    fun start(port: Int) {
        if (isRunning) {
            logger.info("WebSocket server is already running")
            return
        }

        scope.launch {
            try {
                server = embeddedServer(Netty, port = port) {
                    install(WebSockets) {
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(60)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false
                    }

                    routing {
                        webSocket("/") {
                            val sessionId = call.request.local.remoteHost
                            connections[sessionId] = this

                            try {
                                for (frame in incoming) {
                                    if (frame is Frame.Text) {
                                        val text = frame.readText()
                                        handleIncomingMessage(text, sessionId)
                                    }
                                }
                            } catch (e: Exception) {
                                logger.error("Error in WebSocket connection", e)
                            } finally {
                                disconnectClient(sessionId)
                            }
                        }
                    }
                }.start(wait = false)

                val inetSocketAddress = server?.environment?.connectors?.get(0)?.host?.let { host ->
                    InetSocketAddress(host, port)
                } ?: InetSocketAddress(port)

                serverPort = inetSocketAddress.port
                isRunning = true
                logger.info("Shared WebSocket server started on port: $serverPort")
            } catch (e: Exception) {
                logger.error("Failed to start Shared WebSocket server", e)
            }
        }
    }

    fun stop() {
        if (!isRunning) return

        scope.launch {
            try {
                // 发送关闭消息到所有连接的客户端
                connections.forEach { (sessionId, session) ->
                    try {
                        val closeMessage = Message(
                            type = MessageType.CLOSE,
                            data = "Server shutdown"
                        )
                        session.send(gson.toJson(closeMessage))
                    } catch (e: Exception) {
                        logger.error("Error sending close message to $sessionId", e)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error during server shutdown", e)
            } finally {
                server?.stop(1000, 2000)
                server = null

                // 清除所有连接
                val oldConnections = ArrayList(connectedDevices.keys)
                connections.clear()
                connectedDevices.clear()
                isRunning = false
                serverPort = 0

                // 通知所有连接断开
                oldConnections.forEach { sessionId ->
                    notifyConnectionClosed(sessionId)
                }

                logger.info("Shared WebSocket server stopped")
            }
        }
    }

    // 断开单个客户端连接
    fun disconnectClient(sessionId: String) {
        scope.launch {
            try {
                // 发送关闭消息给客户端
                connections[sessionId]?.let { session ->
                    val closeMessage = Message(
                        type = MessageType.CLOSE,
                        data = "Server initiated disconnect"
                    )
                    session.send(gson.toJson(closeMessage))
                    session.close()
                }
            } catch (e: Exception) {
                logger.error("Error disconnecting client: $sessionId", e)
            } finally {
                // 无论是否发送成功，都从集合中移除
                connections.remove(sessionId)
                val device = connectedDevices.remove(sessionId)

                if (device != null) {
                    logger.info("Client disconnected: $sessionId (${device.deviceName})")
                    notifyConnectionClosed(sessionId)
                }
            }
        }
    }

    fun isRunning(): Boolean = isRunning

    fun getServerPort(): Int = serverPort

    fun getServerAddress(): String? {
        if (!isRunning) return null
        val ipAddress = NetworkUtil.getLocalIpAddress() ?: return null
        return "ws://$ipAddress:$serverPort"
    }

    fun getConnectedDeviceCount(): Int = connectedDevices.size

    // 获取已连接设备列表
    fun getConnectedDevices(): List<ConnectedDevice> {
        return connectedDevices.values.toList()
    }

    /**
     * 检查设备是否连接
     */
    fun isDeviceConnected(deviceId: String): Boolean {
        return connectedDevices.containsKey(deviceId)
    }

    /**
     * 向设备列表发送命令
     */
    fun sendCommandToDevices(json: String, targetDevices: List<ConnectedDevice>) {
        if (!isRunning || connections.isEmpty()) {
            logger.warn("Cannot send command: server not running or no connections")
            return
        }
        scope.launch {
            try {
                if (targetDevices.isEmpty()) {
                    // Send to all devices if no target devices specified
                    connections.forEach { (sessionId, session) ->
                        session.send(json)
                    }
                } else {
                    // Send to specified devices only
                    for (device in targetDevices) {
                        val sessionId = device.sessionId
                        val session = connections[sessionId]
                        session?.send(json)
                    }
                }
            } catch (e: Exception) {
                logger.error("Error sending command to devices", e)
            }
        }
    }

    // 向设备列表发送附带文件byte的命令
    fun sendCommandToDevices(
        bytes: ByteArray,
        json: String,
        targetDevices: List<ConnectedDevice>
    ) {
        if (!isRunning || connections.isEmpty()) {
            logger.warn("Cannot send folder: server not running or no connections")
            return
        }

        scope.launch {
            try {
                if (targetDevices.isEmpty()) {
                    // Send to all devices if no target devices specified
                    connections.forEach { (sessionId, session) ->
                        sendZip(bytes, json, session)
                    }
                } else {
                    // Send to specified devices only
                    for (device in targetDevices) {
                        val sessionId = device.sessionId
                        val session = connections[sessionId]
                        if (session != null) {
                            sendZip(bytes, json, session)
                        } else {
                            logger.warn("Device ${device.deviceName} not connected, cannot send folder")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error sending folder", e)
            }
        }
    }

    //向设备发送zip文件
    private suspend fun sendZip(bytes: ByteArray, json: String, session: DefaultWebSocketSession) {
        //先发文件
        session.send(bytes)
        //后发命令
        session.send(json)
    }

    // 添加连接监听器，标识特定项目的监听器
    fun addConnectionListener(projectId: String, listener: ConnectionListener) {
        connectionListeners.computeIfAbsent(projectId) { mutableListOf() }.add(listener)
    }

    // 移除连接监听器
    fun removeConnectionListener(projectId: String, listener: ConnectionListener) {
        connectionListeners[projectId]?.remove(listener)
        // 如果项目的监听器为空，移除该项目
        if (connectionListeners[projectId]?.isEmpty() == true) {
            connectionListeners.remove(projectId)
        }
    }

    // 清除特定项目的所有监听器
    fun clearProjectListeners(projectId: String) {
        connectionListeners.remove(projectId)
    }

    // 通知连接建立
    private fun notifyConnectionEstablished(sessionId: String, device: ConnectedDevice) {
        connectionListeners.forEach { (_, listeners) ->
            listeners.forEach { it.onConnectionEstablished(sessionId, device) }
        }
    }

    // 通知连接关闭
    private fun notifyConnectionClosed(sessionId: String) {
        connectionListeners.forEach { (_, listeners) ->
            listeners.forEach { it.onConnectionClosed(sessionId) }
        }
    }

    private suspend fun handleIncomingMessage(message: String, sessionId: String) {
        try {
            val msg = gson.fromJson(message, Message::class.java)
            logger.info("Received message from client $sessionId: $message")
            when (msg.type) {
                MessageType.PING -> {
                    // Respond to ping message
                    val pong = Message(
                        type = MessageType.PONG,
                        data = msg.data //将ping消息传过来的System.currentTimeMillis()原封不动返回
                    )
                    connections[sessionId]?.send(gson.toJson(pong))
                }

                MessageType.HELLO -> {
                    // 解析 data 为 Hello 对象
                    val helloData = try {
                        when (msg.data) {
                            is Map<*, *> -> gson.fromJson(gson.toJson(msg.data), Hello::class.java)
                            else -> {
                                val jsonObject = gson.fromJson(message, com.google.gson.JsonObject::class.java)
                                if (jsonObject.has("data") && jsonObject.get("data").isJsonObject) {
                                    val dataObject = jsonObject.getAsJsonObject("data")
                                    gson.fromJson(dataObject, Hello::class.java)
                                } else {
                                    null
                                }
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to parse Hello data: ${e.message}", e)
                        null
                    }

                    // 如果成功解析到 Hello 数据
                    if (helloData != null) {
                        // 构建并发送响应
                        val response = HelloResponse(
                            data = "ok",
                            debug = DEBUG,
                            messageId = Helper.generateMessageId(),
                            type = MessageType.HELLO,
                            version = Helper.getPluginVersion()
                        )
                        connections[sessionId]?.send(gson.toJson(response))

                        // 创建设备信息对象
                        val device = ConnectedDevice(
                            sessionId = sessionId,
                            deviceName = helloData.deviceName,
                            appVersion = helloData.appVersion
                        )

                        // 存储设备信息
                        connectedDevices[sessionId] = device

                        logger.info("Client connected: $sessionId - Device: ${device.deviceName}")

                        // 通知连接建立
                        notifyConnectionEstablished(sessionId, device)
                    } else {
                        logger.warn("收到了Hello消息，但无法解析Hello Data: $message")
                    }
                }

                MessageType.LOG -> {
                    // 处理日志消息
                    var logContent: String

                    try {
                        when (msg.data) {
                            is String -> {
                                // 尝试解析为 LogData
                                try {
                                    val logData = gson.fromJson(msg.data.toString(), LogData::class.java)
                                    if (logData != null && logData.log.isNotEmpty()) {
                                        logContent = logData.log
                                    } else {
                                        logContent = msg.data.toString()
                                    }
                                } catch (e: Exception) {
                                    // 如果无法解析为 LogData，直接使用原始字符串
                                    logContent = msg.data.toString()
                                }
                            }
                            is Map<*, *> -> {
                                // 将 Map 转换为 JSON 字符串
                                val jsonString = gson.toJson(msg.data)
                                // 尝试解析为 LogData
                                val logData = gson.fromJson(jsonString, LogData::class.java)
                                if (logData != null && logData.log.isNotEmpty()) {
                                    logContent = logData.log
                                } else {
                                    // 如果没有 log 字段，尝试获取 message 字段
                                    val jsonObject = gson.fromJson(jsonString, com.google.gson.JsonObject::class.java)
                                    logContent = jsonObject.get("message")?.asString ?: jsonString
                                }
                            }
                            else -> logContent = msg.data.toString()
                        }
                    } catch (e: Exception) {
                        logger.error("解析日志消息失败: ${e.message}", e)
                        logContent = "解析日志失败: $msg"
                    }
                    
                    // 获取设备信息
                    val device = connectedDevices[sessionId]
                    val deviceName = device?.deviceName ?: "Unknown Device"
                    
                    logger.info("收到设备[$deviceName]日志: $logContent")
                    
                    // 通知日志监听器
                    notifyLogReceived(sessionId, deviceName, logContent)
                }

                else -> {
                    logger.info("Received message of type: ${msg.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling message: $message", e)
        }
    }

    /**
     * 向设备列表发送文本命令（不含二进制数据的命令）
     * 例如：保存脚本、运行脚本、停止脚本等
     */
    fun sendTextCommandToDevices(
        json: String,
        targetDevices: List<ConnectedDevice>
    ) {
        if (!isRunning || connections.isEmpty()) {
            logger.warn("Cannot send text command: server not running or no connections")
            return
        }

        scope.launch {
            try {
                if (targetDevices.isEmpty()) {
                    // Send to all devices if no target devices specified
                    connections.forEach { (sessionId, session) ->
                        session.send(json)
                    }
                    logger.info("Text command sent to all connected devices")
                } else {
                    // Send to specified devices only
                    for (device in targetDevices) {
                        val sessionId = device.sessionId
                        val session = connections[sessionId]
                        if (session != null) {
                            session.send(json)
                            logger.info("Text command sent to device: ${device.deviceName}")
                        } else {
                            logger.warn("Device ${device.deviceName} not connected, cannot send command")
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("Error sending text command", e)
            }
        }
    }

    // 通知日志消息接收
    private fun notifyLogReceived(sessionId: String, deviceName: String, logMessage: String) {
        connectionListeners.forEach { (_, listeners) ->
            listeners.forEach { it.onLogReceived(sessionId, deviceName, logMessage) }
        }
    }

    override fun dispose() {
        stop()
    }

    companion object {
        fun getInstance(): SharedWebSocketService = ApplicationManager.getApplication().service()
    }
} 