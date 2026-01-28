package com.smartblog;

import com.smartblog.bootstrap.AppBootstrap;

public class CreateUser {
    public static void main(String[] args) {
        var ctx = AppBootstrap.start();
        
        // Create admin user
        try {
            long id = ctx.userService.register("admin", "admin@smartblog.com", "admin123", "ADMIN");
            System.out.println("Created admin user with ID: " + id);
            System.out.println("Login with username: admin, password: admin123");
        } catch (Exception e) {
            System.out.println("Admin user already exists");
        }
        
        // Create author user
        try {
            long id = ctx.userService.register("author", "author@smartblog.com", "author123", "AUTHOR");
            System.out.println("Created author user with ID: " + id);
            System.out.println("Login with username: author, password: author123");
        } catch (Exception e) {
            System.out.println("Author user already exists");
        }
    }
}
