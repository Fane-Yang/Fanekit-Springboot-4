package com.fanekit.common.util.gps

import kotlin.compareTo
import kotlin.math.*

/**
 * 地理工具：坐标转换、距离/方位、几何判断、编码等
 *
 * 说明：
 * - 几何计算默认统一转换至 WGS84 再进行（除非特别标注）
 * - 中国境外：WGS84↔GCJ02 不做偏移（直接返回原值）
 * - WEB_MERCATOR：在 GeoPoint 中以 (lat=y米, lon=x米) 存储
 */
object GeoUtils {

    // ---------------------- 常量区（WGS-84 椭球 & 常用参数） ----------------------
    private const val WGS84_A = 6378137.0                    // 椭球长半轴 a
    private const val WGS84_F = 1.0 / 298.257223563          // 扁率 f
    private val WGS84_B = WGS84_A * (1 - WGS84_F)            // 短半轴 b
    private const val EARTH_R_MEAN = 6_371_008.8             // 平均地球半径（米）用于 Haversine

    // Web Mercator
    private const val MERCATOR_MAX_LAT = 85.05112878
    private const val PI_180 = Math.PI / 180.0

    // GCJ/BD 常量
    private const val GCJ_A = 6378245.0
    private const val GCJ_EE = 0.00669342162296594323
    private const val BD_X_PI = Math.PI * 3000.0 / 180.0

    // ---------------------- 基础工具 ----------------------

    /** 度→弧度 */
    @JvmStatic fun deg2rad(d: Double) = d * PI_180
    /** 弧度→度 */
    @JvmStatic fun rad2deg(r: Double) = r / PI_180

    /** 经度归一化到 [-180,180) */
    @JvmStatic fun normalizeLon(lon: Double): Double {
        var x = lon % 360.0
        if (x >= 180.0) x -= 360.0
        if (x < -180.0) x += 360.0
        return x
    }

    /** 参数校验：纬度[-90,90] 经度[-180,180] */
    @JvmStatic fun validateLatLon(lat: Double, lon: Double) {
        require(lat in -90.0..90.0) { "lat out of range [-90,90]" }
        require(lon in -180.0..180.0) { "lon out of range [-180,180]" }
    }

    /** 是否在中国范围外（GCJ 偏移范围） */
    @JvmStatic fun isOutOfChina(lat: Double, lon: Double): Boolean {
        return lon !in 73.66..135.05 || lat !in 3.86..53.55
    }

    private fun clamp(v: Double, lo: Double, hi: Double) = max(lo, min(hi, v))

    // ---------------------- 1) 坐标系转换 ----------------------

    /**
     * 将点转换到目标坐标系
     */
    @JvmStatic fun convert(p: GeoPoint, target: CoordType): GeoPoint {
        if (p.type == target) return p
        val wgs = toWGS84(p)
        return fromWGS84(wgs, target)
    }

    /** 任意 -> WGS84 */
    @JvmStatic fun toWGS84(p: GeoPoint): GeoPoint = when (p.type) {
        CoordType.WGS84 -> p
        CoordType.GCJ02 -> gcjToWgsExact(p)
        CoordType.BD09 -> gcjToWgsExact(bdToGcj(p))
        CoordType.WEB_MERCATOR -> webMercatorToWGS84(p)
    }

    /** WGS84 -> 任意 */
    @JvmStatic fun fromWGS84(wgs: GeoPoint, target: CoordType): GeoPoint = when (target) {
        CoordType.WGS84 -> wgs
        CoordType.GCJ02 -> wgsToGcj(wgs)
        CoordType.BD09  -> gcjToBd(wgsToGcj(wgs))
        CoordType.WEB_MERCATOR -> wgs84ToWebMercator(wgs)
    }

    // ---- WGS84 <-> GCJ-02 ----

    /**
     * WGS84 → GCJ-02
     * - 中国境外：直接返回原值但类型改为 GCJ02（不偏移）
     * @param wgs 输入点，类型必须为 WGS84
     * @return 精确转换后的 GCJ02 点
     */
    @JvmStatic fun wgsToGcj(wgs: GeoPoint): GeoPoint {
        require(wgs.type == CoordType.WGS84) { "input must be WGS84" }
        val (lat, lon) = wgs
        validateLatLon(lat, lon)
        if (isOutOfChina(lat, lon)) return GeoPoint(lat, lon, CoordType.GCJ02)
        val (dLat, dLon) = gcjDelta(lat, lon)
        return GeoPoint(lat + dLat, lon + dLon, CoordType.GCJ02)
    }

