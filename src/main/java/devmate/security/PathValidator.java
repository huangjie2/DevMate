package devmate.security;

import devmate.config.ConfigLoader;
import devmate.config.McpConfig;
import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.nio.file.Path;
import java.util.List;

/**
 * Path Validator
 * 
 * Ensures file operations are only within allowed paths
 */
@ApplicationScoped
public class PathValidator {

    @Inject
    ConfigLoader configLoader;

    /**
     * Validate if path is within allowed range
     * 
     * @param path Path to validate
     * @return Validation result
     */
    public Result<Path> validate(String path) {
        return validate(Path.of(path));
    }

    /**
     * Validate if path is within allowed range
     * 
     * @param path Path to validate
     * @return Validation result
     */
    public Result<Path> validate(Path path) {
        // Normalize path
        Path normalizedPath = path.normalize().toAbsolutePath();

        // Get allowed paths list
        List<String> allowedPaths = getAllowedPaths();

        // Check if within allowed range
        for (String allowed : allowedPaths) {
            Path allowedPath = Path.of(allowed).normalize().toAbsolutePath();
            if (normalizedPath.startsWith(allowedPath)) {
                Log.debugf("Path validated: %s (allowed by: %s)", normalizedPath, allowedPath);
                return Result.success(normalizedPath);
            }
        }

        // Path not in allowed range
        String error = String.format(
            "Path '%s' is not within allowed directories. Allowed directories: %s",
            normalizedPath,
            allowedPaths
        );
        Log.warnf(error);
        return Result.failure(error);
    }

    /**
     * Check if path is within allowed range (without error message)
     * 
     * @param path Path to check
     * @return Whether allowed
     */
    public boolean isAllowed(String path) {
        return isAllowed(Path.of(path));
    }

    /**
     * Check if path is within allowed range (without error message)
     * 
     * @param path Path to check
     * @return Whether allowed
     */
    public boolean isAllowed(Path path) {
        return validate(path).isSuccess();
    }

    /**
     * Check if path exists
     * 
     * @param path Path
     * @return Whether exists
     */
    public boolean exists(Path path) {
        return path.toFile().exists();
    }

    /**
     * Check if path is a file
     * 
     * @param path Path
     * @return Whether it's a file
     */
    public boolean isFile(Path path) {
        return path.toFile().isFile();
    }

    /**
     * Check if path is a directory
     * 
     * @param path Path
     * @return Whether it's a directory
     */
    public boolean isDirectory(Path path) {
        return path.toFile().isDirectory();
    }

    /**
     * Get allowed paths list
     */
    private List<String> getAllowedPaths() {
        return configLoader.loadMcpConfig()
            .map(McpConfig::allowedPaths)
            .orElse(List.of(configLoader.getProjectRoot().toString()));
    }

    /**
     * Get project root directory
     */
    public Path getProjectRoot() {
        return configLoader.getProjectRoot();
    }
}