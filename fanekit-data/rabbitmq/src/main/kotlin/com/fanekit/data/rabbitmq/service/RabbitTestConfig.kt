//package com.fanekit.data.rabbitmq.service
//
//import org.springframework.amqp.core.Binding
//import org.springframework.amqp.core.BindingBuilder
//import org.springframework.amqp.core.DirectExchange
//import org.springframework.amqp.core.Queue
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//
//@Configuration
//class RabbitTestConfig {
//
//    companion object {
//        const val ORDER_QUEUE = "order.queue"
//        const val ORDER_EXCHANGE = "order.exchange"
//        const val ORDER_ROUTING_KEY = "order.created"
//    }
//
//    @Bean
//    fun orderQueue(): Queue {
//        return Queue(ORDER_QUEUE, true)
//    }
//
//    @Bean
//    fun orderExchange(): DirectExchange {
//        return DirectExchange(ORDER_EXCHANGE)
//    }
//
//    @Bean
//    fun orderBinding(orderQueue: Queue, orderExchange: DirectExchange): Binding {
//        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_ROUTING_KEY)
//    }
//}
//
