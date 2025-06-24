package com.dattran.kafka_etl.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityAggregate {
    private String aggregateKey; // country, region, continent, etc.
    private String aggregateValue;

    @JsonProperty("city_count")
    private Long cityCount;

    @JsonProperty("total_population")
    private Long totalPopulation;

    @JsonProperty("average_population")
    private Double averagePopulation;

    @JsonProperty("total_area")
    private BigDecimal totalArea;

    @JsonProperty("capital_count")
    private Long capitalCount;

    @JsonProperty("largest_city")
    private String largestCity;

    @JsonProperty("smallest_city")
    private String smallestCity;

    @JsonProperty("average_founded_year")
    private Double averageFoundedYear;

    @JsonProperty("window_start")
    private Instant windowStart;

    @JsonProperty("window_end")
    private Instant windowEnd;

    @JsonProperty("last_updated")
    private Instant lastUpdated;
}
