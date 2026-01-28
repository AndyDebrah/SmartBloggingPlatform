package com.smartblog.infrastructure.nosql;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

public final class MongoClientFactory {
    private static volatile MongoClient client;

    private MongoClientFactory() {}

    public static MongoDatabase getDatabase(String uri, String dbName) {
        if (client == null) {
            synchronized (MongoClientFactory.class) {
                if (client == null) client = MongoClients.create(uri);
            }
        }
        return client.getDatabase(dbName);
    }

    public static void close() {
        if (client != null) { client.close(); client = null; }
    }
}