package com.library.config;

import jakarta.persistence.EntityManagerFactory;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class CloudTimestampDefaultsFix implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CloudTimestampDefaultsFix.class);

    private final JdbcTemplate jdbcTemplate;

    public CloudTimestampDefaultsFix(JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        Objects.requireNonNull(entityManagerFactory);
    }

    @Override
    public void run(ApplicationArguments args) {
        alter(
                "registration_requests",
                "ALTER TABLE registration_requests MODIFY submitted_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)");
        alter(
                "notifications",
                "ALTER TABLE notifications MODIFY created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)");
        alter(
                "deletion_requests",
                "ALTER TABLE deletion_requests MODIFY requested_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)");
    }

    private void alter(String table, String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            Throwable root = ex.getMostSpecificCause();
            log.warn(
                    "Could not add timestamp default on {} ({}). App will still set timestamps in Java.",
                    table,
                    root != null ? root.getMessage() : ex.getMessage());
        }
    }
}
