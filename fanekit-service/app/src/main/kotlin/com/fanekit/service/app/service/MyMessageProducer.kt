package com.fanekit.service.app.service

import com.alibaba.fastjson2.JSON
import com.fanekit.data.rabbitmq.producer.AbstractRabbitMqProducer
import org.springframework.stereotype.Service

@Service
class MyMessageProducer : AbstractRabbitMqProducer() {
    fun sendOrderMessage(order: Order) {
        // 自行序列化为 String 发送
        val jsonString = JSON.toJSONString(order)
        send("order.exchange", "order.created", jsonString)
    }

    fun sendDelayMessage(orderId: String) {
        // 发送延时消息 (5秒)
        sendDelayed("delay.exchange", "order.timeout", orderId, 5000)
    }
}
