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
 * File Write Skill
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
        return "Write content to a specified file. Creates non-existent directories. Overwrites existing file by default.";
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
        
        var contentProp = factory.objectNode();
        contentProp.put("type", "string");
        contentProp.put("description", "Content to write");
        properties.set("content", contentProp);
        
        var appendProp = factory.objectNode();
        appendProp.put("type", "boolean");
        appendProp.put("description", "Append mode (default false, overwrite)");
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
        return true; // File write requires confirmation
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path");
        String content = input.getString("content");
        boolean append = input.getBoolean("append", false);

        if (content == null) {
            return Result.failure("Content cannot be empty");
        }

        // Validate path
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        try {
            // Create parent directories
            Path parentDir = path.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
                Log.infof("Created directory: %s", parentDir);
            }

            // Write file
            StandardOpenOption openOption = append 
                ? StandardOpenOption.APPEND 
                : StandardOpenOption.TRUNCATE_EXISTING;

            if (!Files.exists(path)) {
                Files.writeString(path, content);
            } else {
                Files.writeString(path, content, openOption);
            }

            // Build metadata
            Map<String, Object> metadata = Map.of(
                "path", path.toString(),
                "size", content.length(),
                "append", append
            );

            Log.infof("Wrote %d bytes to file: %s (append: %s)", content.length(), path, append);

            return Result.success(new SkillResult(
                String.format("Successfully wrote %d bytes to %s", content.length(), path),
                metadata
            ));

        } catch (IOException e) {
            Log.errorf(e, "Failed to write file: %s", path);
            return Result.failure("Failed to write file: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "file";
    }
}