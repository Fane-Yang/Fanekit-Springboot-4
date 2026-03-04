package com.fanekit.common.util.qr

import com.fanekit.common.util.encryption.Base64Utils
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.*
import javax.imageio.ImageIO
import java.io.File
import java.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 二维码生成工具类
 * 支持自定义背景色和前景色
 */
object QRCodeUtil {

    /**
     * 生成二维码
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param foregroundColor 前景色（默认黑色）
     * @param backgroundColor 背景色（默认白色）
     * @param outputFile 输出文件
     */
    fun generateQRCode(
        content: String,
        width: Int,
        height: Int,
        foregroundColor: Color = Color.BLACK,
        backgroundColor: Color = Color.WHITE,
        outputFile: File
    ) {
        val qrCodeWriter = QRCodeWriter()
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1 // 设置二维码边距

        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bufferedImage.setRGB(
                    x, y,
                    if (bitMatrix[x, y]) foregroundColor.rgb else backgroundColor.rgb
                )
            }
        }

        // 输出到文件
        ImageIO.write(bufferedImage, "png", outputFile)
    }

    /**
     * 生成二维码并返回Base64字符串
     * @param content 二维码内容
     * @param width 宽度
     * @param height 高度
     * @param foregroundColor 前景色（支持#开头的颜色值，默认黑色）
     * @param backgroundColor 背景色（支持#开头的颜色值，默认白色）
     * @return Base64字符串
     */
    fun generateQRCodeBase64(
        content: String,
        width: Int,
        height: Int,
        foregroundColor: String = "#000000",
        backgroundColor: String = "#FFFFFF"
    ): String {
        val qrCodeWriter = QRCodeWriter()
        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        hints[EncodeHintType.MARGIN] = 1 // 设置二维码边距

        val bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints)
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)

        val fgColor = Color.decode(foregroundColor)
        val bgColor = Color.decode(backgroundColor)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bufferedImage.setRGB(
                    x, y,
                    if (bitMatrix[x, y]) fgColor.rgb else bgColor.rgb
                )
            }
        }

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", outputStream)
        return Base64.getEncoder().encodeToString(outputStream.toByteArray())
    }

    /**
     * 解析二维码内容
     * @param base64QRCode Base64编码的二维码图片
     * @return 二维码内容
     */
    fun decodeQRCodeFromBase64(base64QRCode: String): String? {
        val decodedBytes = Base64.getDecoder().decode(base64QRCode)
        val inputStream = ByteArrayInputStream(decodedBytes)
        val bufferedImage = ImageIO.read(inputStream)
        return decodeQRCodeFromImage(bufferedImage)
    }

    /**
     * 解析二维码内容
     * @param file 二维码图片文件
     * @return 二维码内容
     */
    fun decodeQRCodeFromFile(file: File): String? {
        val bufferedImage = ImageIO.read(file)
        return decodeQRCodeFromImage(bufferedImage)
    }

    /**
     * 从BufferedImage解析二维码内容
     * @param bufferedImage 二维码图片
     * @return 二维码内容
     */
    private fun decodeQRCodeFromImage(bufferedImage: BufferedImage): String? {
        val source = BufferedImageLuminanceSource(bufferedImage)
        val bitmap = BinaryBitmap(HybridBinarizer(source))
        return try {
            val result = MultiFormatReader().decode(bitmap)
            result.text
        } catch (e: Exception) {
            println("解析二维码失败: ${e.message}")
            null
        }
    }
}
