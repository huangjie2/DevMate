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
 * 自动建议提供器
 * 实时显示匹配的命令/文件，类似 iFlow 体验
 */
public class AutoSuggestionProvider {

    public static final List<Suggestion> BUILTIN_COMMANDS = List.of(
        new Suggestion("/help", "显示帮助信息", "h"),
        new Suggestion("/exit", "退出程序", "q", "quit"),
        new Suggestion("/reset", "清空对话上下文", null),
        new Suggestion("/skills", "列出可用的技能", null),
        new Suggestion("/config", "显示当前配置", null),
        new Suggestion("/clear", "清屏", null)
    );

    private final SkillRegistry skillRegistry;
    private final Path projectRoot;

    public AutoSuggestionProvider(SkillRegistry skillRegistry, Path projectRoot) {
        this.skillRegistry = skillRegistry;
        this.projectRoot = projectRoot;
    }

    /**
     * 建议项
     */
    public record Suggestion(
        String value,       // 实际值
        String description, // 描述
        String alias,       // 别名
        String... aliases   // 其他别名
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
     * 获取所有匹配的建议
     */
    public List<SuggestionResult> getSuggestions(String input, int cursor) {
        List<SuggestionResult> results = new ArrayList<>();
        
        if (input == null || input.isEmpty() || cursor == 0) {
            return results;
        }

        String currentWord = getCurrentWord(input, cursor);
        
        // 命令建议 (/)
        if (currentWord.startsWith("/")) {
            results.addAll(getCommandSuggestions(currentWord));
        }
        // 文件建议 (@)
        else if (currentWord.startsWith("@")) {
            results.addAll(getFileSuggestions(currentWord));
        }
        // 空输入时显示所有命令
        else if (currentWord.isEmpty() && input.endsWith(" ")) {
            // 可以在这里提供上下文相关的建议
        }

        return results;
    }

    /**
     * 获取当前光标所在的单词
     */
    private String getCurrentWord(String buffer, int cursor) {
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(buffer.charAt(start - 1))) {
            start--;
        }
        return buffer.substring(start, cursor);
    }

    /**
     * 获取命令建议
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
     * 获取文件建议
     */
    private List<SuggestionResult> getFileSuggestions(String currentWord) {
        List<SuggestionResult> results = new ArrayList<>();
        
        // 提取 @ 后面的路径部分
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
                    .limit(20)  // 限制数量
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        boolean isDir = Files.isDirectory(p);
                        String relativePath = projectRoot.relativize(p).toString();
                        
                        results.add(new SuggestionResult(
                            "@" + relativePath,
                            isDir ? "目录" : "文件",
                            name + (isDir ? "/" : ""),
                            "file"
                        ));
                    });
            } catch (IOException e) {
                // 忽略
            }
        }
        
        return results;
    }

    /**
     * 建议结果
     */
    public record SuggestionResult(
        String value,       // 完整值（用于替换）
        String description, // 描述
        String display,     // 显示文本
        String type         // 类型: command, file
    ) {}
}
