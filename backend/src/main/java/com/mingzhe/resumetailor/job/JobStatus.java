package com.mingzhe.resumetailor.job;

public enum JobStatus {
    SAVED(1, "SAVED"),
    APPLIED(2, "APPLIED"),
    INTERVIEWING(3, "INTERVIEWING"),
    OFFER(4, "OFFER"),
    REJECTED(5, "REJECTED");

    private final int code;
    private final String dbValue;

    JobStatus(int code, String dbValue) {
        this.code = code;
        this.dbValue = dbValue;
    }

    public int getCode() {
        return code;
    }

    public String getDbValue() {
        return dbValue;
    }

    // iterate over the codes to validate status
    public static JobStatus fromCode(Integer code) {
        if (code == null) {
            return SAVED;
        }

        for (JobStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }

        throw new IllegalArgumentException("Invalid job status: " + code);
    }
}
