package com.fanekit.common.util.net.ssh.tunnel

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder
import net.schmizz.sshj.connection.channel.direct.Parameters

import java.net.ServerSocket
import java.io.IOException
import java.util.concurrent.CountDownLatch

/**
 * 本地端口转发器
 * 支持两种转发模式：SSHJ原生API和netcat命令
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 */
class LocalPortForwarderClient(
    private val sshClient: SSHClient,
    private val localPort: Int,
    private val remoteHost: String,
    private val remotePort: Int,
    private val useNetcat: Boolean = false // 默认使用SSHJ原生API
) : AutoCloseable {

    private var serverSocket: ServerSocket? = null
    private var forwarderThread: Thread? = null
    private var sshjForwarder: LocalPortForwarder? = null
    private var isRunning = false
    private val connectionThreads = mutableListOf<Thread>()
    private val startLatch = CountDownLatch(1)

    /**
     * 获取实际绑定的端口号
     */
    val boundPort: Int
        get() = serverSocket?.localPort ?: localPort

    fun start() {
        if (isRunning) return

        try {
            if (useNetcat) {
                startNetcatForwarder()
            } else {
                startBuiltinForwarder()
            }

            // 等待转发器启动完成
            startLatch.await()
            isRunning = true

            println("本地端口转发已启动: localhost:$boundPort -> $remoteHost:$remotePort (${if (useNetcat) "netcat模式" else "内置模式"})")

        } catch (e: Exception) {
            println("启动本地端口转发失败: ${e.message}")
            throw e
        }
    }

    /**
     * 使用SSHJ内置的端口转发功能
     */
    private fun startBuiltinForwarder() {
        try {
            // 创建ServerSocket
            serverSocket = ServerSocket(localPort).apply {
                reuseAddress = true
            }

            // 创建Parameters对象
            val parameters = Parameters(
                "127.0.0.1",
                boundPort,
                remoteHost,
                remotePort
            )

            // 使用SSHJ的内置本地端口转发
            sshjForwarder = sshClient.newLocalPortForwarder(parameters, serverSocket!!)

            forwarderThread = Thread {
                try {
                    startLatch.countDown()
                    sshjForwarder?.listen()
                } catch (e: IOException) {
                    if (isRunning) {
                        println("内置端口转发异常: ${e.message}")
                    }
                }
            }.apply {
                isDaemon = true
                name = "Builtin-LocalPortForwarder-$localPort"
                start()
            }

        } catch (e: Exception) {
            startLatch.countDown()
            throw e
        }
    }

    /**
     * 使用netcat命令进行端口转发
     */
    private fun startNetcatForwarder() {
        try {
            serverSocket = ServerSocket(localPort)

            forwarderThread = Thread {
                try {
                    startLatch.countDown()
                    acceptConnections()
                } catch (e: Exception) {
                    if (isRunning) {
                        println("Netcat端口转发异常: ${e.message}")
                    }
                }
            }.apply {
                isDaemon = true
                name = "Netcat-LocalPortForwarder-$localPort"
                start()
            }

        } catch (e: Exception) {
            startLatch.countDown()
            throw e
        }
    }

    /**
     * 接受客户端连接并使用netcat转发
     */
    private fun acceptConnections() {
        val socket = serverSocket ?: return

        while (isRunning && !socket.isClosed) {
            try {
                val clientSocket = socket.accept()

                val connectionThread = Thread {
                    try {
                        val session = sshClient.startSession()

                        // 使用netcat命令转发连接
                        val command = session.exec("nc $remoteHost $remotePort")

                        // 创建双向数据流转发
                        val forwardToRemote = Thread {
                            try {
                                clientSocket.inputStream.copyTo(command.outputStream)
                            } catch (_: Exception) {
                                // 连接关闭是正常的
                            }
                        }

                        val forwardToLocal = Thread {
                            try {
                                command.inputStream.copyTo(clientSocket.outputStream)
                            } catch (_: Exception) {
                                // 连接关闭是正常的
                            }
                        }

                        forwardToRemote.start()
                        forwardToLocal.start()

                        forwardToRemote.join()
                        forwardToLocal.join()

                        session.close()
                        clientSocket.close()

                    } catch (e: Exception) {
                        println("连接处理异常: ${e.message}")
                        try {
                            clientSocket.close()
                        } catch (_: Exception) {}
                    }
                }

                connectionThread.isDaemon = true
                connectionThread.name = "NetcatConnection-${clientSocket.remoteSocketAddress}"
                connectionThread.start()

                connectionThreads.add(connectionThread)

            } catch (e: Exception) {
                if (isRunning) {
                    println("接受连接时异常: ${e.message}")
                }
            }
        }
    }

    override fun close() {
        isRunning = false

        // 停止SSHJ转发器
        sshjForwarder?.close()

        // 关闭服务器socket
        try {
            serverSocket?.close()
        } catch (_: Exception) {
            // 忽略关闭异常
        }

        // 中断并等待转发线程
        forwarderThread?.interrupt()
        try {
            forwarderThread?.join(5000)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // 关闭所有连接线程
        connectionThreads.forEach { thread ->
            thread.interrupt()
            try {
                thread.join(1000)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        connectionThreads.clear()

        println("本地端口转发已停止: localhost:$boundPort -> $remoteHost:$remotePort")
    }
}
