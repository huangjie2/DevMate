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
 * Git 提交 Skill
 */
@ApplicationScoped
public class GitCommitSkill implements Skill {

    @Override
    public String name() {
        return "git_commit";
    }

    @Override
    public String description() {
        return "提交暂存区的更改到本地仓库。";
    }

    @Override
    public JsonNode inputSchema() {
        return JsonNodeFactory.instance.objectNode()
            .put("type", "object")
            .set("properties", JsonNodeFactory.instance.objectNode()
                .set("message", JsonNodeFactory.instance.objectNode()
                    .put("type", "string")
                    .put("description", "提交信息"))
                .set("files", JsonNodeFactory.instance.objectNode()
                    .put("type", "array")
                    .set("items", JsonNodeFactory.instance.objectNode()
                        .put("type", "string"))
                    .put("description", "要提交的文件列表（可选，默认全部暂存）"))
                .set("workdir", JsonNodeFactory.instance.objectNode()
                    .put("type", "string")
                    .put("description", "工作目录（可选）"))
            );
    }

    @Override
    public boolean requiresConfirmation() {
        return true; // 提交操作需要确认
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String message = input.getString("message");
        @SuppressWarnings("unchecked")
        List<String> files = (List<String>) input.params().get("files");
        String workdir = input.getString("workdir");

        if (message == null || message.isBlank()) {
            return Result.failure("提交信息不能为空");
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

            // 获取 commit hash
            String hash = executeGit(workdir, "rev-parse", "--short", "HEAD").trim();

            Map<String, Object> metadata = Map.of(
                "commitHash", hash,
                "message", message,
                "files", files != null ? files : List.of("all")
            );

            Log.infof("Git commit: %s (%s)", hash, message);

            return Result.success(new SkillResult(
                String.format("提交成功: %s\n%s", hash, output),
                metadata
            ));

        } catch (Exception e) {
            Log.errorf(e, "Git commit failed");
            return Result.failure("Git 操作失败: " + e.getMessage());
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
            throw new RuntimeException("Git 命令超时");
        }

        String output = new String(process.getInputStream().readAllBytes());

        if (process.exitValue() != 0) {
            throw new RuntimeException("Git 命令失败:\n" + output);
        }

        return output;
    }
}
