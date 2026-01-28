package com.smartblog.infrastructure.migration;

import java.io.InputStream;
import java.util.Properties;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.smartblog.infrastructure.nosql.MongoClientFactory;

public class VerifyMongoComments {
    public static void main(String[] args) throws Exception {
        Properties p = new Properties();
        try (InputStream in = VerifyMongoComments.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) throw new IllegalStateException("application.properties not found");
            p.load(in);
        }
        String uri = p.getProperty("mongodb.uri", "mongodb://localhost:27017");
        String dbName = p.getProperty("mongodb.database", "smart_blog_nosql");

        MongoDatabase db = MongoClientFactory.getDatabase(uri, dbName);
        MongoCollection<Document> coll = db.getCollection("comments");

        System.out.println("Latest comments in MongoDB (collection: comments):");
        for (Document d : coll.find().sort(new Document("createdAt", -1)).limit(5)) {
            System.out.println(d.toJson());
        }
    }
}
