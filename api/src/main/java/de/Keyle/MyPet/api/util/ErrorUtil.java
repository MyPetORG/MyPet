/*
 * This file is part of MyPet
 *
 * Copyright © 2011-2025 Keyle
 * MyPet is licensed under the GNU Lesser General Public License.
 *
 * MyPet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyPet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.Keyle.MyPet.api.util;

import de.Keyle.MyPet.MyPetApi;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for centralized error reporting.
 * Handles logging to console and automatic Sentry error reporting.
 */
public class ErrorUtil {

    private ErrorUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Reports an error with SEVERE level.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param e the exception to report
     */
    public static void report(Throwable e) {
        report(e.getMessage(), e);
    }

    /**
     * Reports an error with SEVERE level and custom message.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param message custom error message
     * @param e the exception to report
     */
    public static void report(String message, Throwable e) {
        reportSevere(message, e);
    }

    /**
     * Reports a warning-level error.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param e the exception to report
     */
    public static void reportWarning(Throwable e) {
        reportWarning(e.getMessage(), e);
    }

    /**
     * Reports a warning-level error with custom message.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param message custom warning message
     * @param e the exception to report
     */
    public static void reportWarning(String message, Throwable e) {
        logAndReport(Level.WARNING, message, e);
    }

    /**
     * Reports an error-level error.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param e the exception to report
     */
    public static void reportError(Throwable e) {
        reportError(e.getMessage(), e);
    }

    /**
     * Reports an error-level error with custom message.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param message custom error message
     * @param e the exception to report
     */
    public static void reportError(String message, Throwable e) {
        logAndReport(Level.SEVERE, message, e);
    }

    /**
     * Reports a severe-level error.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param e the exception to report
     */
    public static void reportSevere(Throwable e) {
        reportSevere(e.getMessage(), e);
    }

    /**
     * Reports a severe-level error with custom message.
     * Logs to console with full stack trace and sends to Sentry if enabled.
     *
     * @param message custom error message
     * @param e the exception to report
     */
    public static void reportSevere(String message, Throwable e) {
        logAndReport(Level.SEVERE, message, e);
    }

    /**
     * Internal method that logs to console and sends to Sentry.
     *
     * @param level logging level
     * @param message error message
     * @param e the exception
     */
    private static void logAndReport(Level level, String message, Throwable e) {
        Logger logger = MyPetApi.getLogger();

        logger.log(level, "✘ MyPet has encountered an error!");
        // Log to console with full stack trace
        if (message != null && !message.isEmpty()) {
            logger.log(level, message, e);
        } else {
            logger.log(level, "An error occurred", e);
        }

        // Send to Sentry if error reporter is available and enabled
        ErrorReporter errorReporter = MyPetApi.getErrorReporter();
        if (errorReporter != null) {
            errorReporter.sendError(e, message != null ? message : "");
        }
    }
}
