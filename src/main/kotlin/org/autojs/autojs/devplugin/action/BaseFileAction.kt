package org.autojs.autojs.devplugin.action

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.autojs.autojs.devplugin.service.ConnectedDevice
import org.autojs.autojs.devplugin.service.Helper
import org.autojs.autojs.devplugin.service.SharedWebSocketService
import org.autojs.autojs.devplugin.ui.DeviceSelectionDialog
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 基础文件操作动作类，封装了发送文件/文件夹的通用功能
 */
abstract class BaseFileAction : AnAction() {
    protected val logger = Logger.getInstance(javaClass)
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 子类实现此方法处理特定的发送逻辑
     */
    protected abstract fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    )

    /**
     * 判断该动作是否可用于当前文件/文件夹
     */
    protected abstract fun isActionAvailable(file: VirtualFile?): Boolean
    
    /**
     * 返回生成的命令
     */
    protected abstract fun generateCommand(fileName: String, fileContent: String?, md5: String): String

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        // 检查当前文件是否适用于此操作
        if (virtualFile != null && !isActionAvailable(virtualFile)) {
            return
        }
        
        val webSocketService = SharedWebSocketService.getInstance()

        if (!webSocketService.isRunning()) {
            logger.warn("WebSocket server is not running")
            return
        }

        // 获取已连接设备列表
        val connectedDevices = webSocketService.getConnectedDevices()
        if (connectedDevices.isEmpty()) {
            logger.warn("No connected devices")
            showErrorNotification(
                project = project,
                title = "No devices connected",
                content = "No devices are connected to send files to"
            )
            return
        }
        //如果只有一个设备连接,就不需要选择
        if (connectedDevices.size == 1) {
            val device = connectedDevices[0]
            handleSendAction(project,virtualFile,webSocketService, listOf(device))
        }else{
            // 显示设备选择对话框
            val isDirectory = virtualFile?.isDirectory ?: false
            val dialog = DeviceSelectionDialog(
                project = project,
                devices = connectedDevices,
                isDirectory = isDirectory
            )

            if (dialog.showAndGet()) {
                val selectedDevices = dialog.getSelectedDevices()
                handleSendAction(project, virtualFile, webSocketService, selectedDevices)
            }
        }
    }

    /**
     * 将文件夹打包成ZIP格式发送
     */
    protected fun sendFolderAsZip(
        project: Project,
        folder: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>,
        generateCommandFunc: (String, String) -> String
    ) {
        try {
            if (folder == null) {
                logger.error("Cannot send null folder")
                showErrorNotification(
                    project = project,
                    title = "Error preparing folder",
                    content = "No folder specified for sending"
                )
                return
            }
            
            if (!folder.isDirectory) {
                logger.error("Not a directory: ${folder.path}")
                showErrorNotification(
                    project = project,
                    title = "Error preparing folder",
                    content = "'${folder.name}' is not a directory"
                )
                return
            }
            
            val byteStream = ByteArrayOutputStream() // 用来获取压缩后的字节流
            val outputStream = ZipOutputStream(byteStream)
            var fileCount = 0
            
            // 文件夹基础路径，用于计算相对路径
            val basePath = folder.path + "/"

            // 遍历文件夹并压缩文件
            VfsUtilCore.iterateChildrenRecursively(folder, null) { childFile ->
                if (!childFile.isDirectory) {
                    try {
                        val byteArr = childFile.contentsToByteArray()
                        
                        // 直接计算文件相对于基础文件夹的路径
                        val relativePath = if (childFile.path.startsWith(basePath)) {
                            childFile.path.substring(basePath.length)
                        } else {
                            childFile.name
                        }
                        
                        outputStream.putNextEntry(ZipEntry(relativePath))
                        outputStream.write(byteArr)
                        outputStream.closeEntry() // 结束当前条目的写入
                        fileCount++
                    } catch (e: IOException) {
                        logger.error("Error compressing file ${childFile.path}", e)
                    }
                }
                true
            }

            // 关闭zip输出流
            outputStream.close()

            if (fileCount == 0) {
                logger.warn("No files found in ${folder.path}")
                showErrorNotification(
                    project = project,
                    title = "Empty content",
                    content = "No files found to send"
                )
                return
            }

            // 发送压缩后的文件
            scope.launch {
                try {
                    val bytes = byteStream.toByteArray()
                    val md5 = Helper.computeMd5(bytes)
                    val command = generateCommandFunc(getFullPath(folder), md5)

                    // 发送给选定的设备
                    webSocketService.sendCommandToDevices(bytes, command, selectedDevices)
                    
                    showNotification(
                        project = project,
                        title = "Folder sent",
                        content = "Folder '${folder.name}' with $fileCount file(s) sent to ${if (selectedDevices.isEmpty()) "all connected devices" else "${selectedDevices.size} device(s)"}"
                    )
                } catch (e: Exception) {
                    logger.error("Error sending zip: ${e.message}", e)
                    showErrorNotification(
                        project = project,
                        title = "Error sending",
                        content = "Failed to send '${folder.name}': ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing ${folder?.path}", e)
            showErrorNotification(
                project = project,
                title = "Error preparing",
                content = "Failed to prepare '${folder?.name}' for sending: ${e.message}"
            )
        }
    }
    
    /**
     * 发送脚本文件（不压缩，直接发送内容）
     */
    protected fun sendScriptFile(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>,
        commandType: (String, String) -> String
    ) {
        try {
            if (file == null) {
                logger.error("Cannot send null file")
                showErrorNotification(
                    project = project,
                    title = "Error sending script",
                    content = "No file specified for sending"
                )
                return
            }
            
            // 读取文件内容
            val content = String(file.contentsToByteArray())
            val fileName = getFullPath(file)
            
            // 发送脚本内容
            scope.launch {
                try {
                    val command = commandType(fileName, content)
                    
                    // 发送给选定的设备
                    webSocketService.sendTextCommandToDevices(command, selectedDevices)
                    
                    showNotification(
                        project = project,
                        title = "Script sent",
                        content = "Script '${file.name}' sent to ${if (selectedDevices.isEmpty()) "all connected devices" else "${selectedDevices.size} device(s)"}"
                    )
                } catch (e: Exception) {
                    logger.error("Error sending script: ${e.message}", e)
                    showErrorNotification(
                        project = project,
                        title = "Error sending script",
                        content = "Failed to send '${file.name}': ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error reading file ${file?.path}", e)
            showErrorNotification(
                project = project,
                title = "Error reading file",
                content = "Failed to read '${file?.name}': ${e.message}"
            )
        }
    }
    
    /**
     * 发送控制命令（如stop, stopAll等）
     */
    protected fun sendControlCommand(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>,
        commandType: (String) -> String
    ) {
        try {
            if (file == null) {
                logger.error("Cannot send control command for null file")
                showErrorNotification(
                    project = project,
                    title = "Error sending command",
                    content = "No file specified for command"
                )
                return
            }
            
            val fileName = getFullPath(file)
            
            // 发送控制命令
            scope.launch {
                try {
                    val command = commandType(fileName)
                    
                    // 发送给选定的设备
                    webSocketService.sendTextCommandToDevices(command, selectedDevices)
                    
                    showNotification(
                        project = project,
                        title = "Command sent",
                        content = "Control command for '${file.name}' sent to ${if (selectedDevices.isEmpty()) "all connected devices" else "${selectedDevices.size} device(s)"}"
                    )
                } catch (e: Exception) {
                    logger.error("Error sending control command: ${e.message}", e)
                    showErrorNotification(
                        project = project,
                        title = "Error sending command",
                        content = "Failed to send command for '${file.name}': ${e.message}"
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error sending control command for ${file?.path}", e)
            showErrorNotification(
                project = project,
                title = "Error sending command",
                content = "Failed to send command for '${file?.name}': ${e.message}"
            )
        }
    }
    
    /**
     * 获取文件或文件夹的完整路径
     * 
     * @param file 要获取路径的文件或文件夹
     * @return 完整路径，文件包含文件名和后缀，文件夹是文件夹全路径
     */
    protected fun getFullPath(file: VirtualFile?): String {
        return file?.path ?: "unknown"
    }


    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val webSocketService = SharedWebSocketService.getInstance()
        
        // 判断动作是否可用
        e.presentation.isEnabled = project != null && 
                                  (virtualFile == null || isActionAvailable(virtualFile)) && 
                                  webSocketService.isRunning() == true &&
                                  webSocketService.getConnectedDeviceCount() > 0
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
    
    /**
     * 检查文件夹是否包含指定文件
     */
    protected fun folderContainsFile(folder: VirtualFile?, fileName: String): Boolean {
        if (folder == null || !folder.isDirectory) return false
        return folder.children.any { it.name == fileName }
    }
    
    /**
     * 检查文件扩展名是否是JavaScript文件
     */
    protected fun isJavaScriptFile(file: VirtualFile?): Boolean {
        if (file == null) return false
        val extension = file.extension?.lowercase() ?: ""
        return extension == "js" || extension == "cjs" || extension == "mjs"
    }
    
    /**
     * 显示通知
     */
    protected fun showNotification(project: Project?, title: String, content: String) {
        if (project == null) return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AutoX Plugin Notifications")
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }
    
    /**
     * 显示错误通知
     */
    protected fun showErrorNotification(project: Project?, title: String, content: String) {
        if (project == null) return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup("AutoX Plugin Notifications")
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }
} 