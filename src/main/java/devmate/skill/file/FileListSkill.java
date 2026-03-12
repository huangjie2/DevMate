package devmate.skill.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import devmate.security.PathValidator;
import devmate.skill.Skill;
import devmate.skill.SkillInput;
import devmate.skill.SkillResult;
import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件列表 Skill
 */
@ApplicationScoped
public class FileListSkill implements Skill {

    @Inject
    PathValidator pathValidator;

    @Override
    public String name() {
        return "list_directory";
    }

    @Override
    public String description() {
        return "列出指定目录下的文件和子目录。支持递归列出。";
    }

    @Override
    public JsonNode inputSchema() {
        return JsonNodeFactory.instance.objectNode()
            .put("type", "object")
            .set("properties", JsonNodeFactory.instance.objectNode()
                .set("path", JsonNodeFactory.instance.objectNode()
                    .put("type", "string")
                    .put("description", "目录路径（默认当前目录）")
                    .put("default", "."))
                .set("recursive", JsonNodeFactory.instance.objectNode()
                    .put("type", "boolean")
                    .put("description", "是否递归列出子目录")
                    .put("default", false))
                .set("pattern", JsonNodeFactory.instance.objectNode()
                    .put("type", "string")
                    .put("description", "文件名过滤模式（glob 格式，如 *.java）"))
            );
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path", ".");
        boolean recursive = input.getBoolean("recursive", false);
        String pattern = input.getString("pattern");

        // 校验路径
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        // 检查是否为目录
        if (!Files.isDirectory(path)) {
            return Result.failure("路径不是目录: " + path);
        }

        try {
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;
            
            try (Stream<Path> stream = Files.walk(path, maxDepth)) {
                List<Map<String, Object>> files = stream
                    .filter(p -> !p.equals(path)) // 排除根目录
                    .filter(p -> matchesPattern(p, pattern))
                    .map(this::toFileInfo)
                    .collect(Collectors.toList());

                // 构建输出
                StringBuilder content = new StringBuilder();
                content.append(String.format("目录 %s 内容 (共 %d 项):\n\n", path, files.size()));
                
                for (Map<String, Object> file : files) {
                    String type = (Boolean) file.get("isDirectory") ? "📁" : "📄";
                    content.append(String.format("%s %s", type, file.get("name")));
                    if (!(Boolean) file.get("isDirectory")) {
                        content.append(String.format(" (%s bytes)", file.get("size")));
                    }
                    content.append("\n");
                }

                Map<String, Object> metadata = Map.of(
                    "path", path.toString(),
                    "total", files.size(),
                    "recursive", recursive
                );

                Log.infof("Listed directory: %s (%d items)", path, files.size());

                return Result.success(new SkillResult(content.toString(), metadata));
            }

        } catch (IOException e) {
            Log.errorf(e, "Failed to list directory: %s", path);
            return Result.failure("列出目录失败: " + e.getMessage());
        }
    }

    /**
     * 检查文件名是否匹配模式
     */
    private boolean matchesPattern(Path path, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        
        String fileName = path.getFileName().toString();
        return fileName.matches(globToRegex(pattern));
    }

    /**
     * Glob 模式转正则表达式
     */
    private String globToRegex(String glob) {
        return glob.replace(".", "\\.")
                   .replace("*", ".*")
                   .replace("?", ".");
    }

    /**
     * 转换为文件信息 Map
     */
    private Map<String, Object> toFileInfo(Path path) {
        boolean isDirectory = Files.isDirectory(path);
        long size = 0;
        try {
            size = Files.size(path);
        } catch (IOException ignored) {
        }

        return Map.of(
            "name", path.getFileName().toString(),
            "path", path.toString(),
            "isDirectory", isDirectory,
            "size", size
        );
    }

    @Override
    public String category() {
        return "file";
    }
}
