package com.fanekit.common.util.net.ssh.scp

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.userauth.keyprovider.KeyProvider
import net.schmizz.sshj.xfer.FileSystemFile
import net.schmizz.sshj.xfer.scp.SCPFileTransfer
import java.io.File
import java.nio.file.Paths

/**
 * SCP文件传输客户端
 * 提供基于SCP协议的文件上传下载功能
 *
 * @param host SSH服务器主机地址
 * @param port SSH服务器端口号
 * @param username 登录用户名
 * @param password 登录密码（可选）
 * @param keyPath 私钥文件路径（可选）
 * @param keyPassword 私钥文件密码（可选）
 */
class SCPClient(
    private val host: String,
    private val port: Int = 22,
    private val username: String,
    private val password: String? = null,
    private val keyPath: String? = null,
    private val keyPassword: String? = null
) : AutoCloseable {

    private var sshClient: SSHClient? = null
    private var scpTransfer: SCPFileTransfer? = null

    /**
     * 建立SSH连接
     */
    fun connect(): SCPClient {
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

        scpTransfer = sshClient!!.newSCPFileTransfer()
        return this
    }

    /**
     * 上传文件
     *
     * @param localFilePath 本地文件路径
     * @param remoteFilePath 远程文件路径
     * @param preserveAttributes 是否保持文件属性
     * @return 传输结果
     */
    fun uploadFile(
        localFilePath: String,
        remoteFilePath: String,
        preserveAttributes: Boolean = true
    ): SCPTransferResult {
        val transfer = scpTransfer ?: throw IllegalStateException("SCP client not connected")
        val localFile = File(localFilePath)

        if (!localFile.exists()) {
            throw IllegalArgumentException("Local file does not exist: $localFilePath")
        }

        return try {
            val startTime = System.currentTimeMillis()

            if (preserveAttributes) {
                transfer.upload(FileSystemFile(localFile), remoteFilePath)
            } else {
                transfer.upload(localFilePath, remoteFilePath)
            }

            val endTime = System.currentTimeMillis()

            SCPTransferResult(
                operation = SCPOperation.UPLOAD,
                localPath = localFilePath,
                remotePath = remoteFilePath,
                fileSize = localFile.length(),
                success = true,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            SCPTransferResult(
                operation = SCPOperation.UPLOAD,
                localPath = localFilePath,
                remotePath = remoteFilePath,
                fileSize = localFile.length(),
                success = false,
                error = e.message,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 下载文件
     *
     * @param remoteFilePath 远程文件路径
     * @param localFilePath 本地文件路径
     * @param preserveAttributes 是否保持文件属性
     * @return 传输结果
     */
    fun downloadFile(
        remoteFilePath: String,
        localFilePath: String,
        preserveAttributes: Boolean = true
    ): SCPTransferResult {
        val transfer = scpTransfer ?: throw IllegalStateException("SCP client not connected")
        val localFile = File(localFilePath)

        // 确保本地目录存在
        localFile.parentFile?.mkdirs()

        return try {
            val startTime = System.currentTimeMillis()

            if (preserveAttributes) {
                transfer.download(remoteFilePath, FileSystemFile(localFile))
            } else {
                transfer.download(remoteFilePath, localFilePath)
            }

            val endTime = System.currentTimeMillis()

            SCPTransferResult(
                operation = SCPOperation.DOWNLOAD,
                localPath = localFilePath,
                remotePath = remoteFilePath,
                fileSize = localFile.length(),
                success = true,
                startTime = startTime,
                endTime = endTime
            )
        } catch (e: Exception) {
            SCPTransferResult(
                operation = SCPOperation.DOWNLOAD,
                localPath = localFilePath,
                remotePath = remoteFilePath,
                fileSize = 0,
                success = false,
                error = e.message,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis()
            )
        }
    }

    /**
     * 上传目录
     *
     * @param localDirPath 本地目录路径
     * @param remoteDirPath 远程目录路径
     * @param recursive 是否递归上传子目录
     * @return 传输结果列表
     */
    fun uploadDirectory(
        localDirPath: String,
        remoteDirPath: String,
        recursive: Boolean = true
    ): List<SCPTransferResult> {
        val localDir = File(localDirPath)
        if (!localDir.exists() || !localDir.isDirectory) {
            throw IllegalArgumentException("Local directory does not exist or is not a directory: $localDirPath")
        }

        val results = mutableListOf<SCPTransferResult>()

        localDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = localDir.toPath().relativize(file.toPath()).toString()
                val remoteFilePath = "$remoteDirPath/$relativePath".replace("\\", "/")

                val result = uploadFile(file.absolutePath, remoteFilePath)
                results.add(result)
            }
        }

        return results
    }

    /**
     * 下载目录
     *
     * @param remoteDirPath 远程目录路径
     * @param localDirPath 本地目录路径
     * @return 传输结果列表
     */
    fun downloadDirectory(
        remoteDirPath: String,
        localDirPath: String
    ): List<SCPTransferResult> {
        val transfer = scpTransfer ?: throw IllegalStateException("SCP client not connected")
        val localDir = File(localDirPath)
        localDir.mkdirs()

        return try {
            val startTime = System.currentTimeMillis()
            transfer.download(remoteDirPath, FileSystemFile(localDir))
            val endTime = System.currentTimeMillis()

            listOf(SCPTransferResult(
                operation = SCPOperation.DOWNLOAD,
                localPath = localDirPath,
                remotePath = remoteDirPath,
                fileSize = calculateDirectorySize(localDir),
                success = true,
                startTime = startTime,
                endTime = endTime
            ))
        } catch (e: Exception) {
            listOf(SCPTransferResult(
                operation = SCPOperation.DOWNLOAD,
                localPath = localDirPath,
                remotePath = remoteDirPath,
                fileSize = 0,
                success = false,
                error = e.message,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis()
            ))
        }
    }

    /**
     * 批量传输文件
     *
     * @param transfers 传输任务列表
     * @return 传输结果列表
     */
    fun batchTransfer(transfers: List<SCPTransferTask>): List<SCPTransferResult> {
        return transfers.map { task ->
            when (task.operation) {
                SCPOperation.UPLOAD -> uploadFile(task.localPath, task.remotePath)
                SCPOperation.DOWNLOAD -> downloadFile(task.remotePath, task.localPath)
            }
        }
    }

    /**
     * 计算目录大小
     */
    private fun calculateDirectorySize(directory: File): Long {
        return directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean = sshClient?.isConnected == true

    override fun close() {
        sshClient?.close()
    }
}





