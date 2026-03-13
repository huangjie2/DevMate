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
 * Shell 命令执行 Skill
 */
@ApplicationScoped
public class ShellExecuteSkill implements Skill {

    @Inject
    CommandBlacklist commandBlacklist;

    private static final long DEFAULT_TIMEOUT = 30_000; // 30 秒

    @Override
    public String name() {
        return "execute_shell";
    }

    @Override
    public String description() {
        return "执行 Shell 命令。危险命令会被拦截。支持设置工作目录和超时时间。";
    }

    @Override
    public JsonNode inputSchema() {
        var factory = JsonNodeFactory.instance;
        var schema = factory.objectNode();
        schema.put("type", "object");
        
        var properties = factory.objectNode();
        
        var commandProp = factory.objectNode();
        commandProp.put("type", "string");
        commandProp.put("description", "要执行的命令");
        properties.set("command", commandProp);
        
        var workdirProp = factory.objectNode();
        workdirProp.put("type", "string");
        workdirProp.put("description", "工作目录（可选，默认当前目录）");
        properties.set("workdir", workdirProp);
        
        var timeoutProp = factory.objectNode();
        timeoutProp.put("type", "integer");
        timeoutProp.put("description", "超时时间（毫秒，可选，默认 30000）");
        properties.set("timeout", timeoutProp);
        
        schema.set("properties", properties);
        
        var required = factory.arrayNode();
        required.add("command");
        schema.set("required", required);
        
        return schema;
    }

    @Override
    public boolean requiresConfirmation() {
        return true; // Shell 命令总是需要确认
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
            return Result.failure("命令不能为空");
        }

        // 黑名单检查
        Result<String> validationResult = commandBlacklist.validate(command);
        if (validationResult.isFailure()) {
            return Result.failure(((Result.Failure<String>) validationResult).error());
        }

        long actualTimeout = timeout != null ? timeout : DEFAULT_TIMEOUT;

        try {
            // 构建进程
            ProcessBuilder pb = new ProcessBuilder();
            
            // 根据操作系统设置 shell
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb.command("cmd", "/c", command);
            } else {
                pb.command("/bin/sh", "-c", command);
            }

            // 设置工作目录
            if (workdir != null && !workdir.isBlank()) {
                File workDir = new File(workdir);
                if (workDir.exists() && workDir.isDirectory()) {
                    pb.directory(workDir);
                } else {
                    return Result.failure("工作目录不存在或不是目录: " + workdir);
                }
            }

            pb.redirectErrorStream(true);

            Log.infof("Executing command: %s (workdir: %s, timeout: %dms)", 
                command, pb.directory(), actualTimeout);

            // 启动进程
            Process process = pb.start();

            // 等待执行完成（带超时）
            boolean finished = process.waitFor(actualTimeout, TimeUnit.MILLISECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                return Result.failure("命令执行超时（" + actualTimeout + "ms）");
            }

            // 读取输出
            String output = new String(process.getInputStream().readAllBytes());

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return Result.failure(
                    "命令执行失败（退出码: " + exitCode + "）\n" + output
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
            return Result.failure("命令执行异常: " + e.getMessage());
        }
    }

    @Override
    public String category() {
        return "shell";
    }
}
