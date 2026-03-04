package com.fanekit.common.util.time

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * 时间工具类
 * 提供各种时间处理、格式化、转换等实用方法
 *
 * @author FaneKit  // DO NOT REPLACE
 * @date 2025/02/25
 * @since 1.0.0
 * @version 1.0.0
 */
object TimeUtils {

    // 常用日期时间格式
    const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
    const val DATE_PATTERN = "yyyy-MM-dd"
    const val TIME_PATTERN = "HH:mm:ss"
    const val DATETIME_PATTERN_COMPACT = "yyyyMMddHHmmss"
    const val DATE_PATTERN_COMPACT = "yyyyMMdd"
    const val DATETIME_PATTERN_MILLIS = "yyyy-MM-dd HH:mm:ss.SSS"
    const val ISO_DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss'Z'"

    // 常用格式化器
    val DEFAULT_DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)
    val DEFAULT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
    val DEFAULT_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN)
    val COMPACT_DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN_COMPACT)
    val COMPACT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN_COMPACT)
    val MILLIS_DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN_MILLIS)
    val ISO_DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(ISO_DATETIME_PATTERN)

    /**
     * 获取当前时间戳（毫秒）
     */
    fun currentTimeMillis(): Long = System.currentTimeMillis()

    /**
     * 获取当前时间戳（秒）
     */
    fun currentTimeSeconds(): Long = System.currentTimeMillis() / 1000

    /**
     * 获取当前时间戳（纳秒）
     */
    fun currentTimeNanos(): Long = System.nanoTime()

    /**
     * 获取当前LocalDateTime
     */
    fun now(): LocalDateTime = LocalDateTime.now()

    /**
     * 获取当前LocalDate
     */
    fun today(): LocalDate = LocalDate.now()

    /**
     * 获取当前LocalTime
     */
    fun currentTime(): LocalTime = LocalTime.now()

    /**
     * 格式化当前时间
     */
    fun formatNow(pattern: String = DATE_TIME_PATTERN): String {
        return now().format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 格式化LocalDateTime
     */
    fun format(dateTime: LocalDateTime, pattern: String = DATE_TIME_PATTERN): String {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 格式化LocalDate
     */
    fun format(date: LocalDate, pattern: String = DATE_PATTERN): String {
        return date.format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 格式化LocalTime
     */
    fun format(time: LocalTime, pattern: String = TIME_PATTERN): String {
        return time.format(DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 解析时间字符串为LocalDateTime
     */
    fun parseDateTime(dateTimeStr: String, pattern: String = DATE_TIME_PATTERN): LocalDateTime {
        return LocalDateTime.parse(dateTimeStr, DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 解析日期字符串为LocalDate
     */
    fun parseDate(dateStr: String, pattern: String = DATE_PATTERN): LocalDate {
        return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 解析时间字符串为LocalTime
     */
    fun parseTime(timeStr: String, pattern: String = TIME_PATTERN): LocalTime {
        return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern(pattern))
    }

    /**
     * 时间戳转LocalDateTime
     */
    fun timestampToDateTime(timestamp: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zone)
    }

    /**
     * LocalDateTime转时间戳
     */
    fun dateTimeToTimestamp(dateTime: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): Long {
        return dateTime.atZone(zone).toInstant().toEpochMilli()
    }

    /**
     * Date转LocalDateTime
     */
    fun dateToLocalDateTime(date: Date): LocalDateTime {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    /**
     * LocalDateTime转Date
     */
    fun localDateTimeToDate(dateTime: LocalDateTime): Date {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant())
    }

    /**
     * 获取今天开始时间 (00:00:00)
     */
    fun startOfToday(): LocalDateTime {
        return today().atStartOfDay()
    }

    /**
     * 获取今天结束时间 (23:59:59.999)
     */
    fun endOfToday(): LocalDateTime {
        return today().atTime(LocalTime.MAX)
    }

    /**
     * 获取本周第一天（周一）
     */
    fun startOfWeek(): LocalDate {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }

    /**
     * 获取本周最后一天（周日）
     */
    fun endOfWeek(): LocalDate {
        return today().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    }

    /**
     * 获取本月第一天
     */
    fun startOfMonth(): LocalDate {
        return today().with(TemporalAdjusters.firstDayOfMonth())
    }

    /**
     * 获取本月最后一天
     */
    fun endOfMonth(): LocalDate {
        return today().with(TemporalAdjusters.lastDayOfMonth())
    }

    /**
     * 获取本年第一天
     */
    fun startOfYear(): LocalDate {
        return today().with(TemporalAdjusters.firstDayOfYear())
    }

    /**
     * 获取本年最后一天
     */
    fun endOfYear(): LocalDate {
        return today().with(TemporalAdjusters.lastDayOfYear())
    }

    /**
     * 计算两个时间之间的天数差
     */
    fun daysBetween(start: LocalDate, end: LocalDate): Long {
        return ChronoUnit.DAYS.between(start, end)
    }

    /**
     * 计算两个时间之间的小时差
     */
    fun hoursBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return ChronoUnit.HOURS.between(start, end)
    }

    /**
     * 计算两个时间之间的分钟差
     */
    fun minutesBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return ChronoUnit.MINUTES.between(start, end)
    }

    /**
     * 计算两个时间之间的秒数差
     */
    fun secondsBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return ChronoUnit.SECONDS.between(start, end)
    }

    /**
     * 计算两个时间之间的毫秒差
     */
    fun millisBetween(start: LocalDateTime, end: LocalDateTime): Long {
        return ChronoUnit.MILLIS.between(start, end)
    }

    /**
     * 增加天数
     */
    fun plusDays(dateTime: LocalDateTime, days: Long): LocalDateTime {
        return dateTime.plusDays(days)
    }

    /**
     * 增加小时
     */
    fun plusHours(dateTime: LocalDateTime, hours: Long): LocalDateTime {
        return dateTime.plusHours(hours)
    }

    /**
     * 增加分钟
     */
    fun plusMinutes(dateTime: LocalDateTime, minutes: Long): LocalDateTime {
        return dateTime.plusMinutes(minutes)
    }

    /**
     * 增加秒数
     */
    fun plusSeconds(dateTime: LocalDateTime, seconds: Long): LocalDateTime {
        return dateTime.plusSeconds(seconds)
    }

    /**
     * 减少天数
     */
    fun minusDays(dateTime: LocalDateTime, days: Long): LocalDateTime {
        return dateTime.minusDays(days)
    }

    /**
     * 减少小时
     */
    fun minusHours(dateTime: LocalDateTime, hours: Long): LocalDateTime {
        return dateTime.minusHours(hours)
    }

    /**
     * 减少分钟
     */
    fun minusMinutes(dateTime: LocalDateTime, minutes: Long): LocalDateTime {
        return dateTime.minusMinutes(minutes)
    }

    /**
     * 减少秒数
     */
    fun minusSeconds(dateTime: LocalDateTime, seconds: Long): LocalDateTime {
        return dateTime.minusSeconds(seconds)
    }

    /**
     * 判断是否为闰年
     */
    fun isLeapYear(year: Int): Boolean {
        return Year.of(year).isLeap
    }

    /**
     * 判断是否为同一天
     */
    fun isSameDay(date1: LocalDate, date2: LocalDate): Boolean {
        return date1 == date2
    }

    /**
     * 判断是否为同一天
     */
    fun isSameDay(dateTime1: LocalDateTime, dateTime2: LocalDateTime): Boolean {
        return dateTime1.toLocalDate() == dateTime2.toLocalDate()
    }

    /**
     * 判断日期是否在指定范围内
     */
    fun isInRange(date: LocalDate, start: LocalDate, end: LocalDate): Boolean {
        return !date.isBefore(start) && !date.isAfter(end)
    }

    /**
     * 判断时间是否在指定范围内
     */
    fun isInRange(dateTime: LocalDateTime, start: LocalDateTime, end: LocalDateTime): Boolean {
        return !dateTime.isBefore(start) && !dateTime.isAfter(end)
    }

    /**
     * 获取年龄
     */
    fun getAge(birthDate: LocalDate, referenceDate: LocalDate = today()): Int {
        return Period.between(birthDate, referenceDate).years
    }

    /**
     * 格式化持续时间（毫秒）
     */
    fun formatDuration(millis: Long): String {
        val days = TimeUnit.MILLISECONDS.toDays(millis)
        val hours = TimeUnit.MILLISECONDS.toHours(millis) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60

        return when {
            days > 0 -> "${days}天${hours}小时${minutes}分${seconds}秒"
            hours > 0 -> "${hours}小时${minutes}分${seconds}秒"
            minutes > 0 -> "${minutes}分${seconds}秒"
            else -> "${seconds}秒"
        }
    }

    /**
     * 格式化持续时间（Duration）
     */
    fun formatDuration(duration: Duration): String {
        return formatDuration(duration.toMillis())
    }

    /**
     * 获取时区信息
     */
    fun getTimeZoneInfo(): Map<String, Any> {
        val zone = ZoneId.systemDefault()
        val offset = ZoneOffset.systemDefault().rules.getOffset(Instant.now())

        return mapOf(
            "zoneId" to zone.id,
            "displayName" to zone.getDisplayName(TextStyle.FULL, Locale.getDefault()),
            "offset" to offset.toString(),
            "offsetSeconds" to offset.totalSeconds
        )
    }

    /**
     * 转换时区
     */
    fun convertTimeZone(dateTime: LocalDateTime, fromZone: ZoneId, toZone: ZoneId): LocalDateTime {
        return dateTime.atZone(fromZone).withZoneSameInstant(toZone).toLocalDateTime()
    }

    /**
     * 获取Unix时间戳（秒）
     */
    fun getUnixTimestamp(dateTime: LocalDateTime = now()): Long {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond()
    }

    /**
     * Unix时间戳转LocalDateTime
     */
    fun fromUnixTimestamp(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneId.systemDefault().rules.getOffset(Instant.now()))
    }

    /**
     * 获取月份的天数
     */
    fun getDaysInMonth(year: Int, month: Int): Int {
        return YearMonth.of(year, month).lengthOfMonth()
    }

    /**
     * 获取年份的天数
     */
    fun getDaysInYear(year: Int): Int {
        return Year.of(year).length()
    }

    /**
     * 获取一周的第几天（1=周一，7=周日）
     */
    fun getDayOfWeek(date: LocalDate): Int {
        return date.dayOfWeek.value
    }

    /**
     * 获取一年的第几天
     */
    fun getDayOfYear(date: LocalDate): Int {
        return date.dayOfYear
    }

    /**
     * 获取一年的第几周
     */
    fun getWeekOfYear(date: LocalDate): Int {
        return date.get(java.time.temporal.WeekFields.ISO.weekOfYear())
    }

    /**
     * 睡眠指定毫秒数
     */
    fun sleep(millis: Long) {
        try {
            Thread.sleep(millis)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Sleep interrupted", e)
        }
    }

    /**
     * 执行代码块并计算耗时
     */
    inline fun <T> measureTime(block: () -> T): Pair<T, Long> {
        val start = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - start
        return Pair(result, elapsed)
    }

    /**
     * 执行代码块并计算耗时（纳秒精度）
     */
    inline fun <T> measureTimeNanos(block: () -> T): Pair<T, Long> {
        val start = System.nanoTime()
        val result = block()
        val elapsed = System.nanoTime() - start
        return Pair(result, elapsed)
    }
}
