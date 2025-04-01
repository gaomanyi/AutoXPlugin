package org.autojs.autojs.devplugin.service

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.security.MessageDigest

class Helper {
    companion object {
        private const val DEFAULT_PLUGIN_ID = "org.autojs.autojs.devplugin" // 默认插件ID

        // 获取版本号的方法
        fun getPluginVersion(): String {
            val pluginId = PluginId.getId(DEFAULT_PLUGIN_ID) // 替换为您的插件ID
            val plugin = PluginManagerCore.getPlugin(pluginId)
            return plugin?.version ?: "Unknown"
        }

        //生成MessageId
        fun generateMessageId(): String {
            return "${System.currentTimeMillis()}_${Math.random()}"
        }

        fun toHex(byteArray: ByteArray): String {
            val result = with(StringBuilder()) {
                byteArray.forEach {
                    val hex = it.toInt() and (0xFF)
                    val hexStr = Integer.toHexString(hex)
                    if (hexStr.length == 1) {
                        this.append("0").append(hexStr)
                    } else {
                        this.append(hexStr)
                    }
                }
                this.toString()
            }
            //转成16进制后是32 字节
            return result
        }

        //计算md5
        fun computeMd5(bs: ByteArray): String {
            val digest = MessageDigest.getInstance("MD5")//用来计算MD5
            digest.update(bs)
            return toHex(digest.digest())
        }
    }
}