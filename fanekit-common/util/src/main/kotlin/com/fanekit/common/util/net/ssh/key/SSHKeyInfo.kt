package com.fanekit.common.util.net.ssh.key

/**
 * SSH密钥信息
 */
data class SSHKeyInfo(
    val keyType: com.fanekit.common.util.net.ssh.key.SSHKeyType,
    val keySize: Int,
    val fingerprint: String,
    val comment: String
)
