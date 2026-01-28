package com.smartblog.core.config;


import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;



/**
 * Simple Connection Manager that loads db.properties from classpath.
 * Uses DriverManager directly (sufficient for this project phase).
 */


public class ConnectionManager {

    private  static String URL;
    private  static String USER;
    private  static String PASSWORD;
    private  static String DRIVER;
    private  static boolean DRIVER_PRESENT = false;

    static {
        try (InputStream in = ConnectionManager.class.getClassLoader().getResourceAsStream("db.properties")) {

            if (in == null) {
                throw new RuntimeException("db.properties not found in classpath");
            }

                Properties p = new Properties();
                p.load(in);

                URL = p.getProperty("db.url");
                USER = p.getProperty("db.user");
                PASSWORD = p.getProperty("db.password");
                DRIVER = p.getProperty("db.driver");

                // If a driver class is provided, try loading it to register with DriverManager
                if (DRIVER != null && !DRIVER.isBlank()) {
                    try {
                        Class.forName(DRIVER);
                        DRIVER_PRESENT = true;
                    } catch (ClassNotFoundException e) {
                        // Do not fail initialization — driver may be provided on the classpath/module-path
                        System.err.println("Warning: JDBC Driver class not found: " + DRIVER + ". If you expect to use JDBC, ensure the driver JAR is on the runtime classpath/module-path.");
                        DRIVER_PRESENT = false;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to load db.properties", e);
            }
        }
    public static Connection getConnection() throws SQLException {
        // If driver wasn't explicitly loaded, DriverManager may still locate it via service loader if it's on the runtime classpath/module-path.
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            // Enhance the exception message with a hint about missing JDBC driver
            if (!DRIVER_PRESENT && DRIVER != null && !DRIVER.isBlank()) {
                throw new SQLException(e.getMessage() + " (Hint: JDBC driver '" + DRIVER + "' was not found during initialization)", e.getSQLState(), e.getErrorCode(), e);
            }
            throw e;
        }
    }

    }
