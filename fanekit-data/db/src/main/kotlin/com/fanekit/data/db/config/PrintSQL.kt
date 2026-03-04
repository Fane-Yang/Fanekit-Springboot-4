package com.fanekit.data.db.config

import com.mybatisflex.core.audit.AuditManager
import com.mybatisflex.core.audit.AuditMessage
import com.mybatisflex.core.audit.MessageCollector
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PrintSQL {
    fun print() {
        val log: Logger = LoggerFactory.getLogger(PrintSQL::class.java)
        // 开启审计功能
        AuditManager.setAuditEnable(true)
        // 自定义sql打印 或 自定义审计功能，也可结合logback将sql日志输出到独立文件中。
        // 详见https://mybatis-flex.com/zh/core/audit.html
        // 设置 SQL 审计收集器
        AuditManager.setMessageCollector(MessageCollector { auditMessage: AuditMessage? ->
            log.info(
                "{} ---- {}ms, row:{},dsName:{}",
                formatSQL(auditMessage!!.getFullSql()),
                auditMessage.elapsedTime,
                auditMessage.queryCount,
                auditMessage.dsName
            )
        }
        )
    }

    fun formatSQL(sql: String): String {
        return sql.replace("\\s+".toRegex(), " ").replace("\\r", " ").replace("\\n", " ")
    }
}
