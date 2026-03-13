package devmate.cli;

import devmate.security.PathValidator;
import devmate.util.Result;
import io.quarkus.logging.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文件引用解析器
 * 
 * 支持格式：
 * - @filename        引用单个文件
 * - @path/to/file    引用相对路径文件
 * - @/absolute/path  引用绝对路径文件
 * - @.               引用整个项目目录结构
 * 
 * 示例：
 * - "帮我分析 @src/main/java/App.java 这个文件"
 * - "对比 @file1.java 和 @file2.java 的区别"
 */
public class FileReferenceParser {

    // 匹配 @文件引用 的正则
    private static final Pattern FILE_REFERENCE_PATTERN = Pattern.compile(
        "@([\\w./\\-~]+)"
    );

    private final Path projectRoot;
    private final PathValidator pathValidator;

    public FileReferenceParser(Path projectRoot, PathValidator pathValidator) {
        this.projectRoot = projectRoot;
        this.pathValidator = pathValidator;
    }

    /**
     * 解析结果
     */
    public record ParseResult(
        String processedInput,      // 处理后的输入（文件引用被替换为内容）
        List<FileReference> references  // 解析出的文件引用列表
    ) {}

    /**
     * 文件引用信息
     */
    public record FileReference(
        String reference,       // 原始引用（如 @src/App.java）
        String path,            // 实际路径
        String content,         // 文件内容
        boolean success,        // 是否成功读取
        String error            // 错误信息（如果失败）
    ) {}

    /**
     * 解析输入中的文件引用
     * 
     * @param input 用户输入
     * @return 解析结果
     */
    public ParseResult parse(String input) {
        List<FileReference> references = new ArrayList<>();
        String processedInput = input;

        Matcher matcher = FILE_REFERENCE_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String reference = matcher.group(0);  // @filename
            String pathStr = matcher.group(1);    // filename

            FileReference ref = resolveReference(pathStr);
            references.add(ref);

            if (ref.success()) {
                // 替换为文件内容块
                String replacement = formatFileContent(ref);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // 保留原始引用，添加错误注释
                String replacement = reference + " [错误: " + ref.error() + "]";
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
        }

        matcher.appendTail(sb);
        processedInput = sb.toString();

        Log.infof("Parsed %d file references from input", references.size());

        return new ParseResult(processedInput, references);
    }

    /**
     * 解析单个文件引用
     */
    private FileReference resolveReference(String pathStr) {
        // 特殊处理: @. 表示项目根目录
        if (".".equals(pathStr)) {
            return resolveProjectStructure();
        }

        // 构建完整路径
        Path path;
        if (pathStr.startsWith("/")) {
            // 绝对路径
            path = Path.of(pathStr);
        } else {
            // 相对路径
            path = projectRoot.resolve(pathStr);
        }

        // 安全校验
        Result<Path> validationResult = pathValidator.validate(path.toString());
        if (validationResult.isFailure()) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                ((Result.Failure<Path>) validationResult).error()
            );
        }

        // 检查是否存在
        if (!Files.exists(path)) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "文件不存在"
            );
        }

        // 检查是否为目录
        if (Files.isDirectory(path)) {
            return resolveDirectory(pathStr, path);
        }

        // 读取文件内容
        try {
            String content = Files.readString(path);
            Log.infof("Read file reference: %s (%d bytes)", pathStr, content.length());
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                content,
                true,
                null
            );
        } catch (IOException e) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "读取失败: " + e.getMessage()
            );
        }
    }

    /**
     * 解析目录结构
     */
    private FileReference resolveDirectory(String pathStr, Path path) {
        StringBuilder sb = new StringBuilder();
        sb.append("目录 ").append(pathStr).append(" 结构:\n\n");

        try {
            Files.walk(path, 3)  // 最多 3 层
                .filter(p -> !p.equals(path))
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .forEach(p -> {
                    String relative = path.relativize(p).toString();
                    String indent = "  ".repeat(Math.max(0, relative.split("/").length - 1));
                    String icon = Files.isDirectory(p) ? "📁 " : "📄 ";
                    sb.append(indent).append(icon).append(p.getFileName()).append("\n");
                });
        } catch (IOException e) {
            return new FileReference(
                "@" + pathStr,
                path.toString(),
                null,
                false,
                "读取目录失败: " + e.getMessage()
            );
        }

        return new FileReference(
            "@" + pathStr,
            path.toString(),
            sb.toString(),
            true,
            null
        );
    }

    /**
     * 解析项目根目录结构 (@.)
     */
    private FileReference resolveProjectStructure() {
        StringBuilder sb = new StringBuilder();
        sb.append("项目结构:\n\n");

        try {
            Files.walk(projectRoot, 2)
                .filter(p -> !p.equals(projectRoot))
                .filter(p -> !p.getFileName().toString().startsWith("."))
                .filter(p -> !p.toString().contains("target"))
                .filter(p -> !p.toString().contains("node_modules"))
                .forEach(p -> {
                    String relative = projectRoot.relativize(p).toString();
                    String indent = "  ".repeat(Math.max(0, relative.split("/").length - 1));
                    String icon = Files.isDirectory(p) ? "📁 " : "📄 ";
                    sb.append(indent).append(icon).append(p.getFileName()).append("\n");
                });
        } catch (IOException e) {
            return new FileReference(
                "@.",
                projectRoot.toString(),
                null,
                false,
                "读取项目结构失败: " + e.getMessage()
            );
        }

        return new FileReference(
            "@.",
            projectRoot.toString(),
            sb.toString(),
            true,
            null
        );
    }

    /**
     * 格式化文件内容为提示词格式
     */
    private String formatFileContent(FileReference ref) {
        return String.format(
            "\n--- 文件: %s ---\n%s\n--- 文件结束 ---\n",
            ref.path(),
            ref.content()
        );
    }

    /**
     * 检查输入中是否包含文件引用
     */
    public boolean hasFileReference(String input) {
        return FILE_REFERENCE_PATTERN.matcher(input).find();
    }

    /**
     * 提取所有文件引用路径
     */
    public List<String> extractFilePaths(String input) {
        List<String> paths = new ArrayList<>();
        Matcher matcher = FILE_REFERENCE_PATTERN.matcher(input);
        while (matcher.find()) {
            paths.add(matcher.group(1));
        }
        return paths;
    }
}
