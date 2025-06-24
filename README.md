## Architecture

```bash
PostgreSQL (Debezium Source Connector)
          │
          ▼
        Kafka ──────────▶ Elasticsearch (Sink Connector)
```

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