package devmate.cli;

import devmate.security.PathValidator;
import devmate.util.Result;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * File Reference Parser
 * 
 * Supported formats:
 * - @filename        Reference single file
 * - @path/to/file    Reference relative path file
 * - @/absolute/path  Reference absolute path file
 * - @.               Reference entire project directory structure
 * 
 * Examples:
 * - "Analyze @src/main/java/App.java"
 * - "Compare @file1.java and @file2.java"
 */
public class FileReferenceParser {

    // Regex to match @file references
    private static final Pattern FILE_REFERENCE_PATTERN = Pattern.compile(
        "@([\\w./\\-~]+)"
    );

    private final Path projectRoot;
    private final PathValidator pathValidator;

    public FileReferenceParser(Path projectRoot, PathValidator pathValidator) {
        this.projectRoot = projectRoot;
        this.pathValidator = pathValidator;
    }

    /**
     * Parse result
     */
    public record ParseResult(
        String processedInput,      // Processed input (file references replaced with content)
        List<FileReference> references  // List of parsed file references
    ) {}

    /**
     * File reference information
     */
    public record FileReference(
        String reference,       // Original reference (e.g. @src/App.java)
        String path,            // Actual path
        String content,         // File content
        boolean success,        // Whether read successfully
        String error            // Error message (if failed)
    ) {}

    /**
     * Parse file references in input
     * 
     * @param input User input
     * @return Parse result
     */
    public ParseResult parse(String input) {
        List<FileReference> references = new ArrayList<>();
        String processedInput = input;

        Matcher matcher = FILE_REFERENCE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String reference = matcher.group(0);  // @filename
            String pathStr = matcher.group(1);    // filename

            FileReference ref = resolveReference(pathStr);
            references.add(ref);

            if (ref.success()) {
                // Replace with file content block
                String replacement = formatFileContent(ref);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // Keep original reference, add error comment
                String replacement = reference + " [Error: " + ref.error() + "]";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }

        matcher.appendTail(sb);
        processedInput = sb.toString();

        Log.infof("Parsed %d file references from input", references.size());

        return new ParseResult(processedInput, references);
    }

    /**
     * Resolve single file reference
     */
    private FileReference resolveReference(String pathStr) {
        // Special handling: @. means project root
        if (".".equals(pathStr)) {
            return resolveProjectStructure();
        }

        // Build full path
        Path path;
        if (pathStr.startsWith("/")) {
            // Absolute path
            path = Path.of(pathStr);
        } else {
            // Relative path
            path = projectRoot.resolve(pathStr);
        }

        // Security validation
        Result<Path> validationResult = pathValidator.validate(path.toString());
        if (validationResult.isFailure()) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                ((Result.Failure<Path>) validationResult).error()
            );
        }

        // Check if exists
        if (!Files.exists(path)) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "File not found"
            );
        }

        // Check if directory
        if (Files.isDirectory(path)) {
            return resolveDirectory(pathStr, path);
        }

        // Read file content
        try {
            String content = Files.readString(path);
            Log.infof("Read file reference: %s (%d bytes)", pathStr, content.length());
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                content,
                true,
                null
            );
        } catch (IOException e) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "Failed to read: " + e.getMessage()
            );
        }
    }

    /**
     * Resolve directory structure
     */
    private FileReference resolveDirectory(String pathStr, Path path) {
        StringBuilder sb = new StringBuilder();
        sb.append("Directory ").append(pathStr).append(" structure:\n\n");

        try {
            Files.walk(path, 3)  // Max 3 levels
                .filter(p -> !p.equals(path))
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .forEach(p -> {
                    String relative = path.relativize(p).toString();
                    String indent = "  ".repeat(Math.max(0, relative.split("/").length - 1));
                    String icon = Files.isDirectory(p) ? "📁 " : "📄 ";
                    sb.append(indent).append(icon).append(p.getFileName()).append("\n");
                });
        } catch (IOException e) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "Failed to read directory: " + e.getMessage()
            );
        }

        return new FileReference(
            "@" + pathStr,
            path.toString(),
            sb.toString(),
            true,
            null
        );
    }

    /**
     * Resolve project root structure (@.)
     */
    private FileReference resolveProjectStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("Project structure:\n\n");

        try {
            Files.walk(projectRoot, 2)
                .filter(p -> !p.equals(projectRoot))
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> !p.toString().contains("node_modules"))
                .forEach(p -> {
                    String relative = projectRoot.relativize(p).toString();
                    String indent = "  ".repeat(Math.max(0, relative.split("/").length - 1));
                    String icon = Files.isDirectory(p) ? "📁 " : "📄 ";
                    sb.append(indent).append(icon).append(p.getFileName()).append("\n");
                });
        } catch (IOException e) {
            return new FileReference(
                "@.",
                projectRoot.toString(),
                null,
                false,
                "Failed to read project structure: " + e.getMessage()
            );
        }

        return new FileReference(
            "@.",
            projectRoot.toString(),
            sb.toString(),
            true,
            null
        );
    }

    /**
     * Format file content for prompt
     */
    private String formatFileContent(FileReference ref) {
        return String.format(
            "\n--- File: %s ---\n%s\n--- End of file ---\n",
            ref.path(),
            ref.content()
        );
    }

    /**
     * Check if input contains file references
     */
    public boolean hasFileReference(String input) {
        return FILE_REFERENCE_PATTERN.matcher(input).find();
    }

    /**
     * Extract all file reference paths
     */
    public List<String> extractFilePaths(String input) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = FILE_REFERENCE_PATTERN.matcher(input);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }
}