    /**
     * GCJ-02 → WGS84（迭代收敛精确反算）
     * - 中国境外：直接返回原值但类型改为 WGS84（不偏移）
     * 注意：此方法为迭代收敛，可能需要多次迭代才能达到精度要求
     * 默认迭代 10,000 次，通常足够精确
     * @param gcj 输入点，类型必须为 GCJ02 或 BD09
     * @return 精确转换后的 WGS84 点
     */
    @JvmStatic fun gcjToWgsExact(gcj: GeoPoint): GeoPoint {
        require(gcj.type == CoordType.GCJ02 || gcj.type == CoordType.BD09) { "input must be GCJ02/BD09" }
        val lat = gcj.lat; val lon = gcj.lon
        validateLatLon(lat, lon)
        if (isOutOfChina(lat, lon)) return GeoPoint(lat, lon, CoordType.WGS84)

        var wgLat = lat
        var wgLon = lon
        val tol = 1e-7 // ~0.011 m
        repeat(10000) {
            val (dLat, dLon) = gcjDelta(wgLat, wgLon)
            val mgLat = wgLat + dLat
            val mgLon = wgLon + dLon
            val dlt = lat - mgLat
            val dln = lon - mgLon
            wgLat += dlt
            wgLon += dln
            if (abs(dlt) < tol && abs(dln) < tol) return@repeat
        }
        return GeoPoint(wgLat, wgLon, CoordType.WGS84)
    }

    /** GCJ-02 → WGS84（快速近似）
     * - 中国境外：直接返回原值但类型改为 WGS84（不偏移）
     * 注意：此方法为快速近似，精度较低
     * 如果需要高精度转换，请使用 [gcjToWgsExact]
     * @param gcj 输入点，类型必须为 GCJ02 或 BD09
     * @return 近似转换后的 WGS84 点
     */
    private fun gcjDelta(lat: Double, lon: Double): Pair<Double, Double> {
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = deg2rad(lat)
        val magic = 1 - GCJ_EE * sin(radLat).let { it * it }
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((GCJ_A * (1 - GCJ_EE)) / (magic * sqrtMagic) * Math.PI)
        dLon = (dLon * 180.0) / (GCJ_A / sqrtMagic * cos(radLat) * Math.PI)
        return dLat to dLon
    }

