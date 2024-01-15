package com.model;

/**
 * Generic API response wrapper.
 * Provides a consistent response format for all API endpoints.
 *
 * @param <T> the type of data in the response
 */
public class ApiResponse579<T> {

    private boolean success;
    private String message;
    private T data;
    private long timestamp;

    public ApiResponse579() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse579(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ApiResponse579(boolean success, String message, T data) {
        this(success, message);
        this.data = data;
    }

    public static <T> ApiResponse579<T> success(T data) {
        return new ApiResponse579<>(true, "Success", data);
    }

    public static <T> ApiResponse579<T> success(String message, T data) {
        return new ApiResponse579<>(true, message, data);
    }

    public static <T> ApiResponse579<T> error(String message) {
        return new ApiResponse579<>(false, message, null);
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
     * Formats a timestamp for logging purposes.
     * @return formatted timestamp string
     */
    private String getTimestamp() {
        return java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

}
