package com.fanekit.common.ip.security

import com.fanekit.common.ip.core.IpAddressUtil
import com.fanekit.common.ip.geo.GeoLite2Util
import com.fanekit.common.ip.geo.Ip2RegionUtil
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 规则接口：命中返回 true（表示“规则生效”）。Engine 根据 allow/block 语义解释。
 */
interface IpRule {
    fun hit(ip: String): Boolean
    val description: String
}
/** CIDR 规则 */
class CidrRule(private val cidrs: List<String>) : IpRule {
    override val description = "CIDR in ${cidrs.joinToString()}"
    override fun hit(ip: String): Boolean = IpAddressUtil.ipInAnyCidr(ip, cidrs)
}
/** 国家匹配（iso2 或名称包含） */
class CountryRule(private val targets: Set<String>, private val useGeoLiteIso: Boolean = true) : IpRule {
    override val description = "Country in ${targets.joinToString()}"
    override fun hit(ip: String): Boolean {
        // 先用 GeoLite2（支持 ISO），不可用再用 ip2region 名称匹配
        return if (useGeoLiteIso) {
            val c = GeoLite2Util.country(ip)
            val iso = c.countryIsoCode?.uppercase()
            val name = c.countryName
            when {
                iso != null && targets.any { it.equals(iso, true) } -> true
                name != null && targets.any { name.contains(it, true) } -> true
                else -> Ip2RegionUtil.country(ip).countryName?.let { n ->
                    targets.any { n.contains(it, true) }
                } ?: false
            }
        } else {
            val n = Ip2RegionUtil.country(ip).countryName ?: return false
            targets.any { n.contains(it, true) }
        }
    }
}
/** ASN 规则（需要 GeoLite2 ASN 库） */
class AsnRule(private val asnList: Set<Int>) : IpRule {
    override val description = "ASN in ${asnList.joinToString()}"
    override fun hit(ip: String): Boolean = GeoLite2Util.asn(ip).asn?.let { it in asnList } == true
}
/** 白/黑名单（精确 IP 或 CIDR 支持） */
class AnyMatchRule(private val ipsOrCidrs: List<String>) : IpRule {
    override val description = "AnyMatch ${ipsOrCidrs.joinToString()}"
    override fun hit(ip: String): Boolean =
        ipsOrCidrs.any { item ->
            when {
                item.contains("/") -> runCatching { IpAddressUtil.ipInCidr(ip, item) }.getOrDefault(false)
                else -> item.equals(ip, true)
            }
        }
}
/** 简单令牌桶限流（每个 IP 独立） */
class RateLimitRule(
    private val capacity: Long,
    private val refillTokens: Long,
    private val refillEvery: Duration
) : IpRule {
    override val description = "RateLimit capacity=$capacity refill=$refillTokens/$refillEvery"
    private data class Bucket(var tokens: AtomicLong, var lastRefillMs: AtomicLong)
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun hit(ip: String): Boolean {
        val now = System.currentTimeMillis()
        val b = buckets.computeIfAbsent(ip) { Bucket(AtomicLong(capacity), AtomicLong(now)) }
        refill(b, now)
        val current = b.tokens.get()
        return if (current > 0) {
            b.tokens.decrementAndGet()
            true // 命中表示“允许通过”
        } else {
            false // 无令牌：命中=否（由 Engine 解释为 block）
        }
    }

    private fun refill(b: Bucket, now: Long) {
        val last = b.lastRefillMs.get()
        val periods = (now - last) / refillEvery.toMillis()
        if (periods > 0) {
            val add = periods * refillTokens
            val newTs = last + periods * refillEvery.toMillis()
            b.tokens.updateAndGet { cur -> (cur + add).coerceAtMost(capacity) }
            b.lastRefillMs.compareAndSet(last, newTs)
        }
    }
}
/** 引擎：先执行 BLOCK 规则列表；若命中 -> 拒绝；再执行 ALLOW 规则；若命中 -> 允许；否则按默认值 */
class IpPolicyEngine(
    private val blockRules: List<IpRule> = emptyList(),
    private val allowRules: List<IpRule> = emptyList(),
    private val defaultAllow: Boolean = true
) {
    data class Decision(val allow: Boolean, val matched: IpRule? = null, val phase: String = "DEFAULT")

    fun evaluate(ip: String): Decision {
        blockRules.forEach { r -> if (r.hit(ip)) return Decision(false, r, "BLOCK") }
        allowRules.forEach { r -> if (r.hit(ip)) return Decision(true, r, "ALLOW") }
        return Decision(defaultAllow, null, "DEFAULT")
    }
}