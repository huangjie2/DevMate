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
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * 文件写入 Skill
 */
@ApplicationScoped
public class FileWriteSkill implements Skill {

    @Inject
    PathValidator pathValidator;

    @Override
    public String name() {
        return "write_file";
    }

    @Override
    public String description() {
        return "写入内容到指定文件。会创建不存在的目录。如果文件存在会覆盖。";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var pathProp = factory.objectNode();
        pathProp.put("type", "string");
        pathProp.put("description", "文件路径（相对或绝对）");
        properties.set("path", pathProp);
        
        var contentProp = factory.objectNode();
        contentProp.put("type", "string");
        contentProp.put("description", "要写入的内容");
        properties.set("content", contentProp);
        
        var appendProp = factory.objectNode();
        appendProp.put("type", "boolean");
        appendProp.put("description", "是否追加模式（默认 false，覆盖）");
        appendProp.put("default", false);
        properties.set("append", appendProp);
        
        schema.set("properties", properties);
        
        var required = factory.arrayNode();
        required.add("path");
        required.add("content");
        schema.set("required", required);
        
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true; // 文件写入需要确认
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path");
        String content = input.getString("content");
        boolean append = input.getBoolean("append", false);

        if (content == null) {
            return Result.failure("内容不能为空");
        }

        // 校验路径
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        try {
            // 创建父目录
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                Log.infof("Created directory: %s", parentDir);
            }

            // 写入文件
            StandardOpenOption openOption = append 
                ? StandardOpenOption.APPEND 
                : StandardOpenOption.TRUNCATE_EXISTING;

            if (!Files.exists(path)) {
                Files.writeString(path, content);
            } else {
                Files.writeString(path, content, openOption);
            }

            // 构建元数据
            Map<String, Object> metadata = Map.of(
                "path", path.toString(),
                "size", content.length(),
                "append", append
            );

            Log.infof("Wrote %d bytes to file: %s (append: %s)", content.length(), path, append);

            return Result.success(new SkillResult(
                String.format("成功写入 %d 字节到 %s", content.length(), path),
                metadata
            ));

        } catch (IOException e) {
            Log.errorf(e, "Failed to write file: %s", path);
            return Result.failure("写入文件失败: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "file";
    }
}
