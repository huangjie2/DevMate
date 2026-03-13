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
 * 命令补全器
 * 支持：
 * - / 命令补全
 * - @ 文件路径补全
 * - skill 名称补全
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

        // 查找当前光标所在的单词
        String currentWord = getCurrentWord(buffer, cursor);

        // 1. 命令补全 (/)
        if (currentWord.startsWith("/") || buffer.trim().startsWith("/")) {
            completeCommand(currentWord, candidates);
            return;
        }

        // 2. 文件引用补全 (@)
        if (currentWord.startsWith("@") || isInFileReference(buffer, cursor)) {
            completeFileReference(buffer, cursor, currentWord, candidates);
            return;
        }

        // 3. 技能名称补全（在调用技能时）
        if (buffer.toLowerCase().contains("use ") || buffer.toLowerCase().contains("调用 ")) {
            completeSkillName(currentWord, candidates);
        }
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
     * 检查光标是否在文件引用中
     */
    private boolean isInFileReference(String buffer, int cursor) {
        // 向前查找最近的 @ 符号
        for (int i = cursor - 1; i >= 0; i--) {
            char c = buffer.charAt(i);
            if (c == '@') {
                // 检查 @ 后面到光标之间是否有空格
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
     * 命令补全
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
     * 文件引用补全
     */
    private void completeFileReference(String buffer, int cursor, String currentWord, List<Candidate> candidates) {
        // 找到 @ 的位置
        int atIndex = buffer.lastIndexOf('@', cursor);
        if (atIndex < 0) {
            return;
        }

        // 获取 @ 后面的路径部分
        String pathAfterAt = buffer.substring(atIndex + 1, cursor);
        
        // 解析目录和文件前缀
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

        // 列出目录下的匹配文件
        if (Files.isDirectory(dir)) {
            try (Stream<Path> stream = Files.list(dir)) {
                stream.filter(p -> {
                    String name = p.getFileName().toString();
                    return prefix.isEmpty() || name.startsWith(prefix);
                }).forEach(p -> {
                    String name = p.getFileName().toString();
                    String fullPath = "@" + projectRoot.relativize(p);
                    
                    // 构建候选补全
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
                // 忽略错误
            }
        }
    }

    /**
     * 技能名称补全
     */
    private void completeSkillName(String currentWord, List<Candidate> candidates) {
        String lowerWord = currentWord.toLowerCase();
        skillRegistry.allSkills().stream()
            .map(skill -> skill.name())
            .filter(name -> name.toLowerCase().startsWith(lowerWord))
            .forEach(name -> candidates.add(new Candidate(name)));
    }
}
