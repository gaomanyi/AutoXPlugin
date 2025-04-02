package org.autojs.autojs.devplugin.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.autojs.autojs.devplugin.message.Command
import org.autojs.autojs.devplugin.service.ConnectedDevice
import org.autojs.autojs.devplugin.service.SharedWebSocketService

/**
 * 保存项目操作 - 将文件夹作为项目发送到设备
 */
class SaveProjectAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file?.isDirectory == true) {
            // 只处理文件夹，使用ZIP格式发送
            sendFolderAsZip(project, file, webSocketService, selectedDevices) { name, md5 ->
                Command.saveProject(name, md5)
            }
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        // SaveProject动作只对文件夹可用
        return file?.isDirectory == true
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        return Command.saveProject(fileName, md5)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        // 菜单仅在文件夹上下文中可见
        e.presentation.isVisible = virtualFile != null && virtualFile.isDirectory
    }
}

/**
 * 运行项目操作 - 将文件夹作为项目发送并运行
 */
class RunProjectAction : BaseFileAction() {
    override fun handleSendAction(
        project: Project,
        file: VirtualFile?,
        webSocketService: SharedWebSocketService,
        selectedDevices: List<ConnectedDevice>
    ) {
        if (file == null) return
        
        // 如果是配置文件，则使用其父目录作为项目目录
        val targetFolder = if (isProjectConfigFile(file)) {
            file.parent
        } else if (file.isDirectory) {
            file
        } else {
            return
        }
        
        // 只处理文件夹，使用ZIP格式发送
        sendFolderAsZip(project, targetFolder, webSocketService, selectedDevices) { name, md5 ->
            Command.runProject(name, md5)
        }
    }

    override fun isActionAvailable(file: VirtualFile?): Boolean {
        if (file == null) return false
        
        // 检查是否是project.json或package.json文件
        if (isProjectConfigFile(file)) {
            return true
        }
        
        // 检查文件夹是否包含项目配置文件
        if (file.isDirectory) {
            return folderContainsFile(file, "project.json") || folderContainsFile(file, "package.json")
        }
        
        return false
    }
    
    /**
     * 判断文件是否为项目配置文件(package.json或project.json)
     */
    private fun isProjectConfigFile(file: VirtualFile): Boolean {
        return !file.isDirectory && 
               (file.name == "package.json" || file.name == "project.json")
    }
    
    override fun generateCommand(fileName: String, fileContent: String?, md5: String): String {
        return Command.runProject(fileName, md5)
    }
    
    override fun update(e: AnActionEvent) {
        super.update(e)
        
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        if (virtualFile == null) {
            e.presentation.isVisible = false
            return
        }
        
        // 菜单可见性规则：
        // 1. 文件夹且包含项目配置文件
        // 2. 是项目配置文件(package.json或project.json)
        val isVisible = if (virtualFile.isDirectory) {
            folderContainsFile(virtualFile, "project.json") || 
            folderContainsFile(virtualFile, "package.json")
        } else {
            isProjectConfigFile(virtualFile)
        }
        
        e.presentation.isVisible = isVisible
    }
} 