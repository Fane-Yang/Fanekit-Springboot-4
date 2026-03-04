package com.fanekit.common.util.net.ssh.sftp

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import org.apache.commons.pool2.BasePooledObjectFactory
import org.apache.commons.pool2.PooledObject
import org.apache.commons.pool2.impl.DefaultPooledObject
import org.apache.commons.pool2.impl.GenericObjectPool
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.nio.file.Paths
import java.time.Duration
import java.io.Closeable

/**
 * SftpClientPool 负责管理 SFTP 连接池，支持密钥或密码登录，自动池化 SFTPClient 实例。
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 *
 * @param host SFTP 服务器主机地址
 * @param port SFTP 服务器端口号
 * @param username 登录用户名
 * @param password 登录密码（可选，若使用密钥可为 null）
 * @param keyPath 私钥文件路径（可选，若用密码登录可为 null）
 * @param keyPassword 私钥文件密码（可选，若私钥无密码可为 null）
 * @param maxTotal 连接池最大可用连接数
 * @param maxWaitMillis 获取连接最大等待时间（毫秒）
 * @param maxIdle 连接池中最大空闲连接数
 * @param minIdle 连接池中最小空闲连接数
 * @param timeBetweenEvictionRunsSeconds 驱逐线程运行间隔（秒）
 * @param minEvictableIdleTimeSeconds 连接池中连接最小可驱逐空闲时间（秒）
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/05/12
 * @since 1.0.0
 * @version 1.0.0
 */
class SftpClientPool(
    host: String,
    port: Int,
    username: String,
    password: String? = null, // 密码可选，若使用密钥登录则为 null
    keyPath: String? = null, // 私钥路径可选，若使用密码登录则为 null
    keyPassword: String? = null, // 密钥路径和密码可选，若使用密码登录则为 null
    maxTotal: Int = 8, // 默认8个最大连接
    maxWaitMillis: Long = 30000, // 默认30秒
    maxIdle: Int = 8, // 默认8个最大空闲连接
    minIdle: Int = 0, // 默认0个空闲连接
    timeBetweenEvictionRunsSeconds: Long = 60, // 默认1分钟
    minEvictableIdleTimeSeconds: Int = 60 // 默认1分钟
): Closeable {
    private val pool: GenericObjectPool<SFTPClient>

    init {
        val config = GenericObjectPoolConfig<SFTPClient>()
        config.maxTotal = maxTotal
        config.blockWhenExhausted = true
        config.setMaxWait(Duration.ofMillis(maxWaitMillis))
        config.maxIdle = maxIdle
        config.minIdle = minIdle
        config.minEvictableIdleDuration = Duration.ofSeconds(minEvictableIdleTimeSeconds.toLong())
        config.timeBetweenEvictionRuns = Duration.ofSeconds(timeBetweenEvictionRunsSeconds)
        config.numTestsPerEvictionRun = -1
        config.testOnBorrow = true
        config.testOnReturn = true
        config.testWhileIdle = true
        pool = GenericObjectPool(
            SFTPClientFactory(host, port, username, password, keyPath, keyPassword),
            config
        )
    }

    private class SFTPClientFactory(
        private val host: String,
        private val port: Int,
        private val username: String,
        private val password: String?,
        private val keyPath: String?,
        private val keyPassword: String?
    ) : BasePooledObjectFactory<SFTPClient>() {

        override fun create(): SFTPClient {
            val sshClient = SSHClient()
            sshClient.addHostKeyVerifier(PromiscuousVerifier())
            sshClient.connect(host, port)
            if (!keyPath.isNullOrEmpty()) {
                // 如果密钥有密码保护,需要提供密码,否则不需要
                val location = Paths.get(keyPath).toString()
                val keyProvider: KeyProvider = if (!keyPassword.isNullOrEmpty()) {
                    sshClient.loadKeys(location, keyPassword)
                } else {
                    sshClient.loadKeys(location)
                }
                sshClient.authPublickey(username, keyProvider)
            } else if (!password.isNullOrEmpty()) {
                sshClient.authPassword(username, password)
            }
            // 以下配置待完善
            // sshClient.setConnectTimeout(30000)
            // sshClient.setTimeout(30000)
            return sshClient.newSFTPClient()
        }

        /**
         * 激活对象，当对象从池中借出时会自动调用，也可以在此处进行一些初始化操作
         */
        override fun activateObject(p: PooledObject<SFTPClient>) {
            val sftpClient = p.`object`
            if (sftpClient == null || !sftpClient.sftpEngine.subsystem.isOpen) {
                // 如果已关闭，抛出异常，让系统尝试删除此对象
                throw IllegalStateException("SFTP Client is not connected or the subsystem is closed.")
            }
        }

        /**
         * 验证对象是否有效，在对象借出前会自动调用，返回false则会销毁此对象
         */
        override fun validateObject(p: PooledObject<SFTPClient>): Boolean {
            val sftpClient = p.`object`
            return sftpClient != null && sftpClient.sftpEngine.subsystem.isOpen
        }

        override fun wrap(client: SFTPClient): PooledObject<SFTPClient> = DefaultPooledObject(client)

        override fun destroyObject(p: PooledObject<SFTPClient>) {
            val client = p.`object`
            client?.close()
        }
    }

    @Throws(Exception::class)
    fun borrowClient(): SFTPClient {
        return pool.borrowObject()
    }

    fun returnClient(client: SFTPClient?) {
        if (client != null) {
            pool.returnObject(client)
        }
    }

    override fun close() {
        try {
            pool.close()
        } catch (e: Exception) {
            // 处理关闭异常
        }
    }

    val isClosed: Boolean
        get() = pool.isClosed
}
