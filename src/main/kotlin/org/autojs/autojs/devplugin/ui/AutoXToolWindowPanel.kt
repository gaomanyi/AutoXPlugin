package org.autojs.autojs.devplugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import org.autojs.autojs.devplugin.service.WebSocketService
import org.autojs.autojs.devplugin.settings.AutoXSettings
import org.autojs.autojs.devplugin.util.QRCodeUtil
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.Timer

class AutoXToolWindowPanel(project: Project) : JPanel(), Disposable {
    private val logger = Logger.getInstance(AutoXToolWindowPanel::class.java)
    private val webSocketService = WebSocketService.getInstance(project)
    private val settings = AutoXSettings.getInstance(project)
    
    private var qrCodeImage: BufferedImage? = null
    private var statusMessage: String = "服务未运行"
    private var serverAddress: String? = null
    private var deviceCountLabel = JBLabel("设备: 0")
    
    // Timer to periodically update connected devices count
    private val updateTimer = Timer(1000, ActionListener {
        updateDeviceCount()
    })
    
    private val qrCodePanel = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            
            val g2d = g as Graphics2D
            val image = qrCodeImage
            
            if (image != null) {
                val x = (width - image.width) / 2
                val y = (height - image.height) / 2
                g2d.drawImage(image, x, y, null)
            } else {
                g2d.color = JBColor.GRAY
                g2d.drawString("QR Code will appear here", 10, height / 2)
            }
        }
        
        init {
            preferredSize = Dimension(300, 300)
            minimumSize = Dimension(200, 200)
        }
    }
    
    private val statusLabel = JBLabel(statusMessage).apply {
        horizontalAlignment = JBLabel.CENTER
    }
    
    private val panel: DialogPanel
    
    init {
        panel = panel {
            row {
                label("AutoX服务")
                    .bold()
                    .align(Align.CENTER)
            }
            
            row {
                cell(qrCodePanel)
                    .align(Align.CENTER)
                    .resizableColumn()
            }.resizableRow()
            
            row {
                cell(statusLabel)
                    .align(Align.CENTER)
            }
            
            row {
                cell(deviceCountLabel)
                    .align(Align.CENTER)
            }
            
            row {
                button("启动服务") {
                    startServer()
                }.align(AlignX.CENTER)
                
                button("结束服务") {
                    stopServer()
                }.align(AlignX.CENTER)
            }.bottomGap(BottomGap.SMALL)
            
            row {
                comment("Settings: Tools > AutoX插件")
                    .align(Align.CENTER)
            }
        }
        
        panel.border = JBUI.Borders.empty(10)
        add(panel)
        
        // Start the update timer
        updateTimer.start()
        
        // Check if server should auto-start
        if (settings.state.autoStartServer && !webSocketService.isRunning()) {
            startServer()
        }
    }
    
    private fun startServer() {
        webSocketService.start(settings.state.port)
        
        // Wait a bit for the server to start
        Thread.sleep(500)
        
        if (webSocketService.isRunning()) {
            serverAddress = webSocketService.getServerAddress()
            
            if (serverAddress != null) {
                qrCodeImage = QRCodeUtil.generateQRCode(serverAddress!!)
                statusMessage = "服务运行在: $serverAddress"
            } else {
                statusMessage = "服务运行，但无法确定IP地址"
            }
        } else {
            statusMessage = "启动服务失败"
        }

        refreshUI()
    }
    
    private fun stopServer() {
        webSocketService.stop()
        qrCodeImage = null
        serverAddress = null
        statusMessage = "服务未运行"
        updateDeviceCount()
        refreshUI()
    }

    private fun refreshUI() {
        statusLabel.text = statusMessage
        qrCodePanel.repaint()
    }
    
    private fun updateDeviceCount() {
        val count = if (webSocketService.isRunning()) {
            webSocketService.getConnectedDeviceCount()
        } else {
            0
        }
        deviceCountLabel.text = "连接的设备: $count"
    }
    
    override fun dispose() {
        updateTimer.stop()
        webSocketService.stop()
    }
} 