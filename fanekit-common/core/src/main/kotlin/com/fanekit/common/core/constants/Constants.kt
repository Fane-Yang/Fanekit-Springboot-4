package com.fanekit.common.core.constants

import java.util.*

object Constants {
    // Mapper包的路径
    const val MAPPER_PACKAGE = "business.mapper"
    const val YES = 1
    const val NO = 0

    const val DELETED = 1
    const val UNDELETED = 0

    /**
     * 通用分隔符
     */
    const val SEPARATOR = ","

    /**
     * UTF-8 字符集
     */
    const val UTF8 = "UTF-8"

    /**
     * GBK 字符集
     */
    const val GBK = "GBK"

    /**
     * 系统语言
     */

    val DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE

    /**
     * http请求
     */
    const val HTTP = "http://"

    /**
     * https请求
     */
    const val HTTPS = "https://"

    /**
     * 通用成功标识
     */
    const val SUCCESS = "0"

    /**
     * 通用失败标识
     */
    const val FAIL = "1"

    /**
     * 登录成功
     */
    const val LOGIN_SUCCESS = "Success"

    /**
     * 注销
     */
    const val LOGOUT = "Logout"

    /**
     * 注册
     */
    const val REGISTER = "Register"

    /**
     * 登录失败
     */
    const val LOGIN_FAIL = "Error"

    /**
     * 系统用户授权缓存
     */
    const val SYS_AUTH_CACHE = "sys-authCache"

    /**
     * 参数管理 cache name
     */
    const val SYS_CONFIG_CACHE = "sys-config"

    /**
     * 参数管理 cache key
     */
    const val SYS_CONFIG_KEY = "sys_config:"

    /**
     * 字典管理 cache name
     */
    const val SYS_DICT_CACHE = "sys-dict"

    /**
     * 字典管理 cache key
     */
    const val SYS_DICT_KEY = "sys_dict:"

    /**
     * 资源映射路径 前缀
     */
    const val RESOURCE_PREFIX = "/profile"

    /**
     * RMI 远程方法调用
     */
    const val LOOKUP_RMI = "rmi:"

    /**
     * LDAP 远程方法调用
     */
    const val LOOKUP_LDAP = "ldap:"

    /**
     * LDAPS 远程方法调用
     */
    const val LOOKUP_LDAPS = "ldaps:"

    /**
     * 定时任务白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     */

    val JOB_WHITELIST_STR = arrayOf("com.ruoyi.quartz.task")

    /**
     * 定时任务违规的字符
     */

    val JOB_ERROR_STR = arrayOf(
        "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
        "org.springframework", "org.apache", "com.ruoyi.common.utils.file", "com.ruoyi.common.config", "com.ruoyi.generator"
    )

}
