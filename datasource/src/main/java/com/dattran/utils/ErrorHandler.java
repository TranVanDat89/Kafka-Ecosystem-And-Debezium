package com.dattran.utils;

import com.dattran.config.ConfigurationManager;

public class ErrorHandler {
    private final int maxRetries;
    private final long retryDelayMs;

    public ErrorHandler() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        this.maxRetries = config.getInt("error.max.retries", 3);
        this.retryDelayMs = config.getLong("error.retry.delay.ms", 1000);
    }

    public <T> T executeWithRetry(RetryableOperation<T> operation, String operationName) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                System.err.printf("Attempt %d/%d failed for %s: %s%n",
                        attempt, maxRetries, operationName, e.getMessage());

                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(retryDelayMs * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                }
            }
        }

        throw new RuntimeException("Operation failed after " + maxRetries + " attempts", lastException);
    }

    @FunctionalInterface
    public interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
