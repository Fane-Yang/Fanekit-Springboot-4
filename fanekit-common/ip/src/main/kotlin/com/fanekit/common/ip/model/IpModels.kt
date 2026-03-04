package com.fanekit.common.ip.model

enum class LoadMode { MEMORY_MAPPED, MEMORY }
enum class QueryLevel { COUNTRY, CITY, FULL }

data class IpCountryInfo(
    val countryName: String? = null,
    val countryIsoCode: String? = null
)

/**
 * IP 地址相关信息
 * - country: 国家信息
 * - city: 城市信息
 * - asn: ASN 信息
 * - isp: 互联网服务提供商（仅 ip2region 可提供）
 * - source: 数据来源标识
 * - toString: 格式化输出信息
 * - 经纬度为WGS84坐标系
 */
data class IpCityInfo(
    val countryName: String? = null,
    val countryIsoCode: String? = null,
    val subdivisionName: String? = null,
    val subdivisionIsoCode: String? = null,
    val cityName: String? = null,
    val timeZone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class IpAsnInfo(
    val asn: Int? = null,
    val org: String? = null,
    val network: String? = null
)

data class IpFullInfo(
    val country: IpCountryInfo = IpCountryInfo(),
    val city: IpCityInfo = IpCityInfo(),
    val asn: IpAsnInfo = IpAsnInfo(),
    val isp: String? = null,        // 仅 ip2region 可提供
    val source: String = "UNKNOWN"  // 标注主要数据来源
) {
    override fun toString(): String =
        "[$source] country=${country.countryName}/${country.countryIsoCode}, " +
                "subdivision=${city.subdivisionName}, city=${city.cityName}, tz=${city.timeZone}, " +
                "lat=${city.latitude}, lon=${city.longitude}, isp=$isp, asn=${asn.asn}/${asn.org}, net=${asn.network}"
}