package com.dattran.kafka_etl.controller;

import com.dattran.kafka_etl.s3model.AbortMultipartRequest;
import com.dattran.kafka_etl.s3model.CompleteMultipartRequest;
import com.dattran.kafka_etl.s3model.InitMultipartRequest;
import com.dattran.kafka_etl.s3model.MultipartUploadResponse;
import com.dattran.kafka_etl.services.S3MultipartUploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/s3")
public class S3UploadController {
    private final S3MultipartUploadService uploadService;

    public S3UploadController(S3MultipartUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/init-multipart")
    public ResponseEntity<MultipartUploadResponse> initMultipartUpload(
            @RequestBody InitMultipartRequest request) {

        MultipartUploadResponse response = uploadService.initializeMultipartUpload(
                request.getFileName(),
                request.getFileSize()
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-multipart")
    public ResponseEntity<String> completeMultipartUpload(
            @RequestBody CompleteMultipartRequest request) {

        uploadService.completeMultipartUpload(
                request.getKey(),
                request.getUploadId(),
                request.getParts()
        );

        return ResponseEntity.ok("Upload completed successfully");
    }

    @PostMapping("/abort-multipart")
    public ResponseEntity<String> abortMultipartUpload(
            @RequestBody AbortMultipartRequest request) {

        uploadService.abortMultipartUpload(request.getKey(), request.getUploadId());
        return ResponseEntity.ok("Upload aborted");
    }
}
