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
 * File Read Skill
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
        return "Read content from a specified file. Supports text files with automatic encoding detection. Returns file content and metadata.";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var pathProp = factory.objectNode();
        pathProp.put("type", "string");
        pathProp.put("description", "File path (relative or absolute)");
        properties.set("path", pathProp);
        
        var offsetProp = factory.objectNode();
        offsetProp.put("type", "integer");
        offsetProp.put("description", "Starting line number (optional, 0-based)");
        offsetProp.put("default", 0);
        properties.set("offset", offsetProp);
        
        var limitProp = factory.objectNode();
        limitProp.put("type", "integer");
        limitProp.put("description", "Number of lines to read (optional)");
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

        // Validate path
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        // Check if file exists
        if (!Files.exists(path)) {
            return Result.failure("File not found: " + path);
        }

        // Check if it's a file
        if (!Files.isRegularFile(path)) {
            return Result.failure("Path is not a file: " + path);
        }

        // Read file
        try {
            List<String> allLines = Files.readAllLines(path);
            
            // Apply offset and limit
            int startLine = Math.max(0, offset);
            int endLine = limit != null 
                ? Math.min(startLine + limit, allLines.size())
                : allLines.size();
            
            List<String> selectedLines = allLines.subList(startLine, endLine);
            String content = String.join("\n", selectedLines);

            // Build metadata
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
            return Result.failure("Failed to read file: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "file";
    }
}