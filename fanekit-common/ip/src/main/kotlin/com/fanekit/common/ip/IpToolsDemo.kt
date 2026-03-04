package com.fanekit.common.ip

import com.fanekit.common.ip.geo.GeoLite2Util
import com.fanekit.common.ip.geo.Ip2RegionUtil
import com.fanekit.common.ip.model.LoadMode

/**
 * 示例：快速大范围筛选
 * 场景：仅处理来自 CN 的 IP，非 CN 直接跳过；符合则用 GeoLite2 做精细定位
 */
object IpToolsDemo {

    @JvmStatic
    fun main(args: Array<String>) {
        // 初始化（根据你的部署路径调整）
        Ip2RegionUtil.init(xdbPath = "E:\\Projects\\BackEnd\\ktor-backend-framework\\fanekit-components\\ip\\src\\main\\resources\\db\\ip2region.xdb", loadToMemory = true)

        GeoLite2Util.init(
            GeoLite2Util.Options(
                cityMmdbPath = "E:\\Projects\\BackEnd\\ktor-backend-framework\\fanekit-components\\ip\\src\\main\\resources\\db\\GeoLite2-City.mmdb",
                countryMmdbPath = "E:\\Projects\\BackEnd\\ktor-backend-framework\\fanekit-components\\ip\\src\\main\\resources\\db\\GeoLite2-Country.mmdb",
                asnMmdbPath = "E:\\Projects\\BackEnd\\ktor-backend-framework\\fanekit-components\\ip\\src\\main\\resources\\db\\GeoLite2-ASN.mmdb",
                language = "zh-CN",
                loadMode = LoadMode.MEMORY_MAPPED, // 或 MEMORY
                enableLruCache = true,
                cacheSize = 50_000
            )
        )

        val ips = listOf(
            "8.8.8.8",      // Google
            "1.1.1.1",      // Cloudflare
            "114.114.114.114", // 114DNS
            "223.5.5.5",     // 阿里DNS
            "14.145.42.23", // 广东广州电信
            "2408:8610:3b10:1100:40::c"
        )

        for (ip in ips) {
            // 1) 先用 ip2region 快速判国家（中文名），非目标直接跳过
            val isCN = Ip2RegionUtil.isFromCountryName(ip, "中国")
            if (!isCN) {
                println("Skip (not CN): $ip, quick=${Ip2RegionUtil.country(ip).countryName}")
                continue
            }

            // 2) 目标命中后，使用 GeoLite2 精细查询
            val full = GeoLite2Util.full(ip)
            println("Hit (CN): $ip -> $full")

            // 也可分别取粒度
            val country = GeoLite2Util.country(ip)
            val city = GeoLite2Util.city(ip)      // 若无 City DB，返回空结构
            val asn = GeoLite2Util.asn(ip)        // 若无 ASN DB，返回空结构

            println("  country=${country}")
            println("  city=${city}")
            println("  asn=${asn}")
        }
        println(GeoLite2Util.full("2408:8610:3b10:1100:40::c"))
    }
}