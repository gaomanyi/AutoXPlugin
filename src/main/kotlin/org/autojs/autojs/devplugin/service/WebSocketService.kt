package org.autojs.autojs.devplugin.service

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.autojs.autojs.devplugin.message.Hello
import org.autojs.autojs.devplugin.message.HelloResponse
import org.autojs.autojs.devplugin.message.Message
import org.autojs.autojs.devplugin.message.MessageType
import org.autojs.autojs.devplugin.settings.AutoXSettings
import org.autojs.autojs.devplugin.util.NetworkUtil
import java.io.InputStream
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

// 表示连接的设备信息
data class ConnectedDevice(
    val id: String,
    val deviceName: String,
    val appVersion: String
)

@Service(Service.Level.PROJECT)
class WebSocketService(private val project: Project) : Disposable {
    private val DEBUG = false
    private val logger = Logger.getInstance(WebSocketService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val settings = AutoXSettings.getInstance(project)
    
    private var server: ApplicationEngine? = null
    private val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    // 存储连接设备的详细信息
    private val connectedDevices = ConcurrentHashMap<String, ConnectedDevice>()
    private var serverPort = 0
    private var isRunning = false
    
    // 添加连接状态监听器
    private val connectionListeners = mutableListOf<ConnectionListener>()
    
    init {
        // Auto-start the server if enabled in settings
        if (settings.state.autoStartServer) {
            start(settings.state.port)
        }
    }
    
    fun start(port: Int = settings.state.port) {
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
                            val sessionId = call.request.origin.remoteHost
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
                logger.info("WebSocket server started on port: $serverPort")
            } catch (e: Exception) {
                logger.error("Failed to start WebSocket server", e)
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
                
                logger.info("WebSocket server stopped")
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
    
    fun sendFile(fileName: String, inputStream: InputStream) {
        if (!isRunning || connections.isEmpty()) {
            logger.warn("Cannot send file: server not running or no connections")
            return
        }
        
        scope.launch {
            try {
                val fileBytes = inputStream.readBytes()
                val fileMessage = Message(
                    type = "file",
                    data = mapOf(
                        "name" to fileName,
                        "content" to fileBytes
                    )
                )
                val json = gson.toJson(fileMessage)
                
                connections.forEach { (sessionId, session) ->
                    session.send(json)
                    logger.info("File $fileName sent to client $sessionId")
                }
            } catch (e: Exception) {
                logger.error("Error sending file", e)
            }
        }
    }

    // 获取版本号的方法
    fun getPluginVersion(): String {
        val pluginId = PluginId.getId("org.autojs.autojs.devplugin") // 替换为您的插件ID
        val plugin = PluginManagerCore.getPlugin(pluginId)
        return plugin?.version ?: "Unknown"
    }
    
    // 添加连接监听器
    fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }
    
    // 移除连接监听器
    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }
    
    // 通知连接建立
    private fun notifyConnectionEstablished(sessionId: String, device: ConnectedDevice) {
        connectionListeners.forEach { it.onConnectionEstablished(sessionId, device) }
    }
    
    // 通知连接关闭
    private fun notifyConnectionClosed(sessionId: String) {
        connectionListeners.forEach { it.onConnectionClosed(sessionId) }
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
                MessageType.HELLO ->{
                    // 解析 data 为 Hello 对象
                    val helloData = try {
                        when (msg.data) {
                            is Map<*, *> -> gson.fromJson(gson.toJson(msg.data), Hello::class.java)
                            else -> {
                                val jsonObject = gson.fromJson(message, JsonObject::class.java)
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
                            messageId = "${System.currentTimeMillis()}_${Math.random()}",
                            type = MessageType.HELLO,
                            version = getPluginVersion()
                        )
                        connections[sessionId]?.send(gson.toJson(response))
                        
                        // 创建设备信息对象
                        val device = ConnectedDevice(
                            id = sessionId,
                            deviceName = helloData.deviceName,
                            appVersion = helloData.appVersion
                        )
                        
                        // 存储设备信息
                        connectedDevices[sessionId] = device
                        
                        logger.info("Client connected: $sessionId - Device: ${device.deviceName}")
                        
                        // 通知连接建立
                        notifyConnectionEstablished(sessionId, device)

                        // 更新已连接设备列表
                        val connectedDeviceIds = settings.state.lastConnectedDevices.toMutableList()
                        if (!connectedDeviceIds.contains(sessionId)) {
                            connectedDeviceIds.add(sessionId)
                            settings.state.lastConnectedDevices = connectedDeviceIds
                        }
                    } else {
                        logger.warn("收到了Hello消息，但无法解析Hello Data: $message")
                    }
                }
                else -> {
                    logger.info("Received message of type: ${msg.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling message: $message", e)
        }
    }
    
    override fun dispose() {
        stop()
    }
    
    companion object {
        fun getInstance(project: Project): WebSocketService = project.service()
    }
}

// 连接状态监听器接口
interface ConnectionListener {
    fun onConnectionEstablished(sessionId: String, device: ConnectedDevice)
    fun onConnectionClosed(sessionId: String)
} 