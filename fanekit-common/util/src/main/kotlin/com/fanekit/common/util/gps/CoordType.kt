package com.fanekit.common.util.gps

/** 坐标系类型 */
enum class CoordType {
    /** WGS-84：GPS/全球通用地理坐标（经纬度） */
    WGS84,
    /** GCJ-02：国测局/火星坐标（高德/腾讯） */
    GCJ02,
    /** BD-09：百度坐标 */
    BD09,
    /** Web Mercator 投影（注意：lat=y(米), lon=x(米)） */
    WEB_MERCATOR
}