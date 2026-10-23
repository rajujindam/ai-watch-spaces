
package com.aiwatchspaces.ai_watch_spaces.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "application", "AI Watch Spaces",
                "message", "Backend API is running"
        );
    }
}
