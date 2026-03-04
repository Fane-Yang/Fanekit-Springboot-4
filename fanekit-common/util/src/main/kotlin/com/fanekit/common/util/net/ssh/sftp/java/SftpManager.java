//package xyz.ktorin.common.util.net.sftp;
//
//import net.schmizz.sshj.sftp.*;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.*;
//
//public class SftpManager {
//
//    private final SftpClientPool clientPool;
//
//    // 成功操作
//    private final int SUCCESS = 0;
//    // REGULAR(32768)：普通文件。这是最常见的文件类型，如文本文件、二进制文件等。
//    private final int FILE_TYPE_FILE = 1;
//    // DIRECTORY(16384)：目录。这种类型的文件用于组织其他文件。
//    private final int FILE_TYPE_DIRECTORY = 2;
//    // BLOCK_SPECIAL(24576)：块设备文件。这种类型的文件用于描述设备，如硬盘、光驱等。
//    private final int FILE_TYPE_BLOCK_SPECIAL = 3;
//    // CHAR_SPECIAL(8192)：字符设备文件。这种类型的文件用于描述设备，如键盘、鼠标等。
//    private final int FILE_TYPE_CHAR_SPECIAL = 4;
//    // FIFO_SPECIAL(4096)：管道文件。这种类型的文件用于进程间通信。
//    private final int FILE_TYPE_FIFO_SPECIAL = 5;
//    // SOCKET_SPECIAL(49152)：套接字文件。这种类型的文件用于网络通信。
//    private final int FILE_TYPE_SOCKET_SPECIAL = 6;
//    // SYMLINK(40960)：符号链接。这种类型的文件是一个指向另一个文件的快捷方式。
//    private final int FILE_TYPE_SYMLINK = 7;
//    // UNKNOWN(0)：未知的文件类型。如果文件的类型不是上述任何一种，那么它的类型就是未知的。
//    private final int FILE_TYPE_UNKNOWN = 8;
//
//    // PERMISSION_DENIED: 权限被拒绝。
//    //FAILURE: 无法完成请求，原因未知。
//    //BAD_MESSAGE: 消息无法解析。
//    //NO_CONNECTION: 没有连接到服务器。
//    //CONNECITON_LOST: 与服务器的连接已断开。
//    //OP_UNSUPPORTED: 服务器不支持请求的操作。
//    //INVALID_HANDLE: 提供的文件句柄无效。
//    //NO_SUCH_PATH: 指定的路径不存在。
//    //FILE_ALREADY_EXISTS: 文件已存在。
//    //WRITE_PROTECT: 文件被写保护。
//    //NO_MEDIA: 设备上没有媒体。
//    //NO_SPACE_ON_FILESYSTEM: 文件系统上没有空间。
//    //QUOTA_EXCEEDED: 已超过配额。
//    //UNKNOWN_PRINCIPAL: 未知的用户或组。
//    //LOCK_CONFLICT: 锁冲突。
//    //DIR_NOT_EMPTY: 目录不为空。
//    //NOT_A_DIRECTORY: 不是一个目录。
//    //INVALID_FILENAME: 文件名无效。
//    //LINK_LOOP: 符号链接形成了循环。
//    //CANNOT_DELETE: 无法删除文件或目录。
//    //INVALID_PARAMETER: 参数无效。
//    //FILE_IS_A_DIRECTORY: 文件是一个目录。
//    //BYTE_RANGE_LOCK_CONFLICT: 字节范围锁冲突。
//    //BYTE_RANGE_LOCK_REFUSED: 字节范围锁被拒绝。
//    //DELETE_PENDING: 文件正在等待删除。
//    //FILE_CORRUPT: 文件已损坏。
//    //OWNER_INVALID: 文件所有者无效。
//    //GROUP_INVALID: 文件组无效。
//    //NO_MATCHING_BYTE_RANGE_LOCK: 没有匹配的字节范围锁。
//
//    // 文件不存在
//    private final int ERROR_CODE_FILE_NOT_EXIST = -1;
//    // 权限不足
//    private final int ERROR_CODE_PERMISSION_DENIED = -2;
//    // 未知错误
//    private final int ERROR_CODE_FAILURE = -3;
//    // 消息无法解析
//    private final int ERROR_CODE_BAD_MESSAGE = -4;
//    // 未连接到服务器
//    private final int ERROR_CODE_NO_CONNECTION = -5;
//    // 与服务器连接已断开
//    private final int ERROR_CODE_CONNECITON_LOST = -6;
//    // 服务器不支持请求的操作
//    private final int ERROR_CODE_OP_UNSUPPORTED = -7;
//    // 文件句柄无效
//    private final int ERROR_CODE_INVALID_HANDLE = -8;
//    // 指定的路径不存在
//    private final int ERROR_CODE_NO_SUCH_PATH = -9;
//    // 文件已存在
//    private final int ERROR_CODE_FILE_ALREADY_EXISTS = -10;
//    // 文件被写保护
//    private final int ERROR_CODE_WRITE_PROTECT = -11;
//    // 设备上没有媒体
//    private final int ERROR_CODE_NO_MEDIA = -12;
//    // 文件系统上没有空间
//    private final int ERROR_CODE_NO_SPACE_ON_FILESYSTEM = -13;
//    // 已超过配额
//    private final int ERROR_CODE_QUOTA_EXCEEDED = -14;
//    // 未知的用户或组
//    private final int ERROR_CODE_UNKNOWN_PRINCIPAL = -15;
//    // 锁冲突
//    private final int ERROR_CODE_LOCK_CONFLICT = -16;
//    // 目录不为空
//    private final int ERROR_CODE_DIR_NOT_EMPTY = -17;
//    // 不是一个目录
//    private final int ERROR_CODE_NOT_A_DIRECTORY = -18;
//    // 文件名无效
//    private final int ERROR_CODE_INVALID_FILENAME = -19;
//    // 符号链接形成了循环
//    private final int ERROR_CODE_LINK_LOOP = -20;
//    // 无法删除文件或目录
//    private final int ERROR_CODE_CANNOT_DELETE = -21;
//    // 参数无效
//    private final int ERROR_CODE_INVALID_PARAMETER = -22;
//    // 文件是一个目录
//    private final int ERROR_CODE_FILE_IS_A_DIRECTORY = -23;
//    // 字节范围锁冲突
//    private final int ERROR_CODE_BYTE_RANGE_LOCK_CONFLICT = -24;
//    // 字节范围锁被拒绝
//    private final int ERROR_CODE_BYTE_RANGE_LOCK_REFUSED = -25;
//    // 文件正在等待删除
//    private final int ERROR_CODE_DELETE_PENDING = -26;
//    // 文件已损坏
//    private final int ERROR_CODE_FILE_CORRUPT = -27;
//    // 文件所有者无效
//    private final int ERROR_CODE_OWNER_INVALID = -28;
//    // 文件组无效
//    private final int ERROR_CODE_GROUP_INVALID = -29;
//    // 没有匹配的字节范围锁
//    private final int ERROR_CODE_NO_MATCHING_BYTE_RANGE_LOCK = -30;
//
//    // 远程文件未完整
//    private final int ERROR_CODE_FILE_NOT_COMPLETE = -101;
//    // 不是一个文件
//    private final int ERROR_CODE_IS_NOT_A_FILE = -102;
//    // 文件下载失败，文件大小不一致
//    private final int ERROR_CODE_FILE_SIZE_NOT_EQUAL = -103;
//
//
//    /**
//     * 初始化
//     * @param host 主机地址
//     * @param port 端口
//     * @param username 用户名
//     * @param password 密码
//     * @param keyPath 密钥路径
//     * @param keyPassword 密钥密码
//     * @param maxTotal 最大可借出对象数
//     * @param maxWaitMillis 最大等待时间
//     * @param maxIdle 最大空闲对象数
//     * @param minIdle 最小空闲对象数
//     * @param minEvictableIdleTimeSeconds 最小可驱逐空闲时间，超过此时间的空闲对象将可以被系统驱逐，秒
//     */
//    public SftpManager(
//            String host,
//            int port,
//            String username,
//            String password,
//            String keyPath,
//            String keyPassword,
//            int maxTotal,
//            long maxWaitMillis,
//            int maxIdle,
//            int minIdle,
//            int minEvictableIdleTimeSeconds
//    ){
//        this.clientPool = new SftpClientPool(
//                host,
//                port,
//                username,
//                password,
//                keyPath,
//                keyPassword,
//                maxTotal,
//                maxWaitMillis,
//                maxIdle,
//                minIdle,
//                minEvictableIdleTimeSeconds);
//    }
//
//    /**
//     * 初始化
//     * @param clientPool SFTP客户端池
//     */
//    public SftpManager(SftpClientPool clientPool) {
//        this.clientPool = clientPool;
//    }
//
//
//    /**
//     * 检查指定目录是否存在任一给定的文件名
//     * @param client SFTP客户端
//     * @param remotePath 远程路径，不包含文件名
//     * @param fileNames 文件名列表
//     * @return 是否存在任一文件,是的话返回0，否则返回错误码
//     * @throws Exception 异常
//     */
//    private int scanFileExist(SFTPClient client, String remotePath, String... fileNames) throws Exception {
//        Set<String> fileNameSet = new HashSet<>();
//        try {
//            for (RemoteResourceInfo file : client.ls(remotePath)) {
//                fileNameSet.add(file.getName());
//            }
//        } catch (SFTPException e) {
//            // 如果路径不存在，client.ls(remotePath)将抛出一个异常
//            return throwSftpErrorCode(e);
//        }
//        for (String fileName : fileNames) {
//            if (fileNameSet.contains(fileName)) {
//                return SUCCESS;
//            }
//        }
//        return ERROR_CODE_FILE_NOT_EXIST;
//    }
//
//    /**
//     * 检查指定目录是否存在任一给定的文件名
//     * @param remotePath 远程路径，不包含文件名
//     * @param fileNames 文件名列表
//     * @return 是否存在任一文件
//     * @throws Exception 异常
//     */
//    public boolean scanFileExist(String remotePath, String... fileNames) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            Set<String> fileNameSet = new HashSet<>();
//            for (RemoteResourceInfo file : client.ls(remotePath)) {
//                fileNameSet.add(file.getName());
//            }
//            for (String fileName : fileNames) {
//                if (fileNameSet.contains(fileName)) {
//                    return true;
//                }
//            }
//            return false;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 扫描指定目录,并传入文件名列表比对，返回存在的文件名列表
//     * @param remotePath 远程路径，不包含文件名
//     * @param fileNames 文件名列表
//     * @return 存在的文件名列表
//     * @throws Exception 异常
//     */
//    public List<String> scanFileExist(String remotePath, List<String> fileNames) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            Set<String> fileNameSet = new HashSet<>();
//            for (RemoteResourceInfo file : client.ls(remotePath)) {
//                fileNameSet.add(file.getName());
//            }
//            List<String> result = new ArrayList<>();
//            for (String fileName : fileNames) {
//                if (fileNameSet.contains(fileName)) {
//                    result.add(fileName);
//                }
//            }
//            return result;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 扫描指定目录，返回存在的文件名列表
//     * @param client SFTP客户端
//     * @param remotePath 远程路径，不包含文件名
//     * @param fileNames 文件名列表
//     * @return 存在的文件名列表
//     * @throws Exception 异常
//     */
//    private List<String> scanFileExist(SFTPClient client, String remotePath, List<String> fileNames) throws Exception {
//        Set<String> fileNameSet = new HashSet<>();
//        for (RemoteResourceInfo file : client.ls(remotePath)) {
//            fileNameSet.add(file.getName());
//        }
//        List<String> result = new ArrayList<>();
//        for (String fileName : fileNames) {
//            if (fileNameSet.contains(fileName)) {
//                result.add(fileName);
//            }
//        }
//        return result;
//    }
//
//    public List<String> listFiles(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            List<String> result = new ArrayList<>();
//            for (RemoteResourceInfo file : client.ls(remotePath)) {
//                result.add(file.getName());
//            }
//            return result;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public List<RemoteResourceInfo> listFilesInfo(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            return client.ls(remotePath);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
////    /**
////     * 检查指定的远程目录是否存在
////     * @param client SFTP客户端
////     * @param remotePath 远程目录路径
////     * @return 目录是否存在
////     * @throws Exception 异常
////     */
////    private boolean checkDirectoryExists(SFTPClient client, String remotePath) throws Exception {
////        try {
////            client.stat(remotePath);
////            return true;
////        } catch (Exception e) {
////            return false;
////        }
////    }
//
//    /**
//     * 检查指定的远程目录是否存在
//     * @param client SFTP客户端
//     * @param remotePath 远程目录路径
//     * @return 目录是否存在
//     * @throws Exception 异常
//     */
//    private boolean checkDirectoryExists(SFTPClient client, String remotePath) throws Exception {
//        try {
//            client.stat(remotePath);
//            return true;
//        } catch (SFTPException e) {
//            // 检查是否是因为文件或目录不存在的错误
//            if (e.getStatusCode() == Response.StatusCode.NO_SUCH_FILE) {
//                return false;
//            }
//            // 如果是其他类型的SFTP异常，可能需要重新抛出异常或进行其他处理
//            throw e;
//        }
//    }
//
//    /**
//     * 级联创建远程目录
//     * @param client SFTP客户端
//     * @param remotePath 要创建的远程目录路径
//     * @throws Exception 异常
//     */
//    private void createDirectories(SFTPClient client, String remotePath) throws Exception {
//        String[] pathElements = remotePath.split("/");
//        StringBuilder path = new StringBuilder();
//        for (String element : pathElements) {
//            if (!element.isEmpty()) {
//                path.append("/").append(element);
//                try {
//                    client.stat(path.toString());
//                } catch (Exception e) {
//                    client.mkdir(path.toString());
//                }
//            }
//        }
//    }
//
//    public void createDirectories(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            createDirectories(client,remotePath);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 上传文件，不报错则上传成功,上传完成后，将临时文件重命名为最终的文件名
//     * @param localPath 本地文件路径,包含文件名
//     * @param remotePath 远程文件路径,包含文件名
//     * @param overwrite 是否覆盖
//     * @throws Exception 异常
//     */
//    public int uploadFile(String localPath, String remotePath,boolean overwrite) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        String tempRemotePath = remotePath + ".lock";
//        try {
//            // 文件存在
//            if (scanFileExist(
//                    client,
//                    remotePath.substring(0, remotePath.lastIndexOf("/")),
//                    remotePath.substring(remotePath.lastIndexOf("/") + 1))
//                    == SUCCESS
//            ) {
//                if (overwrite) {
//                    // 需要覆盖，则删除
//                    client.rm(remotePath);
//                } else {
//                    // 不需要覆盖，则返回文件已存在
//                    return ERROR_CODE_FILE_ALREADY_EXISTS;
//                }
//            } else if (!checkDirectoryExists(client, remotePath.substring(0, remotePath.lastIndexOf("/")))) {
//                // 文件不存在，检测目录是否存在
//                // 不存在则级联创建目录,创建时报错则无权限
//                createDirectories(client, remotePath.substring(0, remotePath.lastIndexOf("/")));
//            }
//            // 先上传到临时文件
//            client.put(localPath, tempRemotePath);
//            // 上传完成后，将临时文件重命名为最终的文件名
//            client.rename(tempRemotePath, remotePath);
//            return SUCCESS;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * moveFile
//     */
//    public void moveFile(String remotePath, String newRemotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            // 检测远程目录是否存在，不存在则创建
//            if (!checkDirectoryExists(client, newRemotePath.substring(0, newRemotePath.lastIndexOf("/")))) {
//                createDirectories(client, newRemotePath.substring(0, newRemotePath.lastIndexOf("/")));
//            }
//            client.rename(remotePath, newRemotePath);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 下载文件
//     * @param remotePath remotePath为远程文件路径,包含文件名
//     * @param localPath localPath为本地文件路径,包含文件名
//     * @param checkSeconds 检查文件是否完整的时间间隔，秒
//     * @return SUCCESS 0 成功，
//     *          >1 文件类型错误，可以直接报错，不重复执行要执行的任务了，
//     *          <0 错误码
//     * @throws Exception 异常
//     */
//    public int downloadFile(String remotePath, String localPath, int checkSeconds) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            // 检测文件是否存在
//            if (scanFileExist(
//                    client,
//                    remotePath.substring(0, remotePath.lastIndexOf("/")),
//                    remotePath.substring(remotePath.lastIndexOf("/") + 1))
//                    != SUCCESS
//            ) {
//                return ERROR_CODE_FILE_NOT_EXIST;
//            }
//            // 检测是否是文件
//            int fileType = checkFileType(client,remotePath);
//            if (fileType != FILE_TYPE_FILE) {
//                return fileType;
//            }
//            // 检查文件是否已经完整
//            if (!isUploadComplete(client, remotePath, checkSeconds)) {
//                return ERROR_CODE_FILE_NOT_COMPLETE;
//            }
//            // 检查文件权限是否可以允许下载
//            if (!isFileReadable(client,remotePath)) {
//                return ERROR_CODE_PERMISSION_DENIED;
//            }
//            // 获取远程文件大小
//            long remoteFileSizeFirst = client.stat(remotePath).getSize();
//            // 下载文件
//            client.get(remotePath, localPath);
//            // 再次获取远程文件大小
//            long remoteFileSizeSecond = client.stat(remotePath).getSize();
//            // 获取本地文件大小
//            Path path = Paths.get(localPath);
//            long localFileSize = Files.size(path);
//            // 比较两次远程文件大小是否一直，和本地文件大小与远程文件大小是否一致，一致则认为下载成功
//            if (remoteFileSizeFirst == remoteFileSizeSecond && remoteFileSizeSecond == localFileSize) {
//                return SUCCESS;
//            } else {
//                // 尝试删除下载的文件
//                Files.deleteIfExists(path);
//                return ERROR_CODE_FILE_SIZE_NOT_EQUAL;
//            }
//        } catch (SFTPException e) {
//            // 正常范围内的错误
//            return throwSftpErrorCode(e);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 是否下载完成
//     * @param remotePath 远程文件路径,包含文件名
//     * @param localPath 本地文件路径,包含文件名
//     * @return
//     * @throws Exception
//     */
//    public boolean isDownloadComplete(String remotePath, String localPath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            // 获取远程文件大小
//            long remoteFileSize = client.stat(remotePath).getSize();
//            // 获取本地文件大小
//            long localFileSize = Files.size(Paths.get(localPath));
//            // 比较两者的大小
//            return remoteFileSize == localFileSize;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//
//    public void deleteFile(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//
//            client.rm(remotePath);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public boolean isFile(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a regular file
//            return mode.getType() == FileMode.Type.REGULAR;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public boolean isFileReadable(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a regular file and readable
//            return mode.getType() == FileMode.Type.REGULAR && (mode.getPermissionsMask() & 0400) != 0;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    private boolean isFileReadable(SFTPClient client, String remotePath) throws Exception {
//        FileAttributes attributes = client.stat(remotePath);
//        FileMode mode = attributes.getMode();
//        // Check if it's a regular file and readable
//        return mode.getType() == FileMode.Type.REGULAR && (mode.getPermissionsMask() & 0400) != 0;
//    }
//
//    public boolean isFileWritable(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a regular file and writable
//            return mode.getType() == FileMode.Type.REGULAR && (mode.getPermissionsMask() & 0200) != 0;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//
//    public boolean isDirectory(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a directory
//            return mode.getType() == FileMode.Type.DIRECTORY;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public boolean isDirectoryReadable(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a directory and readable
//            return mode.getType() == FileMode.Type.DIRECTORY && (mode.getPermissionsMask() & 0400) != 0;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public boolean isDirectoryWritable(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode mode = attributes.getMode();
//            // Check if it's a directory and writable
//            return mode.getType() == FileMode.Type.DIRECTORY && (mode.getPermissionsMask() & 0200) != 0;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    public int checkFileType(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode.Type type = attributes.getMode().getType();
//            // Check the type
//            switch (type) {
//                case REGULAR:
//                    return FILE_TYPE_FILE;
//                case DIRECTORY:
//                    return FILE_TYPE_DIRECTORY;
//                case BLOCK_SPECIAL:
//                    return FILE_TYPE_BLOCK_SPECIAL;
//                case CHAR_SPECIAL:
//                    return FILE_TYPE_CHAR_SPECIAL;
//                case FIFO_SPECIAL:
//                    return FILE_TYPE_FIFO_SPECIAL;
//                case SOCKET_SPECIAL:
//                    return FILE_TYPE_SOCKET_SPECIAL;
//                case SYMLINK:
//                    return FILE_TYPE_SYMLINK;
//                default:
//                    return FILE_TYPE_UNKNOWN;
//            }
//        } catch (SFTPException e) {
//            return throwSftpErrorCode(e);
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    private int checkFileType(SFTPClient client, String remotePath) throws Exception {
//        try {
//            FileAttributes attributes = client.stat(remotePath);
//            FileMode.Type type = attributes.getMode().getType();
//            // Check the type
//            switch (type) {
//                case REGULAR:
//                    return FILE_TYPE_FILE;
//                case DIRECTORY:
//                    return FILE_TYPE_DIRECTORY;
//                case BLOCK_SPECIAL:
//                    return FILE_TYPE_BLOCK_SPECIAL;
//                case CHAR_SPECIAL:
//                    return FILE_TYPE_CHAR_SPECIAL;
//                case FIFO_SPECIAL:
//                    return FILE_TYPE_FIFO_SPECIAL;
//                case SOCKET_SPECIAL:
//                    return FILE_TYPE_SOCKET_SPECIAL;
//                case SYMLINK:
//                    return FILE_TYPE_SYMLINK;
//                default:
//                    return FILE_TYPE_UNKNOWN;
//            }
//        } catch (SFTPException e) {
//            return throwSftpErrorCode(e);
//        }
//    }
//
//    public FileMode getFileMode(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            return client.stat(remotePath).getMode();
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 返回远程文件大小
//     * @param remotePath
//     * @return
//     * @throws Exception
//     */
//    public long getFileSize(String remotePath) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            return (int) client.stat(remotePath).getSize();
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 检查上传是否完成
//     * (可能文件被其他人上传时，还处于上传中状态，
//     * 所以检测到文件存在之后还需要延迟检测，
//     * 如果检测到文件大小不再变化，就认为上传完成)
//     * @param remotePath 远程文件路径,包含文件名
//     * @param seconds 检测间隔时间,单位秒,超出时间文件无变化认为上传完成
//     * @return 认为是否上传完成
//     * @throws Exception 异常
//     */
//    public boolean isUploadComplete(String remotePath,int seconds) throws Exception {
//        SFTPClient client = clientPool.borrowClient();
//        try {
//            long lastSize = -1;
//            long currentSize = client.stat(remotePath).getSize();
//            while (currentSize != lastSize) {
//                lastSize = currentSize;
//                Thread.sleep(1000 * seconds); // 等待seconds秒再次检查
//                currentSize = client.stat(remotePath).getSize();
//            }
//            return true;
//        } finally {
//            clientPool.returnClient(client);
//        }
//    }
//
//    /**
//     * 检查上传是否完成
//     * (可能文件被其他人上传时，还处于上传中状态，
//     * 所以检测到文件存在之后还需要延迟检测，
//     * 如果检测到文件大小不再变化，就认为上传完成)
//     * @param remotePath 远程文件路径,包含文件名
//     * @param seconds 检测间隔时间,单位秒,超出时间文件无变化认为上传完成
//     * @return 认为是否上传完成
//     * @throws Exception 异常
//     */
//    private boolean isUploadComplete(SFTPClient client, String remotePath,int seconds) throws Exception {
//        long lastSize = -1;
//        long currentSize = client.stat(remotePath).getSize();
//        while (currentSize != lastSize) {
//            lastSize = currentSize;
//            Thread.sleep(1000 * seconds); // 等待seconds秒再次检查
//            currentSize = client.stat(remotePath).getSize();
//        }
//        return true;
//    }
//
//    /**
//     * 抛出SFTP错误码
//     * @param e SFTP异常
//     * @return 错误码
//     * @throws Exception 将未识别的错误码抛出为其他异常
//     */
//    public int throwSftpErrorCode(SFTPException e) throws Exception {
//        switch (e.getStatusCode()) {
//            case NO_SUCH_FILE:
//                return ERROR_CODE_FILE_NOT_EXIST;
//            case PERMISSION_DENIED:
//                return ERROR_CODE_PERMISSION_DENIED;
//            case FAILURE:
//                return ERROR_CODE_FAILURE;
//            case BAD_MESSAGE:
//                return ERROR_CODE_BAD_MESSAGE;
//            case NO_CONNECTION:
//                return ERROR_CODE_NO_CONNECTION;
//            case CONNECITON_LOST:
//                return ERROR_CODE_CONNECITON_LOST;
//            case OP_UNSUPPORTED:
//                return ERROR_CODE_OP_UNSUPPORTED;
//            case INVALID_HANDLE:
//                return ERROR_CODE_INVALID_HANDLE;
//            case NO_SUCH_PATH:
//                return ERROR_CODE_NO_SUCH_PATH;
//            case FILE_ALREADY_EXISTS:
//                return ERROR_CODE_FILE_ALREADY_EXISTS;
//            case WRITE_PROTECT:
//                return ERROR_CODE_WRITE_PROTECT;
//            case NO_MEDIA:
//                return ERROR_CODE_NO_MEDIA;
//            case NO_SPACE_ON_FILESYSTEM:
//                return ERROR_CODE_NO_SPACE_ON_FILESYSTEM;
//            case QUOTA_EXCEEDED:
//                return ERROR_CODE_QUOTA_EXCEEDED;
//            case UNKNOWN_PRINCIPAL:
//                return ERROR_CODE_UNKNOWN_PRINCIPAL;
//            case LOCK_CONFLICT:
//                return ERROR_CODE_LOCK_CONFLICT;
//            case DIR_NOT_EMPTY:
//                return ERROR_CODE_DIR_NOT_EMPTY;
//            case NOT_A_DIRECTORY:
//                return ERROR_CODE_NOT_A_DIRECTORY;
//            case INVALID_FILENAME:
//                return ERROR_CODE_INVALID_FILENAME;
//            case LINK_LOOP:
//                return ERROR_CODE_LINK_LOOP;
//            case CANNOT_DELETE:
//                return ERROR_CODE_CANNOT_DELETE;
//            case INVALID_PARAMETER:
//                return ERROR_CODE_INVALID_PARAMETER;
//            case FILE_IS_A_DIRECTORY:
//                return ERROR_CODE_FILE_IS_A_DIRECTORY;
//            case BYTE_RANGE_LOCK_CONFLICT:
//                return ERROR_CODE_BYTE_RANGE_LOCK_CONFLICT;
//            case BYTE_RANGE_LOCK_REFUSED:
//                return ERROR_CODE_BYTE_RANGE_LOCK_REFUSED;
//            case DELETE_PENDING:
//                return ERROR_CODE_DELETE_PENDING;
//            case FILE_CORRUPT:
//                return ERROR_CODE_FILE_CORRUPT;
//            case OWNER_INVALID:
//                return ERROR_CODE_OWNER_INVALID;
//            case GROUP_INVALID:
//                return ERROR_CODE_GROUP_INVALID;
//            case NO_MATCHING_BYTE_RANGE_LOCK:
//                return ERROR_CODE_NO_MATCHING_BYTE_RANGE_LOCK;
//            default:
//                throw e;
//        }
//    }
//
//
////    public void test() {
////        try {
////            SFTPClient client = clientPool.borrowClient();
////            log.info("Listing root directory");
////            client.ls("/").forEach(file -> log.info(file.toString()));
////            log.info("Listing done");
////            log.info("Uploading file to /root/test.txt");
////            uploadFile("E:\\WEB\\test.txt", "/root/test1222222.txt");
////            log.info("Uploading done");
////            log.info("Downloading file from /root/fff");
////            downloadFile("/root/fffffffffffffffff", "E:\\WEB\\fffa");
////            log.info("Downloading done");
////
////
////
//////            client.close();
////            clientPool.returnClient(client);
////
//////            log.info("扫描结果" + scanFileExist("/root", "test.txt", "fff"));
////            ArrayList<String> list = new ArrayList<>();
////            list.add("test.txt");
////            list.add("fff");
////            log.info("扫描结果" + scanFileExist(client, "/root",list));
////        } catch (Exception e) {
////            log.error("Error during SFTP operation", e);
////        }
////    }
//}
//
