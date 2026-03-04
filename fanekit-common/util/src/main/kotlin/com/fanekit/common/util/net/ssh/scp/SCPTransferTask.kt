package com.fanekit.common.util.net.ssh.scp

/**
 * SCP传输任务
 */
data class SCPTransferTask(
    val operation: SCPOperation,
    val localPath: String,
    val remotePath: String
)