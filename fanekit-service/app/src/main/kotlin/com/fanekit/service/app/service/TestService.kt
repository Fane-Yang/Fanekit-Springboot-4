//package com.fanekit.service.app.service
//
//import com.fanekit.components.spring.data.kafka.producer.KafkaProducer
//import lombok.RequiredArgsConstructor
//import org.springframework.stereotype.Service
//
//@Service
//@RequiredArgsConstructor
//class TestService(private final val kafkaProducerService: KafkaProducer) {
//    fun test(){
//        kafkaProducerService.sendMessageSync("test_topic1", "test", "111")
//    }
//}