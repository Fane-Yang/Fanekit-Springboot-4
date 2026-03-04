package com.fanekit.common.ip.core

import java.math.BigInteger
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * IPv4/IPv6 校验、私网/保留网、CIDR/范围判断、客户端真实 IP 解析等
 */
object IpAddressUtil {

    fun isValidIp(ip: String): Boolean = runCatching { InetAddress.getByName(ip); true }.getOrDefault(false)
    fun isIpv4(ip: String): Boolean = runCatching { InetAddress.getByName(ip) is Inet4Address }.getOrDefault(false)
    fun isIpv6(ip: String): Boolean = runCatching { InetAddress.getByName(ip) is Inet6Address }.getOrDefault(false)

    fun toBigInt(ip: String): BigInteger {
        val bytes = InetAddress.getByName(ip).address
        return BigInteger(1, bytes)
    }

    fun cidrToRange(cidr: String): Pair<BigInteger, BigInteger> {
        val parts = cidr.split("/")
        val base = toBigInt(parts[0])
        val prefix = parts.getOrNull(1)?.toInt() ?: throw IllegalArgumentException("Invalid CIDR: $cidr")
        val totalBits = if (isIpv4(parts[0])) 32 else 128
        val mask = BigInteger.ONE.shiftLeft(totalBits - prefix).subtract(BigInteger.ONE).not().and(BigInteger.ONE.shiftLeft(totalBits).subtract(BigInteger.ONE))
        val start = base.and(mask)
        val end = start.add(mask.not().and(BigInteger.ONE.shiftLeft(totalBits).subtract(BigInteger.ONE)))
        return start to end
    }

    fun ipInCidr(ip: String, cidr: String): Boolean {
        val (start, end) = cidrToRange(cidr)
        val x = toBigInt(ip)
        return x >= start && x <= end
    }

    fun ipInAnyCidr(ip: String, cidrs: Collection<String>): Boolean = cidrs.any { runCatching { ipInCidr(ip, it) }.getOrDefault(false) }

    fun ipInRange(ip: String, startIp: String, endIp: String): Boolean {
        val x = toBigInt(ip); val s = toBigInt(startIp); val e = toBigInt(endIp)
        return x >= s && x <= e
    }

    /** 私网/链路本地/环回/文档地址/多播等（IPv4/IPv6） */
    fun isPrivate(ip: String): Boolean {
        return when {
            isIpv4(ip) -> isPrivateIpv4(ip)
            isIpv6(ip) -> isPrivateIpv6(ip)
            else -> false
        }
    }

    fun isLoopback(ip: String): Boolean = try { InetAddress.getByName(ip).isLoopbackAddress } catch (_: Exception) { false }

    private fun isPrivateIpv4(ip: String): Boolean = listOf(
        "10.0.0.0/8",
        "172.16.0.0/12",
        "192.168.0.0/16",
        "100.64.0.0/10",   // CGNAT
        "127.0.0.0/8",     // loopback
        "169.254.0.0/16"   // link-local
    ).any { ipInCidr(ip, it) }

    private fun isPrivateIpv6(ip: String): Boolean = listOf(
        "fc00::/7",      // Unique local
        "fe80::/10",     // Link-local
        "::1/128",       // Loopback
        "ff00::/8"       // Multicast
    ).any { ipInCidr(ip, it) }

    /**
     * 从代理头部提取真实客户端 IP：
     * - 优先 X-Forwarded-For（第一个非私网/非保留的地址）
     * - 其次 X-Real-IP
     * - 最后 remoteAddr
     * @param trustedProxies 可选：可信代理的 CIDR 列表（遇到它们追加出站）
     */
    fun resolveClientIp(
        headers: Map<String, String>,
        remoteAddr: String?,
        trustedProxies: List<String> = emptyList()
    ): String? {
        val xff = headers.entries.firstOrNull { it.key.equals("X-Forwarded-For", true) }?.value
        if (!xff.isNullOrBlank()) {
            val parts = xff.split(",").map { it.trim() }.filter { it.isNotBlank() }
            for (candidate in parts) {
                if (isValidIp(candidate) && !isPrivate(candidate)) {
                    return candidate
                }
            }
            // 全是私网时，取第一个
            parts.firstOrNull()?.let { if (isValidIp(it)) return it }
        }
        val xri = headers.entries.firstOrNull { it.key.equals("X-Real-IP", true) }?.value
        if (!xri.isNullOrBlank() && isValidIp(xri)) return xri

        // 可选：如果 remoteAddr 属于可信代理，则不作为真实 IP
        if (!remoteAddr.isNullOrBlank() && isValidIp(remoteAddr)) {
            if (trustedProxies.isNotEmpty() && ipInAnyCidr(remoteAddr, trustedProxies)) return null
            return remoteAddr
        }
        return null
    }
}