package com.typing.backend.service;

import java.sql.Date;
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

    public TypingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String getPrompt() {
        return PROMPTS[random.nextInt(PROMPTS.length)];
    }

    public void saveResult(double speed) {
        jdbcTemplate.update("INSERT INTO typing_speed (speed) VALUES (?)", speed);
    }

    public List<Map<String, Object>> getScores() {
        return jdbcTemplate.query(
                "SELECT speed, test_date FROM typing_speed ORDER BY test_date DESC LIMIT 10",
                (rs, rowNum) -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("speed", rs.getDouble("speed"));
                    Date testDate = rs.getDate("test_date");
                    row.put("date", testDate != null ? testDate.toString() : null);
                    return row;
                });
    }
}
