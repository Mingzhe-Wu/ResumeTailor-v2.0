package com.mingzhe.resumetailor.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class BulletChunkParser {

    public List<String> parseBullets(String text) {
        List<String> bullets = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return bullets;
        }

        String[] lines = text.split("\\R");

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("*")) {
                String bullet = trimmed.substring(1).trim();

                if (!bullet.isBlank()) {
                    bullets.add(bullet);
                }
            }
        }

        return bullets;
    }
}
