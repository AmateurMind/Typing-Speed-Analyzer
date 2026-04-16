package com.typing.backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.typing.backend.service.TypingService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
public class TypingController {

    private final TypingService typingService;

    public TypingController(TypingService typingService) {
        this.typingService = typingService;
    }

    @GetMapping("/prompt")
    public String getPrompt() {
        return typingService.getPrompt();
    }

    @PostMapping("/result")
    public ResponseEntity<Map<String, String>> saveResult(@RequestBody Map<String, Object> data) {
        Map<String, String> response = new HashMap<>();

        try {
            Object wpmValue = data.get("wpm");
            if (!(wpmValue instanceof Number)) {
                wpmValue = data.get("speed");
            }
            Object accuracyValue = data.get("accuracy");

            if (!(wpmValue instanceof Number) || !(accuracyValue instanceof Number)) {
                response.put("message", "Invalid wpm/accuracy value");
                return ResponseEntity.badRequest().body(response);
            }

            double wpm = ((Number) wpmValue).doubleValue();
            double accuracy = ((Number) accuracyValue).doubleValue();
            typingService.saveResult(wpm, accuracy);
            response.put("message", "Saved");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "Error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/scores")
    public List<Map<String, Object>> getScores() {
        return typingService.getScores();
    }
}
