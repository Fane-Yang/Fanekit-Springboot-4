package com.fanekit.data.db

import com.mybatisflex.core.FlexGlobalConfig
import org.apache.ibatis.session.SqlSessionFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * MyBatis Flex启动时检查配置 - 在MyBatis Flex配置完成后立即检测
 *
 * @author fanekit
 * @since 2024/1/1
 */
@Component
class MyBatisFlexStartupChecker(
    private val applicationContext: ApplicationContext
) : InitializingBean {
    private val log: Logger = LoggerFactory.getLogger(javaClass)
    @Throws(Exception::class)
    override fun afterPropertiesSet() {
        // 在Bean属性设置完成后立即执行MyBatis Flex检查
        checkMyBatisFlexConfiguration()
    }

    private fun checkMyBatisFlexConfiguration() {
        try {
            log.info("=== MyBatis Flex配置检查信息 ===")
            // 检查多数据源配置
            checkMultiDataSourceConfiguration()

            // 检查MyBatis Flex全局配置
            checkFlexGlobalConfiguration()

            log.info("=== MyBatis Flex配置检查完成 ===")
        } catch (e: Exception) {
            log.error("=== MyBatis Flex配置检查失败 ===")
            log.error("错误信息: {}", e.message)
            log.error("请检查以下配置项:")
            log.error("1. 数据库连接配置是否正确")
            log.error("2. MyBatis Flex相关依赖是否完整")
            log.error("3. 数据源配置是否正确")
            log.error("4. 实体类映射是否正确")
            log.warn("MyBatis Flex检查失败，但应用将继续启动")
        }
    }

    private fun checkMultiDataSourceConfiguration() {
        try {
            // 获取所有数据源Bean
            val dataSourceBeans = applicationContext.getBeansOfType(DataSource::class.java)

            if (dataSourceBeans.isEmpty()) {
                log.warn("⚠ 未发现任何数据源配置")
                return
            }

            log.info("✓ 发现 {} 个数据源Bean", dataSourceBeans.size)

            // 检查是否是MyBatis Flex的FlexDataSource
            val flexDataSource = dataSourceBeans["dataSource"]
            if (flexDataSource != null && flexDataSource.javaClass.getSimpleName() == "FlexDataSource") {
                log.info("  检测到MyBatis Flex多数据源配置")

                // 检查FlexDataSource中的实际数据源，不再输出外层的成功/失败统计
                // 因为内部已经有详细的统计信息了
                checkFlexDataSources(flexDataSource)
            } else {
                // 检查普通数据源时才输出外层统计
                var successCount = 0
                var failCount = 0

                for (entry in dataSourceBeans.entries) {
                    val dsName: String = entry.key!!
                    val dataSource: DataSource = entry.value!!

                    log.info("  检查数据源: {}", dsName)

                    if (checkSingleDataSource(dsName, dataSource)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }

                log.info("✓ 数据源检查完成 - 成功: {}, 失败: {}", successCount, failCount)
            }
        } catch (e: Exception) {
            log.warn("⚠ 多数据源配置检查异常: {}", e.message)
        }
    }

    private fun checkFlexDataSources(flexDataSource: DataSource): Boolean {
        try {
            // 尝试多种方式获取FlexDataSource内部的数据源
            var innerDataSources: MutableMap<String?, DataSource?>? = null

            // 方式1：尝试获取dataSources字段
            try {
                val datasourcesField = flexDataSource.javaClass.getDeclaredField("dataSources")
                datasourcesField.setAccessible(true)
                val ds = datasourcesField.get(flexDataSource) as MutableMap<String?, DataSource?>?
                innerDataSources = ds
            } catch (e: NoSuchFieldException) {
                log.info("    未找到dataSources字段，尝试其他方式")
            }

            // 方式2：尝试获取dataSourceMap字段
            if (innerDataSources == null) {
                try {
                    val dataSourceMapField = flexDataSource.javaClass.getDeclaredField("dataSourceMap")
                    dataSourceMapField.setAccessible(true)
                    val ds = dataSourceMapField.get(flexDataSource) as MutableMap<String?, DataSource?>?
                    innerDataSources = ds
                } catch (e: NoSuchFieldException) {
                    log.info("    未找到dataSourceMap字段，尝试其他方式")
                }
            }

            // 方式3：尝试通过方法获取
            if (innerDataSources == null) {
                try {
                    val getDataSourcesMethod = flexDataSource.javaClass.getDeclaredMethod("getDataSources")
                    getDataSourcesMethod.setAccessible(true)
                    val ds = getDataSourcesMethod.invoke(flexDataSource) as MutableMap<String?, DataSource?>?
                    innerDataSources = ds
                } catch (e: NoSuchMethodException) {
                    log.info("    未找到getDataSources方法")
                }
            }

            // 方式4：尝试查看所有字段和方法（调试用）
            if (innerDataSources == null) {
                log.info("    FlexDataSource类的所有字段:")
                val fields = flexDataSource.javaClass.getDeclaredFields()
                for (field in fields) {
                    log.info("      - 字段: {} (类型: {})", field.getName(), field.getType().getSimpleName())

                    // 尝试访问Map类型的字段
                    if (MutableMap::class.java.isAssignableFrom(field.getType())) {
                        try {
                            field.setAccessible(true)
                            val value = field.get(flexDataSource)
                            if (value is MutableMap<*, *>) {
                                val potentialDataSources = value as MutableMap<String?, DataSource?>
                                if (!potentialDataSources.isEmpty()) {
                                    // 检查是否包含DataSource
                                    val firstValue: Any? = potentialDataSources.values.iterator().next()
                                    if (firstValue is DataSource) {
                                        innerDataSources = potentialDataSources
                                        log.info("      ✓ 找到包含DataSource的Map字段: {}", field.getName())
                                        break
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            log.info("      访问字段 {} 失败: {}", field.getName(), e.message)
                        }
                    }
                }
            }

            if (innerDataSources != null && !innerDataSources.isEmpty()) {
                log.info("    FlexDataSource包含 {} 个内部数据源:", innerDataSources.size)

                var successCount = 0
                var failCount = 0

                for (entry in innerDataSources.entries) {
                    val dsName = entry.key
                    val dataSource: DataSource = entry.value!!

                    log.info("    检查内部数据源: {}", dsName)

                    if (checkSingleDataSource("    " + dsName, dataSource)) {
                        successCount++
                    } else {
                        failCount++
                    }
                }

                log.info("    FlexDataSource内部数据源检查完成 - 成功: {}, 失败: {}", successCount, failCount)
                return failCount == 0
            } else {
                log.warn("    FlexDataSource内部未发现数据源配置，回退到直接测试")
                return checkSingleDataSource("FlexDataSource", flexDataSource)
            }
        } catch (e: Exception) {
            log.warn("    FlexDataSource内部数据源检查失败: {}", e.message)
            // 如果反射失败，回退到直接测试FlexDataSource
            return checkSingleDataSource("FlexDataSource", flexDataSource)
        }
    }

    private fun checkSingleDataSource(dsName: String, dataSource: DataSource): Boolean {
        try {
            log.info("    ======================================")
            log.info("    - 数据源名称: {}", dsName.trim { it <= ' ' })
            log.info("    - 数据源类型: {}", dataSource.javaClass.getSimpleName())

            // 测试连接
            try {
                dataSource.getConnection().use { connection ->
                    val metaData = connection.getMetaData()
                    val dbProductName = metaData.getDatabaseProductName()
                    val dbProductVersion = metaData.getDatabaseProductVersion()
                    val driverName = metaData.getDriverName()
                    val url = metaData.getURL()

                    log.info("    - 数据库类型: {} {}", dbProductName, dbProductVersion)
                    log.info("    - 驱动程序: {}", driverName)
                    log.info("    - 连接地址: {}", maskSensitiveInfo(url))
                    connection.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT 1 as test_result").use { rs ->
                            if (rs.next()) {
                                val result = rs.getInt("test_result")
                                if (result == 1) {
                                    log.info("    ✓ 连接测试成功")
                                    log.info("\n    ======================================")
                                    return true
                                } else {
                                    log.warn("    ⚠ SQL测试异常，返回值: {}", result)
                                    log.info("\n    ======================================")
                                    return false
                                }
                            } else {
                                log.warn("    ⚠ SQL测试失败，无返回结果")
                                log.info("\n    ======================================")
                                return false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                log.error("    - 数据库连接: 无法获取 (连接失败)")
                log.error("    - 驱动程序: 无法获取 (连接失败)")
                log.error("    - 连接地址: 无法获取 (连接失败)")
                log.error("    ✗ 连接测试失败: {}", e.message)
                log.info("\n    ======================================")
                return false
            }
        } catch (e: Exception) {
            log.error("    ✗ 数据源检查异常: {}", e.message)
            log.info("\n    ======================================")
            return false
        }
    }

    private fun checkFlexGlobalConfiguration() {
        try {
            val globalConfig = FlexGlobalConfig.getDefaultConfig()
            if (globalConfig != null) {
                log.info("✓ MyBatis Flex全局配置加载成功")

                // 检查逻辑删除配置
                checkLogicDeleteConfiguration(globalConfig)
            } else {
                log.warn("⚠ MyBatis Flex全局配置未找到")
            }
        } catch (e: Exception) {
            log.warn("⚠ MyBatis Flex全局配置检查异常: {}", e.message)
        }
    }

    private fun checkLogicDeleteConfiguration(globalConfig: FlexGlobalConfig) {
        try {
            // 检查逻辑删除相关配置
            val logicDeleteColumn = globalConfig.getLogicDeleteColumn()
            val normalValue = globalConfig.getNormalValueOfLogicDelete()
            val deletedValue = globalConfig.getDeletedValueOfLogicDelete()

            log.info("  - 逻辑删除配置:")
            log.info("    * 逻辑删除字段: {}", if (logicDeleteColumn != null) logicDeleteColumn else "默认(deleted)")
            log.info("    * 正常值: {}", if (normalValue != null) normalValue else "默认(0)")
            log.info("    * 删除值: {}", if (deletedValue != null) deletedValue else "默认(1)")
        } catch (e: Exception) {
            log.info("  - 逻辑删除配置: 使用默认配置")
        }
    }


    private fun maskSensitiveInfo(url: String?): String {
        if (url == null) return "未知"
        // 简单的敏感信息脱敏，隐藏密码等信息
        return url.replace("password=[^&;]*".toRegex(), "password=***")
            .replace("pwd=[^&;]*".toRegex(), "pwd=***")
    }
}