package com.mingzhe.resumetailor.rag;

public enum EmbeddingStatus {

    PENDING("PENDING"),
    READY("READY"),
    FAILED("FAILED");

    private final String status;

    EmbeddingStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
