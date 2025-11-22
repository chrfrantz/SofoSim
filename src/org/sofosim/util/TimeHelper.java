package org.sofosim.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Helper class supporting time tracking for simulations by allowing for
 * registration of start time (via startTime()) and
 * evaluation of duration (via stopTime()).
 */
public class TimeHelper {

    /**
     * Cached start time. To be initialized via {@link #startTime()}.
     */
    private static long startTime = -1L;

    /**
     * Sets start time for later generation of runtime calculation.
     * Returns start time as formatted string.
     */
    public static String startTime() {
        startTime = System.currentTimeMillis();
        // Print current time
        LocalDateTime dateTime = LocalDateTime.now(); // Get current date and time

        // Define custom format (yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Print date and time in the defined format
        StringBuilder buffer = new StringBuilder();

        buffer.append("Start Time: ").append(dateTime.format(formatter));

        return buffer.toString();
    }

    /**
     * Generates string based on difference between start time (initialized via {@link #startTime()}) and current time.
     * Includes both current time and duration of execution in returned string for downstream use.
     * Checks whether start time has been properly initialized.
     * Can be called repeatedly to indicate progression (does not reset start time).
     * @return
     */
    public static String stopTime() {

        // Check for proper initialization
        if (startTime == -1L) {
            return "Invalid start time (check for proper initialization via startTime())";
        }

        // Print current time
        LocalDateTime dateTime = LocalDateTime.now(); // Get current date and time

        // Define custom format (yyyy-MM-dd HH:mm:ss)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Print date and time in the defined format
        StringBuilder buffer = new StringBuilder();

        buffer.append("Current Time: ").append(dateTime.format(formatter));
        buffer.append(System.lineSeparator());

        // Calculate duration
        long durationInMillis = System.currentTimeMillis() - startTime;

        // Convert milliseconds to hours, minutes, and seconds
        long hours = durationInMillis / (1000 * 60 * 60); // Convert to hours
        long minutes = (durationInMillis / (1000 * 60)) % 60; // Convert to minutes
        long seconds = (durationInMillis / 1000) % 60; // Convert to seconds

        // Print the formatted duration
        String duration = String.format("Execution duration: %02d:%02d:%02d\n", hours, minutes, seconds);

        buffer.append(duration);
        return buffer.toString();
    }


}
