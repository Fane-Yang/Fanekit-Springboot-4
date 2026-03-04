package com.fanekit.common.ip.geo

import com.fanekit.common.ip.model.IpAsnInfo
import com.fanekit.common.ip.model.IpCityInfo
import com.fanekit.common.ip.model.IpCountryInfo
import com.fanekit.common.ip.model.IpFullInfo
import com.fanekit.common.ip.model.LoadMode
import com.maxmind.db.Reader.FileMode
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.AsnResponse
import com.maxmind.geoip2.model.CityResponse
import java.io.File
import java.net.InetAddress
import java.util.concurrent.atomic.AtomicReference

/**
 * GeoLite2：支持 IPv4/IPv6；City/Country/ASN 多库组合；可选 LRU 缓存
 */
object GeoLite2Util {

    data class Options(
        val cityMmdbPath: String? = "GeoLite2-City.mmdb",
        val countryMmdbPath: String? = "GeoLite2-Country.mmdb",
        val asnMmdbPath: String? = "GeoLite2-ASN.mmdb",
        val language: String = "zh-CN",
        val enableLruCache: Boolean = false,
        val cacheSize: Int = 10000,
        val loadMode: LoadMode = LoadMode.MEMORY_MAPPED
    ) {
        val fileMode: FileMode by lazy {
            when (loadMode) {
                LoadMode.MEMORY_MAPPED -> FileMode.MEMORY_MAPPED
                LoadMode.MEMORY -> FileMode.MEMORY
            }
        }
    }

    private val cityRef = AtomicReference<DatabaseReader?>()
    private val countryRef = AtomicReference<DatabaseReader?>()
    private val asnRef = AtomicReference<DatabaseReader?>()
    @Volatile private var language: String = "zh-CN"
    @Volatile private var initialized = false

    private class Lru<K, V>(private val max: Int) : LinkedHashMap<K, V>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean = size > max
        @Synchronized fun getSafe(k: K) = super.get(k)
        @Synchronized fun putSafe(k: K, v: V) { super.put(k, v) }
    }
    private var cache: Lru<String, IpFullInfo>? = null

    @Synchronized
    fun init(opts: Options = Options()) {
        fun build(path: String?): DatabaseReader? {
            if (path.isNullOrBlank()) return null
            val f = File(path); if (!f.exists()) return null
            return DatabaseReader.Builder(f).fileMode(opts.fileMode).build()
        }
        cityRef.set(build(opts.cityMmdbPath))
        countryRef.set(build(opts.countryMmdbPath))
        asnRef.set(build(opts.asnMmdbPath))
        language = opts.language
        cache = if (opts.enableLruCache && opts.cacheSize > 0) Lru(opts.cacheSize) else null
        initialized = true
    }

    private fun ensureInit() = check(initialized) { "GeoLite2 未初始化" }
    private fun inet(ip: String) = InetAddress.getByName(ip)
    private fun nameLang(names: Map<String, String>?, lang: String) = names?.get(lang) ?: names?.get("en")

    fun country(ip: String): IpCountryInfo {
        ensureInit()
        val addr = inet(ip)
        cityRef.get()?.let { r ->
            val resp = runCatching { r.city(addr) }.getOrNull()
            resp?.let {
                return IpCountryInfo(
                    countryName = nameLang(it.country?.names, language) ?: it.country?.name,
                    countryIsoCode = it.country?.isoCode
                )
            }
        }
        countryRef.get()?.let { r ->
            val resp = runCatching { r.country(addr) }.getOrNull()
            resp?.let {
                return IpCountryInfo(
                    countryName = nameLang(it.country?.names, language) ?: it.country?.name,
                    countryIsoCode = it.country?.isoCode
                )
            }
        }
        return IpCountryInfo()
    }

    fun city(ip: String): IpCityInfo {
        ensureInit()
        val r = cityRef.get() ?: return IpCityInfo()
        val addr = inet(ip)
        val resp: CityResponse = runCatching { r.city(addr) }.getOrNull() ?: return IpCityInfo()
        val countryName = nameLang(resp.country?.names, language) ?: resp.country?.name
        val countryIso = resp.country?.isoCode
        val sub = resp.subdivisions?.firstOrNull()
        val subName = nameLang(sub?.names, language) ?: sub?.name
        val subIso = sub?.isoCode
        val cityName = nameLang(resp.city?.names, language) ?: resp.city?.name
        val tz = resp.location?.timeZone
        val lat = resp.location?.latitude
        val lon = resp.location?.longitude
        return IpCityInfo(countryName, countryIso, subName, subIso, cityName, tz, lat, lon)
    }

    fun asn(ip: String): IpAsnInfo {
        ensureInit()
        val r = asnRef.get() ?: return IpAsnInfo()
        val addr = inet(ip)
        val resp: AsnResponse = runCatching { r.asn(addr) }.getOrNull() ?: return IpAsnInfo()
        return IpAsnInfo(
            asn = resp.autonomousSystemNumber?.toInt(),
            org = resp.autonomousSystemOrganization,
            network = resp.network?.toString()
        )
    }

    fun isFromCountry(ip: String, isoOrName: String): Boolean {
        val c = country(ip)
        if (!c.countryIsoCode.isNullOrBlank() && isoOrName.length == 2) {
            return isoOrName.equals(c.countryIsoCode, ignoreCase = true)
        }
        val name = c.countryName ?: return false
        return name.contains(isoOrName, ignoreCase = true)
    }

    fun full(ip: String): IpFullInfo {
        ensureInit()
        cache?.getSafe(ip)?.let { return it }
        val cty = city(ip)
        val cn = if (cty.countryIsoCode != null || cty.countryName != null) {
            IpCountryInfo(cty.countryName, cty.countryIsoCode)
        } else {
            country(ip)
        }
        val asnInfo = asn(ip)
        val info = IpFullInfo(country = cn, city = cty, asn = asnInfo, source = "GEOLITE2")
        cache?.putSafe(ip, info)
        return info
    }
}