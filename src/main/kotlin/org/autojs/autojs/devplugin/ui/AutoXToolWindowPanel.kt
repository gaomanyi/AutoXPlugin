package org.autojs.autojs.devplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.autojs.autojs.devplugin.message.Command
import org.autojs.autojs.devplugin.service.ConnectedDevice
import org.autojs.autojs.devplugin.service.ConnectionListener
import org.autojs.autojs.devplugin.service.WebSocketService
import org.autojs.autojs.devplugin.settings.AutoXSettings
import org.autojs.autojs.devplugin.util.NotificationUtil
import org.autojs.autojs.devplugin.util.QRCodeUtil
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.Timer
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument

class AutoXToolWindowPanel(private val project: Project) : JPanel(), Disposable, ConnectionListener {
    private val logger = Logger.getInstance(AutoXToolWindowPanel::class.java)
    private val webSocketService = WebSocketService.getInstance(project)
    private val settings = AutoXSettings.getInstance(project)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var qrCodeImage: BufferedImage? = null
    private var statusMessage: String = "服务未运行"
    private var serverAddress: String? = null
    
    // 添加红色和灰色图标
    private val stopActiveIcon = AllIcons.Actions.Suspend
    private val stopDisabledIcon = IconLoader.getDisabledIcon(stopActiveIcon)
    
    // 修改启动和结束服务按钮
    private val startServerButton = JButton("启动服务", AllIcons.Actions.Execute).apply {
        addActionListener { startServer() }
        alignmentX = Component.LEFT_ALIGNMENT
        border = JBUI.Borders.empty(5, 10)
    }
    
    private val stopServerButton = JButton("结束服务", IconLoader.getDisabledIcon(AllIcons.Actions.Suspend)).apply {
        addActionListener { stopServer() }
        alignmentX = Component.LEFT_ALIGNMENT
        border = JBUI.Borders.empty(5, 10)
        isEnabled = false
    }
    
    // 修改停止所有脚本按钮
    private val stopAllScriptsButton = JButton("停止所有脚本", stopDisabledIcon).apply {
        isEnabled = false
        addActionListener {
            sendStopAllCommand()
        }
    }
    
    // 设备表格数据模型
    private val deviceTableModel = DeviceTableModel()
    private val deviceTable = JBTable(deviceTableModel).apply {
        setShowGrid(false)
        tableHeader.reorderingAllowed = false
        tableHeader.resizingAllowed = true
        rowHeight = 32
        autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        
        // 设置断开按钮的渲染器和编辑器
        getColumnModel().getColumn(2).cellRenderer = ButtonRenderer()
        getColumnModel().getColumn(2).cellEditor = ButtonEditor()
        
        // 调整列宽
        columnModel.getColumn(0).preferredWidth = 180  // 设备名称列宽
        columnModel.getColumn(1).preferredWidth = 100  // APP版本列宽
        columnModel.getColumn(2).preferredWidth = 100  // 操作按钮列宽
        
        // 设置表格为空时的显示信息
        emptyText.text = "没有设备连接"
    }
    
    private val deviceTableScrollPane = JBScrollPane(deviceTable).apply {
        preferredSize = Dimension(300, 150)
        minimumSize = Dimension(200, 100)
        border = JBUI.Borders.empty(5)
    }
    
    // Timer to periodically update connected devices count and list (as a backup)
    private val updateTimer = Timer(5000) {
        updateDevicesList()
    }

