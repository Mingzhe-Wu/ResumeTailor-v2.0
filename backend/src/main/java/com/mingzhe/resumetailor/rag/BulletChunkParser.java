package com.mingzhe.resumetailor.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class BulletChunkParser {

    private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("\\R");

    public List<String> parseBullets(String text) {
        String normalizedDescription = text == null ? "" : text.trim();

        if (normalizedDescription.isEmpty()) {
            return List.of();
        }

        String[] rawChunks;
        if (normalizedDescription.contains("*")) {
            rawChunks = normalizedDescription.split("\\*", -1);
        } else if (LINE_BREAK_PATTERN.matcher(normalizedDescription).find()) {
            rawChunks = LINE_BREAK_PATTERN.split(normalizedDescription, -1);
        } else {
            rawChunks = new String[]{normalizedDescription};
        }

        List<String> chunks = new ArrayList<>();
        for (String rawChunk : rawChunks) {
            String normalizedChunk = rawChunk.trim();
            if (!normalizedChunk.isEmpty()) {
                chunks.add(normalizedChunk);
            }
        }

        return chunks;
    }
}
