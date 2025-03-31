package org.autojs.autojs.devplugin.util

import com.intellij.openapi.diagnostic.Logger
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtil {
    private val logger = Logger.getInstance(NetworkUtil::class.java)
    
    /**
     * Gets the local IP address of the machine
     * Prefers non-loopback IPv4 addresses
     */
    fun getLocalIpAddress(): String? {
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
            
            // First, try to find a non-loopback IPv4 address
            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback || networkInterface.isVirtual) {
                    continue
                }
                
                val addresses = networkInterface.inetAddresses.toList()
                val ipv4Address = addresses.find { 
                    it is Inet4Address && !it.isLoopbackAddress 
                }
                
                if (ipv4Address != null) {
                    return ipv4Address.hostAddress
                }
            }
            
            // If no suitable address is found, fall back to localhost
            return InetAddress.getLocalHost().hostAddress
        } catch (e: Exception) {
            logger.error("Error getting local IP address", e)
            return null
        }
    }
    
    /**
     * Generates a WebSocket URL from an IP address and port
     */
    fun generateWebSocketUrl(ipAddress: String, port: Int): String {
        return "ws://$ipAddress:$port"
    }
} 