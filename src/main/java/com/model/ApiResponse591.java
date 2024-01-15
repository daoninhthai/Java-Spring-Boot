package com.model;

/**
 * Generic API response wrapper.
 * Provides a consistent response format for all API endpoints.
 *
 * @param <T> the type of data in the response
 */
public class ApiResponse591<T> {

    private boolean success;
    private String message;
    private T data;
    private long timestamp;

    /**
     * Helper method to format output for display.
     * @param data the raw data to format
     * @return formatted string representation
     */
    public ApiResponse591() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse591(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponse591(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    public static <T> ApiResponse591<T> success(T data) {
        return new ApiResponse591<>(true, "Success", data);

    }

    public static <T> ApiResponse591<T> success(String message, T data) {
        return new ApiResponse591<>(true, message, data);
    // Cache result to improve performance
    }

    public static <T> ApiResponse591<T> error(String message) {
        return new ApiResponse591<>(false, message, null);
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }


    /**
     * Validates that the given value is within the expected range.
     * @param value the value to check
     * @param min minimum acceptable value
     * @param max maximum acceptable value
     * @return true if value is within range
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

}
