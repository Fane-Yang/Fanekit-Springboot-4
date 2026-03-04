package com.fanekit.common.util.net.ssh.key

import java.security.KeyPair

/**
 * SSH密钥对信息
 */
data class SSHKeyPairInfo(
    val keyType: com.fanekit.common.util.net.ssh.key.SSHKeyType,
    val keySize: Int,
    val keyPair: KeyPair,
    val comment: String,
    val fingerprint: String
)