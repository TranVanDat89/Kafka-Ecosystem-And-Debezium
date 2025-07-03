package com.dattran.kafka_etl.topology;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//@Configuration
public class CitiesKafkaTopology {
    @Value("${kafka.topics.cities-cdc}")
    private String citiesCdcTopic;

    @Value("${kafka.topics.cities-enriched}")
    private String citiesEnrichedTopic;

    @Bean
    public KStream<String, String> citiesStream(StreamsBuilder builder) {

        // Step 1: Đọc từ topic CDC đầu vào
        KStream<String, String> sourceStream = builder.stream(citiesCdcTopic);

        // Step 2: Enrichment (ví dụ thêm thông tin)
        KStream<String, String> enrichedStream = sourceStream.mapValues(value -> enrichCity(value));

        // Step 3: Validate (ví dụ filter city hợp lệ)
        KStream<String, String> validatedStream = enrichedStream.filter((key, value) -> validateCity(value));

        // Step 4: Aggregation (ví dụ đơn giản đếm số bản ghi theo key)
        validatedStream
                .groupByKey()
                .count(Materialized.as("cities-counts"))
                .toStream()
                .foreach((key, count) -> System.out.println("City: " + key + " -> Count: " + count));

        // Step 5: Ghi output ra topic
        validatedStream.to(citiesEnrichedTopic);

        return validatedStream;
    }

    private String enrichCity(String value) {
        // Logic enrich tùy bạn (ví dụ thêm chuỗi "[ENRICHED]")
        return value + " [ENRICHED]";
    }

    private boolean validateCity(String value) {
        // Giả định đơn giản hợp lệ nếu value không rỗng
        return value != null && !value.trim().isEmpty();
    }
}
