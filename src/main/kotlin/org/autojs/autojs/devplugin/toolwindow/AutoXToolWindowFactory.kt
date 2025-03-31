package org.autojs.autojs.devplugin.toolwindow

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.autojs.autojs.devplugin.ui.AutoXToolWindowPanel

class AutoXToolWindowFactory : ToolWindowFactory {
    private val logger = Logger.getInstance(AutoXToolWindowFactory::class.java)
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val panel = AutoXToolWindowPanel(project)
        val content = contentFactory.createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
        logger.info("AutoX Tool Window created")
    }
} 