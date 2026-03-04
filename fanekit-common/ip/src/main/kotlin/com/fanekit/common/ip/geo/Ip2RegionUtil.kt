package com.fanekit.common.ip.geo

import com.fanekit.common.ip.model.IpCityInfo
import com.fanekit.common.ip.model.IpCountryInfo
import com.fanekit.common.ip.model.IpFullInfo
import org.lionsoul.ip2region.xdb.Searcher
import java.io.File
import java.util.concurrent.atomic.AtomicReference

/**
 * ip2region：IPv4；超快；国家/省/市/ISP
 */
object Ip2RegionUtil {
    private const val DEFAULT_XDB = "ip2region.xdb"
    private val searcherRef = AtomicReference<Searcher>()
    @Volatile private var initialized = false

    @Synchronized
    fun init(xdbPath: String = DEFAULT_XDB, loadToMemory: Boolean = true) {
        val f = File(xdbPath); require(f.exists()) { "ip2region.xdb 不存在: $xdbPath" }
        val s = if (loadToMemory) Searcher.newWithBuffer(f.readBytes()) else Searcher.newWithFileOnly(f.absolutePath)
        searcherRef.set(s)
        initialized = true
    }

    private fun ensureInit() = check(initialized) { "Ip2Region 未初始化" }

    /** 原始格式：国家|区域|省|市|运营商 */
    fun raw(ip: String): String {
        ensureInit()
        return runCatching { searcherRef.get().search(ip) }.getOrElse { "0|0|0|0|0" }
    }

    fun country(ip: String): IpCountryInfo {
        val arr = raw(ip).split("|")
        val name = arr.getOrNull(0)?.takeIf { it != "0" }
        return IpCountryInfo(countryName = name)
    }

    fun city(ip: String): IpCityInfo {
        val arr = raw(ip).split("|")
        return IpCityInfo(
            countryName = arr.getOrNull(0)?.takeIf { it != "0" },
            subdivisionName = arr.getOrNull(2)?.takeIf { it != "0" },
            cityName = arr.getOrNull(3)?.takeIf { it != "0" }
        )
    }

    fun full(ip: String): IpFullInfo {
        val arr = raw(ip).split("|")
        val country = arr.getOrNull(0)?.takeIf { it != "0" }
        val province = arr.getOrNull(2)?.takeIf { it != "0" }
        val city = arr.getOrNull(3)?.takeIf { it != "0" }
        val isp = arr.getOrNull(4)?.takeIf { it != "0" }
        return IpFullInfo(
            country = IpCountryInfo(countryName = country),
            city = IpCityInfo(countryName = country, subdivisionName = province, cityName = city),
            isp = isp,
            source = "IP2REGION"
        )
    }

    fun isFromCountryName(ip: String, countryName: String): Boolean =
        country(ip).countryName?.contains(countryName) == true
}
