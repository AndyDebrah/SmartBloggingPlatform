
package com.smartblog.infrastructure.migration;

import java.util.Properties;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;

/**
 * Executes Flyway SQL migrations at boot.
 * Keeps schema versioned & in sync across environments.
 */
public final class MigrationRunner {
    public static  void migrate(DataSource ds, Properties props) {
        boolean enabled = Boolean.parseBoolean(props.getProperty("flyway.enabled", "true"));
        if (!enabled) return;

        String locations = props.getProperty("flyway.locations", "classpath:db/migration");
        Thread.currentThread().setContextClassLoader(MigrationRunner.class.getClassLoader());
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations(locations)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }
}
