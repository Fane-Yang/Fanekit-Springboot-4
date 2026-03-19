package com.fanekit.data.rabbitmq.config

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONReader
import com.alibaba.fastjson2.JSONWriter
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.support.converter.AbstractMessageConverter
import org.springframework.amqp.support.converter.MessageConversionException
import org.springframework.amqp.support.converter.SmartMessageConverter
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.nio.charset.Charset

/**
 * FastJson2 消息转换器 - 支持 SmartMessageConverter 以便处理带类型的消息
 */
class FastJson2MessageConverter : AbstractMessageConverter(), SmartMessageConverter {

    private val log = LoggerFactory.getLogger(FastJson2MessageConverter::class.java)

    override fun createMessage(objectToSend: Any, messageProperties: MessageProperties): Message {
        try {
            val jsonBytes = JSON.toJSONBytes(objectToSend, JSONWriter.Feature.WriteMapNullValue)
            messageProperties.contentType = MessageProperties.CONTENT_TYPE_JSON
            messageProperties.contentEncoding = Charset.defaultCharset().name()
            messageProperties.contentLength = jsonBytes.size.toLong()
            return Message(jsonBytes, messageProperties)
        } catch (e: Exception) {
            throw MessageConversionException("Failed to convert object to Message", e)
        }
    }

    override fun fromMessage(message: Message): Any {
        return fromMessage(message, null)
    }

    override fun fromMessage(message: Message, conversionHint: Any?): Any {
        return try {
            val content = message.body
            val charset = getCharset(message.messageProperties)
            val jsonString = String(content, charset)

            // 如果监听器参数是 String 类型，直接返回字符串，不进行反序列化
            if (conversionHint == String::class.java) {
                return jsonString
            }

            when (conversionHint) {
                is ParameterizedType -> JSON.parseObject(jsonString, conversionHint)
                is Class<*> -> JSON.parseObject(jsonString, conversionHint)
                is Type -> JSON.parseObject(jsonString, conversionHint)
                else -> JSON.parse(jsonString, JSONReader.Feature.SupportSmartMatch)
            }!!
        } catch (e: Exception) {
            log.error("FastJson2 conversion failed", e)
            throw MessageConversionException("Failed to convert Message to object", e)
        }
    }

    private fun getCharset(messageProperties: MessageProperties): Charset {
        val contentEncoding = messageProperties.contentEncoding
        return if (contentEncoding != null) Charset.forName(contentEncoding) else Charset.defaultCharset()
    }
}

