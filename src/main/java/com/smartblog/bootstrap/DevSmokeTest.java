package com.smartblog.bootstrap;

import com.smartblog.core.dto.PostDTO;
import com.smartblog.core.exceptions.NotAuthorizedException;

public class DevSmokeTest {
    public static void main(String[] args) {
        var ctx = AppBootstrap.start();

        long uid;
        try {
            uid = ctx.userService.register("prodAndy", "prod.andy@example.com", "S3cure_Pass!", "AUTHOR");
            System.out.println("Created user id: " + uid);
        } catch (RuntimeException e) {
            uid = ctx.userService.findByUsername("prodAndy").orElseThrow().id();
            System.out.println("Existing user id: " + uid);
        }

        long pid = ctx.postService.createDraft(uid, "Production-grade Phase 2", "This is a real blogging platform.");
        System.out.println("Created post id: " + pid);

        try {
            ctx.postService.publish(pid);
        } catch (NotAuthorizedException nae) {
            System.out.println("Publish skipped: " + nae.getMessage());
        }

        for (PostDTO p : ctx.postService.list(1, 10)) {
            System.out.println("Post: " + p.id() + " | " + p.title() + " | published=" + p.published());
        }

        long tagId;
        try {
            tagId = ctx.tagService.create("JavaFX");
        } catch (RuntimeException e) {
            tagId = ctx.tagService.listAll().stream()
                    .filter(t -> "javafx".equalsIgnoreCase(t.name()))
                    .findFirst()
                    .map(t -> t.id())
                    .orElseThrow(() -> e);
        }
        ctx.tagService.assignToPost(pid, tagId);
        System.out.println("Tag 'JavaFX' added to post " + pid);

        long cid = ctx.commentService.add(pid, uid, "Looks great!");
        System.out.println("Comment id: " + cid);

        System.out.println("Comments: " + ctx.commentService.listForPost(pid, 1, 10).size());
    }
}
