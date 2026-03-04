package com.fanekit.common.util.gps

/**
 * 经纬度点
 * @param lat  纬度（单位：度；若 WEB_MERCATOR，此字段存 y（米））
 * @param lon  经度（单位：度；若 WEB_MERCATOR，此字段存 x（米））
 * @param type 坐标类型
 */
data class GeoPoint(
    val lat: Double,
    val lon: Double,
    val type: CoordType
) {
    /** 拷贝并替换坐标类型（不改数值） */
    fun withType(t: CoordType) = copy(type = t)
}