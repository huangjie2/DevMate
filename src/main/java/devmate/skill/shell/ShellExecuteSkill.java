package devmate.skill.shell;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import devmate.security.CommandBlacklist;
import devmate.skill.Skill;
import devmate.skill.SkillInput;
import devmate.skill.SkillResult;
import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Shell Command Execution Skill
 */
@ApplicationScoped
public class ShellExecuteSkill implements Skill {

    @Inject
    CommandBlacklist commandBlacklist;

    private static final long DEFAULT_TIMEOUT = 30_000; // 30 seconds

    @Override
    public String name() {
        return "execute_shell";
    }

    @Override
    public String description() {
        return "Execute shell commands. Dangerous commands are blocked. Supports working directory and timeout settings.";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var commandProp = factory.objectNode();
        commandProp.put("type", "string");
        commandProp.put("description", "Command to execute");
        properties.set("command", commandProp);
        
        var workdirProp = factory.objectNode();
        workdirProp.put("type", "string");
        workdirProp.put("description", "Working directory (optional, default: current directory)");
        properties.set("workdir", workdirProp);
        
        var timeoutProp = factory.objectNode();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "Timeout in milliseconds (optional, default: 30000)");
        properties.set("timeout", timeoutProp);
        
        schema.set("properties", properties);
        
        var required = factory.arrayNode();
        required.add("command");
        schema.set("required", required);
        
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true; // Shell commands always require confirmation
    }

    @Override
    public long timeout() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String command = input.getString("command");
        String workdir = input.getString("workdir");
        Long timeout = input.getLong("timeout");

        if (command == null || command.isBlank()) {
            return Result.failure("Command cannot be empty");
        }

        // Blacklist check
        Result<String> validationResult = commandBlacklist.validate(command);
        if (validationResult.isFailure()) {
            return Result.failure(((Result.Failure<String>) validationResult).error());
        }

        long actualTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        try {
            // Build process
            ProcessBuilder pb = new ProcessBuilder();
            
            // Set shell based on OS
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("/bin/sh", "-c", command);
            }

            // Set working directory
            if (workdir != null && !workdir.isBlank()) {
                File workDir = new File(workdir);
                if (workDir.exists() && workDir.isDirectory()) {
                    pb.directory(workDir);
                } else {
                    return Result.failure("Working directory does not exist or is not a directory: " + workdir);
                }
            }

            pb.redirectErrorStream(true);

            Log.infof("Executing command: %s (workdir: %s, timeout: %dms)", 
                command, pb.directory(), actualTimeout);

            // Start process
            Process process = pb.start();

            // Wait for completion with timeout
            boolean finished = process.waitFor(actualTimeout, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return Result.failure("Command execution timeout (" + actualTimeout + "ms)");
            }

            // Read output
            String output = new String(process.getInputStream().readAllBytes());

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return Result.failure(
                    "Command execution failed (exit code: " + exitCode + ")\n" + output
                );
            }

            Map<String, Object> metadata = Map.of(
                "exitCode", exitCode,
                "command", command,
                "workdir", workdir != null ? workdir : ".",
                "timeout", actualTimeout
            );

            Log.infof("Command executed successfully: %s (exit code: %d)", command, exitCode);

            return Result.success(new SkillResult(output, metadata));

        } catch (Exception e) {
            Log.errorf(e, "Failed to execute command: %s", command);
            return Result.failure("Command execution exception: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "shell";
    }
}