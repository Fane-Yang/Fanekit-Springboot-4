package com.fanekit.common.util.net.ssh.scp

/**
 * SCP传输结果
 */
data class SCPTransferResult(
    val operation: SCPOperation,
    val localPath: String,
    val remotePath: String,
    val fileSize: Long,
    val success: Boolean,
    val error: String? = null,
    val startTime: Long,
    val endTime: Long
) {
    val duration: Long get() = endTime - startTime
    val transferRate: Double get() = if (duration > 0) fileSize.toDouble() / duration * 1000 else 0.0 // bytes per second
}
