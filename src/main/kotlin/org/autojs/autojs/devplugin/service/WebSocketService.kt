package org.autojs.autojs.devplugin.service

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
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
import org.autojs.autojs.devplugin.message.HelloResponse
import org.autojs.autojs.devplugin.message.Message
import org.autojs.autojs.devplugin.settings.AutoXSettings
import org.autojs.autojs.devplugin.util.NetworkUtil
import java.io.InputStream
import java.net.InetSocketAddress
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class WebSocketService(project: Project) : Disposable {
    private val logger = Logger.getInstance(WebSocketService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gson = Gson()
    private val settings = AutoXSettings.getInstance(project)
    
    private var server: ApplicationEngine? = null
    private val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private var serverPort = 0
    private var isRunning = false
    
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
                                connections.remove(sessionId)
                                logger.info("Client disconnected: $sessionId")
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
        
        server?.stop(1000, 2000)
        server = null
        connections.clear()
        isRunning = false
        serverPort = 0
        logger.info("WebSocket server stopped")
    }
    
    fun isRunning(): Boolean = isRunning
    
    fun getServerPort(): Int = serverPort
    
    fun getServerAddress(): String? {
        if (!isRunning) return null
        val ipAddress = NetworkUtil.getLocalIpAddress() ?: return null
        return "ws://$ipAddress:$serverPort"
    }
    
    fun getConnectedDeviceCount(): Int = connections.size
    
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
    
    private suspend fun handleIncomingMessage(message: String, sessionId: String) {
        try {
            val msg = gson.fromJson(message, Message::class.java)
            
            when (msg.type) {
                "hello" -> {
                    // Respond to hello message
                    val response = HelloResponse(
                        data = "connected",
                        debug = false,
                        messageId = "",
                        type = "hello_response",
                        version = "1.0"
                    )
                    connections[sessionId]?.send(gson.toJson(response))
                    logger.info("Client connected: $sessionId")
                    
                    // Update connected devices list in settings
                    val connectedDevices = settings.state.lastConnectedDevices.toMutableList()
                    if (!connectedDevices.contains(sessionId)) {
                        connectedDevices.add(sessionId)
                        settings.state.lastConnectedDevices = connectedDevices
                    }
                }
                else -> {
                    logger.info("Received message of type: ${msg.type}")
                }
            }
        } catch (e: Exception) {
            logger.error("Error handling message", e)
        }
    }
    
    override fun dispose() {
        stop()
    }
    
    companion object {
        fun getInstance(project: Project): WebSocketService = project.service()
    }
} 