    private val qrCodePanel = object : JPanel() {
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            
            val g2d = g as Graphics2D
            val image = qrCodeImage
            
            if (image != null) {
                // 计算边距
                val margin = 8  // 稍微减小边距
                val availableWidth = width - (margin * 2)
                val availableHeight = height - (margin * 2)
                
                // 计算缩放比例，保持宽高比
                val scale = minOf(
                    availableWidth.toFloat() / image.width,
                    availableHeight.toFloat() / image.height
                )
                
                // 计算居中位置
                val scaledWidth = (image.width * scale).toInt()
                val scaledHeight = (image.height * scale).toInt()
                val x = (width - scaledWidth) / 2
                val y = (height - scaledHeight) / 2
                
                // 绘制二维码
                g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null)
            } else {
                g2d.color = UIUtil.getLabelForeground()
                g2d.drawString("请启动服务生成连接二维码", 10, height / 2)
            }
        }
        
        init {
            preferredSize = Dimension(170, 170)  // 原来是 250x250
            minimumSize = Dimension(130, 130)    // 原来是 200x200
            border = JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1)
            background = UIUtil.getPanelBackground()
        }
    }
    
    private val statusLabel = JBLabel(statusMessage).apply {
        horizontalAlignment = JBLabel.CENTER
    }
    
    // 创建按钮面板
    private val buttonPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.emptyBottom(10)
        
        // 添加按钮
        add(startServerButton)
        add(Box.createVerticalStrut(5))
        add(stopServerButton)
        add(Box.createVerticalStrut(5))
        add(stopAllScriptsButton.apply {
            alignmentX = Component.LEFT_ALIGNMENT
        })
    }
    
    // 添加日志文本区域
    private val logTextPane = JTextPane().apply {
        isEditable = false
        document = DefaultStyledDocument()
        font = JBUI.Fonts.create("Monospaced", 12)
        margin = JBUI.insets(5)  // 添加内边距
        
        // 创建普通文本样式
        val normalStyle = addStyle("normal", null)
        StyleConstants.setForeground(normalStyle, UIUtil.getLabelForeground())
        
        // 创建错误文本样式
        val errorStyle = addStyle("error", null)
        StyleConstants.setForeground(errorStyle, JBColor(0xFF0000, 0xFF0000))  // 红色
    }
    
    private val logScrollPane = JBScrollPane(logTextPane).apply {
        preferredSize = Dimension(300, 150)  // 减小高度
        minimumSize = Dimension(200, 100)    // 减小最小高度
        border = JBUI.Borders.customLine(JBColor.border(), 1, 1, 1, 1)
        background = UIUtil.getPanelBackground()
        verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    }
    
    // 清除日志按钮
    private val clearLogButton = JButton("清除日志").apply {
        addActionListener {
            clearLog()
        }
    }
    
    private val panel: DialogPanel
    
    init {
        panel = panel {
            row {
                label("AutoX WebSocket 服务")
                    .bold()
                    .align(Align.CENTER)
            }
            
            // 状态信息
            row {
                cell(statusLabel)
                    .align(Align.FILL)
            }.bottomGap(BottomGap.SMALL)
            
            // 按钮和二维码区域
            row {
                cell(buttonPanel)
                    .align(AlignX.LEFT)
                    .resizableColumn()
                
                cell(qrCodePanel)
                    .align(Align.CENTER)
                    .resizableColumn()
            }.resizableRow()
            
            // 添加设备列表区域
            group("已连接设备") {
                row {
                    cell(deviceTableScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
            }
            
            // 添加日志显示区域
            group("设备日志") {
                row {
                    cell(clearLogButton)
                        .align(AlignX.RIGHT)
                }.bottomGap(BottomGap.SMALL)
                
                row {
                    cell(logScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
                    .bottomGap(BottomGap.SMALL)  // 添加底部间距
            }
            
            row {
                comment("设置: Tools > AutoX插件")
                    .align(Align.CENTER)
            }
        }
        
        panel.border = JBUI.Borders.empty(10)
        add(panel)
        
        // 注册为连接监听器
        webSocketService.addConnectionListener(this)
        
        // Start the update timer (as a backup)
        updateTimer.start()
        
        // 立即同步当前服务状态和已连接设备列表
        syncServiceState()
        
        // Check if server should auto-start
        if (settings.state.autoStartServer && !webSocketService.isRunning()) {
            startServer()
        }
    }
    
    /**
     * 同步当前服务状态和已连接设备
     * 当打开新项目时调用，确保UI状态与服务状态一致
     */
    private fun syncServiceState() {
        // 同步服务状态
        if (webSocketService.isRunning()) {
            // 获取服务地址和生成二维码
            serverAddress = webSocketService.getServerAddress()
            
            if (serverAddress != null) {
                qrCodeImage = QRCodeUtil.generateQRCode(serverAddress!!)
                statusMessage = "服务运行在: $serverAddress"
                
                // 更新按钮状态
                startServerButton.isEnabled = false
                stopServerButton.isEnabled = true
                stopServerButton.icon = AllIcons.Actions.Suspend
            } else {
                statusMessage = "服务运行，但无法确定IP地址"
            }
            
            // 更新设备列表
            updateDevicesList()
        } else {
            statusMessage = "服务未运行"
            qrCodeImage = null
            serverAddress = null
            deviceTableModel.clearDevices()
            
            // 更新按钮状态
            startServerButton.isEnabled = true
            stopServerButton.isEnabled = false
            stopServerButton.icon = IconLoader.getDisabledIcon(AllIcons.Actions.Suspend)
        }
        
        // 更新Stop All按钮状态
        updateStopAllButtonState()
        
        // 刷新UI
        refreshUI()
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
                
                // 更新按钮状态
                startServerButton.isEnabled = false
                stopServerButton.isEnabled = true
                stopServerButton.icon = AllIcons.Actions.Suspend
            } else {
                statusMessage = "服务运行，但无法确定IP地址"
            }
        } else {
            statusMessage = "启动服务失败"
        }

        // 更新停止所有脚本按钮状态
        updateStopAllButtonState()
        refreshUI()
    }
    
    private fun stopServer() {
        webSocketService.stop()
        qrCodeImage = null
        serverAddress = null
        statusMessage = "服务未运行"
        deviceTableModel.clearDevices()
        
        // 更新按钮状态
        startServerButton.isEnabled = true
        stopServerButton.isEnabled = false
        stopServerButton.icon = IconLoader.getDisabledIcon(AllIcons.Actions.Suspend)
        
        // 禁用停止所有脚本按钮
        stopAllScriptsButton.isEnabled = false
        stopAllScriptsButton.icon = stopDisabledIcon
        refreshUI()
    }

    private fun refreshUI() {
        statusLabel.text = statusMessage
        qrCodePanel.repaint()
    }
    
    private fun updateDevicesList() {
        if (webSocketService.isRunning()) {
            val devices = webSocketService.getConnectedDevices()
            deviceTableModel.updateDevices(devices)
            
            // 根据连接设备情况更新按钮状态
            updateStopAllButtonState()
        } else {
            deviceTableModel.clearDevices()
            stopAllScriptsButton.isEnabled = false
        }
    }
    
    /**
     * 更新Stop All按钮的状态
     */
    private fun updateStopAllButtonState() {
        val isEnabled = webSocketService.isRunning() && 
                        webSocketService.getConnectedDeviceCount() > 0
        stopAllScriptsButton.isEnabled = isEnabled
        stopAllScriptsButton.icon = if (isEnabled) stopActiveIcon else stopDisabledIcon
    }
    
    // 实现 ConnectionListener 接口
    override fun onConnectionEstablished(sessionId: String, device: ConnectedDevice) {
        // 添加新连接的设备
        deviceTableModel.addOrUpdateDevice(device)
        
        // 更新按钮状态
        updateStopAllButtonState()
    }
    
    override fun onConnectionClosed(sessionId: String) {
        // 移除断开连接的设备
        deviceTableModel.removeDevice(sessionId)
        
        // 更新按钮状态
        updateStopAllButtonState()
    }
    
    override fun dispose() {
        webSocketService.removeConnectionListener(this)
        updateTimer.stop()
        webSocketService.stop()
    }
    
    // 设备表格的数据模型
    private inner class DeviceTableModel : AbstractTableModel() {
        private val columnNames = arrayOf("设备名称", "APP版本", "操作")
        private var devices: MutableList<ConnectedDevice> = mutableListOf()
        
        fun updateDevices(newDevices: List<ConnectedDevice>) {
            devices.clear()
            devices.addAll(newDevices)
            fireTableDataChanged()
        }
        
        fun addOrUpdateDevice(device: ConnectedDevice) {
            val index = devices.indexOfFirst { it.sessionId == device.sessionId }
            if (index >= 0) {
                devices[index] = device
                fireTableRowsUpdated(index, index)
            } else {
                devices.add(device)
                fireTableRowsInserted(devices.size - 1, devices.size - 1)
            }
        }
        
        fun removeDevice(sessionId: String) {
            val index = devices.indexOfFirst { it.sessionId == sessionId }
            if (index >= 0) {
                devices.removeAt(index)
                fireTableRowsDeleted(index, index)
            }
        }
        
        fun clearDevices() {
            devices.clear()
            fireTableDataChanged()
        }
        
        fun getDeviceAt(row: Int): ConnectedDevice {
            return devices[row]
        }
        
        override fun getRowCount(): Int = devices.size
        
        override fun getColumnCount(): Int = columnNames.size
        
        override fun getColumnName(column: Int): String = columnNames[column]
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            if (devices.isEmpty() || rowIndex >= devices.size) return ""
            
            val device = devices[rowIndex]
            return when (columnIndex) {
                0 -> device.deviceName
                1 -> device.appVersion
                2 -> "断开连接"
                else -> ""
            }
        }
        
        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            return columnIndex == 2 // 只有操作列可编辑
        }
    }
    
    // 按钮渲染器
    private inner class ButtonRenderer : TableCellRenderer {
        private val button = JButton("断开连接").apply {
            isOpaque = true
            border = JBUI.Borders.empty(2, 8)
        }
        
        override fun getTableCellRendererComponent(
            table: JTable, 
            value: Any?,
            isSelected: Boolean, 
            hasFocus: Boolean, 
            row: Int, 
            column: Int
        ): Component {
            button.text = (value as? String) ?: "断开连接"
            return button
        }
    }
    
    // 按钮编辑器
    private inner class ButtonEditor : AbstractCellEditor(), TableCellEditor {
        private val button = JButton("断开连接").apply {
            isOpaque = true
            border = JBUI.Borders.empty(2, 8)
            
            addActionListener { 
                val selectedRow = deviceTable.selectedRow
                if (selectedRow >= 0) {
                    val device = deviceTableModel.getDeviceAt(selectedRow)
                    webSocketService.disconnectClient(device.sessionId)
                }
                stopCellEditing() // 完成编辑
            }
        }
        
        override fun getTableCellEditorComponent(
            table: JTable, 
            value: Any?,
            isSelected: Boolean, 
            row: Int, 
            column: Int
        ): Component {
            button.text = (value as? String) ?: "断开连接"
            return button
        }
        
        override fun getCellEditorValue(): Any {
            return button.text
        }
    }
    
    /**
     * 发送停止所有脚本的命令
     */
    private fun sendStopAllCommand() {
        if (!webSocketService.isRunning() || webSocketService.getConnectedDeviceCount() == 0) {
            NotificationUtil.showWarningNotification(project, "无法执行操作", "没有连接的设备或WebSocket服务未运行")
            return
        }
        
        val devices = webSocketService.getConnectedDevices()
        val commandJson = Command.stopAll()
        
        scope.launch {
            try {
                webSocketService.sendTextCommandToDevices(commandJson, devices)
                NotificationUtil.showInfoNotification(
                    project, 
                    "命令已发送", 
                    "停止所有脚本命令已发送到 ${devices.size} 个设备"
                )
            } catch (e: Exception) {
                logger.error("Error sending stop all command: ${e.message}", e)
                NotificationUtil.showErrorNotification(
                    project, 
                    "发送命令失败", 
                    "无法发送停止所有脚本命令: ${e.message}"
                )
            }
        }
    }
    
    // 清除日志
    private fun clearLog() {
        logTextPane.text = ""
    }
    
    // 添加日志
    private fun addLog(deviceName: String, message: String) {
        // 如果消息为空，则不添加
        if (message.isBlank()) return
        
        val dateFormat = SimpleDateFormat("HH:mm:ss")
        val timestamp = dateFormat.format(Date())
        
        // 处理多行日志
        val formattedMessage = message.replace("\n", "\n[$timestamp] [$deviceName] ")
        val logEntry = "[$timestamp] [$deviceName] $formattedMessage\n"
        
        // 使用 invokeLater 确保在 EDT 线程中更新 UI
        SwingUtilities.invokeLater {
            val doc = logTextPane.document as StyledDocument
            val length = doc.length
            
            // 添加文本
            doc.insertString(length, logEntry, null)
            
            // 检查是否包含错误信息
            if (message.contains("Error:", ignoreCase = true) || 
                message.contains("/E:", ignoreCase = true) ||
                message.contains("错误:", ignoreCase = true)) {
                // 将错误信息设置为红色
                doc.setCharacterAttributes(
                    length,
                    logEntry.length,
                    logTextPane.getStyle("error"),
                    true
                )
            } else {
                // 将普通文本设置为默认颜色
                doc.setCharacterAttributes(
                    length,
                    logEntry.length,
                    logTextPane.getStyle("normal"),
                    true
                )
            }
            
            // 自动滚动到底部
            logTextPane.caretPosition = doc.length
            
            // 计算当前日志条数（通过计算换行符的数量）
            val logCount = doc.getText(0, doc.length).count { it == '\n' }
            
            // 如果日志条数超过1000条，删除最早的500条
            if (logCount > 1000) {
                val text = doc.getText(0, doc.length)
                val lines = text.split('\n')
                val newText = lines.drop(500).joinToString("\n") + "\n"
                doc.remove(0, doc.length)
                doc.insertString(0, newText, null)
                
                // 重新应用样式
                if (newText.contains("Error:", ignoreCase = true) || 
                    newText.contains("/E:", ignoreCase = true) ||
                    newText.contains("错误:", ignoreCase = true)) {
                    doc.setCharacterAttributes(
                        0,
                        newText.length,
                        logTextPane.getStyle("error"),
                        true
                    )
                } else {
                    doc.setCharacterAttributes(
                        0,
                        newText.length,
                        logTextPane.getStyle("normal"),
                        true
                    )
                }
            }
        }
    }
    
    // 实现 ConnectionListener 接口的日志接收方法
    override fun onLogReceived(sessionId: String, deviceName: String, logMessage: String) {
        addLog(deviceName, logMessage)
    }
} 