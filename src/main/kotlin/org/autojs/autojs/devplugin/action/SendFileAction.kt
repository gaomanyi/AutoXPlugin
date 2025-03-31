package org.autojs.autojs.devplugin.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import org.autojs.autojs.devplugin.service.WebSocketService

class SendFileAction : AnAction() {
    private val logger = Logger.getInstance(SendFileAction::class.java)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        
        if (virtualFile.isDirectory) {
            logger.warn("Cannot send directory: ${virtualFile.path}")
            return
        }
        
        val webSocketService = WebSocketService.getInstance(project)
        
        if (!webSocketService.isRunning()) {
            logger.warn("WebSocket server is not running")
            return
        }
        
        try {
            virtualFile.inputStream.use { inputStream ->
                webSocketService.sendFile(virtualFile.name, inputStream)
            }
        } catch (ex: Exception) {
            logger.error("Error sending file: ${virtualFile.path}", ex)
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        
        e.presentation.isEnabledAndVisible = project != null && 
                virtualFile != null && 
                !virtualFile.isDirectory &&
                WebSocketService.getInstance(project).isRunning()
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }
} 