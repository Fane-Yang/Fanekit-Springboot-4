package com.fanekit.common.util.net.ssh.sftp

import net.schmizz.sshj.sftp.*
import java.nio.file.Files
import java.nio.file.Paths
import java.io.Closeable

/**
 * SFTP管理器，提供文件上传、下载、管理等功能
 * 注意，临时使用时必须手动释放资源，或者使用 use 函数自动释放。
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/05/13
 * @since 1.0.0
 * @version 1.0.0
 */
class SftpManager: Closeable {
    private val clientPool: SftpClientPool

    // 成功操作
    private val SUCCESS = 0

    // REGULAR(32768)：普通文件。这是最常见的文件类型，如文本文件、二进制文件等。
    private val FILE_TYPE_FILE = 1
    // DIRECTORY(16384)：目录。这种类型的文件用于组织其他文件。
    private val FILE_TYPE_DIRECTORY = 2
    // BLOCK_SPECIAL(24576)：块设备文件。这种类型的文件用于描述设备，如硬盘、光驱等。
    private val FILE_TYPE_BLOCK_SPECIAL = 3
    // CHAR_SPECIAL(8192)：字符设备文件。这种类型的文件用于描述设备，如键盘、鼠标等。
    private val FILE_TYPE_CHAR_SPECIAL = 4
    // FIFO_SPECIAL(4096)：管道文件。这种类型的文件用于进程间通信。
    private val FILE_TYPE_FIFO_SPECIAL = 5
    // SOCKET_SPECIAL(49152)：套接字文件。这种类型的文件用于网络通信。
    private val FILE_TYPE_SOCKET_SPECIAL = 6
    // SYMLINK(40960)：符号链接。这种类型的文件是一个指向另一个文件的快捷方式。
    private val FILE_TYPE_SYMLINK = 7
    // UNKNOWN(0)：未知的文件类型。如果文件的类型不是上述任何一种，那么它的类型就是未知的。
    private val FILE_TYPE_UNKNOWN = 8

    // 文件不存在
    private val ERROR_CODE_FILE_NOT_EXIST = -1
    // 权限不足
    private val ERROR_CODE_PERMISSION_DENIED = -2
    // 未知错误
    private val ERROR_CODE_FAILURE = -3
    // 消息无法解析
    private val ERROR_CODE_BAD_MESSAGE = -4
    // 未连接到服务器
    private val ERROR_CODE_NO_CONNECTION = -5
    // 与服务器连接已断开
    private val ERROR_CODE_CONNECITON_LOST = -6
    // 服务器不支持请求的操作
    private val ERROR_CODE_OP_UNSUPPORTED = -7
    // 文件句柄无效
    private val ERROR_CODE_INVALID_HANDLE = -8
    // 指定的路径不存在
    private val ERROR_CODE_NO_SUCH_PATH = -9
    // 文件已存在
    private val ERROR_CODE_FILE_ALREADY_EXISTS = -10
    // 文件被写保护
    private val ERROR_CODE_WRITE_PROTECT = -11
    // 设备上没有媒体
    private val ERROR_CODE_NO_MEDIA = -12
    // 文件系统上没有空间
    private val ERROR_CODE_NO_SPACE_ON_FILESYSTEM = -13
    // 已超过配额
    private val ERROR_CODE_QUOTA_EXCEEDED = -14
    // 未知的用户或组
    private val ERROR_CODE_UNKNOWN_PRINCIPAL = -15
    // 锁冲突
    private val ERROR_CODE_LOCK_CONFLICT = -16
    // 目录不为空
    private val ERROR_CODE_DIR_NOT_EMPTY = -17
    // 不是一个目录
    private val ERROR_CODE_NOT_A_DIRECTORY = -18
    // 文件名无效
    private val ERROR_CODE_INVALID_FILENAME = -19
    // 符号链接形成了循环
    private val ERROR_CODE_LINK_LOOP = -20
    // 无法删除文件或目录
    private val ERROR_CODE_CANNOT_DELETE = -21
    // 参数无效
    private val ERROR_CODE_INVALID_PARAMETER = -22
    // 文件是一个目录
    private val ERROR_CODE_FILE_IS_A_DIRECTORY = -23
    // 字节范围锁冲突
    private val ERROR_CODE_BYTE_RANGE_LOCK_CONFLICT = -24
    // 字节范围锁被拒绝
    private val ERROR_CODE_BYTE_RANGE_LOCK_REFUSED = -25
    // 文件正在等待删除
    private val ERROR_CODE_DELETE_PENDING = -26
    // 文件已损坏
    private val ERROR_CODE_FILE_CORRUPT = -27
    // 文件所有者无效
    private val ERROR_CODE_OWNER_INVALID = -28
    // 文件组无效
    private val ERROR_CODE_GROUP_INVALID = -29
    // 没有匹配的字节范围锁
    private val ERROR_CODE_NO_MATCHING_BYTE_RANGE_LOCK = -30

