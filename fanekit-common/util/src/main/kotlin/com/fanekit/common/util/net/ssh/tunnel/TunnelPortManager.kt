package com.fanekit.common.util.net.ssh.tunnel

import java.net.InetSocketAddress
import java.net.ServerSocket
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 隧道端口管理器
 * 负责分配和管理本地端口，避免端口冲突
 */
class TunnelPortManager {
    companion object {
        private const val MAX_PORT = 65536
        private const val MIN_PORT = 1024
    }

    private val portsHandedOut = ConcurrentHashMap<Int, Boolean>()

    /**
     * 租用一个新的可用端口
     *
     * @param startFrom 起始端口号
     * @return 绑定好的ServerSocket
     * @throws IllegalStateException 如果找不到可用端口
     */
    fun leaseNewPort(startFrom: Int = MIN_PORT): ServerSocket {
        for (port in startFrom until MAX_PORT) {
            if (isLeased(port)) {
                continue
            }

            val socket = tryBind(port)
            if (socket != null) {
                portsHandedOut[port] = true
                println("分配端口 $port 用于本地绑定")
                return socket
            }
        }
        throw IllegalStateException("在范围 [$startFrom-$MAX_PORT] 内找不到可用端口")
    }

    /**
     * 归还端口
     *
     * @param socket 要归还的ServerSocket
     */
    @Synchronized
    fun returnPort(socket: ServerSocket) {
        val port = socket.localPort
        portsHandedOut.remove(port)
        try {
            socket.close()
        } catch (e: IOException) {
            // 忽略关闭异常
        }
        println("归还端口 $port")
    }

    /**
     * 检查端口是否已被租用
     */
    private fun isLeased(port: Int): Boolean {
        return portsHandedOut.containsKey(port)
    }

    /**
     * 尝试绑定指定端口
     */
    private fun tryBind(localPort: Int): ServerSocket? {
        return try {
            ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress("localhost", localPort))
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 获取当前已分配的端口列表
     */
    fun getAllocatedPorts(): Set<Int> {
        return portsHandedOut.keys.toSet()
    }

    /**
     * 清理所有已分配的端口
     */
    fun cleanup() {
        portsHandedOut.clear()
        println("端口管理器已清理")
    }
}
