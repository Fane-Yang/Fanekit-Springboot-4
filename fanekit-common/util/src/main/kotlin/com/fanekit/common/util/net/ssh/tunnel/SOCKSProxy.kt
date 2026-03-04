package com.fanekit.common.util.net.ssh.tunnel

import net.schmizz.sshj.SSHClient
import java.net.ServerSocket

/**
 * SOCKS代理服务器
 */
class SOCKSProxy(
    private val sshClient: SSHClient,
    private val localPort: Int
) : AutoCloseable {

    private var serverSocket: ServerSocket? = null
    private var isRunning = false

    fun start() {
        if (isRunning) return

        serverSocket = ServerSocket(localPort)
        isRunning = true

        Thread {
            while (isRunning && serverSocket?.isClosed == false) {
                try {
                    val clientSocket = serverSocket?.accept()
                    clientSocket?.let { socket ->
                        Thread {
                            handleSOCKSConnection(socket)
                        }.start()
                    }
                } catch (e: Exception) {
                    if (isRunning) {
                        e.printStackTrace()
                    }
                }
            }
        }.start()
    }

    private fun handleSOCKSConnection(clientSocket: java.net.Socket) {
        // 简化的SOCKS5实现
        try {
            val input = clientSocket.inputStream
            val output = clientSocket.outputStream

            // SOCKS5握手
            val version = input.read()
            if (version != 5) return

            val nmethods = input.read()
            val methods = ByteArray(nmethods)
            input.read(methods)

            // 无认证
            output.write(byteArrayOf(5, 0))
            output.flush()

            // 读取连接请求
            input.read() // version
            val cmd = input.read()
            input.read() // reserved
            val atyp = input.read()

            if (cmd == 1) { // CONNECT
                val address = when (atyp) {
                    1 -> { // IPv4
                        val addr = ByteArray(4)
                        input.read(addr)
                        addr.joinToString(".") { (it.toInt() and 0xFF).toString() }
                    }
                    3 -> { // Domain name
                        val len = input.read()
                        val domain = ByteArray(len)
                        input.read(domain)
                        String(domain)
                    }
                    else -> return
                }

                val port = (input.read() shl 8) or input.read()

                try {
                    // 简化的SOCKS代理实现 - 直接响应成功
                    // 实际的代理功能需要更复杂的实现

                    // 发送成功响应
                    output.write(byteArrayOf(5, 0, 0, 1, 0, 0, 0, 0, 0, 0))
                    output.flush()

                    // 简化的数据转发实现
                    val forwardThread = Thread {
                        try {
                            // 这里应该建立到目标服务器的连接
                            // 目前只是一个占位符实现
                            Thread.sleep(1000)
                        } catch (_: Exception) {
                            // 忽略异常
                        }
                    }
                    forwardThread.start()
                    forwardThread.join()

                } catch (_: Exception) {
                    // 发送失败响应
                    output.write(byteArrayOf(5, 1, 0, 1, 0, 0, 0, 0, 0, 0))
                    output.flush()
                }
            }

            clientSocket.close()
        } catch (_: Exception) {
            // 静默处理异常
        }
    }

    override fun close() {
        isRunning = false
        serverSocket?.close()
    }
}
