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
public class MultipartUploadResponse {
    private String uploadId;
    private String key;
    private int totalParts;
    private List<PresignedUrlInfo> presignedUrls;
}
