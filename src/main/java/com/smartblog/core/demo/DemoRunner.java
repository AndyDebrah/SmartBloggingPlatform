package com.smartblog.core.demo;

import com.smartblog.bootstrap.AppBootstrap;
import com.smartblog.bootstrap.AppBootstrap.Context;
import com.smartblog.core.dto.UserDTO;

import java.sql.SQLException;

/**
 * Console smoke-test entrypoint.
 * Uses the service layer + Hikari + Flyway so it mirrors real app wiring.
 */
public class DemoRunner {
    public static void main(String[] args) throws SQLException {
        Context ctx = AppBootstrap.start();

        var users = ctx.userService;

        String username = "teacherAndy";
        var existing = users.findByUsername(username);
        long userId;
        if (existing.isPresent()) {
            UserDTO dto = existing.get();
            userId = dto.id();
            System.out.println("User already exists: id=" + userId + " username=" + dto.username());
        } else {
            userId = users.register(username, "teacher.andy@example.com", "P@ssw0rd!", "AUTHOR");
            System.out.println("Created user id=" + userId);
        }

        users.get(userId).ifPresent(u ->
                System.out.println("Fetched: " + u.username() + " " + u.email()));

        boolean updated = users.updateProfile(userId, "andy.updated@example.com");
        System.out.println("Profile updated: " + updated);

        System.out.println("All users (first page):");
        users.list(1, 10).forEach(u ->
                System.out.println(" - " + u.id() + " " + u.username() + " " + u.email()));

        // Close pool on exit
        ctx.ds.unwrap(com.zaxxer.hikari.HikariDataSource.class).close();
    }
}
