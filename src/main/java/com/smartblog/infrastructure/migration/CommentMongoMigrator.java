package com.smartblog.infrastructure.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Properties;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.smartblog.infrastructure.nosql.MongoClientFactory;

public class CommentMongoMigrator {

    public static void main(String[] args) throws Exception {
        // Usage: run before enabling NoSQL; reads application.properties from classpath
        Properties p = new Properties();
        try (var in = CommentMongoMigrator.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) throw new IllegalStateException("application.properties not found on classpath");
            p.load(in);
        }

        boolean enabled = Boolean.parseBoolean(p.getProperty("comments.nosql.enabled", "false"));
        String mongoUri = p.getProperty("mongodb.uri", "mongodb://localhost:27017");
        String mongoDb = p.getProperty("mongodb.database", "smart_blog_nosql");


        MongoDatabase db = MongoClientFactory.getDatabase(mongoUri, mongoDb);
        migrateAll(db);

        if (!enabled) {
            System.out.println("Note: comments.nosql.enabled=false — you can enable it after migration.");
        }
    }

    public static void migrateAll(MongoDatabase db) throws Exception {
        MongoCollection<Document> coll = db.getCollection("comments");

        // create simple indexes for query performance
        coll.createIndex(new Document("postId", 1), new IndexOptions().background(true));
        coll.createIndex(new Document("userId", 1), new IndexOptions().background(true));
        coll.createIndex(new Document("createdAt", 1), new IndexOptions().background(true));

        String sql = "SELECT id, post_id, user_id, content, created_at FROM comments";
        try (Connection c = com.smartblog.core.config.ConnectionManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            int migrated = 0;
            while (rs.next()) {
                long mysqlId = rs.getLong("id");
                long postId = rs.getLong("post_id");
                long userId = rs.getLong("user_id");
                String content = rs.getString("content");
                java.sql.Timestamp ts = rs.getTimestamp("created_at");

                Document doc = new Document();
                doc.append("mysqlId", mysqlId);
                doc.append("postId", postId);
                doc.append("userId", userId);
                doc.append("content", content);
                if (ts != null) {
                    doc.append("createdAt", ts.toInstant().toEpochMilli());
                } else {
                    doc.append("createdAt", ZonedDateTime.now(ZoneOffset.UTC).toInstant().toEpochMilli());
                }

                coll.insertOne(doc);
                ObjectId oid = doc.getObjectId("_id");
                migrated++;
                if (migrated % 500 == 0) System.out.println("Migrated " + migrated + " comments (last mysqlId=" + mysqlId + ", mongoId=" + oid + ")");
            }
            System.out.println("Migration complete. Total migrated: " + migrated);
        }
    }
}
