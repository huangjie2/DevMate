package devmate.util;

import java.util.function.Function;

/**
 * Unified Result Type - Using Java 21 Sealed Classes
 * Replaces Vavr Either, provides type-safe success/failure handling
 */
public sealed interface Result<T> {

    /**
     * Check if successful
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * Check if failed
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * Get success value, throw exception if failed
     */
    default T getOrThrow() {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T>(var error, var cause) ->
                throw new RuntimeException(error, cause);
        };
    }

    /**
     * Get success value, return default if failed
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T> ignored -> defaultValue;
        };
    }

    /**
     * Map success value
     */
    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T>(var value) -> new Success<>(mapper.apply(value));
            case Failure<T>(var error, var cause) -> new Failure<>(error, cause);
        };
    }

    /**
     * Flat map
     */
    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return switch (this) {
            case Success<T>(var value) -> mapper.apply(value);
            case Failure<T>(var error, var cause) -> new Failure<>(error, cause);
        };
    }

    /**
     * Success result
     */
    record Success<T>(T value) implements Result<T> {
        public Success {
            if (value == null) {
                throw new IllegalArgumentException("Success value cannot be null");
            }
        }
    }

    /**
     * Failure result
     */
    record Failure<T>(String error, Throwable cause) implements Result<T> {
        
        public Failure(String error) {
            this(error, null);
        }

        public Failure {
            if (error == null || error.isBlank()) {
                throw new IllegalArgumentException("Error message cannot be null or blank");
            }
        }
    }

    // ========== Static Factory Methods ==========

    /**
     * Create success result
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Create failure result
     */
    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    /**
     * Create failure result with exception
     */
    static <T> Result<T> failure(String error, Throwable cause) {
        return new Failure<>(error, cause);
    }

    /**
     * Create result from operation that may throw exception
     */
    static <T> Result<T> of(CheckedSupplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Exception e) {
            return failure(e.getMessage(), e);
        }
    }

    @FunctionalInterface
    interface CheckedSupplier<T> {
        T get() throws Exception;
    }
}