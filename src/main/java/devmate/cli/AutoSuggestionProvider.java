package devmate.cli;

import devmate.skill.SkillRegistry;
import org.jline.reader.Candidate;
import org.jline.reader.impl.DefaultParser;
import org.jline.utils.AttributedString;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Auto Suggestion Provider
 * Real-time display of matching commands/files, similar to iFlow experience
 */
public class AutoSuggestionProvider {

    public static final List<Suggestion> BUILTIN_COMMANDS = List.of(
        new Suggestion("/help", "Show help", "h"),
        new Suggestion("/exit", "Exit program", "q", "quit"),
        new Suggestion("/reset", "Clear conversation context", null),
        new Suggestion("/skills", "List available skills", null),
        new Suggestion("/config", "Show current configuration", null),
        new Suggestion("/clear", "Clear screen", null)
    );

    private final SkillRegistry skillRegistry;
    private final Path projectRoot;

    public AutoSuggestionProvider(SkillRegistry skillRegistry, Path projectRoot) {
        this.skillRegistry = skillRegistry;
        this.projectRoot = projectRoot;
    }

    /**
     * Suggestion item
     */
    public record Suggestion(
        String value,       // Actual value
        String description, // Description
        String alias,       // Alias
        String... aliases   // Other aliases
    ) {
        public boolean matches(String input) {
            String lower = input.toLowerCase();
            if (value.toLowerCase().startsWith(lower)) return true;
            if (alias != null && alias.toLowerCase().startsWith(lower)) return true;
            if (aliases != null) {
                for (String a : aliases) {
                    if (a.toLowerCase().startsWith(lower)) return true;
                }
            }
            return false;
        }
    }

    /**
     * Get all matching suggestions
     */
    public List<SuggestionResult> getSuggestions(String input, int cursor) {
        List<SuggestionResult> results = new ArrayList<>();
        
        if (input == null || input.isEmpty() || cursor == 0) {
            return results;
        }

        String currentWord = getCurrentWord(input, cursor);
        
        // Command suggestions (/)
        if (currentWord.startsWith("/")) {
            results.addAll(getCommandSuggestions(currentWord));
        }
        // File suggestions (@)
        else if (currentWord.startsWith("@")) {
            results.addAll(getFileSuggestions(currentWord));
        }
        // Show all commands when empty input
        else if (currentWord.isEmpty() && input.endsWith(" ")) {
            // Can provide context-aware suggestions here
        }

        return results;
    }

    /**
     * Get current word at cursor position
     */
    private String getCurrentWord(String buffer, int cursor) {
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(buffer.charAt(start - 1))) {
            start--;
        }
        return buffer.substring(start, cursor);
    }

    /**
     * Get command suggestions
     */
    private List<SuggestionResult> getCommandSuggestions(String prefix) {
        List<SuggestionResult> results = new ArrayList<>();
        
        for (Suggestion cmd : BUILTIN_COMMANDS) {
            if (cmd.matches(prefix)) {
                results.add(new SuggestionResult(
                    cmd.value(),
                    cmd.description(),
                    cmd.value(),
                    "command"
                ));
            }
        }
        
        return results;
    }

    /**
     * Get file suggestions
     */
    private List<SuggestionResult> getFileSuggestions(String currentWord) {
        List<SuggestionResult> results = new ArrayList<>();
        
        // Extract path part after @
        String pathPart = currentWord.substring(1);
        
        Path dir;
        String prefix;
        
        if (pathPart.contains("/")) {
            int lastSlash = pathPart.lastIndexOf('/');
            dir = projectRoot.resolve(pathPart.substring(0, lastSlash));
            prefix = pathPart.substring(lastSlash + 1);
        } else {
            dir = projectRoot;
            prefix = pathPart;
        }

        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .filter(p -> prefix.isEmpty() || p.getFileName().toString().startsWith(prefix))
                    .limit(20)  // Limit count
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        boolean isDir = Files.isDirectory(p);
                        String relativePath = projectRoot.relativize(p).toString();
                        
                        results.add(new SuggestionResult(
                            "@" + relativePath,
                            isDir ? "Directory" : "File",
                            name + (isDir ? "/" : ""),
                            "file"
                        ));
                    });
            } catch (IOException e) {
                // Ignore
            }
        }
        
        return results;
    }

    /**
     * Suggestion result
     */
    public record SuggestionResult(
        String value,       // Full value (for replacement)
        String description, // Description
        String display,     // Display text
        String type         // Type: command, file
    ) {}
}