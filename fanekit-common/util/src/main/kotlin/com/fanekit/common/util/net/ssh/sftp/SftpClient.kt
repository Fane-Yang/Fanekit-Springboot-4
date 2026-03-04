package com.fanekit.common.util.net.ssh.sftp

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.sftp.SFTPException
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.io.Closeable
import java.nio.file.Paths

/**
 * SFTP 客户端工具类，用于创建和管理单个 SFTP 连接
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 *
 * TODO 需要继续完善单个工具类的功能，支持更多操作如重命名、移动等，并且需要优化返回代码逻辑报错等
 *
 * @param host SFTP 服务器主机地址
 * @param port SFTP 服务器端口号
 * @param username 登录用户名
 * @param password 登录密码（可选，若使用密钥可为 null）
 * @param keyPath 私钥文件路径（可选，若用密码登录可为 null）
 * @param keyPassword 私钥文件密码（可选，若私钥无密码可为 null）
 * @param connectTimeoutMs 连接超时时间（毫秒）
 * @param socketTimeoutMs 套接字超时时间（毫秒）
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/05/11
 * @since 1.0.0
 * @version 1.0.0
 */
class SftpClient(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val keyPath: String? = null,
    private val keyPassword: String? = null,
    private val connectTimeoutMs: Int = 30000,
    private val socketTimeoutMs: Int = 30000
) : Closeable {

    private var sshClient: SSHClient? = null
    private var sftpClient: SFTPClient? = null

    init {
        connect()
    }

    /**
     * 创建并返回 SFTP 客户端连接
     */
    fun connect(): SFTPClient {
        if (sftpClient == null || !sftpClient!!.sftpEngine.subsystem.isOpen) {
            val ssh = SSHClient().apply {
                addHostKeyVerifier(PromiscuousVerifier())
                connectTimeout = connectTimeoutMs
                timeout = socketTimeoutMs
                connect(host, port)

                // 认证
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
                        throw IllegalArgumentException("必须提供密码或密钥路径进行认证")
                    }
                }
            }

            sshClient = ssh
            sftpClient = ssh.newSFTPClient()
            return sftpClient!!
        }
        return sftpClient!!
    }

    /**
     * 获取当前的 SFTP 客户端，如果未连接则自动连接
     */
    fun getSftpClient(): SFTPClient {
        return sftpClient?.takeIf { it.sftpEngine.subsystem.isOpen } ?: connect()
    }

    /**
     * 检查连接是否有效
     */
    fun isConnected(): Boolean {
        return sftpClient?.sftpEngine?.subsystem?.isOpen == true && sshClient?.isConnected == true
    }

    /**
     * 关闭连接
     */
    override fun close() {
        try {
            sftpClient?.close()
        } catch (e: Exception) {
            // 忽略关闭异常
        }
        try {
            sshClient?.disconnect()
        } catch (e: Exception) {
            // 忽略关闭异常
        }
        sftpClient = null
        sshClient = null
    }

    /**
     * 上传文件
     */
    fun upload(localPath: String, remotePath: String) {
        getSftpClient().use { client ->
            client.put(localPath, remotePath)
        }
    }
    /**
     * 下载文件
     */
    fun download(remotePath: String, localPath: String) {
        getSftpClient().use { client ->
            client.get(remotePath, localPath)
        }
    }
    /**
     * 删除文件
     */
    fun delete(remotePath: String) {
        getSftpClient().use { client ->
            client.rm(remotePath)
        }
    }
    /**
     * 创建目录
     */
    fun mkdir(remotePath: String) {
        getSftpClient().use { client ->
            client.mkdirs(remotePath)
        }
    }
    /**
     * 列出目录内容
     */
    fun list(remotePath: String): List<String> {
        return getSftpClient().use { client ->
            client.ls(remotePath).map { it.name }
        }
    }
    /**
     * 检查文件或目录是否存在
     */
    fun exists(remotePath: String): Boolean {
        return try {
            getSftpClient().use { client ->
                client.stat(remotePath) != null
            }
        } catch (e: SFTPException) {
            // SFTP 特定异常，通常表示文件不存在或权限问题
            when (e.statusCode) {
                net.schmizz.sshj.sftp.Response.StatusCode.NO_SUCH_FILE -> false  // 文件不存在
                net.schmizz.sshj.sftp.Response.StatusCode.PERMISSION_DENIED -> {
                    // 权限被拒绝，文件可能存在但无法访问
                    throw IllegalAccessException("无权限访问路径: $remotePath")
                }
                else -> {
                    // 其他 SFTP 错误，重新抛出
                    throw e
                }
            }
        } catch (e: Exception) {
            // 网络连接等其他异常，重新抛出
            throw e
        }
    }
    /**
     * 检查文件列表是否存在，返回存在的文件列表 List<String>
     */
    fun checkFilesExist(remotePaths: List<String>): List<String> {
        return remotePaths.filter { path ->
            try {
                exists(path)
            } catch (e: Exception) {
                throw Exception(e)
            }
        }
    }


    /**
     * 构建器模式创建 SftpClient
     */
    class Builder {
        private var host: String = ""
        private var port: Int = 22
        private var username: String = ""
        private var password: String? = null
        private var keyPath: String? = null
        private var keyPassword: String? = null
        private var connectTimeoutMs: Int = 30000
        private var socketTimeoutMs: Int = 30000

        fun host(host: String) = apply { this.host = host }
        fun port(port: Int) = apply { this.port = port }
        fun username(username: String) = apply { this.username = username }
        fun password(password: String) = apply { this.password = password }
        fun keyPath(keyPath: String) = apply { this.keyPath = keyPath }
        fun keyPassword(keyPassword: String) = apply { this.keyPassword = keyPassword }
        fun connectTimeout(timeoutMs: Int) = apply { this.connectTimeoutMs = timeoutMs }
        fun socketTimeout(timeoutMs: Int) = apply { this.socketTimeoutMs = timeoutMs }

        fun build(): SftpClient {
            require(host.isNotEmpty()) { "主机地址不能为空" }
            require(username.isNotEmpty()) { "用户名不能为空" }
            require(!password.isNullOrEmpty() || !keyPath.isNullOrEmpty()) {
                "必须提供密码或密钥路径"
            }

            return SftpClient(
                host, port, username, password, keyPath, keyPassword,
                connectTimeoutMs, socketTimeoutMs
            )
        }
    }

    companion object {
        /**
         * 创建构建器
         */
        fun builder() = Builder()

        /**
         * 快速创建密码认证的 SFTP 客户端
         */
        fun withPassword(host: String, username: String, password: String, port: Int = 22): SftpClient {
            return SftpClient(host, port, username, password)
        }

        /**
         * 快速创建密钥认证的 SFTP 客户端
         */
        fun withKey(host: String, username: String, keyPath: String, keyPassword: String? = null, port: Int = 22): SftpClient {
            return SftpClient(host, port, username, null, keyPath, keyPassword)
        }
    }
}