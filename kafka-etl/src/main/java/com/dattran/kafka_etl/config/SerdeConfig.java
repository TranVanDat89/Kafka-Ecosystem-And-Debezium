package com.dattran.kafka_etl.config;

import com.dattran.kafka_etl.model.CityEvent;
import com.dattran.kafka_etl.model.EnrichedCity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
public class SerdeConfig {
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Serde<JsonNode> jsonNodeSerde() {
        return new JsonSerde<>(JsonNode.class);
    }

    @Bean
    public Serde<CityEvent> cityEventSerde() {
        return new JsonSerde<>(CityEvent.class);
    }

    @Bean
    public Serde<EnrichedCity> enrichedCitySerde() {
        return new JsonSerde<>(EnrichedCity.class);
    }

    @Bean
    public Serde<String> stringSerde() {
        return Serdes.String();
    }
}
