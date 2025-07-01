package com.dattran;

public class Main {
    public static void main(String[] args) {
        String filePath = "D:/Backend/Yelp JSON/yelp_dataset/yelp_academic_dataset_review.json";
        String tableName = "reviews";
        JsonToPostgreSQLProcessor processor = new JsonToPostgreSQLProcessor();
        try {
            long startTime = System.currentTimeMillis();
            processor.processLargeJsonFile(filePath, tableName);
            long endTime = System.currentTimeMillis();
            System.out.println("Thời gian xử lý: " + (endTime - startTime) / 1000.0 + " giây");
        } catch (Exception e) {
            System.err.println("Lỗi: " + e.getMessage());
            e.printStackTrace();
        } finally {
            processor.shutdown();
        }
    }
}