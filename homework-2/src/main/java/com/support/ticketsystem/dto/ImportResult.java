package com.support.ticketsystem.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResult {
    private int total;
    private int successful;
    private int failed;
    private List<ImportError> errors;

    public ImportResult() {
        this.errors = new ArrayList<>();
    }

    public ImportResult(int total, int successful, int failed, List<ImportError> errors) {
        this.total = total;
        this.successful = successful;
        this.failed = failed;
        this.errors = errors;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }

    public List<ImportError> getErrors() {
        return errors;
    }

    public void setErrors(List<ImportError> errors) {
        this.errors = errors;
    }

    public static class ImportError {
        private int row;
        private String reason;

        public ImportError() {}

        public ImportError(int row, String reason) {
            this.row = row;
            this.reason = reason;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}
