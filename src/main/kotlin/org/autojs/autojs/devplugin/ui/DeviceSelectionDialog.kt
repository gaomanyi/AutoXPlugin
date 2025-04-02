package org.autojs.autojs.devplugin.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import org.autojs.autojs.devplugin.i18n.I18n
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
    private val selectAllCheckbox = JBCheckBox(I18n.msg("device.select.all")).apply {
        addActionListener {
            val selected = isSelected
            checkboxes.values.forEach { it.isSelected = selected }
        }
    }

    init {
        title = if (isDirectory) I18n.msg("device.select.receive.folder") else I18n.msg("device.select.receive.file")
        init()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            // Title row
            row {
                val messageText =
                    if (isDirectory) I18n.msg("device.select.send.folder") else if (fileCount == 1) I18n.msg("device.select.send.file") else I18n.msg("device.select.send.file.hint",fileCount)
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
                    label(I18n.msg("device.connect.none"))
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