    // 远程文件未完整
    private val ERROR_CODE_FILE_NOT_COMPLETE = -101
    // 不是一个文件
    private val ERROR_CODE_IS_NOT_A_FILE = -102
    // 文件下载失败，文件大小不一致
    private val ERROR_CODE_FILE_SIZE_NOT_EQUAL = -103

    /**
     * 初始化
     * @param host 主机地址
     * @param port 端口
     * @param username 用户名
     * @param password 密码
     * @param keyPath 密钥路径
     * @param keyPassword 密钥密码
     * @param maxTotal 最大可借出对象数
     * @param maxWaitMillis 最大等待时间
     * @param maxIdle 最大空闲对象数
     * @param minIdle 最小空闲对象数
     * @param minEvictableIdleTimeSeconds 最小可驱逐空闲时间，超过此时间的空闲对象将可以被系统驱逐，秒
     */
    constructor(
        host: String,
        port: Int,
        username: String,
        password: String?,
        keyPath: String?,
        keyPassword: String?,
        maxTotal: Int,
        maxWaitMillis: Long,
        maxIdle: Int,
        minIdle: Int,
        minEvictableIdleTimeSeconds: Int
    ) {
        this.clientPool = SftpClientPool(
            host,
            port,
            username,
            password,
            keyPath,
            keyPassword,
            maxTotal,
            maxWaitMillis,
            maxIdle,
            minIdle,
            60, // timeBetweenEvictionRunsSeconds 默认值
            minEvictableIdleTimeSeconds
        )
    }

    /**
     * 初始化
     * @param clientPool SFTP客户端池
     */
    constructor(clientPool: SftpClientPool) {
        this.clientPool = clientPool
    }

    /**
     * 关闭连接池，释放所有资源
     */
    override fun close() {
        try {
            clientPool.close()
        } catch (e: Exception) {
            // 记录关闭异常但不抛出
            println("关闭 SFTP 连接池时发生异常: ${e.message}")
        }
    }

    /**
     * 检查连接池是否已关闭
     */
    fun isClosed(): Boolean {
        return clientPool.isClosed
    }

    /**
     * 检查指定目录是否存在任一给定的文件名
     * @param client SFTP客户端
     * @param remotePath 远程路径，不包含文件名
     * @param fileNames 文件名列表
     * @return 是否存在任一文件,是的话返回0，否则返回错误码
     * @throws Exception 异常
     */
    private fun scanFileExist(client: SFTPClient, remotePath: String, vararg fileNames: String): Int {
        val fileNameSet = mutableSetOf<String>()
        try {
            for (file in client.ls(remotePath)) {
                fileNameSet.add(file.name)
            }
        } catch (e: SFTPException) {
            // 如果路径不存在，client.ls(remotePath)将抛出一个异常
            return throwSftpErrorCode(e)
        }
        for (fileName in fileNames) {
            if (fileNameSet.contains(fileName)) {
                return SUCCESS
            }
        }
        return ERROR_CODE_FILE_NOT_EXIST
    }

