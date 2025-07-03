package com.dattran.kafka_etl.s3model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PresignedUrlInfo {
    private int partNumber;
    private String url;
}
