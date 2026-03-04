package com.fanekit.common.util.net.ssh.tunnel

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder
import net.schmizz.sshj.connection.channel.forwarded.ConnectListener
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

/**
 * SSH端口转发/隧道管理器
 * 支持本地端口转发、远程端口转发和动态端口转发
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 *
 * @param host SSH服务器主机地址
 * @param port SSH服务器端口号
 * @param username 登录用户名
 * @param password 登录密码（可选）
 * @param keyPath 私钥文件路径（可选）
 * @param keyPassword 私钥文件密码（可选）
 */
class SSHTunnelManager(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val keyPath: String? = null,
    private val keyPassword: String? = null
) : AutoCloseable {

    private var sshClient: SSHClient? = null
    private val activeTunnels = ConcurrentHashMap<String, TunnelInfo>()

    /**
     * 建立SSH连接
     */
    fun connect(): SSHTunnelManager {
        if (sshClient?.isConnected == true) {
            return this
        }

        sshClient = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connectTimeout = 30000
            connect(host, port)

            when {
                !keyPath.isNullOrEmpty() -> {
                    val location = Paths.get(keyPath).toString()
                    val keyProvider: KeyProvider = if (!keyPassword.isNullOrEmpty()) {
                        loadKeys(location, keyPassword)
                    } else {
                        loadKeys(location)
                    }
                    authPublickey(username, keyProvider)
                }
                !password.isNullOrEmpty() -> {
                    authPassword(username, password)
                }
                else -> {
                    throw IllegalArgumentException("Either password or keyPath must be provided")
                }
            }
        }
        return this
    }

    /**
     * 创建本地端口转发
     * 将本地端口的流量转发到远程主机的指定端口
     *
     * @param localPort 本地端口
     * @param remoteHost 远程目标主机
     * @param remotePort 远程目标端口
     * @param useNetcat 是否使用netcat模式（默认false，使用SSHJ原生API）
     * @return 转发器对象
     */
    fun createLocalPortForward(
        localPort: Int,
        remoteHost: String,
        remotePort: Int,
        useNetcat: Boolean = false
    ): LocalPortForwarderClient {
        val client = sshClient ?: throw IllegalStateException("SSH client not connected")

        val forwarder = LocalPortForwarderClient(
            client,
            localPort,
            remoteHost,
            remotePort,
            useNetcat
        )

        val tunnelId = "local_${localPort}_${remoteHost}_${remotePort}_${if (useNetcat) "nc" else "sshj"}"
        activeTunnels[tunnelId] = TunnelInfo(
            id = tunnelId,
            type = TunnelType.LOCAL,
            localPort = localPort,
            remoteHost = remoteHost,
            remotePort = remotePort,
            forwarder = forwarder
        )

        return forwarder
    }

    /**
     * 创建远程端口转发
     * 将远程端口的流量转发到本地主机的指定端口
     *
     * @param remotePort 远程端口
     * @param localHost 本地目标主机
     * @param localPort 本地目标端口
     * @return 绑定的远��端口转发对象
     */
    fun createRemotePortForward(
        remotePort: Int,
        localHost: String = "localhost",
        localPort: Int
    ): RemotePortForwarder.Forward {
        val client = sshClient ?: throw IllegalStateException("SSH client not connected")

        val forwarder = client.remotePortForwarder
        val boundForward = forwarder.bind(
            RemotePortForwarder.Forward(remotePort),
            object : ConnectListener {
                override fun gotConnect(chan: net.schmizz.sshj.connection.channel.Channel.Forwarded) {
                    // 处理连接回调 - 这里可以添加日志或其他处理逻辑
                }
            }
        )

        val tunnelId = "remote_${remotePort}_${localHost}_${localPort}"
        activeTunnels[tunnelId] = TunnelInfo(
            id = tunnelId,
            type = TunnelType.REMOTE,
            localPort = localPort,
            remoteHost = localHost,
            remotePort = remotePort,
            boundPort = boundForward.port
        )

        return boundForward
    }

    /**
     * 创建动态端口转发（SOCKS代理）
     *
     * @param localPort 本地SOCKS代理端口
     * @return SOCKS代理服务器
     */
    fun createDynamicPortForward(localPort: Int): SOCKSProxy {
        val client = sshClient ?: throw IllegalStateException("SSH client not connected")

        val proxy = SOCKSProxy(client, localPort)

        val tunnelId = "dynamic_${localPort}"
        activeTunnels[tunnelId] = TunnelInfo(
            id = tunnelId,
            type = TunnelType.DYNAMIC,
            localPort = localPort,
            proxy = proxy
        )

        return proxy
    }

    /**
     * 获取活跃的隧道列表
     */
    fun getActiveTunnels(): List<TunnelInfo> = activeTunnels.values.toList()

    /**
     * 关闭指定隧道
     */
    fun closeTunnel(tunnelId: String) {
        activeTunnels[tunnelId]?.let { tunnel ->
            when (tunnel.type) {
                TunnelType.LOCAL -> tunnel.forwarder?.close()
                TunnelType.REMOTE -> {
                    // 远程端口转发的关闭需要通过SSH客户端的remotePortForwarder
                    // 这里需要存储RemotePortForwarder.Forward引用来正确关闭
                    println("关闭远程端口转发: ${tunnel.id}")
                }
                TunnelType.DYNAMIC -> tunnel.proxy?.close()
            }
            activeTunnels.remove(tunnelId)
        }
    }

    /**
     * 关闭所有隧道
     */
    fun closeAllTunnels() {
        activeTunnels.keys.forEach { closeTunnel(it) }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = sshClient?.isConnected == true

    override fun close() {
        closeAllTunnels()
        sshClient?.close()
    }
}
