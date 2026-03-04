package com.fanekit.common.util.system

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.MemoryUsage
import java.lang.management.ThreadMXBean
import java.net.NetworkInterface

/**
 * 系统工具类
 * 提供获取本机MAC地址的功能。
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2024/08/18
 * @since 1.0.0
 * @version 1.0.0
 */
class SystemUtils {
    companion object {
        val log: Logger = LoggerFactory.getLogger(SystemUtils::class.java)
        /**
         * 获取本机全部MAC地址
         */
        @Throws(Exception::class)
        fun getMacAddresses(): List<String> {
            val macAddresses: MutableList<String> = ArrayList()
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()

            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                val mac = networkInterface.hardwareAddress

                if (mac != null) {
                    val macAddress = StringBuilder()
                    for (i in mac.indices) {
                        macAddress.append(String.format("%02X", mac[i]))
                        if (i < mac.size - 1) {
                            // 是否自动添加-
                            macAddress.append("-")
                        }
                    }
                    macAddresses.add(macAddress.toString())
                }
            }

            return macAddresses
        }

        /**
         * 监测本机系统状态
         */
        fun printSystemStatus() {
            log.info("【系统状态】")
            // CPU 核心数
            val osBean = ManagementFactory.getOperatingSystemMXBean()
            log.info("系统 CPU 核心数: ${osBean.availableProcessors}")

            // 系统负载
            val systemLoad = osBean.systemLoadAverage
            if (systemLoad >= 0) {
                log.info("系统平均负载: $systemLoad")
            } else {
                log.info("系统平均负载: 不支持")
            }

            // 磁盘空间
            val file = File("/")
            val totalDisk = file.totalSpace / (1024 * 1024 * 1024)
            val freeDisk = file.freeSpace / (1024 * 1024 * 1024)
            log.info("磁盘空间: 总计 ${totalDisk}GB，剩余 ${freeDisk}GB")
        }

        /**
         * 监测JVM程序状态
         */
        fun printJvmStatus() {
            log.info("【程序状态】")

            // 获取 JVM 内存
            val memoryMXBean = ManagementFactory.getMemoryMXBean()
            val heapMemoryUsage: MemoryUsage = memoryMXBean.heapMemoryUsage
            val nonHeapMemoryUsage: MemoryUsage = memoryMXBean.nonHeapMemoryUsage
            log.info("JVM 堆内存: 已使用 ${heapMemoryUsage.used / (1024 * 1024)}MB / 最大 ${heapMemoryUsage.max / (1024 * 1024)}MB")
            log.info("JVM 非堆内存: 已使用 ${nonHeapMemoryUsage.used / (1024 * 1024)}MB")

            // 当前线程数量
            val threadMXBean: ThreadMXBean = ManagementFactory.getThreadMXBean()
            val threadCount = threadMXBean.threadCount
            log.info("当前线程数量: $threadCount")

            // 垃圾回收器状态
            val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
            for (gc in gcBeans) {
                log.info("垃圾回收器 ${gc.name}: 总回收次数 ${gc.collectionCount}, 总回收时间 ${gc.collectionTime}ms")
            }
        }
    }

}