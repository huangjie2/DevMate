package devmate.security;

import devmate.util.Result;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 命令黑名单校验器
 * 
 * 阻止危险的 Shell 命令执行
 */
@ApplicationScoped
public class CommandBlacklist {

    /**
     * 危险命令黑名单
     */
    private static final Set<String> BLACKLIST_COMMANDS = Set.of(
        // 删除命令
        "rm", "rmdir", "del", "erase",
        // 格式化命令
        "format", "mkfs", "fdisk",
        // 系统命令
        "shutdown", "reboot", "halt", "poweroff", "init",
        // 磁盘操作
        "dd",
        // 权限修改
        "chmod", "chown",
        // 网络危险命令
        "iptables", "ip6tables", "ufw",
        // 用户管理
        "userdel", "useradd", "passwd",
        // 危险脚本
        "eval", "exec"
    );

    /**
     * 危险模式（正则匹配）
     */
    private static final List<Pattern> DANGEROUS_PATTERNS = List.of(
        // rm -rf /
        Pattern.compile("rm\\s+(-[rf]+\\s+)*(/|\\*)", Pattern.CASE_INSENSITIVE),
        // 删除系统目录
        Pattern.compile("rm\\s+.*(/bin|/boot|/dev|/etc|/lib|/proc|/root|/sbin|/sys|/usr)", Pattern.CASE_INSENSITIVE),
        // 格式化磁盘
        Pattern.compile("mkfs\\s+", Pattern.CASE_INSENSITIVE),
        // dd 写入
        Pattern.compile("dd\\s+.*of=/dev/", Pattern.CASE_INSENSITIVE),
        // 权限提升
        Pattern.compile("chmod\\s+[0-7]*777", Pattern.CASE_INSENSITIVE),
        // 管道到 shell
        Pattern.compile("\\|\\s*(ba)?sh", Pattern.CASE_INSENSITIVE),
        // 反引号或 $() 命令替换
        Pattern.compile("`[^`]+`"),
        Pattern.compile("\\$\\([^)]+\\)"),
        // 环境变量注入
        Pattern.compile("\\$\\{[^}]*:-[^}]*\\}")
    );

    /**
     * 校验命令是否安全
     * 
     * @param command 要校验的命令
     * @return 校验结果
     */
    public Result<String> validate(String command) {
        if (command == null || command.isBlank()) {
            return Result.failure("命令不能为空");
        }

        // 提取命令的第一个词
        String firstWord = extractFirstWord(command);

        // 检查黑名单命令
        if (BLACKLIST_COMMANDS.contains(firstWord.toLowerCase())) {
            String error = String.format("危险命令 '%s' 已被禁止", firstWord);
            Log.warnf(error);
            return Result.failure(error);
        }

        // 检查危险模式
        for (Pattern pattern : DANGEROUS_PATTERNS) {
            if (pattern.matcher(command).find()) {
                String error = String.format("命令包含危险模式: %s", pattern.pattern());
                Log.warnf("Dangerous command pattern detected: %s in command: %s", pattern.pattern(), command);
                return Result.failure(error);
            }
        }

        Log.debugf("Command validated: %s", command);
        return Result.success(command);
    }

    /**
     * 检查命令是否安全（不返回错误信息）
     * 
     * @param command 要检查的命令
     * @return 是否安全
     */
    public boolean isSafe(String command) {
        return validate(command).isSuccess();
    }

    /**
     * 获取命令黑名单
     */
    public Set<String> getBlacklist() {
        return Set.copyOf(BLACKLIST_COMMANDS);
    }

    /**
     * 提取命令的第一个词
     */
    private String extractFirstWord(String command) {
        return Arrays.stream(command.trim().split("\\s+"))
            .findFirst()
            .orElse("");
    }

    /**
     * 添加自定义黑名单命令
     * 注意：这是运行时修改，不会持久化
     */
    public void addToBlacklist(String command) {
        // 由于 BLACKLIST_COMMANDS 是不可变的 Set，这里只能记录日志
        Log.warnf("Attempted to add '%s' to blacklist, but runtime modification is not supported", command);
    }
}
