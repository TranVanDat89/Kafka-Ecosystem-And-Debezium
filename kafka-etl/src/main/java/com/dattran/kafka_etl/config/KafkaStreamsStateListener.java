package com.dattran.kafka_etl.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.errors.StreamsUncaughtExceptionHandler;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.config.StreamsBuilderFactoryBeanConfigurer;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class KafkaStreamsStateListener implements StreamsBuilderFactoryBeanConfigurer {

    @Override
    public void configure(StreamsBuilderFactoryBean factoryBean) {
        factoryBean.setStateListener((newState, oldState) -> {
            log.info("Kafka Streams state changed from {} to {}", oldState, newState);

            if (newState == KafkaStreams.State.RUNNING) {
                log.info("Kafka Streams application is now running");
            } else if (newState == KafkaStreams.State.ERROR) {
                log.error("Kafka Streams application encountered an error");
            }
        });

        factoryBean.setStreamsUncaughtExceptionHandler((exception) -> {
            log.error("Uncaught exception in Kafka Streams: {}", exception.getMessage(), exception);
            return StreamsUncaughtExceptionHandler.StreamThreadExceptionResponse.REPLACE_THREAD;
        });
    }
}
