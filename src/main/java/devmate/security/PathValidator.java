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
 * 路径校验器
 * 
 * 确保文件操作只在允许的路径范围内进行
 */
@ApplicationScoped
public class PathValidator {

    @Inject
    ConfigLoader configLoader;

    /**
     * 校验路径是否在允许范围内
     * 
     * @param path 要校验的路径
     * @return 校验结果
     */
    public Result<Path> validate(String path) {
        return validate(Path.of(path));
    }

    /**
     * 校验路径是否在允许范围内
     * 
     * @param path 要校验的路径
     * @return 校验结果
     */
    public Result<Path> validate(Path path) {
        // 规范化路径
        Path normalizedPath = path.normalize().toAbsolutePath();

        // 获取允许的路径列表
        List<String> allowedPaths = getAllowedPaths();

        // 检查是否在允许范围内
        for (String allowed : allowedPaths) {
            Path allowedPath = Path.of(allowed).normalize().toAbsolutePath();
            if (normalizedPath.startsWith(allowedPath)) {
                Log.debugf("Path validated: %s (allowed by: %s)", normalizedPath, allowedPath);
                return Result.success(normalizedPath);
            }
        }

        // 路径不在允许范围内
        String error = String.format(
            "路径 '%s' 不在允许访问的目录内。允许的目录: %s",
            normalizedPath,
            allowedPaths
        );
        Log.warnf(error);
        return Result.failure(error);
    }

    /**
     * 检查路径是否在允许范围内（不返回错误信息）
     * 
     * @param path 要检查的路径
     * @return 是否允许
     */
    public boolean isAllowed(String path) {
        return isAllowed(Path.of(path));
    }

    /**
     * 检查路径是否在允许范围内（不返回错误信息）
     * 
     * @param path 要检查的路径
     * @return 是否允许
     */
    public boolean isAllowed(Path path) {
        return validate(path).isSuccess();
    }

    /**
     * 检查路径是否存在
     * 
     * @param path 路径
     * @return 是否存在
     */
    public boolean exists(Path path) {
        return path.toFile().exists();
    }

    /**
     * 检查路径是否为文件
     * 
     * @param path 路径
     * @return 是否为文件
     */
    public boolean isFile(Path path) {
        return path.toFile().isFile();
    }

    /**
     * 检查路径是否为目录
     * 
     * @param path 路径
     * @return 是否为目录
     */
    public boolean isDirectory(Path path) {
        return path.toFile().isDirectory();
    }

    /**
     * 获取允许的路径列表
     */
    private List<String> getAllowedPaths() {
        return configLoader.loadMcpConfig()
            .map(McpConfig::allowedPaths)
            .orElse(List.of(configLoader.getProjectRoot().toString()));
    }

    /**
     * 获取项目根目录
     */
    public Path getProjectRoot() {
        return configLoader.getProjectRoot();
    }
}
