package com.fanekit.data.db

import com.fanekit.data.db.config.PrintSQL
import com.mybatisflex.core.FlexGlobalConfig
import com.mybatisflex.core.query.QueryColumnBehavior
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer
import org.springframework.context.annotation.Configuration

/**
 * @author fanekit
 * @since 2024/5/11 14:58
 */
@Configuration
class MybatisFlexConfiguration : MyBatisFlexCustomizer {
    init {
        // 使用内置规则自动忽略 null
        QueryColumnBehavior.setIgnoreFunction(QueryColumnBehavior.IGNORE_NULL)
//        // 使用内置规则自动忽略 null 和 空字符串
//        QueryColumnBehavior.setIgnoreFunction(QueryColumnBehavior.IGNORE_EMPTY)
//        // 使用内置规则自动忽略 null 和 空白字符串
//        QueryColumnBehavior.setIgnoreFunction(QueryColumnBehavior.IGNORE_BLANK)
//        // 关闭全局null参数忽略设置
//        QueryColumnBehavior.setIgnoreFunction(QueryColumnBehavior.IGNORE_NONE)
//        QueryColumnBehavior.setIgnoreFunction(QueryColumnBehavior.IGNORE_NONE);// 关闭全局null参数忽略设置
        // 启用主从数据库读写分离操作。
//        DataSourceManager.setDataSourceShardingStrategy(DataSourceRoutingStrategy())
        // 启用日志审计功能
        PrintSQL.print()
    }

    override fun customize(flexGlobalConfig: FlexGlobalConfig?) {
    }
}
