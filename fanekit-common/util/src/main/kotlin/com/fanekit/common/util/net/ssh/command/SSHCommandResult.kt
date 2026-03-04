package com.fanekit.common.util.net.ssh.command


/**
 * SSH命令执行结果
 */
data class SSHCommandResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executionTime: Long
) {
    val isSuccess: Boolean get() = exitCode == 0
    val hasError: Boolean get() = stderr.isNotEmpty()
}