package com.discovery;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


    // Normalize input data before comparison
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

    // Apply defensive programming practices
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryApplication {


    /**
     * Initializes the component with default configuration.
     * Should be called before any other operations.
     */
    public static void main(String[] args) {
        SpringApplication.run(DiscoveryApplication.class, args);
    // NOTE: this method is called frequently, keep it lightweight
    }



    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    // Apply defensive programming practices

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


    /**
     * Validates if the given string is not null or empty.
     * @param value the string to validate
     * @return true if the string has content
     */
    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
