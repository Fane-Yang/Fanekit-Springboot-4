package com.fanekit.common.util.captcha

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.random.Random
import java.util.Base64

enum class CaptchaType {
    CALCULATION, CHINESE, ENGLISH
}

data class CaptchaResult(
    val content: String,        // 展示在图片上的内容，如 2 + 3 × 4
    val answer: String,         // 正确答案，计算题是数字，文字题是展示内容
    val base64Image: String     // 图片Base64
)

object CaptchaUtil {

    private val chineseWords = "演员导演编剧镜头字幕舞台剧话剧片名".toCharArray()
    private val englishLetters = ('A'..'Z') + ('a'..'z')

    /**
     * 生成验证码
     */
    fun generateCaptcha(
        type: CaptchaType,
        width: Int = 160,
        height: Int = 60,
        fontSize: Int = 36,
        operators: List<Char> = listOf('+', '-', '*', '/'),
        minNum: Int = 1,
        maxNum: Int = 10,
        allowMultiExpr: Boolean = true
    ): CaptchaResult {
        val (text, answer) = when (type) {
            CaptchaType.CALCULATION -> genExpression(operators, minNum, maxNum, allowMultiExpr)
            CaptchaType.CHINESE -> genChineseCaptcha(4)
            CaptchaType.ENGLISH -> genEnglishCaptcha(5)
        }
        val image = generateImage(text, width, height, fontSize)
        val base64 = imageToBase64(image)
        return CaptchaResult(content = text, answer = answer, base64Image = base64)
    }

    // 生成计算表达式及结果
    private fun genExpression(
        ops: List<Char>, min: Int, max: Int, allowMulti: Boolean
    ): Pair<String, String> {
        val numOps = if (allowMulti) Random.nextInt(1, 3) else 1
        val sb = StringBuilder()
        var result = Random.nextInt(min, max + 1).toDouble()
        sb.append(result.toInt())

        repeat(numOps) {
            val op = ops.random()
            val next = Random.nextInt(min, max + 1).toDouble()
            sb.append(" $op ").append(next.toInt())
            result = when (op) {
                '+' -> result + next
                '-' -> result - next
                '*' -> result * next
                '/' -> if (next == 0.0) result else result / next
                else -> result
            }
        }

        val expr = sb.toString()
        val finalResult = if (result % 1 == 0.0) result.toInt().toString() else String.format("%.2f", result)
        return Pair(expr, finalResult)
    }

    private fun genChineseCaptcha(len: Int): Pair<String, String> {
        val text = (1..len).joinToString("") { chineseWords.random().toString() }
        return Pair(text, text)
    }

    private fun genEnglishCaptcha(len: Int): Pair<String, String> {
        val text = (1..len).joinToString("") { englishLetters.random().toString() }
        return Pair(text, text)
    }

    private fun generateImage(text: String, width: Int, height: Int, fontSize: Int): BufferedImage {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        try {
            g.color = Color.WHITE
            g.fillRect(0, 0, width, height)

            applyDistortion(g, width, height)
            applyNoise(g, width, height)

            val font = Font("Arial", Font.BOLD, fontSize)
            g.font = font
            val fm = g.fontMetrics

            val x = (width - fm.stringWidth(text)) / 2
            val y = (height - fm.height) / 2 + fm.ascent

            // 文字扭曲绘制
            for ((i, c) in text.withIndex()) {
                val theta = Random.nextDouble(-0.3, 0.3)
                val transform = AffineTransform()
                transform.rotate(theta, x.toDouble(), y.toDouble())
                val old = g.transform
                g.transform = transform
                g.color = randomColor()
                g.drawString(c.toString(), x + i * fontSize, y)
                g.transform = old
            }

        } finally {
            g.dispose()
        }
        return image
    }

    private fun applyNoise(g: Graphics2D, w: Int, h: Int) {
        repeat(10) {
            g.color = randomColor()
            val x1 = Random.nextInt(w)
            val y1 = Random.nextInt(h)
            val x2 = x1 + Random.nextInt(-30, 30)
            val y2 = y1 + Random.nextInt(-30, 30)
            g.drawLine(x1, y1, x2, y2)
        }
        repeat(20) {
            g.color = randomColor()
            g.fillOval(Random.nextInt(w), Random.nextInt(h), 5, 5)
        }
    }

    private fun applyDistortion(g: Graphics2D, width: Int, height: Int) {
        val waveHeight = 5
        val image = g.deviceConfiguration.createCompatibleImage(width, height)
        val tempGraphics = image.createGraphics()
        tempGraphics.drawImage(g.deviceConfiguration.createCompatibleImage(width, height), 0, 0, null)
        tempGraphics.dispose()
        val data = image.raster

        for (y in 0 until height) {
            val offset = (waveHeight * Math.sin(2.0 * Math.PI * y / 64)).toInt()
            g.copyArea(0, y, width, 1, offset, 0)
        }
    }

    private fun imageToBase64(image: BufferedImage): String {
        val output = ByteArrayOutputStream()
        ImageIO.write(image, "png", output)
        return Base64.getEncoder().encodeToString(output.toByteArray())
    }

    private fun randomColor(): Color {
        val r = Random.nextInt(50, 200)
        val g = Random.nextInt(50, 200)
        val b = Random.nextInt(50, 200)
        return Color(r, g, b)
    }
}


fun main() {
    val result = CaptchaUtil.generateCaptcha(
        type = CaptchaType.CALCULATION,
        operators = listOf('+', '*', '-'),
        minNum = 1,
        maxNum = 20,
        allowMultiExpr = true
    )
    println("表达式: ${result.content}")
    println("答案: ${result.answer}")
    println("base64: ${result.base64Image}")
    println("Base64图像: ${result.base64Image.take(30)}...") // 可用于 <img src="data:image/png;base64,...">
}
