package com.dattran.kafka_etl.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public class EnrichedCity {
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

    // Enriched fields
    @JsonProperty("population_density")
    private BigDecimal populationDensity;

    @JsonProperty("continent")
    private String continent;

    @JsonProperty("city_size_category")
    private String citySizeCategory; // SMALL, MEDIUM, LARGE, MEGA

    @JsonProperty("coordinates")
    private String coordinates; // "lat,lng" format

    @JsonProperty("country_code")
    private String countryCode; // ISO 3166-1 alpha-2

    @JsonProperty("age_category")
    private String ageCategory; // ANCIENT, MEDIEVAL, MODERN, CONTEMPORARY

    // Processing metadata
    @JsonProperty("processed_at")
    private Instant processedAt;

    @JsonProperty("processing_version")
    private String processingVersion;

    @JsonProperty("data_quality_score")
    private Double dataQualityScore;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;
}