    /**
     * 检查指定目录是否存在任一给定的文件名
     * @param remotePath 远程路径，不包含文件名
     * @param fileNames 文件名列表
     * @return 是否存在任一文件
     * @throws Exception 异常
     */
    fun scanFileExist(remotePath: String, vararg fileNames: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val fileNameSet = mutableSetOf<String>()
            for (file in client.ls(remotePath)) {
                fileNameSet.add(file.name)
            }
            for (fileName in fileNames) {
                if (fileNameSet.contains(fileName)) {
                    return true
                }
            }
            return false
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 扫描指定目录,并传入文件名列表比对，返回存在的文件名列表
     * @param remotePath 远程路径，不包含文件名
     * @param fileNames 文件名列表
     * @return 存在的文件名列表
     * @throws Exception 异常
     */
    fun scanFileExist(remotePath: String, fileNames: List<String>): List<String> {
        val client = clientPool.borrowClient()
        try {
            val fileNameSet = mutableSetOf<String>()
            for (file in client.ls(remotePath)) {
                fileNameSet.add(file.name)
            }
            val result = mutableListOf<String>()
            for (fileName in fileNames) {
                if (fileNameSet.contains(fileName)) {
                    result.add(fileName)
                }
            }
            return result
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 扫描指定目录，返回存在的文件名列表
     * @param client SFTP客户端
     * @param remotePath 远程路径，不包含文件名
     * @param fileNames 文件名列表
     * @return 存在的文件名列表
     * @throws Exception 异常
     */
    private fun scanFileExist(client: SFTPClient, remotePath: String, fileNames: List<String>): List<String> {
        val fileNameSet = mutableSetOf<String>()
        for (file in client.ls(remotePath)) {
            fileNameSet.add(file.name)
        }
        val result = mutableListOf<String>()
        for (fileName in fileNames) {
            if (fileNameSet.contains(fileName)) {
                result.add(fileName)
            }
        }
        return result
    }

    fun listFiles(remotePath: String): List<String> {
        val client = clientPool.borrowClient()
        try {
            val result = mutableListOf<String>()
            for (file in client.ls(remotePath)) {
                result.add(file.name)
            }
            return result
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun listFilesInfo(remotePath: String): List<RemoteResourceInfo> {
        val client = clientPool.borrowClient()
        try {
            return client.ls(remotePath)
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 检查指定的远程目录是否存在
     * @param client SFTP客户端
     * @param remotePath 远程目录路径
     * @return 目录是否存在
     * @throws Exception 异常
     */
    private fun checkDirectoryExists(client: SFTPClient, remotePath: String): Boolean {
        return try {
            client.stat(remotePath)
            true
        } catch (e: SFTPException) {
            // 检查是否是因为文件或目录不存在的错误
            if (e.statusCode == Response.StatusCode.NO_SUCH_FILE) {
                false
            } else {
                // 如果是其他类型的SFTP异常，可能需要重新抛出异常或进行其他处理
                throw e
            }
        }
    }

    /**
     * 级联创建远程目录
     * @param client SFTP客户端
     * @param remotePath 要创建的远程目录路径
     * @throws Exception 异常
     */
    private fun createDirectories(client: SFTPClient, remotePath: String) {
        val pathElements = remotePath.split("/")
        val path = StringBuilder()
        for (element in pathElements) {
            if (element.isNotEmpty()) {
                path.append("/").append(element)
                try {
                    client.stat(path.toString())
                } catch (e: Exception) {
                    client.mkdir(path.toString())
                }
            }
        }
    }

    fun createDirectories(remotePath: String) {
        val client = clientPool.borrowClient()
        try {
            createDirectories(client, remotePath)
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 上传文件，不报错则上传成功,上传完成后，将临时文件重命名为最终的文件名
     * @param localPath 本地文件路径,包含文件名
     * @param remotePath 远程文件路径,包含文件名
     * @param overwrite 是否覆盖
     * @throws Exception 异常
     */
    fun uploadFile(localPath: String, remotePath: String, overwrite: Boolean): Int {
        val client = clientPool.borrowClient()
        val tempRemotePath = "$remotePath.lock"
        try {
            // 文件存在
            if (scanFileExist(
                    client,
                    remotePath.substring(0, remotePath.lastIndexOf("/")),
                    remotePath.substring(remotePath.lastIndexOf("/") + 1)
                ) == SUCCESS
            ) {
                if (overwrite) {
                    // 需要覆盖，则删除
                    client.rm(remotePath)
                } else {
                    // 不需要覆盖，则返回文件已存在
                    return ERROR_CODE_FILE_ALREADY_EXISTS
                }
            } else if (!checkDirectoryExists(client, remotePath.substring(0, remotePath.lastIndexOf("/")))) {
                // 文件不存在，检测目录是否存在
                // 不存在则级联创建目录,创建时报错则无权限
                createDirectories(client, remotePath.substring(0, remotePath.lastIndexOf("/")))
            }
            // 先上传到临时文件
            client.put(localPath, tempRemotePath)
            // 上传完成后，将临时文件重命名为最终的文件名
            client.rename(tempRemotePath, remotePath)
            return SUCCESS
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * moveFile
     */
    fun moveFile(remotePath: String, newRemotePath: String) {
        val client = clientPool.borrowClient()
        try {
            // 检测远程目录是否存在，不存在则创建
            if (!checkDirectoryExists(client, newRemotePath.substring(0, newRemotePath.lastIndexOf("/")))) {
                createDirectories(client, newRemotePath.substring(0, newRemotePath.lastIndexOf("/")))
            }
            client.rename(remotePath, newRemotePath)
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 下载文件
     * @param remotePath remotePath为远程文件路径,包含文件名
     * @param localPath localPath为本地文件路径,包含文件名
     * @param checkSeconds 检查文件是否完整的时间间隔，秒
     * @return SUCCESS 0 成功，
     *          >1 文件类型错误，可以直接报错，不重复执行要执行的任务了，
     *          <0 错误码
     * @throws Exception 异常
     */
    fun downloadFile(remotePath: String, localPath: String, checkSeconds: Int): Int {
        val client = clientPool.borrowClient()
        try {
            // 检测文件是否存在
            if (scanFileExist(
                    client,
                    remotePath.substring(0, remotePath.lastIndexOf("/")),
                    remotePath.substring(remotePath.lastIndexOf("/") + 1)
                ) != SUCCESS
            ) {
                return ERROR_CODE_FILE_NOT_EXIST
            }
            // 检测是否是文件
            val fileType = checkFileType(client, remotePath)
            if (fileType != FILE_TYPE_FILE) {
                return fileType
            }
            // 检查文件是否已经完整
            if (!isUploadComplete(client, remotePath, checkSeconds)) {
                return ERROR_CODE_FILE_NOT_COMPLETE
            }
            // 检查文件权限是否可以允许下载
            if (!isFileReadable(client, remotePath)) {
                return ERROR_CODE_PERMISSION_DENIED
            }
            // 获取远程文件大小
            val remoteFileSizeFirst = client.stat(remotePath).size
            // 下载文件
            client.get(remotePath, localPath)
            // 再次获取远程文件大小
            val remoteFileSizeSecond = client.stat(remotePath).size
            // 获取本地文件大小
            val path = Paths.get(localPath)
            val localFileSize = Files.size(path)
            // 比较两次远程文件大小是否一直，和本地文件大小与远程文件大小是否一致，一致则认为下载成功
            return if (remoteFileSizeFirst == remoteFileSizeSecond && remoteFileSizeSecond == localFileSize) {
                SUCCESS
            } else {
                // 尝试删除下载的文件
                Files.deleteIfExists(path)
                ERROR_CODE_FILE_SIZE_NOT_EQUAL
            }
        } catch (e: SFTPException) {
            // 正常范围内的错误
            return throwSftpErrorCode(e)
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 是否下载完成
     * @param remotePath 远程文件路径,包含文件名
     * @param localPath 本地文件路径,包含文件名
     * @return
     * @throws Exception
     */
    fun isDownloadComplete(remotePath: String, localPath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            // 获取远程文件大小
            val remoteFileSize = client.stat(remotePath).size
            // 获取本地文件大小
            val localFileSize = Files.size(Paths.get(localPath))
            // 比较两者的大小
            return remoteFileSize == localFileSize
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun deleteFile(remotePath: String) {
        val client = clientPool.borrowClient()
        try {
            client.rm(remotePath)
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun isFile(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a regular file
            return mode.type == FileMode.Type.REGULAR
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun isFileReadable(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a regular file and readable
            return mode.type == FileMode.Type.REGULAR && (mode.permissionsMask and 0x100) != 0
        } finally {
            clientPool.returnClient(client)
        }
    }

    private fun isFileReadable(client: SFTPClient, remotePath: String): Boolean {
        val attributes = client.stat(remotePath)
        val mode = attributes.mode
        // Check if it's a regular file and readable
        return mode.type == FileMode.Type.REGULAR && (mode.permissionsMask and 0x100) != 0
    }

    fun isFileWritable(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a regular file and writable
            return mode.type == FileMode.Type.REGULAR && (mode.permissionsMask and 0x80) != 0
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun isDirectory(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a directory
            return mode.type == FileMode.Type.DIRECTORY
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun isDirectoryReadable(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a directory and readable
            return mode.type == FileMode.Type.DIRECTORY && (mode.permissionsMask and 0x100) != 0
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun isDirectoryWritable(remotePath: String): Boolean {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val mode = attributes.mode
            // Check if it's a directory and writable
            return mode.type == FileMode.Type.DIRECTORY && (mode.permissionsMask and 0x80) != 0
        } finally {
            clientPool.returnClient(client)
        }
    }

    fun checkFileType(remotePath: String): Int {
        val client = clientPool.borrowClient()
        try {
            val attributes = client.stat(remotePath)
            val type = attributes.mode.type
            // Check the type
            return when (type) {
                FileMode.Type.REGULAR -> FILE_TYPE_FILE
                FileMode.Type.DIRECTORY -> FILE_TYPE_DIRECTORY
                FileMode.Type.BLOCK_SPECIAL -> FILE_TYPE_BLOCK_SPECIAL
                FileMode.Type.CHAR_SPECIAL -> FILE_TYPE_CHAR_SPECIAL
                FileMode.Type.FIFO_SPECIAL -> FILE_TYPE_FIFO_SPECIAL
                FileMode.Type.SOCKET_SPECIAL -> FILE_TYPE_SOCKET_SPECIAL
                FileMode.Type.SYMLINK -> FILE_TYPE_SYMLINK
                else -> FILE_TYPE_UNKNOWN
            }
        } catch (e: SFTPException) {
            return throwSftpErrorCode(e)
        } finally {
            clientPool.returnClient(client)
        }
    }

    private fun checkFileType(client: SFTPClient, remotePath: String): Int {
        return try {
            val attributes = client.stat(remotePath)
            val type = attributes.mode.type
            // Check the type
            when (type) {
                FileMode.Type.REGULAR -> FILE_TYPE_FILE
                FileMode.Type.DIRECTORY -> FILE_TYPE_DIRECTORY
                FileMode.Type.BLOCK_SPECIAL -> FILE_TYPE_BLOCK_SPECIAL
                FileMode.Type.CHAR_SPECIAL -> FILE_TYPE_CHAR_SPECIAL
                FileMode.Type.FIFO_SPECIAL -> FILE_TYPE_FIFO_SPECIAL
                FileMode.Type.SOCKET_SPECIAL -> FILE_TYPE_SOCKET_SPECIAL
                FileMode.Type.SYMLINK -> FILE_TYPE_SYMLINK
                else -> FILE_TYPE_UNKNOWN
            }
        } catch (e: SFTPException) {
            throwSftpErrorCode(e)
        }
    }

    fun getFileMode(remotePath: String): FileMode {
        val client = clientPool.borrowClient()
        try {
            return client.stat(remotePath).mode
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 返回远程文件大小
     * @param remotePath
     * @return
     * @throws Exception
     */
    fun getFileSize(remotePath: String): Long {
        val client = clientPool.borrowClient()
        try {
            return client.stat(remotePath).size
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 检查上传是否完成
     * (可能文件被其他人上传时，还处于上传中状态，
     * 所以检测到文件存在之后还需要延迟检测，
     * 如果检测到文件大小不再变化，就认为上传完成)
     * @param remotePath 远程文件路径,包含文件名
     * @param seconds 检测间隔时间,单位秒,超出时间文件无变化认为上传完成
     * @return 认为是否上传完成
     * @throws Exception 异常
     */
    fun isUploadComplete(remotePath: String, seconds: Int): Boolean {
        val client = clientPool.borrowClient()
        try {
            var lastSize = -1L
            var currentSize = client.stat(remotePath).size
            while (currentSize != lastSize) {
                lastSize = currentSize
                Thread.sleep((1000 * seconds).toLong()) // 等待seconds秒再次检查
                currentSize = client.stat(remotePath).size
            }
            return true
        } finally {
            clientPool.returnClient(client)
        }
    }

    /**
     * 检查上传是否完成
     * (可能文件被其他人上传时，还处于上传中状态，
     * 所以检测到文件存在之后还需要延迟检测，
     * 如果检测到文件大小不再变化，就认为上传完成)
     * @param remotePath 远程文件路径,包含文件名
     * @param seconds 检测间隔时间,单位秒,超出时间文件无变化认为上传完成
     * @return 认为是否上传完成
     * @throws Exception 异常
     */
    private fun isUploadComplete(client: SFTPClient, remotePath: String, seconds: Int): Boolean {
        var lastSize = -1L
        var currentSize = client.stat(remotePath).size
        while (currentSize != lastSize) {
            lastSize = currentSize
            Thread.sleep((1000 * seconds).toLong()) // 等待seconds秒再次检查
            currentSize = client.stat(remotePath).size
        }
        return true
    }

    /**
     * 抛出SFTP错误码
     * @param e SFTP异常
     * @return 错误码
     * @throws Exception 将未识别的错误码抛出为其他异常
     */
    fun throwSftpErrorCode(e: SFTPException): Int {
        return when (e.statusCode) {
            Response.StatusCode.NO_SUCH_FILE -> ERROR_CODE_FILE_NOT_EXIST
            Response.StatusCode.PERMISSION_DENIED -> ERROR_CODE_PERMISSION_DENIED
            Response.StatusCode.FAILURE -> ERROR_CODE_FAILURE
            Response.StatusCode.BAD_MESSAGE -> ERROR_CODE_BAD_MESSAGE
            Response.StatusCode.NO_CONNECTION -> ERROR_CODE_NO_CONNECTION
            Response.StatusCode.CONNECITON_LOST -> ERROR_CODE_CONNECITON_LOST
            Response.StatusCode.OP_UNSUPPORTED -> ERROR_CODE_OP_UNSUPPORTED
            Response.StatusCode.INVALID_HANDLE -> ERROR_CODE_INVALID_HANDLE
            Response.StatusCode.NO_SUCH_PATH -> ERROR_CODE_NO_SUCH_PATH
            Response.StatusCode.FILE_ALREADY_EXISTS -> ERROR_CODE_FILE_ALREADY_EXISTS
            Response.StatusCode.WRITE_PROTECT -> ERROR_CODE_WRITE_PROTECT
            Response.StatusCode.NO_MEDIA -> ERROR_CODE_NO_MEDIA
            Response.StatusCode.NO_SPACE_ON_FILESYSTEM -> ERROR_CODE_NO_SPACE_ON_FILESYSTEM
            Response.StatusCode.QUOTA_EXCEEDED -> ERROR_CODE_QUOTA_EXCEEDED
            Response.StatusCode.UNKNOWN_PRINCIPAL -> ERROR_CODE_UNKNOWN_PRINCIPAL
            Response.StatusCode.LOCK_CONFLICT -> ERROR_CODE_LOCK_CONFLICT
            Response.StatusCode.DIR_NOT_EMPTY -> ERROR_CODE_DIR_NOT_EMPTY
            Response.StatusCode.NOT_A_DIRECTORY -> ERROR_CODE_NOT_A_DIRECTORY
            Response.StatusCode.INVALID_FILENAME -> ERROR_CODE_INVALID_FILENAME
            Response.StatusCode.LINK_LOOP -> ERROR_CODE_LINK_LOOP
            Response.StatusCode.CANNOT_DELETE -> ERROR_CODE_CANNOT_DELETE
            Response.StatusCode.INVALID_PARAMETER -> ERROR_CODE_INVALID_PARAMETER
            Response.StatusCode.FILE_IS_A_DIRECTORY -> ERROR_CODE_FILE_IS_A_DIRECTORY
            Response.StatusCode.BYTE_RANGE_LOCK_CONFLICT -> ERROR_CODE_BYTE_RANGE_LOCK_CONFLICT
            Response.StatusCode.BYTE_RANGE_LOCK_REFUSED -> ERROR_CODE_BYTE_RANGE_LOCK_REFUSED
            Response.StatusCode.DELETE_PENDING -> ERROR_CODE_DELETE_PENDING
            Response.StatusCode.FILE_CORRUPT -> ERROR_CODE_FILE_CORRUPT
            Response.StatusCode.OWNER_INVALID -> ERROR_CODE_OWNER_INVALID
            Response.StatusCode.GROUP_INVALID -> ERROR_CODE_GROUP_INVALID
            Response.StatusCode.NO_MATCHING_BYTE_RANGE_LOCK -> ERROR_CODE_NO_MATCHING_BYTE_RANGE_LOCK
            else -> throw e
        }
    }
}
