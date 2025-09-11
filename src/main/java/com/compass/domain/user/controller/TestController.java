package com.compass.domain.user.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {
    
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @PostMapping("/encode")
    public Map<String, String> encodePassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("password");
        String encoded = passwordEncoder.encode(rawPassword);
        
        Map<String, String> response = new HashMap<>();
        response.put("raw", rawPassword);
        response.put("encoded", encoded);
        response.put("info", "Use this encoded password in database");
        
        return response;
    }
    
    @PostMapping("/verify")
    public Map<String, Object> verifyPassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("rawPassword");
        String encodedPassword = request.get("encodedPassword");
        
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("rawPassword", rawPassword);
        response.put("encodedPassword", encodedPassword);
        response.put("matches", matches);
        
        return response;
    }
}