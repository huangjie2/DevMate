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
 * Git 状态 Skill
 */
@ApplicationScoped
public class GitStatusSkill implements Skill {

    @Override
    public String name() {
        return "git_status";
    }

    @Override
    public String description() {
        return "获取 Git 仓库状态，包括当前分支、暂存区和工作区变更。";
    }

    @Override
    public JsonNode inputSchema() {
        return JsonNodeFactory.instance.objectNode()
            .put("type", "object")
            .set("properties", JsonNodeFactory.instance.objectNode()
                .set("workdir", JsonNodeFactory.instance.objectNode()
                    .put("type", "string")
                    .put("description", "工作目录（可选，默认当前目录）"))
                .set("short", JsonNodeFactory.instance.objectNode()
                    .put("type", "boolean")
                    .put("description", "是否使用简短格式")
                    .put("default", false))
            );
    }

    @Override
    public Result<SkillResult> execute(SkillInput input) {
        String workdir = input.getString("workdir");
        boolean shortFormat = input.getBoolean("short", false);

        try {
            // git status
            List<String> args = new ArrayList<>();
            args.add("status");
            if (shortFormat) {
                args.add("-s");
            }
            
            String output = executeGit(workdir, args.toArray(new String[0]));

            // 获取当前分支
            String branch = executeGit(workdir, "branch", "--show-current").trim();

            Map<String, Object> metadata = Map.of(
                "branch", branch,
                "workdir", workdir != null ? workdir : "."
            );

            return Result.success(new SkillResult(
                String.format("当前分支: %s\n\n%s", branch, output),
                metadata
            ));

        } catch (Exception e) {
            return Result.failure("Git 操作失败: " + e.getMessage());
        }
    }

    @Override
    public boolean requiresConfirmation() {
        return false; // 只读操作，不需要确认
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
