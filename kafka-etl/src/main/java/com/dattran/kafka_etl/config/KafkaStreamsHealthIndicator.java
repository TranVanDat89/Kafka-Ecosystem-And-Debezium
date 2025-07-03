package com.dattran.kafka_etl.config;

import org.apache.kafka.streams.KafkaStreams;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

//@Component
public class KafkaStreamsHealthIndicator implements HealthIndicator {
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public KafkaStreamsHealthIndicator(StreamsBuilderFactoryBean streamsBuilderFactoryBean) {
        this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
    }

    @Override
    public Health health() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        if (kafkaStreams == null) {
            return Health.down()
                    .withDetail("kafka-streams", "Not initialized")
                    .build();
        }

        KafkaStreams.State state = kafkaStreams.state();

        if (state == KafkaStreams.State.RUNNING) {
            return Health.up()
                    .withDetail("kafka-streams", state.name())
                    .withDetail("threads", kafkaStreams.metadataForLocalThreads().size())
                    .build();
        } else {
            return Health.down()
                    .withDetail("kafka-streams", state.name())
                    .build();
        }
    }
}
