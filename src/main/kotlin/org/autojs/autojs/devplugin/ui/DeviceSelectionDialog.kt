package org.autojs.autojs.devplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.autojs.autojs.devplugin.service.ConnectedDevice
import javax.swing.JComponent

/**
 * Dialog for selecting which devices to send files to
 */
class DeviceSelectionDialog(
    private val project: Project,
    private val devices: List<ConnectedDevice>,
    private val fileCount: Int = 1,
    private val isDirectory: Boolean = false
) : DialogWrapper(project) {
    
    private val checkboxes = mutableMapOf<ConnectedDevice, JBCheckBox>()
    private val selectAllCheckbox = JBCheckBox("全选").apply {
        addActionListener {
            val selected = isSelected
            checkboxes.values.forEach { it.isSelected = selected }
        }
    }
    
    init {
        title = if (isDirectory) "选择接收文件夹的设备" else "选择接收文件的设备"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return panel {
            // Title row
            row {
                val messageText = if (isDirectory) "选择要发送文件夹到哪些设备" else if (fileCount == 1) "选择要发送文件到哪些设备" else "选择要发送 $fileCount 个文件到哪些设备"
                label(messageText)
                    .bold()
                    .align(Align.CENTER)
            }
            
            // Select all option
            row {
                cell(selectAllCheckbox)
                    .align(AlignX.LEFT)
            }
            
            // Device list
            if (devices.isEmpty()) {
                row {
                    label("没有连接的设备")
                        .align(Align.CENTER)
                }
            } else {
                // Create checkboxes for each device
                for (device in devices) {
                    row {
                        val checkbox = JBCheckBox("${device.deviceName} (${device.appVersion})").apply {
                            isSelected = true  // Default to selected
                        }
                        cell(checkbox)
                            .align(AlignX.LEFT)
                        
                        checkboxes[device] = checkbox
                    }
                }
                
                // Default all selected
                selectAllCheckbox.isSelected = true
            }
        }
    }
    
    /**
     * Get the list of selected devices
     */
    fun getSelectedDevices(): List<ConnectedDevice> {
        return checkboxes.entries
            .filter { it.value.isSelected }
            .map { it.key }
    }
} 