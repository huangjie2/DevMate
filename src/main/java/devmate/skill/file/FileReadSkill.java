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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件读取 Skill
 */
@ApplicationScoped
public class FileReadSkill implements Skill {

    @Inject
    PathValidator pathValidator;

    @Override
    public String name() {
        return "read_file";
    }

    @Override
    public String description() {
        return "读取指定文件的内容。支持文本文件，自动检测编码。返回文件内容和元数据。";
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
        
        var offsetProp = factory.objectNode();
        offsetProp.put("type", "integer");
        offsetProp.put("description", "起始行号（可选，0-based）");
        offsetProp.put("default", 0);
        properties.set("offset", offsetProp);
        
        var limitProp = factory.objectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "读取行数限制（可选）");
        properties.set("limit", limitProp);
        
        schema.set("properties", properties);
        
        var required = factory.arrayNode();
        required.add("path");
        schema.set("required", required);
        
        return schema;
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path");
        int offset = input.getInteger("offset", 0);
        Integer limit = input.getInteger("limit");

        // 校验路径
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        // 检查文件是否存在
        if (!Files.exists(path)) {
            return Result.failure("文件不存在: " + path);
        }

        // 检查是否为文件
        if (!Files.isRegularFile(path)) {
            return Result.failure("路径不是文件: " + path);
        }

        // 读取文件
        try {
            List<String> allLines = Files.readAllLines(path);
            
            // 应用 offset 和 limit
            int startLine = Math.max(0, offset);
            int endLine = limit != null 
                ? Math.min(startLine + limit, allLines.size())
                : allLines.size();
            
            List<String> selectedLines = allLines.subList(startLine, endLine);
            String content = String.join("\n", selectedLines);

            // 构建元数据
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("path", path.toString());
            metadata.put("totalLines", allLines.size());
            metadata.put("returnedLines", selectedLines.size());
            metadata.put("offset", startLine);
            metadata.put("size", Files.size(path));

            Log.infof("Read file: %s (lines %d-%d of %d)", path, startLine, endLine - 1, allLines.size());

            return Result.success(new SkillResult(content, metadata));

        } catch (IOException e) {
            Log.errorf(e, "Failed to read file: %s", path);
            return Result.failure("读取文件失败: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "file";
    }
}
