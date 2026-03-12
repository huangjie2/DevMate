package devmate.util;

import java.util.function.Function;

/**
 * 统一返回类型 - 使用 Java 21 Sealed Classes 实现
 * 替代 Vavr Either，提供类型安全的成功/失败处理
 */
public sealed interface Result<T> {

    /**
     * 判断是否成功
     */
    default boolean isSuccess() {
        return this instanceof Success;
    }

    /**
     * 判断是否失败
     */
    default boolean isFailure() {
        return this instanceof Failure;
    }

    /**
     * 获取成功值，失败时抛出异常
     */
    default T getOrThrow() {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T>(var error, var cause) ->
                throw new RuntimeException(error, cause);
        };
    }

    /**
     * 获取成功值，失败时返回默认值
     */
    default T getOrElse(T defaultValue) {
        return switch (this) {
            case Success<T>(var value) -> value;
            case Failure<T> ignored -> defaultValue;
        };
    }

    /**
     * 映射成功值
     */
    default <U> Result<U> map(Function<T, U> mapper) {
        return switch (this) {
            case Success<T>(var value) -> new Success<>(mapper.apply(value));
            case Failure<T>(var error, var cause) -> new Failure<>(error, cause);
        };
    }

    /**
     * 扁平映射
     */
    default <U> Result<U> flatMap(Function<T, Result<U>> mapper) {
        return switch (this) {
            case Success<T>(var value) -> mapper.apply(value);
            case Failure<T>(var error, var cause) -> new Failure<>(error, cause);
        };
    }

    /**
     * 成功结果
     */
    record Success<T>(T value) implements Result<T> {
        public Success {
            if (value == null) {
                throw new IllegalArgumentException("Success value cannot be null");
            }
        }
    }

    /**
     * 失败结果
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

    // ========== 静态工厂方法 ==========

    /**
     * 创建成功结果
     */
    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * 创建失败结果
     */
    static <T> Result<T> failure(String error) {
        return new Failure<>(error);
    }

    /**
     * 创建失败结果（带异常）
     */
    static <T> Result<T> failure(String error, Throwable cause) {
        return new Failure<>(error, cause);
    }

    /**
     * 从可能抛出异常的操作创建结果
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
