package com.fanekit.common.util.net.ssh.command

import net.schmizz.sshj.connection.channel.direct.Session


/**
 * SSH交互式会话
 */
class SSHInteractiveSession(private val session: Session) : AutoCloseable {

    private val shell by lazy { session.startShell() }

    /**
     * 发送命令到shell
     */
    fun sendCommand(command: String) {
        shell.outputStream.write("$command\n".toByteArray())
        shell.outputStream.flush()
    }

    /**
     * 读取输出
     */
    fun readOutput(timeoutMs: Long = 5000): String {
        val buffer = ByteArray(1024)
        val result = StringBuilder()
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (shell.inputStream.available() > 0) {
                val bytesRead = shell.inputStream.read(buffer)
                if (bytesRead > 0) {
                    result.append(String(buffer, 0, bytesRead))
                }
            } else {
                Thread.sleep(100)
            }
        }

        return result.toString()
    }

    override fun close() {
        shell.close()
        session.close()
    }
}
