package com.mingzhe.resumetailor.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mingzhe.resumetailor.rag.EmbeddingStatus;
import com.mingzhe.resumetailor.rag.ProfileEmbeddingChunk;
import com.mingzhe.resumetailor.rag.ProfileEmbeddingChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ChunkEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkEmbeddingService.class);

    private static final String EMBEDDING_URL = "https://api.openai.com/v1/embeddings";
    private static final String EMBEDDING_MODEL = "text-embedding-3-small";
    private static final int EXPECTED_DIMENSION = 1536;

    private final String apiKey = System.getenv("OPENAI_API_KEY");
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProfileEmbeddingChunkMapper chunkMapper;

    public ChunkEmbeddingService(ProfileEmbeddingChunkMapper chunkMapper) {
        this.chunkMapper = chunkMapper;
    }

    public void embedPendingChunksByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User id cannot be null.");
        }

        List<ProfileEmbeddingChunk> pendingChunks =
                chunkMapper.findByUserIdAndStatus(
                        userId,
                        EmbeddingStatus.PENDING
                );

        // Embedding failures are isolated per chunk so one bad source does not
        // block the rest of the user's retrieval corpus from becoming READY.
        for (ProfileEmbeddingChunk chunk : pendingChunks) {
            try {
                float[] embedding = createEmbedding(chunk.getContentText());

                ProfileEmbeddingChunk update = new ProfileEmbeddingChunk();
                update.setId(chunk.getId());
                update.setEmbedding(embedding);
                update.setEmbeddingStatus(EmbeddingStatus.READY);
                update.setEmbeddingModel(EMBEDDING_MODEL);

                chunkMapper.updateById(update);

            } catch (Exception e) {
                log.warn("Failed to embed chunk id={}", chunk.getId(), e);
            }
        }
    }

    public float[] createEmbedding(String input) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("OPENAI_API_KEY is missing.");
        }

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Embedding input cannot be blank.");
        }

        try {
            URL url = new URL(EMBEDDING_URL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", EMBEDDING_MODEL);
            body.put("input", input);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(objectMapper.writeValueAsBytes(body));
            }

            int statusCode = conn.getResponseCode();

            InputStream inputStream = statusCode >= 200 && statusCode < 300
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            String responseBody = readResponse(inputStream);

            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException(
                        "OpenAI embedding API call failed with status code: " + statusCode
                );
            }

            JsonNode root = objectMapper.readTree(responseBody);

            JsonNode dataNode = root.path("data");

            if (!dataNode.isArray() || dataNode.isEmpty()) {
                throw new RuntimeException("Invalid OpenAI embedding response: data is missing or empty.");
            }

            JsonNode embeddingNode = dataNode.get(0).path("embedding");

            if (!embeddingNode.isArray()) {
                throw new RuntimeException("Invalid OpenAI embedding response.");
            }

            if (embeddingNode.size() != EXPECTED_DIMENSION) {
                // pgvector column dimensions are fixed by migration, so reject
                // unexpected model output before it reaches the database.
                throw new RuntimeException(
                        "Invalid embedding dimension: expected "
                                + EXPECTED_DIMENSION
                                + ", got "
                                + embeddingNode.size()
                );
            }

            float[] embedding = new float[EXPECTED_DIMENSION];

            for (int i = 0; i < EXPECTED_DIMENSION; i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }

            return embedding;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create embedding.", e);
        }
    }

    private String readResponse(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        )) {
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        }
    }
}
