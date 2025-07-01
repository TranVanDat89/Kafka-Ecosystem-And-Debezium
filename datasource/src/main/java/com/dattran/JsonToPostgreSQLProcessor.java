package com.dattran;

import com.dattran.config.ConfigurationManager;
import com.dattran.utils.ErrorHandler;
import com.dattran.utils.PerformanceMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class JsonToPostgreSQLProcessor {
    protected static final int CHUNK_SIZE = 10000; // Số records mỗi batch
    protected static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    protected static final int CONNECTION_POOL_SIZE = THREAD_POOL_SIZE * 2;
    protected static final long FILE_CHUNK_SIZE = 50 * 1024 * 1024; // 50MB mỗi chunk

    // Database connection pool
    protected HikariDataSource dataSource;
    private final ExecutorService executorService;
    private final ObjectMapper objectMapper;
    private final AtomicLong processedRecords = new AtomicLong(0);

    // Enhanced features
    private final PerformanceMonitor monitor;
    private final ErrorHandler errorHandler;
    private final ConfigurationManager configManager;

    public JsonToPostgreSQLProcessor() {
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.objectMapper = new ObjectMapper();
        // Initialize enhanced features
        this.monitor = new PerformanceMonitor();
        this.errorHandler = new ErrorHandler();
        this.configManager = ConfigurationManager.getInstance();
        setupConnectionPool();
    }

    private void setupConnectionPool() {
        HikariConfig config = new HikariConfig();

        // Kết nối DB
        assert configManager != null;
        config.setJdbcUrl(configManager.getString("db.url", ""));
        config.setUsername(configManager.getString("db.username", ""));
        config.setPassword(configManager.getString("db.password", ""));

        // Connection pool settings
        config.setMaximumPoolSize(configManager.getInt("db.pool.maximum", 10));
        config.setMinimumIdle(configManager.getInt("db.pool.minimum", 5));
        config.setConnectionTimeout(configManager.getLong("db.pool.connection.timeout", 30000));
        config.setIdleTimeout(configManager.getLong("db.pool.idle.timeout", 600000));
        config.setMaxLifetime(configManager.getLong("db.pool.max.lifetime", 1800000));
        config.setLeakDetectionThreshold(configManager.getLong("db.pool.leak.detection.threshold", 60000));

        // PostgreSQL Optimization
        config.addDataSourceProperty("cachePrepStmts", configManager.getBoolean("db.cache.prep.stmts", true));
        config.addDataSourceProperty("prepStmtCacheSize", configManager.getInt("db.prep.stmt.cache.size", 250));
        config.addDataSourceProperty("prepStmtCacheSqlLimit", configManager.getInt("db.prep.stmt.cache.sql.limit", 2048));
        config.addDataSourceProperty("useServerPrepStmts", configManager.getBoolean("db.use.server.prep.stmts", true));
        config.addDataSourceProperty("rewriteBatchedStatements", configManager.getBoolean("db.rewrite.batched.statements", true));

        this.dataSource = new HikariDataSource(config);
    }

    public void processLargeJsonFile(String filePath, String tableName) throws Exception {
        System.out.println("=== Enhanced JSON Processor Starting ===");
        System.out.println("Configuration:");
        System.out.println("- Chunk Size: " + configManager.getInt("processing.chunk.size", CHUNK_SIZE));
        System.out.println("- Thread Pool Size: " + configManager.getInt("processing.thread.pool.size", THREAD_POOL_SIZE));
        System.out.println("- File Chunk Size: " + configManager.getLong("processing.file.chunk.size", FILE_CHUNK_SIZE));
        System.out.println("- Connection Pool Size: " + configManager.getInt("db.pool.maximum", CONNECTION_POOL_SIZE));

        try {
            // Pre-processing checks
            validateFile(filePath);
            validateDatabaseConnection();

            // Process file with error handling
            errorHandler.executeWithRetry(() -> {
                processLargeJsonFileInternal(filePath, tableName);
                return null;
            }, "File Processing");

        } catch (Exception e) {
            System.err.println("Processing failed: " + e.getMessage());
            throw e;
        } finally {
            monitor.shutdown();
        }
    }

    private void processLargeJsonFileInternal(String filePath, String tableName) throws Exception {
        System.out.println("Bắt đầu xử lý file: " + filePath);

        // Tạo bảng nếu chưa tồn tại
        createTableIfNotExists(tableName);

        File file = new File(filePath);
        long fileSize = file.length();
        System.out.println("Kích thước file: " + formatBytes(fileSize));

        // Chia file thành chunks và xử lý song song
        List<FileChunk> chunks = createFileChunks(file);
        System.out.println("Chia file thành " + chunks.size() + " chunks");

        // Sử dụng CompletableFuture để xử lý song song
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final FileChunk chunk = chunks.get(i);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    processFileChunk(chunk, tableName, chunkIndex);
                } catch (Exception e) {
                    System.err.println("Lỗi xử lý chunk " + chunkIndex + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, executorService);

            futures.add(future);
        }

        // Chờ tất cả chunks hoàn thành
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        System.out.println("Hoàn thành! Đã xử lý " + processedRecords.get() + " records");
    }

    private void validateFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filePath);
        }
        if (!file.canRead()) {
            throw new IOException("Cannot read file: " + filePath);
        }

        // Check available disk space
        long freeSpace = file.getParentFile().getFreeSpace();
        long fileSize = file.length();

        if (freeSpace < fileSize * 0.1) { // Need at least 10% free space
            System.out.println("WARNING: Low disk space. Available: " +
                    formatBytes(freeSpace) + ", File size: " + formatBytes(fileSize));
        }
    }

    private void validateDatabaseConnection() throws Exception {
        errorHandler.executeWithRetry(() -> {
            try (Connection conn = dataSource.getConnection()) {
                if (!conn.isValid(5)) {
                    throw new SQLException("Database connection validation failed");
                }
                return null;
            }
        }, "Database Connection Validation");
    }

    private List<FileChunk> createFileChunks(File file) throws IOException {
        List<FileChunk> chunks = new ArrayList<>();
        long fileSize = file.length();
        long currentPos = 0;
        int chunkId = 0;

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            while (currentPos < fileSize) {
                long chunkStart = currentPos;
                long chunkEnd = Math.min(currentPos + FILE_CHUNK_SIZE, fileSize);

                // Điều chỉnh để không cắt giữa JSON object
                if (chunkEnd < fileSize) {
                    raf.seek(chunkEnd);
                    // Tìm điểm kết thúc JSON object gần nhất
                    while (chunkEnd < fileSize) {
                        int ch = raf.read();
                        chunkEnd++;
                        if (ch == '\n' || ch == '\r') {
                            break;
                        }
                    }
                }

                FileChunk chunk = new FileChunk(chunkId++, chunkStart, chunkEnd);
                chunk.setFilePath(file.getAbsolutePath());
                chunks.add(chunk);
                currentPos = chunkEnd;
            }
        }

        return chunks;
    }

    private void processFileChunk(FileChunk chunk, String tableName, int chunkIndex) throws Exception {
        System.out.println("Bắt đầu xử lý chunk " + chunkIndex +
                " (từ " + chunk.start + " đến " + chunk.end + ")");

        List<JsonNode> batch = new ArrayList<>();
        long chunkProcessed = 0;

        try (RandomAccessFile raf = new RandomAccessFile(chunk.getFilePath(), "r")) {
            raf.seek(chunk.start);

            StringBuilder jsonBuilder = new StringBuilder();
            long currentPos = chunk.start;

            while (currentPos < chunk.end) {
                String line = raf.readLine();
                if (line == null) break;

                currentPos = raf.getFilePointer();
                line = line.trim();

                if (!line.isEmpty()) {
                    try {
                        JsonNode jsonNode = objectMapper.readTree(line);
                        batch.add(jsonNode);
                        chunkProcessed++;

                        // Ghi batch khi đủ kích thước
                        if (batch.size() >= CHUNK_SIZE) {
                            insertBatch(batch, tableName);
                            batch.clear();

                            long total = processedRecords.addAndGet(CHUNK_SIZE);
                            if (total % 50000 == 0) {
                                System.out.println("Đã xử lý: " + total + " records");
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Lỗi parse JSON ở chunk " + chunkIndex + ": " + line);
                    }
                }
            }

            // Ghi batch cuối cùng
            if (!batch.isEmpty()) {
                insertBatch(batch, tableName);
                processedRecords.addAndGet(batch.size());
            }
        }

        System.out.println("Hoàn thành chunk " + chunkIndex + ": " + chunkProcessed + " records");
    }

    protected void insertBatch(List<JsonNode> batch, String tableName) throws SQLException {
        try {
            errorHandler.executeWithRetry(() -> {
                insertBatchInternal(batch, tableName);
                monitor.recordProcessed(batch.size());
                return null;
            }, "Batch Insert");
        } catch (Exception e) {
            monitor.recordError();
            if (e instanceof SQLException) {
                throw (SQLException) e;
            } else {
                throw new SQLException("Batch insert failed", e);
            }
        }
    }

    private void insertBatchInternal(List<JsonNode> batch, String tableName) throws SQLException {
        String sql = generateInsertSQL(tableName, batch.get(0));

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);

            for (JsonNode record : batch) {
                setParametersFromJson(pstmt, record);
                pstmt.addBatch();
            }

            pstmt.executeBatch();
            conn.commit();

        } catch (SQLException e) {
            System.err.println("Lỗi insert batch: " + e.getMessage());
            throw e;
        }
    }

    private String generateInsertSQL(String tableName, JsonNode sampleRecord) {
        return "INSERT INTO " + tableName + " (review_id, user_id, business_id, stars, useful, funny, cool, text, date) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    private void setParametersFromJson(PreparedStatement pstmt, JsonNode record) throws SQLException {
        // 1. review_id (VARCHAR)
        JsonNode reviewId = record.get("review_id");
        pstmt.setString(1, reviewId != null && !reviewId.isNull() ? reviewId.asText() : null);

        // 2. user_id (VARCHAR)
        JsonNode userId = record.get("user_id");
        pstmt.setString(2, userId != null && !userId.isNull() ? userId.asText() : null);

        // 3. business_id (VARCHAR)
        JsonNode businessId = record.get("business_id");
        pstmt.setString(3, businessId != null && !businessId.isNull() ? businessId.asText() : null);

        // 4. stars (INTEGER)
        JsonNode stars = record.get("stars");
        if (stars != null && !stars.isNull()) {
            pstmt.setInt(4, stars.asInt());
        } else {
            pstmt.setNull(4, Types.INTEGER);
        }

        // 5. useful (INTEGER)
        JsonNode useful = record.get("useful");
        if (useful != null && !useful.isNull()) {
            pstmt.setInt(5, useful.asInt());
        } else {
            pstmt.setNull(5, Types.INTEGER);
        }

        // 6. funny (INTEGER)
        JsonNode funny = record.get("funny");
        if (funny != null && !funny.isNull()) {
            pstmt.setInt(6, funny.asInt());
        } else {
            pstmt.setNull(6, Types.INTEGER);
        }

        // 7. cool (INTEGER)
        JsonNode cool = record.get("cool");
        if (cool != null && !cool.isNull()) {
            pstmt.setInt(7, cool.asInt());
        } else {
            pstmt.setNull(7, Types.INTEGER);
        }

        // 8. text (TEXT)
        JsonNode text = record.get("text");
        pstmt.setString(8, text != null && !text.isNull() ? text.asText() : null);

        // 9. date (TIMESTAMP)
        JsonNode date = record.get("date");
        if (date != null && !date.isNull()) {
            try {
                // Thử parse date string thành Timestamp
                String dateStr = date.asText();
                // Giả sử format là "YYYY-MM-DD HH:mm:ss" hoặc ISO format
                Timestamp timestamp = Timestamp.valueOf(dateStr.replace("T", " ").replace("Z", ""));
                pstmt.setTimestamp(9, timestamp);
            } catch (Exception e) {
                System.err.println("Lỗi parse date: " + date.asText() + " - " + e.getMessage());
                pstmt.setNull(9, Types.TIMESTAMP);
            }
        } else {
            pstmt.setNull(9, Types.TIMESTAMP);
        }
    }

    private void createTableIfNotExists(String tableName) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "review_id VARCHAR(50), " +
                "user_id VARCHAR(50), " +
                "business_id VARCHAR(50), " +
                "stars INTEGER, " +
                "useful INTEGER, " +
                "funny INTEGER, " +
                "cool INTEGER, " +
                "text TEXT, " +
                "date TIMESTAMP" +
                ")";

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            // Tạo các index cho performance
            String[] indexSqls = {
                    "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_review_id ON " + tableName + " (review_id)",
                    "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_user_id ON " + tableName + " (user_id)",
                    "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_business_id ON " + tableName + " (business_id)",
                    "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_date ON " + tableName + " (date)",
                    "CREATE INDEX IF NOT EXISTS idx_" + tableName + "_stars ON " + tableName + " (stars)"
            };

            for (String indexSql : indexSqls) {
                stmt.execute(indexSql);
            }
        }
    }

    protected String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }

        if (dataSource != null) {
            dataSource.close();
        }

        if (monitor != null) {
            monitor.shutdown();
        }
    }

    // Inner class cho File Chunk
    private static class FileChunk {
        private final int id;
        private final long start;
        private final long end;
        private String filePath;

        public FileChunk(int id, long start, long end) {
            this.id = id;
            this.start = start;
            this.end = end;
        }

        // Getters
        public int getId() { return id; }
        public long getStart() { return start; }
        public long getEnd() { return end; }
        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}
