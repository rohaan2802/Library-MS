package com.library.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BorrowRequestSchemaMigrationService {

    private static final Logger log = LoggerFactory.getLogger(BorrowRequestSchemaMigrationService.class);
    private static final String CONSTRAINT_NAME = "chk_borrow_requests_duration";
    private static final String EXPECTED_CLAUSE = "requested_duration_days in (7,14,21,28)";

    private final JdbcTemplate jdbcTemplate;

    public BorrowRequestSchemaMigrationService(
            JdbcTemplate jdbcTemplate, EntityManagerFactory entityManagerFactory) {
        this.jdbcTemplate = jdbcTemplate;
        Objects.requireNonNull(entityManagerFactory, "JPA must initialize before schema migration");
    }

    @PostConstruct
    public void ensureBorrowRequestDurationConstraint() {
        try {
            String currentClause = jdbcTemplate.query(
                    """
                    select lower(replace(replace(cc.check_clause, '`', ''), ' ', ''))
                    from information_schema.table_constraints tc
                    join information_schema.check_constraints cc
                      on cc.constraint_schema = tc.constraint_schema
                     and cc.constraint_name = tc.constraint_name
                    where tc.constraint_schema = database()
                      and tc.table_name = 'borrow_requests'
                      and tc.constraint_name = ?
                      and tc.constraint_type = 'CHECK'
                    """,
                    ps -> ps.setString(1, CONSTRAINT_NAME),
                    rs -> rs.next() ? rs.getString(1) : null);

            if (EXPECTED_CLAUSE.equals(currentClause)) {
                return;
            }

            if (currentClause == null) {
                log.info("Adding borrow request duration check on empty/cloud database.");
                jdbcTemplate.execute(
                        """
                        alter table borrow_requests
                            add constraint chk_borrow_requests_duration
                            check (requested_duration_days in (7, 14, 21, 28))
                        """);
                return;
            }

            log.info("Updating borrow request duration constraint to allow 7, 14, 21, and 28 days.");
            jdbcTemplate.execute(
                    """
                    alter table borrow_requests
                        drop check chk_borrow_requests_duration,
                        add constraint chk_borrow_requests_duration
                        check (requested_duration_days in (7, 14, 21, 28))
                    """);
        } catch (DataAccessException ex) {
            Throwable root = ex.getMostSpecificCause();
            log.warn(
                    "Borrow-request constraint migration skipped ({}). App will continue on a fresh database.",
                    root != null ? root.getMessage() : ex.getMessage());
        }
    }
}
