//package common.utils.net.ip
//
//
//import com.google.common.io.Resources
//import com.maxmind.geoip2.DatabaseReader
//import com.maxmind.geoip2.model.CityResponse
//import com.maxmind.geoip2.model.CountryResponse
//import java.io.File
//import java.net.InetAddress
//
///**
// * 数据库下载地址
// *(有三个需要下载)
// * https://github.com/P3TERX/GeoLite.mmdb?tab=readme-ov-file
// */
//class IPUtils {
//    companion object {
//        fun getCountry(ip: String): CountryResponse? {
//            val database = File(Resources.getResource("db/Country.mmdb").path)
//            val reader = DatabaseReader.Builder(database).build()
//            return try {
//                val inetAddress = InetAddress.getByName(ip)
//                reader.country(inetAddress)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            } finally {
//                reader.close()
//            }
//        }
//
//        fun getIpInfo(ip: String):CityResponse? {
//            val database = File(Resources.getResource("db/Country.mmdb").path)
//            val reader = DatabaseReader.Builder(database).build()
//            return try {
//                val inetAddress = InetAddress.getByName(ip)
//                reader.use {
//                    return reader.city(inetAddress)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                null
//            } finally {
//            }
//        }
//    }
//}
//
//
//
//// 使用示例
//fun main() {
////    val dbPath = "E:\\Projects\\BackEnd\\ktor\\ktor\\server\\src\\main\\resources\\Country.mmdb" // 下载并指定数据库路径
//    val dbPath = Resources.getResource("db/GeoLite2-Country.mmdb").path
//    val ip = "114.114.114.114"
//    val geoInfo = IPUtils.getIpInfo(ip)
//    if (geoInfo != null) {
//        println("Country: ${geoInfo.country.name}")
//        println("City: ${geoInfo.city.name}")
//        println("Latitude: ${geoInfo.location.latitude}")
//        println("Longitude: ${geoInfo.location.longitude}")
//    } else {
//        println("未找到IP信息。")
//    }
//}
//
//
//
///**
// * 查询IP地址的GeoIP信息
// * @param ip 要查询的IP地址
// * @param dbPath GeoLite2数据库的路径
// * @return 城市响应对象，包含丰富的地理位置信息
// */
//fun getGeoInfo(ip: String, dbPath: String): CityResponse? {
//    val database = File(dbPath)
//    val reader = DatabaseReader.Builder(database).build()
//    return try {
//        val inetAddress = InetAddress.getByName(ip)
//        reader.city(inetAddress)
//    } catch (e: Exception) {
//        e.printStackTrace()
//        null
//    } finally {
//        reader.close()
//    }
//}
//
//fun main1() {
//    val dbPath = "path/to/GeoLite2-City.mmdb" // 替换为实际的数据库路径
//    val ip = "8.8.8.8"
//    val geoInfo = getGeoInfo(ip, dbPath)
//    if (geoInfo != null) {
//        println("IP: $ip")
//        println("国家: ${geoInfo.country.name} (${geoInfo.country.isoCode})")
//        println("州/省: ${geoInfo.subdivisions.joinToString { it.name }}")
//        println("城市: ${geoInfo.city.name}")
//        println("邮政编码: ${geoInfo.postal.code}")
//        println("时区: ${geoInfo.location.timeZone}")
//        println("纬度: ${geoInfo.location.latitude}")
//        println("经度: ${geoInfo.location.longitude}")
//        println("ISP: ${geoInfo.traits.isp}")
//        println("组织: ${geoInfo.traits.organization}")
//        println("自治系统号: ${geoInfo.traits.autonomousSystemNumber}")
//        println("自治系统组织: ${geoInfo.traits.autonomousSystemOrganization}")
//    } else {
//        println("未找到IP信息。")
//    }
//}