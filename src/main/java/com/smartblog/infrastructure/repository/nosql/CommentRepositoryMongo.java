package com.smartblog.infrastructure.repository.nosql;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.smartblog.core.model.Comment;
import com.smartblog.infrastructure.nosql.MongoClientFactory;

/**
 * Minimal Mongo-backed repository for comments used during dual-write.
 * Stores MySQL id (mysqlId) when available so MySQL remains source-of-truth.
 */
public class CommentRepositoryMongo {
    private final MongoCollection<Document> col;

    public CommentRepositoryMongo(String uri, String dbName) {
        MongoDatabase db = MongoClientFactory.getDatabase(uri, dbName);
        this.col = db.getCollection("comments");
    }

    public void save(Comment c) {
        Document d = new Document();
        if (c.getId() > 0) d.append("mysqlId", c.getId());
        d.append("postId", c.getPostId());
        d.append("userId", c.getUserId());
        d.append("content", c.getContent());
        if (c.getCreatedAt() != null) {
            long epoch = c.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            d.append("createdAt", epoch);
        } else {
            d.append("createdAt", System.currentTimeMillis());
        }
        col.insertOne(d);
        try {
            Object idObj = d.get("_id");
            String oid = null;
            if (idObj != null) oid = idObj.toString();
            if (oid != null) {
                c.setMongoId(oid);
                System.out.println("[Mongo] inserted comment _id=" + oid + " mysqlId=" + d.get("mysqlId"));
            }
        } catch (Exception ignored) {}
    }

    public List<Comment> listByPost(long postId, int page, int size) {
        List<Comment> out = new ArrayList<>();
        int offset = Math.max(0, (page - 1) * size);
        var cursor = col.find(Filters.eq("postId", postId))
                .skip(offset)
                .limit(size)
                .iterator();
        try (cursor) {
            while (cursor.hasNext()) {
                Document doc = cursor.next();
                Comment c = new Comment();
                if (doc.containsKey("mysqlId")) {
                    Object mid = doc.get("mysqlId");
                    if (mid instanceof Number) c.setId(((Number) mid).intValue());
                }
                if (doc.containsKey("_id")) {
                    var o = doc.get("_id");
                    if (o != null) c.setMongoId(o.toString());
                }
                if (doc.containsKey("postId")) {
                    Object pidObj = doc.get("postId");
                    if (pidObj instanceof Number) c.setPostId(((Number) pidObj).intValue());
                }
                if (doc.containsKey("userId")) {
                    Object uidObj = doc.get("userId");
                    if (uidObj instanceof Number) c.setUserId(((Number) uidObj).intValue());
                }
                if (doc.containsKey("content")) c.setContent(doc.getString("content"));
                if (doc.containsKey("createdAt")) {
                    Object msObj = doc.get("createdAt");
                    long ms = 0L;
                    if (msObj instanceof Number) ms = ((Number) msObj).longValue();
                    else if (msObj instanceof String) {
                        try { ms = Long.parseLong((String) msObj); } catch (Exception ignored) {}
                    }
                    LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ms), ZoneId.systemDefault());
                    c.setCreatedAt(dt);
                }
                out.add(c);
            }
        }
        return out;
    }
}