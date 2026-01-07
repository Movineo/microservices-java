package com.p2p.realtime.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.trip-events}")
    private String tripEventsTopic;

    @Value("${kafka.topics.vehicle-location-updates}")
    private String vehicleLocationUpdatesTopic;

    @Bean
    public NewTopic tripEventsTopic() {
        return new NewTopic(tripEventsTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic vehicleLocationUpdatesTopic() {
        return new NewTopic(vehicleLocationUpdatesTopic, 3, (short) 1);
    }

}