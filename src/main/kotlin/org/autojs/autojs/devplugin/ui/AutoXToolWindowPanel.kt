package org.autojs.autojs.devplugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import org.autojs.autojs.devplugin.service.ConnectedDevice
import org.autojs.autojs.devplugin.service.ConnectionListener
import org.autojs.autojs.devplugin.service.WebSocketService
import org.autojs.autojs.devplugin.settings.AutoXSettings
import org.autojs.autojs.devplugin.util.QRCodeUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.Timer
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

class AutoXToolWindowPanel(private val project: Project) : JPanel(), Disposable, ConnectionListener {
    private val logger = Logger.getInstance(AutoXToolWindowPanel::class.java)
    private val webSocketService = WebSocketService.getInstance(project)
    private val settings = AutoXSettings.getInstance(project)
    
    private var qrCodeImage: BufferedImage? = null
    private var statusMessage: String = "服务未运行"
    private var serverAddress: String? = null
    
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
    private val updateTimer = Timer(5000, ActionListener {
        updateDevicesList()
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
                g2d.drawString("请启动服务生成连接二维码", 10, height / 2)
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
                label("AutoX WebSocket 服务")
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
            
            // 添加设备列表区域
            group("已连接设备") {
                row {
                    cell(deviceTableScrollPane)
                        .align(Align.FILL)
                        .resizableColumn()
                }.resizableRow()
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
        deviceTableModel.clearDevices()
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
        } else {
            deviceTableModel.clearDevices()
        }
    }
    
    // 实现 ConnectionListener 接口
    override fun onConnectionEstablished(sessionId: String, device: ConnectedDevice) {
        // 添加新连接的设备
        deviceTableModel.addOrUpdateDevice(device)
    }
    
    override fun onConnectionClosed(sessionId: String) {
        // 移除断开连接的设备
        deviceTableModel.removeDevice(sessionId)
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
            val index = devices.indexOfFirst { it.id == device.id }
            if (index >= 0) {
                devices[index] = device
                fireTableRowsUpdated(index, index)
            } else {
                devices.add(device)
                fireTableRowsInserted(devices.size - 1, devices.size - 1)
            }
        }
        
        fun removeDevice(sessionId: String) {
            val index = devices.indexOfFirst { it.id == sessionId }
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
                    webSocketService.disconnectClient(device.id)
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
} 