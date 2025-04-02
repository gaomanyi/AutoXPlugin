package org.autojs.autojs.devplugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.autojs.autojs.devplugin.message.Command
import org.autojs.autojs.devplugin.service.ConnectedDevice
import org.autojs.autojs.devplugin.service.SharedWebSocketService

/**
 * 保存脚本操作 - 将JS文件发送到设备
 */
class SaveScriptAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file == null) {
            logger.error("Cannot save null script file")
            return
        }
        
        sendScriptFile(project, file, webSocketService, selectedDevices) { name, script ->
            Command.save(name, script)
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        // SaveScript动作只对JS文件可用
        return file != null && !file.isDirectory && isJavaScriptFile(file)
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        if (fileContent == null) return ""
        return Command.save(fileName, fileContent)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        // 菜单仅在JavaScript文件上下文中可见
        e.presentation.isVisible = virtualFile != null && 
                                   !virtualFile.isDirectory && 
                                   isJavaScriptFile(virtualFile)
    }
}

/**
 * 运行脚本操作 - 将JS文件发送到设备并运行
 */
class RunScriptAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file == null) {
            logger.error("Cannot run null script file")
            return
        }
        
        sendScriptFile(project, file, webSocketService, selectedDevices) { name, script ->
            Command.run(name, script)
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        // RunScript动作只对JS文件可用
        return file != null && !file.isDirectory && isJavaScriptFile(file)
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        if (fileContent == null) return ""
        return Command.run(fileName, fileContent)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        // 菜单仅在JavaScript文件上下文中可见
        e.presentation.isVisible = virtualFile != null && 
                                   !virtualFile.isDirectory && 
                                   isJavaScriptFile(virtualFile)
    }
}

/**
 * 重新运行脚本操作 - 将JS文件发送到设备并重新运行
 */
class ReRunScriptAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file == null) {
            logger.error("Cannot re-run null script file")
            return
        }
        
        sendScriptFile(project, file, webSocketService, selectedDevices) { name, script ->
            Command.reRun(name, script)
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        // ReRunScript动作只对JS文件可用
        return file != null && !file.isDirectory && isJavaScriptFile(file)
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        if (fileContent == null) return ""
        return Command.reRun(fileName, fileContent)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        // 菜单仅在JavaScript文件上下文中可见
        e.presentation.isVisible = virtualFile != null && 
                                   !virtualFile.isDirectory && 
                                   isJavaScriptFile(virtualFile)
    }
}

/**
 * 停止脚本操作 - 停止设备上运行的脚本
 */
class StopScriptAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file == null) {
            logger.error("Cannot stop script for null file")
            return
        }
        
        sendControlCommand(project, file, webSocketService, selectedDevices) { name ->
            Command.stop(name)
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        // StopScript动作只对JS文件可用
        return file != null && !file.isDirectory && isJavaScriptFile(file)
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        return Command.stop(fileName)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        // 菜单仅在JavaScript文件上下文中可见
        e.presentation.isVisible = virtualFile != null && 
                                   !virtualFile.isDirectory && 
                                   isJavaScriptFile(virtualFile)
    }
} 