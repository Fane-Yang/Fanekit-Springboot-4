//package xyz.ktorin.common.util.net.sftp;
//
//import net.schmizz.sshj.SSHClient;
//import net.schmizz.sshj.sftp.SFTPClient;
//import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
//import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
//import org.apache.commons.pool2.BasePooledObjectFactory;
//import org.apache.commons.pool2.PooledObject;
//import org.apache.commons.pool2.impl.DefaultPooledObject;
//import org.apache.commons.pool2.impl.GenericObjectPool;
//import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
//
//import java.io.IOException;
//import java.nio.file.Paths;
//
//public class SftpClientPool {
//    private final GenericObjectPool<SFTPClient> pool;
//
//    /**
//     * 构造函数
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
//    public SftpClientPool(
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
//    ) {
//        GenericObjectPoolConfig<SFTPClient> config = new GenericObjectPoolConfig<>();
//        config.setMaxTotal(maxTotal);
//        config.setBlockWhenExhausted(true);
//        config.setMaxWaitMillis(maxWaitMillis);
//        config.setMaxIdle(maxIdle);
//        config.setMinIdle(minIdle);
//        config.setMinEvictableIdleTimeMillis(1000L * minEvictableIdleTimeSeconds);
//        // 以下配置待添加
//        // 设置空闲对象检测的频率，例如每分钟检查一次
//        config.setTimeBetweenEvictionRunsMillis(60000);
//        // 设置在进行空闲对象检测时，检查的对象数量的上限，-1表示检测全部对象
//        config.setNumTestsPerEvictionRun(-1);
//        // 设置在借出前是否进行验证
//        config.setTestOnBorrow(true);
//        // 设置在归还到池中时是否进行验证
//        config.setTestOnReturn(true);
//        // 设置在空闲时是否进行验证
//        config.setTestWhileIdle(true);
//        this.pool = new GenericObjectPool<>(new SFTPClientFactory(host, port, username, password, keyPath, keyPassword), config);
//    }
//
//    private static class SFTPClientFactory extends BasePooledObjectFactory<SFTPClient> {
//        private final String host;
//        private final int port;
//        private final String username;
//        private final String password;
//        private final String keyPath;
//        private final String keyPassword;
//
//        SFTPClientFactory(String host, int port, String username, String password, String keyPath, String keyPassword) {
//            this.host = host;
//            this.port = port;
//            this.username = username;
//            this.password = password;
//            this.keyPath = keyPath;
//            this.keyPassword = keyPassword;
//        }
//
//        @Override
//        public SFTPClient create() throws IOException {
//            SSHClient sshClient = new SSHClient();
//            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
//            sshClient.connect(host, port);
//            if (keyPath != null && !keyPath.isEmpty()) {
//                // 如果密钥有密码保护,需要提供密码,否则不需要
//                KeyProvider keyProvider;
//                String location = Paths.get(keyPath).toString();
//                if (keyPassword != null && !keyPassword.isEmpty()) {
//                    keyProvider = sshClient.loadKeys(location,keyPassword);
//                } else {
//                    keyProvider = sshClient.loadKeys(location);
//                }
//                sshClient.authPublickey(username, keyProvider);
//            } else {
//                sshClient.authPassword(username, password);
//            }
//            // 以下配置待完善
////            sshClient.setConnectTimeout(30000);
////            sshClient.setTimeout(30000);
//            return sshClient.newSFTPClient();
//        }
//
//        /**
//         * 激活对象，当对象从池中借出时会自动调用，也可以在此处进行一些初始化操作
//         */
//        @Override
//        public void activateObject(PooledObject<SFTPClient> p) throws Exception {
//            SFTPClient sftpClient = p.getObject();
//            if (sftpClient == null || !sftpClient.getSFTPEngine().getSubsystem().isOpen()) {
//                // 如果已关闭，抛出异常，让系统尝试删除此对象
//                throw new IllegalStateException("SFTP Client is not connected or the subsystem is closed.");
//            }
//        }
//
//        /**
//         * 验证对象是否有效，在对象借出前会自动调用，返回false则会销毁此对象
//         */
//        @Override
//        public boolean validateObject(PooledObject<SFTPClient> p) {
//            SFTPClient sftpClient = p.getObject();
//            return sftpClient != null && sftpClient.getSFTPEngine().getSubsystem().isOpen();
//        }
//
//        @Override
//        public PooledObject<SFTPClient> wrap(SFTPClient client) {
//            return new DefaultPooledObject<>(client);
//        }
//
//        @Override
//        public void destroyObject(PooledObject<SFTPClient> p) throws Exception {
//            SFTPClient client = p.getObject();
//            if (client != null) {
//                client.close();
//            }
//        }
//
//    }
//
//    public SFTPClient borrowClient() throws Exception {
//        return pool.borrowObject();
//    }
//
//    public void returnClient(SFTPClient client) {
//        if (client != null) {
//            pool.returnObject(client);
//        }
//    }
//}
//