    /**
     * GCJ-02 → BD-09
     */
    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y +
                0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * Math.PI) + 40.0 * sin(y / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * Math.PI) + 320 * sin(y * Math.PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    /**
     * GCJ-02 → BD-09
     */
    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x +
                0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * Math.PI) + 20.0 * sin(2.0 * x * Math.PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * Math.PI) + 40.0 * sin(x / 3.0 * Math.PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * Math.PI) + 300.0 * sin(x / 30.0 * Math.PI)) * 2.0 / 3.0
        return ret
    }

    // ---- GCJ-02 <-> BD-09 ----

    /**
     * GCJ-02 → BD-09
     * - 中国境外：直接返回原值但类型改为 BD09（不偏移）
     * @param gcj 输入点，类型必须为 GCJ02 或 WGS84
     * @return 转换后的 BD-09 点
     */
    @JvmStatic fun gcjToBd(gcj: GeoPoint): GeoPoint {
        require(gcj.type == CoordType.GCJ02 || gcj.type == CoordType.WGS84) { "input must be GCJ02/WGS84" }
        val p = if (gcj.type == CoordType.WGS84) wgsToGcj(gcj) else gcj
        val x = p.lon; val y = p.lat
        val z = sqrt(x * x + y * y) + 0.00002 * sin(y * BD_X_PI)
        val theta = atan2(y, x) + 0.000003 * cos(x * BD_X_PI)
        val bdLon = z * cos(theta) + 0.0065
        val bdLat = z * sin(theta) + 0.006
        return GeoPoint(bdLat, bdLon, CoordType.BD09)
    }

    /**
     * BD-09 → GCJ-02
     * - 中国境外：直接返回原值但类型改为 GCJ02（不偏移）
     * @param bd 输入点，类型必须为 BD09
     * @return 转换后的 GCJ-02 点
     */
    @JvmStatic fun bdToGcj(bd: GeoPoint): GeoPoint {
        require(bd.type == CoordType.BD09) { "input must be BD09" }
        val x = bd.lon - 0.0065; val y = bd.lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * BD_X_PI)
        val theta = atan2(y, x) - 0.000003 * cos(x * BD_X_PI)
        val ggLon = z * cos(theta)
        val ggLat = z * sin(theta)
        return GeoPoint(ggLat, ggLon, CoordType.GCJ02)
    }

    // ---- WGS84 <-> WebMercator ----

    /**
     * WGS84 → WebMercator（返回点的 lat=y米, lon=x米, type=WEB_MERCATOR）
     * @param wgs 输入点，类型必须为 WGS84
     * @return 转换后的 WebMercator 点
     */
    @JvmStatic fun wgs84ToWebMercator(wgs: GeoPoint): GeoPoint {
        require(wgs.type == CoordType.WGS84) { "input must be WGS84" }
        val lat = clamp(wgs.lat, -MERCATOR_MAX_LAT, MERCATOR_MAX_LAT)
        val lon = normalizeLon(wgs.lon)
        val x = WGS84_A * deg2rad(lon)
        val y = WGS84_A * ln(tan(Math.PI / 4 + deg2rad(lat) / 2))
        return GeoPoint(y, x, CoordType.WEB_MERCATOR)
    }

    /**
     * WebMercator → WGS84
     * - 输入点的 lat=y米, lon=x米, type=WEB_MERCATOR
     * @param web 输入点，类型必须为 WEB_MERCATOR
     * @return 转换后的 WGS84 点
     */
    @JvmStatic fun webMercatorToWGS84(web: GeoPoint): GeoPoint {
        require(web.type == CoordType.WEB_MERCATOR) { "input must be WEB_MERCATOR" }
        val x = web.lon
        val y = web.lat
        val lon = rad2deg(x / WGS84_A)
        val lat = rad2deg(2 * atan(exp(y / WGS84_A)) - Math.PI / 2)
        return GeoPoint(lat, lon, CoordType.WGS84)
    }

    // ---------------------- 2) 距离 & 方位 ----------------------

    /**
     * Haversine（球面）距离（米） 用于计算两点间的球面距离
     * - WGS84 坐标系下，返回两点间的球面距离（米）
     * - 适用于大范围距离计算
     * - 注意：此方法假设地球为完美球体，实际地球为椭球体
     * 快速计算
     * @param a 起点
     * @param b 终点
     * @return 两点间的球面距离（米）
     * @note 计算精度受地球半径影响，平均地球半径约 6,371,008.8 米
     */
    @JvmStatic fun distanceHaversine(a: GeoPoint, b: GeoPoint): Double {
        val A = toWGS84(a); val B = toWGS84(b)
        val φ1 = deg2rad(A.lat); val φ2 = deg2rad(B.lat)
        val dφ = φ2 - φ1
        val dλ = deg2rad(normalizeLon(B.lon - A.lon))
        val h = sin(dφ / 2).let { it * it } + cos(φ1) * cos(φ2) * sin(dλ / 2).let { it * it }
        return 2 * EARTH_R_MEAN * asin(sqrt(h))
    }

    /**
     * Vincenty 逆解（WGS-84 椭球，高精度，米） 用于计算两点间的精确距离
     * - 对于极端反经线/对跖点可能不收敛，实际迭代 100 次足够
     * - 适用于小范围内的高精度距离计算
     * 高精度计算
     * @param a 起点
     * @param b 终点
     * @return 两点间的精确距离（米）
     * @note 适用于高精度距离计算，尤其是小范围内
     */
    @JvmStatic fun distanceVincenty(a: GeoPoint, b: GeoPoint): Double {
        val A = toWGS84(a); val B = toWGS84(b)
        val φ1 = deg2rad(A.lat); val λ1 = deg2rad(A.lon)
        val φ2 = deg2rad(B.lat); val λ2 = deg2rad(B.lon)

        val L = λ2 - λ1
        val U1 = atan((1 - WGS84_F) * tan(φ1))
        val U2 = atan((1 - WGS84_F) * tan(φ2))
        val sinU1 = sin(U1); val cosU1 = cos(U1)
        val sinU2 = sin(U2); val cosU2 = cos(U2)

        var λ = L
        var λPrev: Double
        var iter = 0
        var sinσ: Double
        var cosσ: Double
        var σ: Double
        var sinα: Double
        var cos2α: Double
        var cos2σm: Double

        do {
            val sinλ = sin(λ); val cosλ = cos(λ)
            sinσ = sqrt((cosU2 * sinλ).pow(2) + (cosU1 * sinU2 - sinU1 * cosU2 * cosλ).pow(2))
            if (sinσ == 0.0) return 0.0 // 重合
            cosσ = sinU1 * sinU2 + cosU1 * cosU2 * cosλ
            σ = atan2(sinσ, cosσ)
            sinα = (cosU1 * cosU2 * sinλ) / sinσ
            cos2α = 1 - sinα * sinα
            cos2σm = if (cos2α == 0.0) 0.0 else (cosσ - 2 * sinU1 * sinU2 / cos2α)
            val C = WGS84_F / 16 * cos2α * (4 + WGS84_F * (4 - 3 * cos2α))
            λPrev = λ
            λ = L + (1 - C) * WGS84_F * sinα * (σ + C * sinσ * (cos2σm + C * cosσ * (-1 + 2 * cos2σm * cos2σm)))
        } while (abs(λ - λPrev) > 1e-12 && ++iter < 100)

        val u2 = cos2α * (WGS84_A * WGS84_A - WGS84_B * WGS84_B) / (WGS84_B * WGS84_B)
        val Acoef = 1 + u2 / 16384.0 * (4096.0 + u2 * (-768.0 + u2 * (320.0 - 175.0 * u2)))
        val Bcoef = u2 / 1024.0 * (256.0 + u2 * (-128.0 + u2 * (74.0 - 47.0 * u2)))
        val Δσ = Bcoef * sinσ * (cos2σm + Bcoef / 4.0 * (cosσ * (-1 + 2 * cos2σm * cos2σm)
                - Bcoef / 6.0 * cos2σm * (-3 + 4 * sinσ * sinσ) * (-3 + 4 * cos2σm * cos2σm)))
        return WGS84_B * Acoef * (σ - Δσ)
    }

    /**
     * 初始方位角（度，0=北，顺时针）
     * 北方向为 0°，东为 90°，南为 180°，西为 270°
     * 计算两点间的初始方位角（从起点到终点的方向）
     * @param a 起点
     * @param b 终点
     * @return 初始方位角（度，0=北，顺时针）
     * @note 适用于大范围方位角计算，假设地球为球体
     */
    @JvmStatic fun initialBearing(a: GeoPoint, b: GeoPoint): Double {
        val A = toWGS84(a); val B = toWGS84(b)
        val φ1 = deg2rad(A.lat); val φ2 = deg2rad(B.lat)
        val dλ = deg2rad(normalizeLon(B.lon - A.lon))
        val y = sin(dλ) * cos(φ2)
        val x = cos(φ1) * sin(φ2) - sin(φ1) * cos(φ2) * cos(dλ)
        return (rad2deg(atan2(y, x)) + 360) % 360
    }

    /**
     * 根据起点、初始方位角与球面距离推算终点（WGS84，球面近似）
     * 计算从起点出发，沿指定方位角和距离到达的终点坐标
     * @param start 起点坐标
     * @param bearingDeg 初始方位角（度，0=北，顺时针）
     * @param distanceMeters 球面距离（米）
     * @return 终点坐标（WGS84）
     */
    @JvmStatic fun destination(start: GeoPoint, bearingDeg: Double, distanceMeters: Double): GeoPoint {
        val S = toWGS84(start)
        val δ = distanceMeters / EARTH_R_MEAN
        val θ = deg2rad(bearingDeg)
        val φ1 = deg2rad(S.lat); val λ1 = deg2rad(S.lon)
        val φ2 = asin(sin(φ1) * cos(δ) + cos(φ1) * sin(δ) * cos(θ))
        val λ2 = λ1 + atan2(sin(θ) * sin(δ) * cos(φ1), cos(δ) - sin(φ1) * sin(φ2))
        return GeoPoint(rad2deg(φ2), normalizeLon(rad2deg(λ2)), CoordType.WGS84)
    }

    /**
     * 两点中点（球面）
     * 计算两点间的中点坐标
     * @param a 起点
     * @param b 终点
     * @return 中点坐标（WGS84）
     */
    @JvmStatic fun midpoint(a: GeoPoint, b: GeoPoint): GeoPoint {
        val A = toWGS84(a); val B = toWGS84(b)
        val φ1 = deg2rad(A.lat); val λ1 = deg2rad(A.lon)
        val φ2 = deg2rad(B.lat); val λ2 = deg2rad(B.lon)
        val Bx = cos(φ2) * cos(λ2 - λ1)
        val By = cos(φ2) * sin(λ2 - λ1)
        val φ3 = atan2(sin(φ1) + sin(φ2), sqrt((cos(φ1) + Bx).pow(2) + By.pow(2)))
        val λ3 = λ1 + atan2(By, cos(φ1) + Bx)
        return GeoPoint(rad2deg(φ3), normalizeLon(rad2deg(λ3)), CoordType.WGS84)
    }

    // ---------------------- 3) 点在圆内 ----------------------

    /**
     * 点是否在圆内（含边界），圆参数：圆心+半径（米）
     * 计算点是否在指定圆内（包括边界）
     * @param p 点坐标
     * @param center 圆心坐标
     * @param radiusMeters 圆半径（米）
     * @return true 如果点在圆内或边界上
     */
    @JvmStatic fun isPointInCircle(p: GeoPoint, center: GeoPoint, radiusMeters: Double): Boolean {
        return distanceHaversine(p, center) <= radiusMeters
    }

    // ---------------------- 4) 点在多边形内 ----------------------

    /**
     * 点是否在多边形内（射线法；平面近似；适用于城市/区域级）
     * 计算点是否在多边形内（使用射线法）
     * @param p 点坐标
     * @param polygon 多边形顶点列表（按顺序）
     * @param allowOnEdge true 表示点落在边/顶点上也视为在内
     * @note 大范围/跨反经线/极区建议使用球面多边形算法
     */
    @JvmStatic fun isPointInPolygon(p: GeoPoint, polygon: List<GeoPoint>, allowOnEdge: Boolean = true): Boolean {
        require(polygon.size >= 3) { "polygon needs >= 3 points" }
        val P = toWGS84(p)
        val poly = polygon.map {
            val w = toWGS84(it)
            doubleArrayOf(w.lat, normalizeLon(w.lon))
        }
        val x = normalizeLon(P.lon); val y = P.lat
        if (allowOnEdge && isPointOnPolygonEdge(y, x, poly)) return true

        var inside = false
        var j = poly.lastIndex
        for (i in poly.indices) {
            val yi = poly[i][0]; val xi = poly[i][1]
            val yj = poly[j][0]; val xj = poly[j][1]
            val intersect = ((yi.compareTo(y)) != (yj.compareTo(y))) &&
                    (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    /**
     * 判断点是否在多边形边上（允许误差）
     * 使用射线法判断点是否在多边形的边上
     * @param y 点的纬度
     * @param x 点的经度
     * @param poly 多边形顶点列表（按顺序）
     * @return true 如果点在多边形的边上
     * 注意：此方法仅检查边界，不检查多边形内部
     */
    private fun isPointOnPolygonEdge(y: Double, x: Double, poly: List<DoubleArray>): Boolean {
        var j = poly.lastIndex
        for (i in poly.indices) {
            val y1 = poly[j][0]; val x1 = poly[j][1]
            val y2 = poly[i][0]; val x2 = poly[i][1]
            if (isPointOnSegment(y, x, y1, x1, y2, x2, 1e-9)) return true
            j = i
        }
        return false
    }

    /**
     * 判断点是否在线段上（允许误差）
     * 使用叉积和点积判断点是否在线段上
     * @param y 点的纬度
     * @param x 点的经度
     * @param y1 线段起点纬度
     * @param x1 线段起点经度
     * @param y2 线段终点纬度
     * @param x2 线段终点经度
     * @param eps 误差容忍度（米）
     * @return true 如果点在指定线段上
     */
    private fun isPointOnSegment(y: Double, x: Double, y1: Double, x1: Double, y2: Double, x2: Double, eps: Double): Boolean {
        val cross = (x - x1) * (y2 - y1) - (y - y1) * (x2 - x1)
        if (abs(cross) > eps) return false
        val dot = (x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)
        if (dot < -eps) return false
        val len2 = (x2 - x1).pow(2) + (y2 - y1).pow(2)
        return dot <= len2 + eps
    }

    // ---------------------- 5) 其他常用方法 ----------------------

    /**
     * 近似：给定纬度处，每度经度≈多少米
     * 计算给定纬度处每度经度对应的米数
     * @param lat 纬度（度）
     * @return 每度经度对应的米数（近似值）
     * 注意：此方法假设地球为球体，实际地球为椭球体
     */
    @JvmStatic fun metersPerDegreeLonAtLat(lat: Double) = cos(deg2rad(lat)) * 111_320.0
    /**
     * 近似：每度纬度≈多少米
     * 计算每度纬度对应的米数
     * @return 每度纬度对应的米数（近似值）
     */
    @JvmStatic fun metersPerDegreeLat() = 110_574.0

    /**
     * 以点为圆心、半径 r 生成外接 BBox（返回与输入点同坐标系的 SW/NE 两点）
     * 计算给定中心点和半径生成的外接矩形（边界框）
     * @param center 圆心坐标
     * @param radiusMeters 半径（米）
     * @return 外接矩形的西南角和东北角坐标（WGS84）
     */
    @JvmStatic fun bboxAround(center: GeoPoint, radiusMeters: Double): Array<GeoPoint> {
        val C = toWGS84(center)
        val dLat = radiusMeters / metersPerDegreeLat()
        val dLon = radiusMeters / metersPerDegreeLonAtLat(C.lat)
        val swW = GeoPoint(C.lat - dLat, normalizeLon(C.lon - dLon), CoordType.WGS84)
        val neW = GeoPoint(C.lat + dLat, normalizeLon(C.lon + dLon), CoordType.WGS84)
        return arrayOf(fromWGS84(swW, center.type), fromWGS84(neW, center.type))
    }

    /**
     * 折线总长（米）
     * 计算折线的总长度
     * @param pts 折线顶点列表
     * @return 折线长度（米）
     */
    @JvmStatic fun polylineLength(pts: List<GeoPoint>): Double {
        if (pts.size < 2) return 0.0
        var sum = 0.0
        for (i in 1 until pts.size) sum += distanceHaversine(pts[i - 1], pts[i])
        return sum
    }

    /**
     * 多边形周长（米）——首尾闭合
     * 计算多边形的周长
     * @param pts 多边形顶点列表（闭合）
     */
    @JvmStatic fun polygonPerimeter(pts: List<GeoPoint>): Double {
        if (pts.size < 2) return 0.0
        var sum = 0.0
        for (i in pts.indices) sum += distanceHaversine(pts[i], pts[(i + 1) % pts.size])
        return sum
    }

    /**
     * 多边形面积（平方米，近似：WGS84→WebMercator 平面后 shoelace）
     * 计算多边形的面积（使用 Shoelace 定理）
     * @param polygon 多边形顶点列表（按顺序）
     */
    @JvmStatic fun polygonAreaSqMeters(polygon: List<GeoPoint>): Double {
        if (polygon.size < 3) return 0.0
        val m = polygon.map { wgs84ToWebMercator(toWGS84(it)) }
        var sum = 0.0
        var j = m.lastIndex
        for (i in m.indices) {
            val xi = m[i].lon; val yi = m[i].lat
            val xj = m[j].lon; val yj = m[j].lat
            sum += (xj * yi - xi * yj)
            j = i
        }
        return abs(sum) * 0.5
    }

    /**
     * 多边形质心（近似：墨卡托平面），返回 WGS84
     * 计算多边形的质心（重心 即平均位置）
     * @param polygon 多边形顶点列表（按顺序）
     * @return 质心坐标（WGS84）
     */
    @JvmStatic fun polygonCentroid(polygon: List<GeoPoint>): GeoPoint {
        require(polygon.size >= 3) { "need >= 3 points" }
        val m = polygon.map { wgs84ToWebMercator(toWGS84(it)) }
        var A = 0.0
        var Cx = 0.0
        var Cy = 0.0
        var j = m.lastIndex
        for (i in m.indices) {
            val xi = m[i].lon; val yi = m[i].lat
            val xj = m[j].lon; val yj = m[j].lat
            val cross = xj * yi - xi * yj
            A += cross
            Cx += (xi + xj) * cross
            Cy += (yi + yj) * cross
            j = i
        }
        A *= 0.5
        if (abs(A) < 1e-12) {
            val sx = m.sumOf { it.lon }
            val sy = m.sumOf { it.lat }
            return webMercatorToWGS84(GeoPoint(sy / m.size, sx / m.size, CoordType.WEB_MERCATOR))
        }
        Cx /= (6 * A); Cy /= (6 * A)
        return webMercatorToWGS84(GeoPoint(Cy, Cx, CoordType.WEB_MERCATOR))
    }

    /**
     * Douglas–Peucker 抽稀（单位：米）
     * 使用 Douglas-Peucker 算法对折线进行抽稀 即简化
     * @param pts 折线顶点列表
     * @param toleranceMeters 抽稀容忍度（米）
     * @return 抽稀后的折线顶点列表
     */
    @JvmStatic fun simplifyDouglasPeucker(pts: List<GeoPoint>, toleranceMeters: Double): List<GeoPoint> {
        if (pts.size <= 2) return pts
        val keep = BooleanArray(pts.size)
        keep[0] = true
        keep[pts.size - 1] = true
        fun recurse(i: Int, j: Int) {
            if (j <= i + 1) return
            var maxD = -1.0
            var idx = -1
            val A = pts[i]; val B = pts[j]
            for (k in i + 1 until j) {
                val d = pointToSegmentDistanceMeters(pts[k], A, B)
                if (d > maxD) { maxD = d; idx = k }
            }
            if (maxD > toleranceMeters) {
                keep[idx] = true
                recurse(i, idx)
                recurse(idx, j)
            }
        }
        recurse(0, pts.lastIndex)
        return pts.filterIndexed { index, _ -> keep[index] }
    }

    /**
     * 点到线段最近距离（米）
     * - 使用等距矩形投影(equirectangular)近似到平面，计算线段距离
     * - 返回点到线段的最短距离（米）
     * @param p 点坐标
     * @param a 线段起点
     * @param b 线段终点
     * @return 点到线段的最短距离（米）
     */
    @JvmStatic fun pointToSegmentDistanceMeters(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val (xA, yA, xB, yB, xP, yP) = toLocalPlane(a, b, p)
        val dx = xB - xA; val dy = yB - yA
        if (dx == 0.0 && dy == 0.0) return hypot(xP - xA, yP - yA)
        var t = ((xP - xA) * dx + (yP - yA) * dy) / (dx * dx + dy * dy)
        t = t.coerceIn(0.0, 1.0)
        val xH = xA + t * dx; val yH = yA + t * dy
        return hypot(xP - xH, yP - yH)
    }

    /**
     * 点在线段上的最近点（返回 WGS84）
     * - 使用等距矩形投影(equirectangular)近似到平面，计算线段上的最近点
     * - 返回点在线段上的最近点坐标（WGS84）
     * @param p 点坐标
     * @param a 线段起点
     * @param b 线段终点
     * @return 点在线段上的最近点坐标（WGS84）
     */
    @JvmStatic fun nearestPointOnSegment(p: GeoPoint, a: GeoPoint, b: GeoPoint): GeoPoint {
        val (xA, yA, xB, yB, xP, yP) = toLocalPlane(a, b, p)
        val dx = xB - xA; val dy = yB - yA
        if (dx == 0.0 && dy == 0.0) return toWGS84(a)
        var t = ((xP - xA) * dx + (yP - yA) * dy) / (dx * dx + dy * dy)
        t = t.coerceIn(0.0, 1.0)
        val xH = xA + t * dx; val yH = yA + t * dy
        // 反投影到 WGS84
        val φ = deg2rad((toWGS84(a).lat + toWGS84(b).lat) / 2.0)
        val lon = rad2deg(xH / (EARTH_R_MEAN * cos(φ)))
        val lat = rad2deg(yH / EARTH_R_MEAN)
        return GeoPoint(lat, lon, CoordType.WGS84)
    }

    /**
     * 将 a,b,p 映射到等距矩形局部平面坐标（单位：米） 就是计算点在平面上的坐标
     * - 使用等距矩形投影(equirectangular)近似到平面
     * - 返回局部平面坐标系下的点
     *
     * 这个方法的作用是：把地理坐标（经纬度）a、b、p三点，转换成一个局部平面上的坐标（单位是米），方便用平面几何的方法计算距离、投影等。
     *
     * 简单理解：
     *
     *
     * 地球是球体，经纬度是球面坐标，直接计算距离和投影很复杂。
     * 这个方法用一种近似，把三点投影到一个二维平面上（类似地图上的x、y坐标），这样后续计算就可以用简单的平面公式了。
     * 常见用途：
     *
     *
     * 计算点到线段的距离
     * 计算点在线段上的最近点
     * 折线简化等
     * 本质上就是“把球面上的点，近似变成平面上的点”，方便后续处理。
     * @param a 线段起点
     * @param b 线段终点
     * @param p 点坐标
     * @return 局部平面坐标系下的点（xA, yA, xB, yB, xP, yP）
     */
    private fun toLocalPlane(a: GeoPoint, b: GeoPoint, p: GeoPoint): LocalPlane {
        val A = toWGS84(a); val B = toWGS84(b); val P = toWGS84(p)
        val φ = deg2rad((A.lat + B.lat) / 2.0)
        val xA = deg2rad(A.lon) * cos(φ) * EARTH_R_MEAN
        val yA = deg2rad(A.lat) * EARTH_R_MEAN
        val xB = deg2rad(B.lon) * cos(φ) * EARTH_R_MEAN
        val yB = deg2rad(B.lat) * EARTH_R_MEAN
        val xP = deg2rad(P.lon) * cos(φ) * EARTH_R_MEAN
        val yP = deg2rad(P.lat) * EARTH_R_MEAN
        return LocalPlane(xA, yA, xB, yB, xP, yP)
    }

    /**
     * 局部平面坐标系下的点（xA, yA, xB, yB, xP, yP）
     */
    private data class LocalPlane(
        val xA: Double, val yA: Double, val xB: Double, val yB: Double, val xP: Double, val yP: Double
    )

    /**
     * 线段相交检测（近似在平面，返回是否相交，含端点/重叠）
     * 检测两条线段是否相交（近似在平面）
     * @param a1 线段1 起点
     * @param a2 线段1 终点
     * @param b1 线段2 起点
     * @param b2 线段2 终点
     * @return true 如果两条线段相交（含端点/重叠）
     */
    @JvmStatic fun segmentsIntersect(a1: GeoPoint, a2: GeoPoint, b1: GeoPoint, b2: GeoPoint): Boolean {
        // 用 WebMercator 平面近似
        fun toM(p: GeoPoint): Pair<Double, Double> {
            val m = wgs84ToWebMercator(toWGS84(p))
            return m.lon to m.lat
        }
        val (x1, y1) = toM(a1); val (x2, y2) = toM(a2)
        val (x3, y3) = toM(b1); val (x4, y4) = toM(b2)

        fun cross(xa: Double, ya: Double, xb: Double, yb: Double) = xa * yb - ya * xb
        fun onSeg(xa: Double, ya: Double, xb: Double, yb: Double, x: Double, y: Double): Boolean {
            val minx = min(xa, xb) - 1e-9; val maxx = max(xa, xb) + 1e-9
            val miny = min(ya, yb) - 1e-9; val maxy = max(ya, yb) + 1e-9
            return x in minx..maxx && y in miny..maxy &&
                    abs(cross(x - xa, y - ya, xb - xa, yb - ya)) < 1e-6
        }
        val d1 = cross(x4 - x3, y4 - y3, x1 - x3, y1 - y3)
        val d2 = cross(x4 - x3, y4 - y3, x2 - x3, y2 - y3)
        val d3 = cross(x2 - x1, y2 - y1, x3 - x1, y3 - y1)
        val d4 = cross(x2 - x1, y2 - y1, x4 - x1, y4 - y1)

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
            ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) return true
        if (d1 == 0.0 && onSeg(x3, y3, x4, y4, x1, y1)) return true
        if (d2 == 0.0 && onSeg(x3, y3, x4, y4, x2, y2)) return true
        if (d3 == 0.0 && onSeg(x1, y1, x2, y2, x3, y3)) return true
        if (d4 == 0.0 && onSeg(x1, y1, x2, y2, x4, y4)) return true
        return false
    }

    /**
     * 计算多边形是否为顺时针（墨卡托平面），true=顺时针
     * 判断多边形顶点顺序是否为顺时针
     * @param polygon 多边形顶点列表（按顺序）
     * @return true 如果多边形顶点顺序为顺时针
     */
    @JvmStatic fun isClockwise(polygon: List<GeoPoint>): Boolean {
        val m = polygon.map { wgs84ToWebMercator(toWGS84(it)) }
        var sum = 0.0
        var j = m.lastIndex
        for (i in m.indices) {
            sum += (m[i].lon - m[j].lon) * (m[i].lat + m[j].lat)
            j = i
        }
        return sum > 0
    }

    /**
     * 多边形凸包（Graham Scan，返回 WGS84）
     * 计算多边形的凸包（即最小凸多边形）
     * @param points 多边形顶点列表（WGS84）
     * @return 凸包顶点列表（WGS84）
     */
    @JvmStatic fun convexHull(points: List<GeoPoint>): List<GeoPoint> {
        if (points.size <= 1) return points.map { toWGS84(it) }
        val pts = points.map { wgs84ToWebMercator(toWGS84(it)) }
            .map { Pair(it.lon, it.lat) }
            .distinct()
            .sortedWith(compareBy<Pair<Double, Double>> { it.first }.thenBy { it.second })

        fun cross(o: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>) =
            (a.first - o.first) * (b.second - o.second) - (a.second - o.second) * (b.first - o.first)

        val lower = mutableListOf<Pair<Double, Double>>()
        for (p in pts) {
            while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), p) <= 0) lower.removeLast()
            lower += p
        }
        val upper = mutableListOf<Pair<Double, Double>>()
        for (p in pts.asReversed()) {
            while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), p) <= 0) upper.removeLast()
            upper += p
        }
        val hull = (lower.dropLast(1) + upper.dropLast(1))
        return hull.map { webMercatorToWGS84(GeoPoint(it.second, it.first, CoordType.WEB_MERCATOR)) }
    }

    // ---------------------- 编解码/地理编码 ----------------------

    /**
     * Google Polyline 编码（输入必须为 WGS84）
     * 将一系列地理点编码为 Google Polyline 格式字符串
     * @param points 地理点列表（WGS84）
     * @param precision 编码精度（小数位数，默认 5）
     * @return 编码后的字符串
     */
    @JvmStatic fun encodePolyline(points: List<GeoPoint>, precision: Int = 5): String {
        fun encodeNumber(num: Int): String {
            var n = num shl 1
            if (num < 0) n = n.inv()
            val sb = StringBuilder()
            while (n >= 0x20) {
                sb.append(((0x20 or (n and 0x1f)) + 63).toChar())
                n = n shr 5
            }
            sb.append((n + 63).toChar())
            return sb.toString()
        }

        val scale = 10.0.pow(precision)
        var lastLat = 0
        var lastLon = 0
        val sb = StringBuilder()
        for (p in points) {
            val w = toWGS84(p)
            val lat = round(w.lat * scale).toInt()
            val lon = round(w.lon * scale).toInt()
            sb.append(encodeNumber(lat - lastLat))
            sb.append(encodeNumber(lon - lastLon))
            lastLat = lat; lastLon = lon
        }
        return sb.toString()
    }

    /**
     * Google Polyline 解码（输出 WGS84）
     * 将 Google Polyline 编码字符串解码为地理点列表
     * @param encoded 编码字符串
     * @param precision 解码精度（小数位数，默认 5）
     * @return 解码后的地理点列表（WGS84）
     */
    @JvmStatic fun decodePolyline(encoded: String, precision: Int = 5): List<GeoPoint> {
        val scale = 10.0.pow(precision)
        val pts = mutableListOf<GeoPoint>()
        var index = 0
        var lat = 0
        var lon = 0

        fun next(): Int {
            var result = 0
            var shift = 0
            var b: Int
            do {
                b = encoded[index++].code - 63
                result = result or ((b and 0x1f) shl shift)
                shift += 5
            } while (b >= 0x20 && index < encoded.length)
            return if ((result and 1) != 0) (result shr 1).inv() else (result shr 1)
        }

        while (index < encoded.length) {
            lat += next()
            if (index >= encoded.length) break
            lon += next()
            pts += GeoPoint(lat / scale, lon / scale, CoordType.WGS84)
        }
        return pts
    }

    /**
     * GeoHash 编码（WGS84）
     * 将经纬度编码为 GeoHash 字符串
     * @param lat 纬度（度）
     * @param lon 经度（度）
     * @param precision 编码精度（字符长度，默认 12）
     * @return 编码后的 GeoHash 字符串
     */
    @JvmStatic fun geoHashEncode(lat: Double, lon: Double, precision: Int = 12): String {
        val base32 = "0123456789bcdefghjkmnpqrstuvwxyz"
        var (minLat, maxLat) = -90.0 to 90.0
        var (minLon, maxLon) = -180.0 to 180.0
        var hash = StringBuilder()
        var isLon = true
        var bit = 0
        var ch = 0
        repeat(precision * 5) {
            if (isLon) {
                val mid = (minLon + maxLon) / 2
                if (lon >= mid) { ch = ch shl 1 or 1; minLon = mid } else { ch = ch shl 1; maxLon = mid }
            } else {
                val mid = (minLat + maxLat) / 2
                if (lat >= mid) { ch = ch shl 1 or 1; minLat = mid } else { ch = ch shl 1; maxLat = mid }
            }
            isLon = !isLon
            bit++
            if (bit == 5) { hash.append(base32[ch]); bit = 0; ch = 0 }
        }
        return hash.toString()
    }

    // ---------------------- WebMercator 瓦片工具 ----------------------

    /**
     * WGS84 -> (xTile,yTile) at zoom（Slippy Map）
     * 将 WGS84 坐标转换为瓦片坐标 (xTile, yTile) 在指定缩放级别 zoom
     * @param lat 纬度（度）
     * @param lon 经度（度）
     * @param zoom 缩放级别（0=全球，1=城市，2=街区等）
     * @return 瓦片坐标 (xTile, yTile)
     */
    @JvmStatic fun latLonToTile(lat: Double, lon: Double, zoom: Int): Pair<Int, Int> {
        val latClamped = clamp(lat, -MERCATOR_MAX_LAT, MERCATOR_MAX_LAT)
        val n = 1 shl zoom
        val x = ((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
        val y = ((1.0 - ln(tan(deg2rad(latClamped)) + 1 / cos(deg2rad(latClamped))) / Math.PI) / 2.0 * n).toInt()
            .coerceIn(0, n - 1)
        return x to y
    }

    /**
     * 瓦片 (x,y,zoom) -> 边界 BBox（WGS84，返回 SW/NE）
     * 将瓦片坐标 (x, y, zoom) 转换为瓦片边界的西南角和东北角坐标（WGS84）
     * @param x 瓦片 X 坐标
     * @param y 瓦片 Y 坐标
     * @param zoom 缩放级别
     * @return 西南角和东北角坐标数组（GeoPoint）
     */
    @JvmStatic fun tileBounds(x: Int, y: Int, zoom: Int): Array<GeoPoint> {
        val n = 1 shl zoom
        fun lon(tx: Int) = tx.toDouble() / n * 360.0 - 180.0
        fun lat(ty: Int): Double {
            val yRad = Math.PI - (2.0 * Math.PI * ty.toDouble()) / n
            return rad2deg(atan(sinh(yRad)))
        }
        val sw = GeoPoint(lat(y + 1), lon(x), CoordType.WGS84)
        val ne = GeoPoint(lat(y), lon(x + 1), CoordType.WGS84)
        return arrayOf(sw, ne)
    }
}
