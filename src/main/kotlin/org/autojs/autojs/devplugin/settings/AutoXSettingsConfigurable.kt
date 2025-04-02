package org.autojs.autojs.devplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import javax.swing.JComponent

class AutoXSettingsConfigurable : Configurable {
    private var settings = AutoXSettings.getInstance()
    private var port = settings.state.port
    private var autoStart = settings.state.autoStartServer
    
    override fun getDisplayName(): String = "AutoX插件设置"
    
    override fun createComponent(): JComponent {
        return panel {
            row("服务器端口:") {
                intTextField()
                    .label("WebSocket服务端口:")
                    .text(port.toString())
                    .onChanged {
                        try {
                            port = it.text.toInt()
                        } catch (e: NumberFormatException) {
                            // 忽略无效输入
                        }
                    }
                    .columns(5)
                    .focused()
            }
            
            row {
                checkBox("项目打开时自动启动服务")
                    .bindSelected({ autoStart }, { autoStart = it })
            }
            
            row {
                comment("更改这些设置后重新启动服务器")
            }
        }
    }
    
    override fun isModified(): Boolean {
        return port != settings.state.port || autoStart != settings.state.autoStartServer
    }
    
    override fun apply() {
        settings.state.port = port
        settings.state.autoStartServer = autoStart
    }
} 