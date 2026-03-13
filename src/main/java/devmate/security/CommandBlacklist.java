package devmate.security;

import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Command Blacklist Validator
 * 
 * Blocks dangerous shell commands from being executed
 */
@ApplicationScoped
public class CommandBlacklist {

    /**
     * Dangerous command blacklist
     */
    private static final Set<String> BLACKLIST_COMMANDS = Set.of(
        // Delete commands
        "rm", "rmdir", "del", "erase",
        // Format commands
        "format", "mkfs", "fdisk",
        // System commands
        "shutdown", "reboot", "halt", "poweroff", "init",
        // Disk operations
        "dd",
        // Permission changes
        "chmod", "chown",
        // Network dangerous commands
        "iptables", "ip6tables", "ufw",
        // User management
        "userdel", "useradd", "passwd",
        // Dangerous scripts
        "eval", "exec"
    );

    /**
     * Dangerous patterns (regex matching)
     */
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        // rm -rf /
        Pattern.compile("rm\\s+(-[rf]+\\s+)*(/|\\*)", Pattern.CASE_INSENSITIVE),
        // Delete system directories
        Pattern.compile("rm\\s+.*(/bin|/boot|/dev|/etc|/lib|/proc|/root|/sbin|/sys|/usr)", Pattern.CASE_INSENSITIVE),
        // Format disk
        Pattern.compile("mkfs\\s+", Pattern.CASE_INSENSITIVE),
        // dd write
        Pattern.compile("dd\\s+.*of=/dev/", Pattern.CASE_INSENSITIVE),
        // Privilege escalation
        Pattern.compile("chmod\\s+[0-7]*777", Pattern.CASE_INSENSITIVE),
        // Pipe to shell
        Pattern.compile("\\|\\s*(ba)?sh", Pattern.CASE_INSENSITIVE),
        // Backticks or $() command substitution
        Pattern.compile("`[^`]+`"),
        Pattern.compile("\\$\\([^)]+\\)"),
        // Environment variable injection
        Pattern.compile("\\$\\{[^}]*:-[^}]*\\}")
    );

    /**
     * Validate if command is safe
     * 
     * @param command Command to validate
     * @return Validation result
     */
    public Result<String> validate(String command) {
        if (command == null || command.isBlank()) {
            return Result.failure("Command cannot be empty");
        }

        // Extract first word of command
        String firstWord = extractFirstWord(command);

        // Check blacklist commands
        if (BLACKLIST_COMMANDS.contains(firstWord.toLowerCase())) {
            String error = String.format("Dangerous command '%s' is prohibited", firstWord);
            Log.warnf(error);
            return Result.failure(error);
        }

        // Check dangerous patterns
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(command).find()) {
                String error = String.format("Command contains dangerous pattern: %s", pattern.pattern());
                Log.warnf("Dangerous command pattern detected: %s in command: %s", pattern.pattern(), command);
                return Result.failure(error);
            }
        }

        Log.debugf("Command validated: %s", command);
        return Result.success(command);
    }

    /**
     * Check if command is safe (without error message)
     * 
     * @param command Command to check
     * @return Whether safe
     */
    public boolean isSafe(String command) {
        return validate(command).isSuccess();
    }

    /**
     * Get command blacklist
     */
    public Set<String> getBlacklist() {
        return Set.copyOf(BLACKLIST_COMMANDS);
    }

    /**
     * Extract first word from command
     */
    private String extractFirstWord(String command) {
        return Arrays.stream(command.trim().split("\\s+"))
            .findFirst()
            .orElse("");
    }

    /**
     * Add custom blacklist command
     * Note: This is runtime modification, will not persist
     */
    public void addToBlacklist(String command) {
        // Since BLACKLIST_COMMANDS is an immutable Set, we can only log
        Log.warnf("Attempted to add '%s' to blacklist, but runtime modification is not supported", command);
    }
}