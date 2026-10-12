package fr.auclairdeso.billing;

import java.util.Locale;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

/**
 * Bill numbers: "2026-0001", "2026-0002"... one series per year, without gaps.
 * <p>
 * One row per year holds the last number. The upsert locks that row until the end of the transaction:
 * two bills issued at the same moment wait for each other instead of taking the same number, and a bill
 * whose transaction fails gives its number back.
 */
@Component
class BillNumbers {

    private final JdbcOperations jdbc;

    BillNumbers(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    String next(int year) {
        var sequence = jdbc.queryForObject("""
                INSERT INTO bill_number (year, last_number) VALUES (?, 1)
                ON CONFLICT (year) DO UPDATE SET last_number = bill_number.last_number + 1
                RETURNING last_number
                """, Integer.class, year);
        return String.format(Locale.ROOT, "%d-%04d", year, sequence);
    }
}
