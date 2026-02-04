
package com.smartblog.infrastructure.datasource;

import java.util.Properties;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Factory for creating and managing a singleton HikariCP DataSource.
 * Configures connection pooling from application properties.
 */
public final class DataSourceFactory {
    private static HikariDataSource ds;

    /**
     * Gets or creates the singleton DataSource instance.
     * 
     * @param props application properties containing database configuration
     * @return configured DataSource instance
     */
    public static synchronized DataSource get(Properties props) {
        if (ds != null) return ds;

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(props.getProperty("db.url"));
        cfg.setUsername(props.getProperty("db.user"));
        cfg.setPassword(props.getProperty("db.password"));
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.max", "15")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.min", "2")));
        cfg.setIdleTimeout(Long.parseLong(props.getProperty("db.pool.idleTimeoutMs", "600000")));
        cfg.setMaxLifetime(Long.parseLong(props.getProperty("db.pool.maxLifetimeMs", "1800000")));
        cfg.setPoolName("SmartBlog-HikariPool");

        ds = new HikariDataSource(cfg);
        return ds;
    }

    /**
     * Closes the DataSource if it exists.
     */
    public static synchronized void close() {
        if (ds != null) ds.close();
    }
}
