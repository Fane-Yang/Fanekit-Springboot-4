package com.fanekit.common.util.net.ssh.command

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * SSH命令执行器，用于远程执行命令
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 *
 * @param host SSH服务器主机地址
 * @param port SSH服务器端口号
 * @param username 登录用户名
 * @param password 登录密码（可选）
 * @param keyPath 私钥文件路径（可选）
 * @param keyPassword 私钥文件密码（可选）
 * @param connectionTimeoutMs 连接超时时间（毫秒）
 * @param commandTimeoutMs 命令执行超时时间（毫秒）
 */
class SSHCommandExecutor(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val keyPath: String? = null,
    private val keyPassword: String? = null,
    private val connectionTimeoutMs: Int = 30000,
    private val commandTimeoutMs: Long = 60000
) : AutoCloseable {

    private var sshClient: SSHClient? = null

    /**
     * 建立SSH连接
     */
    fun connect(): SSHCommandExecutor {
        if (sshClient?.isConnected == true) {
            return this
        }

        sshClient = SSHClient().apply {
            addHostKeyVerifier(PromiscuousVerifier())
            connectTimeout = connectionTimeoutMs
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
                    throw IllegalArgumentException("Either password or keyPath must be provided")
                }
            }
        }
        return this
    }

    /**
     * 执行单个命令
     *
     * @param command 要执行的命令
     * @return 命令执行结果
     */
    fun executeCommand(command: String): SSHCommandResult {
        val client = sshClient ?: throw IllegalStateException("SSH client not connected")

        return client.startSession().use { session ->
            val cmd = session.exec(command)

            // 等待命令完成
            cmd.join(commandTimeoutMs, TimeUnit.MILLISECONDS)

            val stdout = cmd.inputStream.bufferedReader().use { it.readText() }
            val stderr = cmd.errorStream.bufferedReader().use { it.readText() }
            val exitCode = cmd.exitStatus

            SSHCommandResult(
                command = command,
                exitCode = exitCode,
                stdout = stdout,
                stderr = stderr,
                executionTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 批量执行命令
     *
     * @param commands 要执行的命令列表
     * @param stopOnError 遇到错误时是否停止执行
     * @return 命令执行结果列表
     */
    fun executeCommands(commands: List<String>, stopOnError: Boolean = false): List<SSHCommandResult> {
        val results = mutableListOf<SSHCommandResult>()

        for (command in commands) {
            try {
                val result = executeCommand(command)
                results.add(result)

                if (stopOnError && result.exitCode != 0) {
                    break
                }
            } catch (e: Exception) {
                val errorResult = SSHCommandResult(
                    command = command,
                    exitCode = -1,
                    stdout = "",
                    stderr = "Exception: ${e.message}",
                    executionTime = System.currentTimeMillis()
                )
                results.add(errorResult)

                if (stopOnError) {
                    break
                }
            }
        }

        return results
    }

    /**
     * 创建交互式会话
     *
     * @return SSH交互式会话
     */
    fun createInteractiveSession(): SSHInteractiveSession {
        val client = sshClient ?: throw IllegalStateException("SSH client not connected")
        return SSHInteractiveSession(client.startSession())
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = sshClient?.isConnected == true

    override fun close() {
        sshClient?.close()
    }
}

