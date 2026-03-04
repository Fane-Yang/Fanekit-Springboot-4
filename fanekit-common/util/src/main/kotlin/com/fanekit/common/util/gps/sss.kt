package com.fanekit.common.util.gps

fun main() {
    // 1) 坐标转换
    val shWgs = GeoPoint(31.2304, 121.4737, CoordType.WGS84)
    val shGcj = GeoUtils.fromWGS84(shWgs, CoordType.GCJ02)
    val shBd  = GeoUtils.fromWGS84(shWgs, CoordType.BD09)
    val back  = GeoUtils.toWGS84(shGcj)
    println("WGS84 -> GCJ02: $shGcj")
    println("WGS84 -> BD09 : $shBd")
    println("GCJ02 -> WGS84: $back")

    // 2) 距离 & 方位
    val bjWgs = GeoPoint(39.9042, 116.4074, CoordType.WGS84)
    println("Haversine SH-BJ (km): ${GeoUtils.distanceHaversine(shWgs, bjWgs) / 1000}")
    println("Vincenty  SH-BJ (km): ${GeoUtils.distanceVincenty(shWgs, bjWgs) / 1000}")
    println("Initial bearing SH->BJ: ${GeoUtils.initialBearing(shWgs, bjWgs)}")
    println("Midpoint SH-BJ: ${GeoUtils.midpoint(shWgs, bjWgs)}")
    println("Dest from SH 10km @45°: ${GeoUtils.destination(shWgs, 45.0, 10_000.0)}")

    // 3) 点在圆内
    val nearSH = GeoPoint(31.2400, 121.4737, CoordType.WGS84)
    println("nearSH in circle(1km around SH)? ${GeoUtils.isPointInCircle(nearSH, shWgs, 1000.0)}")

    // 4) 点在多边形内 + 面积/质心
    val rect = listOf(
        GeoPoint(31.20, 121.44, CoordType.WGS84),
        GeoPoint(31.20, 121.50, CoordType.WGS84),
        GeoPoint(31.26, 121.50, CoordType.WGS84),
        GeoPoint(31.26, 121.44, CoordType.WGS84)
    )
    println("SH in rect? ${GeoUtils.isPointInPolygon(shWgs, rect, true)}")
    println("rect perimeter(m): ${GeoUtils.polygonPerimeter(rect)}")
    println("rect area(m^2): ${GeoUtils.polygonAreaSqMeters(rect)}")
    println("rect centroid: ${GeoUtils.polygonCentroid(rect)}")

    // 5) 折线总长/抽稀/最近点
    val path = listOf(
        GeoPoint(31.2300, 121.4700, CoordType.WGS84),
        GeoPoint(31.2310, 121.4710, CoordType.WGS84),
        GeoPoint(31.2320, 121.4720, CoordType.WGS84),
        GeoPoint(31.2330, 121.4730, CoordType.WGS84),
        GeoPoint(31.2340, 121.4740, CoordType.WGS84)
    )
    println("path length(m): ${GeoUtils.polylineLength(path)}")
    val simplified = GeoUtils.simplifyDouglasPeucker(path, 30.0)
    println("path simplified size: ${simplified.size}")
    val nearest = GeoUtils.nearestPointOnSegment(GeoPoint(31.2315, 121.4715, CoordType.WGS84), path[1], path[3])
    println("nearest to segment: $nearest")

    // 6) 线段相交
    val a1 = GeoPoint(31.23, 121.47, CoordType.WGS84)
    val a2 = GeoPoint(31.235, 121.475, CoordType.WGS84)
    val b1 = GeoPoint(31.23, 121.475, CoordType.WGS84)
    val b2 = GeoPoint(31.235, 121.47, CoordType.WGS84)
    println("segments intersect? ${GeoUtils.segmentsIntersect(a1, a2, b1, b2)}")

    // 7) 凸包
    val hull = GeoUtils.convexHull(path + listOf(
        GeoPoint(31.2400, 121.4800, CoordType.WGS84)
    ))
    println("convex hull size: ${hull.size}")

    // 8) BBox
    val bbox = GeoUtils.bboxAround(shWgs, 1000.0)
    println("bbox SW/NE (1km): ${bbox.contentToString()}")

    // 9) Polyline 编解码
    val encoded = GeoUtils.encodePolyline(path)
    val decoded = GeoUtils.decodePolyline(encoded)
    println("polyline encoded: $encoded")
    println("polyline decoded size: ${decoded.size}")

    // 10) GeoHash
    println("GeoHash(SH,7): ${GeoUtils.geoHashEncode(shWgs.lat, shWgs.lon, 7)}")

    // 11) WebMercator 瓦片
    val (tx, ty) = GeoUtils.latLonToTile(shWgs.lat, shWgs.lon, 12)
    val tb = GeoUtils.tileBounds(tx, ty, 12)
    println("tile=$tx/$ty@12, bounds(SW,NE)=${tb.contentToString()}")
}
