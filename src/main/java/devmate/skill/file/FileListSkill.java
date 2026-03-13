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
 * File List Skill
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
        return "List files and subdirectories in a specified directory. Supports recursive listing.";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var pathProp = factory.objectNode();
        pathProp.put("type", "string");
        pathProp.put("description", "Directory path (default: current directory)");
        pathProp.put("default", ".");
        properties.set("path", pathProp);
        
        var recursiveProp = factory.objectNode();
        recursiveProp.put("type", "boolean");
        recursiveProp.put("description", "Recursively list subdirectories");
        recursiveProp.put("default", false);
        properties.set("recursive", recursiveProp);
        
        var patternProp = factory.objectNode();
        patternProp.put("type", "string");
        patternProp.put("description", "File name filter pattern (glob format, e.g. *.java)");
        properties.set("pattern", patternProp);
        
        schema.set("properties", properties);
        
        return schema;
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String pathStr = input.getString("path", ".");
        boolean recursive = input.getBoolean("recursive", false);
        String pattern = input.getString("pattern");

        // Validate path
        Result<Path> pathResult = pathValidator.validate(pathStr);
        if (pathResult.isFailure()) {
            return Result.failure(((Result.Failure<Path>) pathResult).error());
        }

        Path path = pathResult.getOrThrow();

        // Check if it's a directory
        if (!Files.isDirectory(path)) {
            return Result.failure("Path is not a directory: " + path);
        }

        try {
            int maxDepth = recursive ? Integer.MAX_VALUE : 1;
            
            try (Stream<Path> stream = Files.walk(path, maxDepth)) {
                List<Map<String, Object>> files = stream
                    .filter(p -> !p.equals(path)) // Exclude root directory
                    .filter(p -> matchesPattern(p, pattern))
                    .map(this::toFileInfo)
                    .collect(Collectors.toList());

                // Build output
                StringBuilder content = new StringBuilder();
                content.append(String.format("Directory %s contents (%d items):\n\n", path, files.size()));
                
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
            return Result.failure("Failed to list directory: " + e.getMessage());
        }
    }

    /**
     * Check if file name matches pattern
     */
    private boolean matchesPattern(Path path, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return true;
        }
        
        String fileName = path.getFileName().toString();
        return fileName.matches(globToRegex(pattern));
    }

    /**
     * Convert glob pattern to regex
     */
    private String globToRegex(String glob) {
        return glob.replace(".", "\\.")
                   .replace("*", ".*")
                   .replace("?", ".");
    }

    /**
     * Convert path to file info map
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