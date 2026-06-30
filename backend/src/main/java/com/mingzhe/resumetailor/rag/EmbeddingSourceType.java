package com.mingzhe.resumetailor.rag;

public enum EmbeddingSourceType {

    EXPERIENCE("EXPERIENCE"),
    PROJECT("PROJECT"),
    SKILL("SKILL"),
    EDUCATION("EDUCATION"),
    SUMMARY("SUMMARY"),;

    private final String sourceType;

    EmbeddingSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceType() {
        return sourceType;
    }
}
