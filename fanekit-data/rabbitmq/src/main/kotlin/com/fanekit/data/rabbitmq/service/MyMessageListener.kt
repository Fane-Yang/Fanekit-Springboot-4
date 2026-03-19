//package com.fanekit.data.rabbitmq.service
//
//import com.alibaba.fastjson2.JSON
//import com.fanekit.data.rabbitmq.consumer.AbstractRabbitMqListener
//import com.rabbitmq.client.Channel
//import org.springframework.amqp.rabbit.annotation.RabbitListener
//import org.springframework.amqp.support.AmqpHeaders
//import org.springframework.messaging.handler.annotation.Header
//import org.springframework.stereotype.Component
//
//
//@Component
//class MyMessageListener : AbstractRabbitMqListener() {
//
//    @RabbitListener(queues = ["order.queue"])
//    fun onOrderCreated(message: String, channel: Channel, @Header(AmqpHeaders.DELIVERY_TAG) tag: Long) {
//        // 使用 processMessage 自动处理 ACK/NACK
//        processMessage<String>(message, channel, tag) { msg ->
//            // 自行反序列化为对象使用
//            try {
//                val order = JSON.parseObject(msg, Order::class.java)
//                println("收到订单: ${order.id}, 订单号: ${order.orderNo}, 金额: ${order.amount}")
//            } catch (e: Exception) {
//                log.error("反序列化订单消息失败: $msg", e)
//                throw e
//            }
//        }
//    }
//}
