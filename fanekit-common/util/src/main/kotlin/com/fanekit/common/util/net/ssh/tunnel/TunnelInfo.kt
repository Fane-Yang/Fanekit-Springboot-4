package com.fanekit.common.util.net.ssh.tunnel

/**
 * 隧道信息
 */
data class TunnelInfo(
    val id: String,
    val type: TunnelType,
    val localPort: Int,
    val remoteHost: String? = null,
    val remotePort: Int? = null,
    val forwarder: LocalPortForwarderClient? = null,
    val boundPort: Int? = null,
    val proxy: SOCKSProxy? = null
)
