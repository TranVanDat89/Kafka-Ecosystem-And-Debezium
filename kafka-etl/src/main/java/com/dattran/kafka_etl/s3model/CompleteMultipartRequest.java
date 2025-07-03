package com.dattran.kafka_etl.s3model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompleteMultipartRequest {
    private String key;
    private String uploadId;
    private List<CompletedPartInfo> parts;
}
