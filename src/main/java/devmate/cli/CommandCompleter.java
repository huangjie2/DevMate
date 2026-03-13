package devmate.cli;

import devmate.skill.SkillRegistry;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Command Completer
 * Supports:
 * - / command completion
 * - @ file path completion
 * - skill name completion
 */
public class CommandCompleter implements Completer {

    private static final List<String> BUILTIN_COMMANDS = List.of(
        "/help", "/h", "/exit", "/quit", "/q", "/reset", "/skills", "/config", "/clear"
    );

    private final SkillRegistry skillRegistry;
    private final Path projectRoot;

    public CommandCompleter(SkillRegistry skillRegistry, Path projectRoot) {
        this.skillRegistry = skillRegistry;
        this.projectRoot = projectRoot;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        String buffer = line.line();
        int cursor = line.cursor();

        if (buffer.isEmpty()) {
            return;
        }

        // Find current word at cursor
        String currentWord = getCurrentWord(buffer, cursor);

        // 1. Command completion (/)
        if (currentWord.startsWith("/") || buffer.trim().startsWith("/")) {
            completeCommand(currentWord, candidates);
            return;
        }

        // 2. File reference completion (@)
        if (currentWord.startsWith("@") || isInFileReference(buffer, cursor)) {
            completeFileReference(buffer, cursor, currentWord, candidates);
            return;
        }

        // 3. Skill name completion (when calling skills)
        if (buffer.toLowerCase().contains("use ")) {
            completeSkillName(currentWord, candidates);
        }
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
     * Check if cursor is in file reference
     */
    private boolean isInFileReference(String buffer, int cursor) {
        // Find nearest @ symbol going backwards
        for (int i = cursor - 1; i >= 0; i--) {
            char c = buffer.charAt(i);
            if (c == '@') {
                // Check if there's whitespace between @ and cursor
                String afterAt = buffer.substring(i + 1, cursor);
                return !afterAt.contains(" ") && !afterAt.contains("\t");
            }
            if (Character.isWhitespace(c)) {
                break;
            }
        }
        return false;
    }

    /**
     * Command completion
     */
    private void completeCommand(String currentWord, List<Candidate> candidates) {
        String prefix = currentWord.isEmpty() ? "/" : currentWord;
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }

        for (String cmd : BUILTIN_COMMANDS) {
            if (cmd.startsWith(prefix)) {
                candidates.add(new Candidate(cmd, cmd, null, null, null, null, true));
            }
        }
    }

    /**
     * File reference completion
     */
    private void completeFileReference(String buffer, int cursor, String currentWord, List<Candidate> candidates) {
        // Find @ position
        int atIndex = buffer.lastIndexOf('@', cursor);
        if (atIndex < 0) {
            return;
        }

        // Get path part after @
        String pathAfterAt = buffer.substring(atIndex + 1, cursor);
        
        // Parse directory and file prefix
        Path dir;
        String prefix;
        
        if (pathAfterAt.contains("/")) {
            int lastSlash = pathAfterAt.lastIndexOf('/');
            dir = projectRoot.resolve(pathAfterAt.substring(0, lastSlash));
            prefix = pathAfterAt.substring(lastSlash + 1);
        } else {
            dir = projectRoot;
            prefix = pathAfterAt;
        }

        // List matching files in directory
        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> {
                    String name = p.getFileName().toString();
                    return prefix.isEmpty() || name.startsWith(prefix);
                }).forEach(p -> {
                    String name = p.getFileName().toString();
                    String fullPath = "@" + projectRoot.relativize(p);
                    
                    // Build completion candidate
                    String display = name;
                    String desc = Files.isDirectory(p) ? "/" : "";
                    
                    candidates.add(new Candidate(
                        fullPath,
                        display,
                        null,
                        desc,
                        null,
                        null,
                        false
                    ));
                });
            } catch (IOException e) {
                // Ignore errors
            }
        }
    }

    /**
     * Skill name completion
     */
    private void completeSkillName(String currentWord, List<Candidate> candidates) {
        String lowerWord = currentWord.toLowerCase();
        skillRegistry.allSkills().stream()
            .map(skill -> skill.name())
            .filter(name -> name.toLowerCase().startsWith(lowerWord))
            .forEach(name -> candidates.add(new Candidate(name)));
    }
}