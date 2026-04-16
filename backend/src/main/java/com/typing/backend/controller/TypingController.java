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
            Object speedValue = data.get("speed");
            if (!(speedValue instanceof Number)) {
                response.put("message", "Invalid speed value");
                return ResponseEntity.badRequest().body(response);
            }

            double speed = ((Number) speedValue).doubleValue();
            typingService.saveResult(speed);
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
