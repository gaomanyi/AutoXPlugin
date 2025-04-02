package org.autojs.autojs.devplugin.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.text
import org.autojs.autojs.devplugin.i18n.I18n
import javax.swing.JComponent

class AutoXSettingsConfigurable : Configurable {
    private var settings = AutoXSettings.getInstance()
    private var port = settings.state.port
    private var autoStart = settings.state.autoStartServer

    override fun getDisplayName(): String = "AutoX"+I18n.msg("settings")

    override fun createComponent(): JComponent {
        return panel {
            row() {
                intTextField()
                    .label(I18n.msg("service.ws.port"))
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
                checkBox(I18n.msg("service.auto.start"))
                    .bindSelected({ autoStart }, { autoStart = it })
            }

            row {
                comment(I18n.msg("service.setting.hint"))
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