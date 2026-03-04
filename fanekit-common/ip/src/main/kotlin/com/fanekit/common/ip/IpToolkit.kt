package com.fanekit.common.ip

import com.fanekit.common.ip.core.IpAddressUtil
import com.fanekit.common.ip.geo.GeoLite2Util
import com.fanekit.common.ip.geo.Ip2RegionUtil
import com.fanekit.common.ip.model.IpAsnInfo
import com.fanekit.common.ip.model.IpCityInfo
import com.fanekit.common.ip.model.IpCountryInfo
import com.fanekit.common.ip.model.IpFullInfo
import com.fanekit.common.ip.model.LoadMode
import com.fanekit.common.ip.security.IpPolicyEngine

/**
 * 统一入口：初始化双引擎、提供常用查询方法、内建安全策略评估
 */
object IpToolkit {

    data class InitOptions(
        val ip2regionPath: String = "ip2region.xdb",
        val ip2regionLoadToMemory: Boolean = true,
        val geoCityPath: String? = "GeoLite2-City.mmdb",
        val geoCountryPath: String? = "GeoLite2-Country.mmdb",
        val geoAsnPath: String? = "GeoLite2-ASN.mmdb",
        val geoLanguage: String = "zh-CN",
        val getLoadMode: LoadMode = LoadMode.MEMORY_MAPPED,
        val geoEnableCache: Boolean = true,
        val geoCacheSize: Int = 50_000,

        // 策略（可选）
        val policyEngine: IpPolicyEngine? = null
    )

    @Volatile private var policyEngine: IpPolicyEngine? = null
    @Volatile private var initialized = false

    @Synchronized
    fun init(opts: InitOptions) {
        Ip2RegionUtil.init(opts.ip2regionPath, opts.ip2regionLoadToMemory)
        GeoLite2Util.init(
            GeoLite2Util.Options(
                cityMmdbPath = opts.geoCityPath,
                countryMmdbPath = opts.geoCountryPath,
                asnMmdbPath = opts.geoAsnPath,
                language = opts.geoLanguage,
                loadMode = opts.getLoadMode,
                enableLruCache = opts.geoEnableCache,
                cacheSize = opts.geoCacheSize
            )
        )
        policyEngine = opts.policyEngine
        initialized = true
    }

    private fun ensureInit() = check(initialized) { "IpToolkit 未初始化" }

    // ---------------- 基础操作 ----------------
    /**
     * 判断 IP 地址是否合法
     * @param ip IP 地址字符串
     * @return 是否有效的 IP 地址
     */
    fun isValidIp(ip: String) = IpAddressUtil.isValidIp(ip)

    /**
     * 判断 IP 地址是否为私有地址
     * @param ip IP 地址字符串
     * @return 是否为私有地址
     */
    fun isPrivate(ip: String) = IpAddressUtil.isPrivate(ip)
    /**
     * 判断 IP 地址是否为回环地址
     * @param ip IP 地址字符串
     * @return 是否为回环地址
     */
    fun isLoopback(ip: String) = IpAddressUtil.isLoopback(ip)
    /**
     * 判断 IP 地址是否为本地地址
     * @param ip IP 地址字符串
     * @return 是否为本地地址
     */
    fun ipInCidr(ip: String, cidr: String) = IpAddressUtil.ipInCidr(ip, cidr)
    /**
     * 判断 IP 地址是否在任意 CIDR 范围内
     * @param ip IP 地址字符串
     * @param cidrs CIDR 列表
     * @return 是否在任意 CIDR 范围内
     */
    fun ipInAnyCidr(ip: String, cidrs: Collection<String>) = IpAddressUtil.ipInAnyCidr(ip, cidrs)
    /**
     * 判断 IP 地址是否在指定范围内
     * @param ip IP 地址字符串
     * @param startIp 起始 IP 地址字符串
     * @param endIp 结束 IP 地址字符串
     * @return 是否在指定范围内
     */
    fun ipInRange(ip: String, startIp: String, endIp: String) = IpAddressUtil.ipInRange(ip, startIp, endIp)
    /**
     * 解析客户端 IP 地址
     * @param headers 请求头映射
     * @param remoteAddr 远程地址（可选）
     * @param trusted 可信任的代理列表（可选）
     * @return 解析后的客户端 IP 地址
     */
    fun resolveClientIp(headers: Map<String, String>, remoteAddr: String?, trusted: List<String> = emptyList()) =
        IpAddressUtil.resolveClientIp(headers, remoteAddr, trusted)

    // ---------------- 快速筛选（先国家） ----------------
    /**
     * 快速判断是否来自目标国家（先 ip2region 名称匹配；不确定时回退 GeoLite2）
     * 支持参数：CN/US（ISO）或中文/英文名（"中国"/"United States"）
     */
    fun isFromCountry(ip: String, isoOrName: String): Boolean {
        ensureInit()
        val n = Ip2RegionUtil.country(ip).countryName
        if (!n.isNullOrBlank() && (n.contains(isoOrName, true))) return true
        return GeoLite2Util.isFromCountry(ip, isoOrName)
    }

    // ---------------- 获取信息（不同粒度） ----------------
    /**
     * 获取 IP 地址的国家信息（先 GeoLite2，拿不到再回 ip2region）
     * 返回：国家名称、ISO 代码
     */
    fun country(ip: String): IpCountryInfo {
        ensureInit()
        // 先用 GeoLite2（含 ISO），拿不到再回退 ip2region
        val c = GeoLite2Util.country(ip)
        return if (c.countryName != null || c.countryIsoCode != null) c else Ip2RegionUtil.country(ip)
    }

    /**
     * 获取 IP 地址的城市信息（先 GeoLite2，拿不到再回 ip2region）
     * 返回：城市名称、子区域名称、国家名称、经纬度、时区等
     * 注意：GeoLite2 可能返回 null
     *       ip2region 只返回城市名称和国家名称
     */
    fun city(ip: String): IpCityInfo {
        ensureInit()
        val c = GeoLite2Util.city(ip)
        return if (c.cityName != null || c.subdivisionName != null || c.countryName != null) c
        else Ip2RegionUtil.city(ip) // 回退
    }

    /**
     * 获取 IP 地址的 ASN 信息（先 GeoLite2，拿不到再回 ip2region）
     * 返回：ASN 号、组织名称
     * 注意：GeoLite2 可能返回 null
     *       ip2region 只返回 ASN 号和组织名称
     */
    fun asn(ip: String): IpAsnInfo {
        ensureInit()
        return GeoLite2Util.asn(ip)
    }

    /**
     * FULL：优先 GeoLite2（含经纬度/时区/ASN），并合并 ip2region 的 ISP 字段
     */
    fun full(ip: String): IpFullInfo {
        ensureInit()
        val g = GeoLite2Util.full(ip)
        val isp = Ip2RegionUtil.full(ip).isp
        return g.copy(isp = isp, source = g.source)
    }

    // ---------------- 安全策略评估 ----------------
    /**
     * 安全策略评估结果
     * @param allow 是否允许访问
     * @param phase 评估阶段（如 "IP_CHECK"）
     * @param matchedRule 匹配的规则描述（可选）
     */
    data class PolicyResult(val allow: Boolean, val phase: String, val matchedRule: String?)

    /**
     * 评估 IP 地址的访问策略
     * @param ip IP 地址字符串
     * @return 策略评估结果
     */
    fun evaluatePolicy(ip: String): PolicyResult {
        ensureInit()
        val engine = policyEngine ?: return PolicyResult(true, "NO_ENGINE", null)
        val d = engine.evaluate(ip)
        return PolicyResult(d.allow, d.phase, d.matched?.description)
    }
}