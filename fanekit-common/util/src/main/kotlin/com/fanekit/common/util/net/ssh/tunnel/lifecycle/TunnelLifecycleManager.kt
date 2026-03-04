//package xyz.ktorin.common.util.ssh.tunnel.lifecycle
//
//import xyz.ktorin.common.util.ssh.tunnel.SSHTunnelManager
//import java.util.concurrent.atomic.AtomicBoolean
//
///**
// * SSH隧道生命周期管理示例
// * 演示隧道如何跟随Java程序的生命周期
// */
//class TunnelLifecycleManager {
//
//    private var tunnelManager: SSHTunnelManager? = null
//    private val isRunning = AtomicBoolean(false)
//
//    init {
//        // 注册JVM关闭钩子，确保程序退出时自动清理隧道
//        Runtime.getRuntime().addShutdownHook(Thread {
//            println("检测到程序退出，正在清理SSH隧道...")
//            shutdown()
//            println("SSH隧道已清理完成")
//        })
//    }
//
//    /**
//     * 启动SSH隧道服务
//     */
//    fun start() {
//        if (isRunning.get()) {
//            println("隧道服务已经在运行中")
//            return
//        }
//
//        try {
//            tunnelManager = SSHTunnelManager(
//                host = "your-ssh-server.com",
//                username = "user",
//                password = "password"
//            ).apply {
//                connect()
//
//                // 创建数据库隧道
//                val dbForwarder = createLocalPortForward(3306, "db.internal.com", 3306)
//                dbForwarder.start()
//
//                // 创建Redis隧道
//                val redisForwarder = createLocalPortForward(6379, "redis.internal.com", 6379)
//                redisForwarder.start()
//
//                println("SSH隧道已启动")
//                println("- 数据库代理: localhost:3306 -> db.internal.com:3306")
//                println("- Redis代理: localhost:6379 -> redis.internal.com:6379")
//            }
//
//            isRunning.set(true)
//
//        } catch (e: Exception) {
//            println("启动SSH隧道失败: ${e.message}")
//            e.printStackTrace()
//            shutdown()
//        }
//    }
//
//    /**
//     * 关闭SSH隧道服务
//     */
//    fun shutdown() {
//        if (!isRunning.get()) {
//            return
//        }
//
//        try {
//            tunnelManager?.let { manager ->
//                // 关闭所有隧道
//                manager.closeAllTunnels()
//
//                // 关闭SSH连接
//                manager.close()
//
//                println("所有SSH隧道已关闭")
//            }
//        } catch (e: Exception) {
//            println("关闭SSH隧道时出错: ${e.message}")
//        } finally {
//            tunnelManager = null
//            isRunning.set(false)
//        }
//    }
//
//    /**
//     * 检查隧道状态
//     */
//    fun isActive(): Boolean {
//        return isRunning.get() && tunnelManager?.isConnected() == true
//    }
//
//    /**
//     * 获取隧道状态信息
//     */
//    fun getStatus(): String {
//        return if (isActive()) {
//            val activeTunnels = tunnelManager?.getActiveTunnels() ?: emptyList()
//            "运行中 - 活跃隧道数: ${activeTunnels.size}"
//        } else {
//            "已停止"
//        }
//    }
//}
//
///**
// * Spring Boot应用中的隧道管理示例
// */
////@Component
//class SpringBootTunnelService {
//
//    private val lifecycleManager = TunnelLifecycleManager()
//
//    //@PostConstruct
//    fun init() {
//        println("Spring Boot应用启动，初始化SSH隧道...")
//        lifecycleManager.start()
//    }
//
//    //@PreDestroy
//    fun destroy() {
//        println("Spring Boot应用关闭，清理SSH隧道...")
//        lifecycleManager.shutdown()
//    }
//
//    //@EventListener(ApplicationReadyEvent::class)
//    fun onApplicationReady() {
//        println("应用就绪，隧道状态: ${lifecycleManager.getStatus()}")
//    }
//}
//
///**
// * 主程序示例 - 演示隧道生命周期
// */
//object TunnelLifecycleDemo {
//
//    @JvmStatic
//    fun main(args: Array<String>) {
//        val lifecycleManager = TunnelLifecycleManager()
//
//        try {
//            // 启动隧道
//            lifecycleManager.start()
//
//            // 模拟应用运行
//            simulateApplicationWork(lifecycleManager)
//
//        } finally {
//            // 手动关闭（通常不需要，JVM关闭钩子会自动处理）
//            lifecycleManager.shutdown()
//        }
//    }
//
//    private fun simulateApplicationWork(lifecycleManager: TunnelLifecycleManager) {
//        println("\n=== 应用开始工作 ===")
//
//        repeat(10) { i ->
//            if (!lifecycleManager.isActive()) {
//                println("隧道连接丢失，退出应用")
//                break
//            }
//
//            println("工作循环 ${i + 1}/10 - 隧道状态: ${lifecycleManager.getStatus()}")
//
//            // 模拟数据库操作
//            simulateDatabaseOperation()
//
//            // 模拟Redis操作
//            simulateRedisOperation()
//
//            Thread.sleep(2000)
//        }
//
//        println("=== 应用工作完成 ===\n")
//    }
//
//    private fun simulateDatabaseOperation() {
//        try {
//            // 这里会通过localhost:3306连接，实际上通过SSH隧道连接到远程数据库
//            println("  -> 模拟数据库查询 (通过隧道 localhost:3306)")
//            // val connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/db", "user", "pass")
//            // ... 数据库操作
//        } catch (e: Exception) {
//            println("  -> 数据库操作失败: ${e.message}")
//        }
//    }
//
//    private fun simulateRedisOperation() {
//        try {
//            // 这里会通过localhost:6379连接，实际上通过SSH隧道连接到远程Redis
//            println("  -> 模拟Redis操作 (通过隧道 localhost:6379)")
//            // val jedis = Jedis("localhost", 6379)
//            // ... Redis操作
//        } catch (e: Exception) {
//            println("  -> Redis操作失败: ${e.message}")
//        }
//    }
//}
//
///**
// * 自动重连示例
// */
//class AutoReconnectTunnelManager {
//
//    private var tunnelManager: SSHTunnelManager? = null
//    private val isRunning = AtomicBoolean(false)
//    private var reconnectThread: Thread? = null
//
//    fun start() {
//        isRunning.set(true)
//        startReconnectMonitor()
//    }
//
//    private fun startReconnectMonitor() {
//        reconnectThread = Thread {
//            while (isRunning.get()) {
//                try {
//                    if (tunnelManager?.isConnected() != true) {
//                        println("检测到连接断开，尝试重连...")
//                        reconnect()
//                    }
//                    Thread.sleep(5000) // 每5秒检查一次
//                } catch (e: InterruptedException) {
//                    break
//                } catch (e: Exception) {
//                    println("重连监控异常: ${e.message}")
//                }
//            }
//        }.apply {
//            isDaemon = true
//            name = "TunnelReconnectMonitor"
//            start()
//        }
//    }
//
//    private fun reconnect() {
//        try {
//            tunnelManager?.close()
//
//            tunnelManager = SSHTunnelManager(
//                host = "your-ssh-server.com",
//                username = "user",
//                password = "password"
//            ).apply {
//                connect()
//                // 重新创建隧道...
//            }
//
//            println("SSH隧道重连成功")
//        } catch (e: Exception) {
//            println("重连失败: ${e.message}")
//        }
//    }
//
//    fun stop() {
//        isRunning.set(false)
//        reconnectThread?.interrupt()
//        tunnelManager?.close()
//    }
//}
