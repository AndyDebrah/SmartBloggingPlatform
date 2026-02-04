package com.smartblog.infrastructure.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Factory for creating and managing a singleton MongoDB client.
 * Thread-safe lazy initialization of MongoClient with double-checked locking.
 */
public final class MongoClientFactory {
    private static volatile MongoClient client;

    private MongoClientFactory() {}

    /**
     * Gets a MongoDB database instance, creating the client if needed.
     * 
     * @param uri MongoDB connection URI
     * @param dbName database name
     * @return MongoDB database instance
     */
    public static MongoDatabase getDatabase(String uri, String dbName) {
        if (client == null) {
            synchronized (MongoClientFactory.class) {
                if (client == null) client = MongoClients.create(uri);
            }
        }
        return client.getDatabase(dbName);
    }

    /**
     * Closes the MongoDB client if it exists.
     */
    public static void close() {
        if (client != null) { client.close(); client = null; }
    }
}