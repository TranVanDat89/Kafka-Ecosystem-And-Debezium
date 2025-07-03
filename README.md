## Architecture

```bash
PostgreSQL (Debezium Source Connector)
          │
          ▼
        Kafka ──────────▶ Elasticsearch (Sink Connector)
```
## Upload Large File To S3
- /init-multipart => Khởi tạo multipart upload, chia thành các part 100MB, tạo presignUrl cho từng part.
- Upload từng part một lên S3.
- /complete-multipart => Gộp các part lại thành file hoàn chỉnh.
- /abort-multipart => Hủy quá trình upload.
- Chú ý: Cần cấu hình CORS cho S3 Bucket, expose ETag. ETag đóng vai trò như một "checksum" để đảm bảo dữ liệu được truyền tải một cách chính xác và complete trong hệ thống multipart upload.

## Upcomming
- Bài toán xử lý dữ liệu với Kafka Streams
```bash
PostgreSQL → Debezium → Kafka → [ETL Processors] → Multiple Sinks
                                      ↓
                            ┌─────────────────────┐
                            │ Kafka Streams Apps  │
                            │ - Data Validation   │
                            │ - Enrichment        │
                            │ - Aggregation       │
                            │ - Routing           │
                            └─────────────────────┘
                                      ↓
                    ┌─────────────────────────────────────┐
                    │        Multiple Outputs             │
                    │ - Elasticsearch (search)            │
                    │ - ClickHouse (analytics)            │
                    │ - Redis (caching)                   │
                    │ - S3 (data lake)                    │
                    └─────────────────────────────────────┘
```