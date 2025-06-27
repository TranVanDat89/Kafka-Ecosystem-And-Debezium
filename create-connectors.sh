#!/bin/bash
set -e
KAFKA_CONNECT_URL="http://localhost:8083"
# Hàm tạo connector
create_connector() {
  local NAME=$1
  local PAYLOAD=$2
  echo "🔎 Kiểm tra connector [$NAME]..."
  # Kiểm tra connector đã tồn tại chưa
  if curl -s -o /dev/null -w "%{http_code}" ${KAFKA_CONNECT_URL}/connectors/${NAME} | grep -q "200"; then
    echo "❌ Connector [$NAME] đã tồn tại. Không thể tạo lại."
    exit 1
  fi
  echo "🚀 Đang tạo connector [$NAME]..."
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST ${KAFKA_CONNECT_URL}/connectors \
    -H "Content-Type: application/json" \
    -d "${PAYLOAD}")
  if [ "$HTTP_CODE" -ne 201 ]; then
    echo "❌ Lỗi khi tạo connector [$NAME] (HTTP $HTTP_CODE)"
    exit 1
  fi
  echo "✅ Connector [$NAME] tạo thành công."
}

# Payload Debezium PostgreSQL Source Connector
DEBEZIUM_PAYLOAD_JSON='{
  "name": "city-postgres-cdc",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "root",
    "database.password": "root123",
    "database.dbname": "city_db",
    "database.server.name": "cities_server",
    "plugin.name": "pgoutput",
    "slot.name": "debezium",
    "table.include.list": "public.cities",
    "decimal.handling.mode": "string",
    "topic.prefix": "cities"
  }
}'

DEBEZIUM_PAYLOAD_AVRO='{
  "name": "postgres-debezium-avro",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": 5432,
    "database.user": "root",
    "database.password": "root123",
    "database.dbname": "ecommerce_db",
    "database.server.name": "cities_server",
    "table.include.list": "public.cities",
    "plugin.name": "pgoutput",
    "publication.name": "debezium_publication",
    "slot.name": "cities_debezium_slot",
    "topic.prefix": "cities",
    "schema.name.adjustment.mode": "avro",
    "field.name.adjustment.mode": "avro"
  }
}'

# Payload Elasticsearch Sink Connector
ELASTICSEARCH_PAYLOAD_JSON='{
  "name": "elasticsearch-sink",
  "config": {
    "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "topics": "cities.public.cities",
    "connection.url": "http://elasticsearch:9200",
    "type.name": "_doc",
    "key.ignore": "false",
    "schema.ignore": true,

    "behavior.on.null.values": "delete",
    "transforms": "unwrap,extractKeyField",
    "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState",
    "transforms.unwrap.drop.tombstones": "false",
    "transforms.unwrap.delete.handling.mode": "rewrite",
    "transforms.extractKeyField.type": "org.apache.kafka.connect.transforms.ExtractField$Key",
    "transforms.extractKeyField.field": "id"
  }
}'

# Payload Elasticsearch Sink Connector
ELASTICSEARCH_PAYLOAD_AVRO='{
  "name": "elasticsearch-sink",
  "config": {
    "connector.class": "io.confluent.connect.elasticsearch.ElasticsearchSinkConnector",
    "tasks.max": "1",
    "topics": "cities.public.cities",
    "connection.url": "http://elasticsearch:9200",
    "connection.username": "",
    "connection.password": "",
    "type.name": "_doc",
    "key.ignore": "true",
    "drop.invalid.message": false,
    "schema.ignore": false,
    "topic.index.map": "cities.public.cities:cities_index",
    "errors.tolerance": "all",
    "errors.log.enable": true,
    "errors.log.include.messages": true,
    "batch.size": 2000,
    "max.in.flight.requests": 5,
    "flush.timeout.ms": 10000,
    "behavior.on.null.values": "ignore",
    "behavior.on.malformed.documents": "warn"
  }
}'

# Tạo từng connector, tạo đầu source
create_connector "city-postgres-cdc" "$DEBEZIUM_PAYLOAD_JSON"
create_connector "elasticsearch-sink" "$ELASTICSEARCH_PAYLOAD_JSON"

echo "🎉 Tất cả connectors đã tạo thành công."
