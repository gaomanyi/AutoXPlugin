package org.autojs.autojs.devplugin.service

import com.google.gson.Gson
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.autojs.autojs.devplugin.settings.AutoXSettings


// 表示连接的设备信息
data class ConnectedDevice(
    val sessionId: String,
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
    
    // 使用应用级别的共享服务
    private val sharedService = SharedWebSocketService.getInstance()
    
    // 当前项目的唯一标识符，用于在共享服务中区分不同项目的监听器
    private val projectId = project.locationHash
    
    // 添加连接状态监听器
    private val connectionListeners = mutableListOf<ConnectionListener>()
    
    // 用于将项目级别的监听器转发到应用级别的代理监听器
    private val connectionListenerProxy = object : ConnectionListener {
        override fun onConnectionEstablished(sessionId: String, device: ConnectedDevice) {
            // 通知当前项目的所有监听器
            connectionListeners.forEach { it.onConnectionEstablished(sessionId, device) }
        }
        
        override fun onConnectionClosed(sessionId: String) {
            // 通知当前项目的所有监听器
            connectionListeners.forEach { it.onConnectionClosed(sessionId) }
        }
        
        override fun onLogReceived(sessionId: String, deviceName: String, logMessage: String) {
            // 通知当前项目的所有监听器
            connectionListeners.forEach { it.onLogReceived(sessionId, deviceName, logMessage) }
        }
    }

    init {
        // 将当前项目的代理监听器注册到共享服务
        sharedService.addConnectionListener(projectId, connectionListenerProxy)
        
        // Auto-start the server if enabled in settings
        if (settings.state.autoStartServer) {
            start(settings.state.port)
        }
    }

    fun start(port: Int = settings.state.port) {
        // 委托给共享服务
        if (!sharedService.isRunning()) {
            sharedService.start(port)
        }
    }

    fun stop() {
        // 除非没有其他项目使用，否则不要关闭共享服务
        // 此处可以考虑增加一个计数机制，但简单起见，我们允许服务继续运行
        // sharedService.stop()
        
        // 只在应用程序退出时才关闭服务
        // 此处可以选择不做任何操作
    }

    // 断开单个客户端连接
    fun disconnectClient(sessionId: String) {
        // 委托给共享服务
        sharedService.disconnectClient(sessionId)
    }

    fun isRunning(): Boolean = sharedService.isRunning()

    fun getServerPort(): Int = sharedService.getServerPort()

    fun getServerAddress(): String? = sharedService.getServerAddress()

    fun getConnectedDeviceCount(): Int = sharedService.getConnectedDeviceCount()

    // 获取已连接设备列表
    fun getConnectedDevices(): List<ConnectedDevice> = sharedService.getConnectedDevices()

    /**
     * Check if the device with given id is connected
     *
     * @param deviceId the device id
     * @return true if the device is connected, false otherwise
     */
    fun isDeviceConnected(deviceId: String): Boolean = sharedService.isDeviceConnected(deviceId)

    /**
     * 向设备列表发送命令
     */
    fun sendCommandToDevices(json: String, targetDevices: List<ConnectedDevice>) {
        sharedService.sendCommandToDevices(json, targetDevices)
    }

    // 向设备列表发送附带文件byte的命令
    fun sendCommandToDevices(
        bytes: ByteArray,
        json: String,
        targetDevices: List<ConnectedDevice>
    ) {
        sharedService.sendCommandToDevices(bytes, json, targetDevices)
    }

    // 添加连接监听器
    fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }

    // 移除连接监听器
    fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }

    /**
     * 向设备列表发送文本命令（不含二进制数据的命令）
     * 例如：保存脚本、运行脚本、停止脚本等
     */
    fun sendTextCommandToDevices(
        json: String,
        targetDevices: List<ConnectedDevice>
    ) {
        sharedService.sendTextCommandToDevices(json, targetDevices)
    }

    override fun dispose() {
        // 移除当前项目在共享服务中的监听器
        sharedService.clearProjectListeners(projectId)
    }

    companion object {
        fun getInstance(project: Project): WebSocketService = project.service()
    }
}

// 连接状态监听器接口
interface ConnectionListener {
    fun onConnectionEstablished(sessionId: String, device: ConnectedDevice)
    fun onConnectionClosed(sessionId: String)
    
    // 添加日志消息处理方法，默认实现为空
    fun onLogReceived(sessionId: String, deviceName: String, logMessage: String) {}
} 