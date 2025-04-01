package org.autojs.autojs.devplugin.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * 通知工具类，用于在IDE中显示通知
 */
object NotificationUtil {
    private const val NOTIFICATION_GROUP_ID = "AutoX Plugin Notifications"
    
    /**
     * 显示信息类型通知
     */
    fun showInfoNotification(project: Project, title: String, content: String) {
        if (project.isDisposed) return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, content, NotificationType.INFORMATION)
            .notify(project)
    }
    
    /**
     * 显示警告类型通知
     */
    fun showWarningNotification(project: Project, title: String, content: String) {
        if (project.isDisposed) return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, content, NotificationType.WARNING)
            .notify(project)
    }
    
    /**
     * 显示错误类型通知
     */
    fun showErrorNotification(project: Project, title: String, content: String) {
        if (project.isDisposed) return
        
        NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, content, NotificationType.ERROR)
            .notify(project)
    }
} 