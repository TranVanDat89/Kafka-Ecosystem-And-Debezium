package com.dattran.kafka_etl.s3model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompletedPartInfo {
    private int partNumber;
    @JsonProperty("eTag")
    private String eTag;
}
