package com.dattran.kafka_etl.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CityEvent {
    private String op; // c=create, u=update, d=delete, r=read

    @JsonProperty("ts_ms")
    private Long timestampMs;

    private CityPayload before;
    private CityPayload after;

    private Source source;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CityPayload {
        private Long id;
        private String name;
        private String country;
        private String region;
        private Integer population;

        @JsonProperty("area_km2")
        private BigDecimal areaKm2;

        private BigDecimal latitude;
        private BigDecimal longitude;
        private String timezone;

        @JsonProperty("is_capital")
        private Boolean isCapital;

        @JsonProperty("founded_year")
        private Integer foundedYear;

        private String description;

        @JsonProperty("created_at")
        private Instant createdAt;

        @JsonProperty("updated_at")
        private Instant updatedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Source {
        private String version;
        private String connector;
        private String name;

        @JsonProperty("ts_ms")
        private Long timestampMs;

        private String snapshot;
        private String db;
        private String sequence;
        private String table;

        @JsonProperty("server_id")
        private Long serverId;

        private String gtid;
        private String file;
        private Long pos;
        private Integer row;
        private Long thread;
        private String query;
    }
}
