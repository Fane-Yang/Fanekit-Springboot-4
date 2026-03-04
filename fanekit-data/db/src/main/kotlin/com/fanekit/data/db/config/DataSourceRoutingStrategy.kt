package com.fanekit.data.db.config

import com.mybatisflex.core.datasource.DataSourceShardingStrategy
import java.lang.reflect.Method

/**
 * 数据源路由策略
 */
class DataSourceRoutingStrategy : DataSourceShardingStrategy {
    private val update = arrayOf<String?>("insert", "delete", "update")

    override fun doSharding(
        currentDataSourceKey: String,
        mapper: Any?, mapperMethod: Method, methodArgs: Array<Any?>?
    ): String {
        // 不管 other 数据源的情况

        if ("other" == currentDataSourceKey) {
            return currentDataSourceKey
        }

        // 判断方法名是否以增删改关键字开头
        if (update.any { prefix -> mapperMethod.name.startsWith(prefix ?: "") }) {
            return "master"
        }

        //其他场景，使用 slave1 或者 slave2 进行负载均衡
        return "slave"
    }
}