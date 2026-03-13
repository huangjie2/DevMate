package devmate.skill.git;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import devmate.skill.Skill;
import devmate.skill.SkillInput;
import devmate.skill.SkillResult;
import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Git Commit Skill
 */
@ApplicationScoped
public class GitCommitSkill implements Skill {

    @Override
    public String name() {
        return "git_commit";
    }

    @Override
    public String description() {
        return "Commit staged changes to local repository.";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var messageProp = factory.objectNode();
        messageProp.put("type", "string");
        messageProp.put("description", "Commit message");
        properties.set("message", messageProp);
        
        var filesProp = factory.objectNode();
        filesProp.put("type", "array");
        filesProp.put("description", "List of files to commit (optional, default: all staged)");
        var items = factory.objectNode();
        items.put("type", "string");
        filesProp.set("items", items);
        properties.set("files", filesProp);
        
        var workdirProp = factory.objectNode();
        workdirProp.put("type", "string");
        workdirProp.put("description", "Working directory (optional)");
        properties.set("workdir", workdirProp);
        
        schema.set("properties", properties);
        
        var required = factory.arrayNode();
        required.add("message");
        schema.set("required", required);
        
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true; // Commit operation requires confirmation
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String message = input.getString("message");
        @SuppressWarnings("unchecked")
        List<String> files = (List<String>) input.params().get("files");
        String workdir = input.getString("workdir");

        if (message == null || message.isBlank()) {
            return Result.failure("Commit message cannot be empty");
        }

        try {
            // 1. git add
            if (files != null && !files.isEmpty()) {
                for (String file : files) {
                    executeGit(workdir, "add", file);
                }
            } else {
                executeGit(workdir, "add", ".");
            }

            // 2. git commit
            String output = executeGit(workdir, "commit", "-m", message);

            // Get commit hash
            String hash = executeGit(workdir, "rev-parse", "--short", "HEAD").trim();

            Map<String, Object> metadata = Map.of(
                "commitHash", hash,
                "message", message,
                "files", files != null ? files : List.of("all")
            );

            Log.infof("Git commit: %s (%s)", hash, message);

            return Result.success(new SkillResult(
                String.format("Commit successful: %s\n%s", hash, output),
                metadata
            ));

        } catch (Exception e) {
            Log.errorf(e, "Git commit failed");
            return Result.failure("Git operation failed: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "git";
    }

    private String executeGit(String workdir, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(List.of(args));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        
        if (workdir != null) {
            File workDir = new File(workdir);
            if (workDir.exists()) {
                pb.directory(workDir);
            }
        }

        Process process = pb.start();
        boolean finished = process.waitFor(10, TimeUnit.SECONDS);
        
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Git command timeout");
        }

        String output = new String(process.getInputStream().readAllBytes());

        if (process.exitValue() != 0) {
            throw new RuntimeException("Git command failed:\n" + output);
        }

        return output;
    }
}