package com.typing.backend.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class TypingService {

    private static final String[] PROMPTS = {
            "The quick brown fox jumps over the lazy dog.",
            "Practice makes perfect when learning a new skill.",
            "Actions speak louder than words in most situations.",
            "Where there is a will, there is a way.",
            "Better late than never."
    };

    private final JdbcTemplate jdbcTemplate;
    private final Random random = new Random();
    private final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TypingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getPrompt() {
        return PROMPTS[random.nextInt(PROMPTS.length)];
    }

    public void saveResult(double speed, double accuracy) {
        ensureSchema();
        jdbcTemplate.update(
                "INSERT INTO typing_speed (speed, accuracy, tested_at) VALUES (?, ?, ?)",
                speed,
                accuracy,
                Timestamp.valueOf(LocalDateTime.now()));
    }

    public List<Map<String, Object>> getScores() {
        ensureSchema();
        return jdbcTemplate.query(
                "SELECT speed, COALESCE(accuracy, 100) AS accuracy, tested_at FROM typing_speed ORDER BY tested_at DESC LIMIT 10",
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("speed", rs.getDouble("speed"));
                    row.put("accuracy", rs.getDouble("accuracy"));
                    Timestamp testedAt = rs.getTimestamp("tested_at");
                    String formattedTimestamp = testedAt != null
                            ? testedAt.toLocalDateTime().format(timestampFormatter)
                            : null;
                    row.put("timestamp", formattedTimestamp);
                    row.put("date", formattedTimestamp);
                    return row;
                });
    }

    private void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS typing_speed (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    speed DOUBLE NOT NULL,
                    accuracy DOUBLE NULL,
                    tested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        try {
            jdbcTemplate.execute("ALTER TABLE typing_speed ADD COLUMN accuracy DOUBLE NULL");
        } catch (Exception ignored) {
        }

        try {
            jdbcTemplate.execute("ALTER TABLE typing_speed ADD COLUMN tested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP");
        } catch (Exception ignored) {
        }

        try {
            jdbcTemplate.execute("UPDATE typing_speed SET tested_at = NOW() WHERE tested_at IS NULL");
        } catch (Exception ignored) {
        }

        try {
            jdbcTemplate.execute("UPDATE typing_speed SET accuracy = 100 WHERE accuracy IS NULL");
        } catch (Exception ignored) {
        }
    }
}
