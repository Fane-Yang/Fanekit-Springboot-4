package com.fanekit.service.app.controller

import com.fanekit.service.app.service.MyMessageProducer
import com.fanekit.service.app.service.Order
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/rabbit")
class RabbitTestController(private val myMessageProducer: MyMessageProducer) {

    @GetMapping("/send")
    fun send(@RequestParam id: String?): String {
        val order = Order(
            id = id ?: UUID.randomUUID().toString(),
            orderNo = "ORD-" + System.currentTimeMillis(),
            amount = 99.9
        )
        myMessageProducer.sendOrderMessage(order)
        return "Message sent: ${order.id}"
    }

    @GetMapping("/sendDelay")
    fun sendDelay(@RequestParam id: String?): String {
        val orderId = id ?: UUID.randomUUID().toString()
        myMessageProducer.sendDelayMessage(orderId)
        return "Delay message sent: $orderId"
    }
}

