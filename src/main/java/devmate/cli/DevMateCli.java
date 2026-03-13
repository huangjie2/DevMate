package devmate.cli;

import devmate.agent.Agent;
import devmate.agent.AgentOutput;
import devmate.config.ConfigLoader;
import devmate.security.PathValidator;
import devmate.security.UserConfirmation;
import devmate.skill.file.FileListSkill;
import devmate.skill.file.FileReadSkill;
import devmate.skill.file.FileWriteSkill;
import devmate.skill.git.GitCommitSkill;
import devmate.skill.git.GitStatusSkill;
import devmate.skill.shell.ShellExecuteSkill;
import devmate.skill.SkillRegistry;
import devmate.util.Result;
import io.quarkus.logging.Log;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.history.DefaultHistory;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * DevMate CLI 入口
 */
@QuarkusMain
public class DevMateCli implements QuarkusApplication {

    @Inject
    Agent agent;

    @Inject
    SkillRegistry skillRegistry;

    @Inject
    ConfigLoader configLoader;

    @Inject
    UserConfirmation userConfirmation;

    @Inject
    PathValidator pathValidator;

    @Inject
    FileReadSkill fileReadSkill;

    @Inject
    FileWriteSkill fileWriteSkill;

    @Inject
    FileListSkill fileListSkill;

    @Inject
    ShellExecuteSkill shellExecuteSkill;

    @Inject
    GitStatusSkill gitStatusSkill;

    @Inject
    GitCommitSkill gitCommitSkill;

    private Terminal terminal;
    private LineReader reader;
    private FileReferenceParser fileReferenceParser;

    @Override
    public int run(String... args) throws Exception {
        // 初始化
        initializeTerminal();
        initializeProjectRoot(args);
        registerSkills();
        initializeFileReferenceParser();

        // 打印欢迎信息
        printWelcome();

        // 主循环
        int exitCode = 0;
        try {
            exitCode = mainLoop();
        } finally {
            cleanup();
        }

        return exitCode;
    }

    private AutoSuggestionProvider suggestionProvider;

    /**
     * 初始化终端
     */
    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();

        // 创建补全器和自动建议提供器
        CommandCompleter completer = new CommandCompleter(skillRegistry, configLoader.getProjectRoot());
        suggestionProvider = new AutoSuggestionProvider(skillRegistry, configLoader.getProjectRoot());

        reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("DevMate")
            .completer(completer)
            .parser(new DefaultParser())
            .history(new DefaultHistory())
            .variable(LineReader.HISTORY_FILE, Path.of(System.getProperty("user.home"), ".devmate_history"))
            .variable(LineReader.HISTORY_SIZE, 1000)
            .variable(LineReader.HISTORY_FILE_SIZE, 1000)
            // 补全样式
            .variable(LineReader.COMPLETION_STYLE_LIST_SELECTION, AttributedStyle.BOLD.foreground(AttributedStyle.CYAN))
            .variable(LineReader.COMPLETION_STYLE_LIST_DESCRIPTION, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW))
            .build();

        // 启用自动补全选项 - 关键配置实现类似 iFlow 的实时补全
        reader.setOpt(LineReader.Option.AUTO_GROUP);        // 自动分组
        reader.setOpt(LineReader.Option.AUTO_MENU);         // 自动显示菜单
        reader.setOpt(LineReader.Option.AUTO_MENU_LIST);    // 自动显示菜单列表
        reader.setOpt(LineReader.Option.CASE_INSENSITIVE);  // 大小写不敏感
        
        // 设置自动提示功能
        setupAutoSuggestions();
    }

    /**
     * 设置自动提示功能 - 输入时实时显示补全建议
     */
    private void setupAutoSuggestions() {
        // 启用自动建议，输入时自动显示补全
        reader.setAutosuggestion(LineReader.SuggestionType.COMPLETER);
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
     * 初始化项目根目录
     */
    private void initializeProjectRoot(String[] args) {
        Path projectRoot;
        if (args.length > 0) {
            projectRoot = Path.of(args[0]).toAbsolutePath();
        } else {
            projectRoot = Path.of(System.getProperty("user.dir"));
        }
        configLoader.setProjectRoot(projectRoot);
        Log.infof("Project root: %s", projectRoot);
    }

    /**
     * 初始化文件引用解析器
     */
    private void initializeFileReferenceParser() {
        fileReferenceParser = new FileReferenceParser(configLoader.getProjectRoot(), pathValidator);
    }

    /**
     * 注册内置 Skills
     */
    private void registerSkills() {
        skillRegistry.register(fileReadSkill);
        skillRegistry.register(fileWriteSkill);
        skillRegistry.register(fileListSkill);
        skillRegistry.register(shellExecuteSkill);
        skillRegistry.register(gitStatusSkill);
        skillRegistry.register(gitCommitSkill);
        Log.infof("Registered %d skills", skillRegistry.size());
    }

    /**
     * 打印欢迎信息
     */
    private void printWelcome() {
        System.out.println();
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                                                           ║");
        System.out.println("║   ██████╗ ███████╗██████╗ ███╗   ███╗                    ║");
        System.out.println("║   ██╔══██╗██╔════╝██╔══██╗████╗ ████║                    ║");
        System.out.println("║   ██║  ██║█████╗  ██║  ██║██╔████╔██║                    ║");
        System.out.println("║   ██║  ██║██╔══╝  ██║  ██║██║╚██╔╝██║                    ║");
        System.out.println("║   ██████╔╝███████╗██████╔╝██║ ╚═╝ ██║                    ║");
        System.out.println("║   ╚═════╝ ╚══════╝╚═════╝ ╚═╝     ╚═╝                    ║");
        System.out.println("║                                                           ║");
        System.out.println("║   AI-Powered Development Assistant                       ║");
        System.out.println("║   Version 1.0.0                                          ║");
        System.out.println("║                                                           ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("输入 /help 查看帮助，/exit 退出，/reset 清空上下文");
        System.out.println("使用 @filename 引用文件，Tab 显示补全菜单");
        System.out.println();
    }

    /**
     * 主循环
     */
    private int mainLoop() {
        while (true) {
            String input;
            try {
                input = reader.readLine("devmate> ");
            } catch (UserInterruptException e) {
                // Ctrl+C
                continue;
            } catch (EndOfFileException e) {
                // Ctrl+D
                break;
            }

            if (input == null || input.trim().isEmpty()) {
                continue;
            }

            String trimmedInput = input.trim();

            // 处理内置命令
            if (trimmedInput.startsWith("/")) {
                if (handleBuiltinCommand(trimmedInput)) {
                    break;
                }
                continue;
            }

            // 解析文件引用
            String processedInput = processFileReferences(trimmedInput);

            // 执行 Agent
            executeAgent(processedInput);
        }

        System.out.println("\n再见!");
        return 0;
    }

    /**
     * 处理文件引用
     */
    private String processFileReferences(String input) {
        if (!fileReferenceParser.hasFileReference(input)) {
            return input;
        }

        FileReferenceParser.ParseResult result = fileReferenceParser.parse(input);
        
        // 显示解析的文件引用
        List<FileReferenceParser.FileReference> refs = result.references();
        if (!refs.isEmpty()) {
            System.out.println();
            System.out.println("📂 检测到 " + refs.size() + " 个文件引用:");
            for (FileReferenceParser.FileReference ref : refs) {
                if (ref.success()) {
                    String size = formatSize(ref.content().length());
                    System.out.println("   ✓ " + ref.reference() + " (" + size + ")");
                } else {
                    System.out.println("   ✗ " + ref.reference() + " - " + ref.error());
                }
            }
            System.out.println();
        }

        return result.processedInput();
    }

    /**
     * 格式化文件大小
     */
    private String formatSize(int bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
    }

    /**
     * 处理内置命令
     * @return 是否应该退出
     */
    private boolean handleBuiltinCommand(String command) {
        return switch (command.toLowerCase()) {
            case "/exit", "/quit", "/q" -> {
                System.out.println("退出 DevMate...");
                yield true;
            }
            case "/reset" -> {
                agent.reset();
                System.out.println("✅ 上下文已清空");
                yield false;
            }
            case "/help", "/h", "/?" -> {
                printHelp();
                yield false;
            }
            case "/skills" -> {
                printSkills();
                yield false;
            }
            case "/config" -> {
                printConfig();
                yield false;
            }
            case "/clear" -> {
                System.out.print("\033[H\033[2J");
                System.out.flush();
                yield false;
            }
            default -> {
                System.out.println("❌ 未知命令: " + command);
                System.out.println("输入 /help 查看可用命令");
                yield false;
            }
        };
    }

    /**
     * 打印帮助
     */
    private void printHelp() {
        System.out.println();
        System.out.println("可用命令:");
        System.out.println("  /help, /h     显示帮助信息");
        System.out.println("  /exit, /q     退出程序");
        System.out.println("  /reset        清空对话上下文");
        System.out.println("  /skills       列出可用的技能");
        System.out.println("  /config       显示当前配置");
        System.out.println("  /clear        清屏");
        System.out.println();
        System.out.println("文件引用:");
        System.out.println("  @filename     引用文件内容");
        System.out.println("  @path/to/file 引用相对路径文件");
        System.out.println("  @.            引用项目结构");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  分析 @src/main/java/App.java 这个文件");
        System.out.println("  对比 @file1.java 和 @file2.java");
        System.out.println();
        System.out.println("按 Tab 键可以自动补全命令和文件路径。");
        System.out.println();
    }

    /**
     * 打印可用技能
     */
    private void printSkills() {
        System.out.println();
        System.out.println("可用技能 (" + skillRegistry.size() + " 个):");
        System.out.println();
        skillRegistry.allSkills().forEach(skill -> {
            System.out.printf("  %-20s %s%n", skill.name(), skill.description());
        });
        System.out.println();
    }

    /**
     * 打印配置
     */
    private void printConfig() {
        System.out.println();
        System.out.println("当前配置:");
        System.out.println();
        System.out.println("项目根目录: " + configLoader.getProjectRoot());
        
        configLoader.loadClaudeConfig().ifPresent(config -> {
            System.out.println("项目名称: " + config.name());
            System.out.println("项目类型: " + config.type());
        });
        
        System.out.println("注册技能数: " + skillRegistry.size());
        System.out.println();
    }

    /**
     * 执行 Agent
     */
    private void executeAgent(String input) {
        System.out.println();
        System.out.print("思考中...");

        try {
            Result<AgentOutput> result = agent.run(input);

            // 清除 "思考中..."
            System.out.print("\r" + " ".repeat(20) + "\r");

            switch (result) {
                case Result.Success<AgentOutput> success -> {
                    AgentOutput output = success.value();
                    System.out.println();
                    System.out.println(output.content());
                    System.out.println();
                    System.out.printf("[完成，步骤: %d，工具调用: %d]%n", output.steps(), output.toolCalls());
                }
                case Result.Failure<AgentOutput> failure -> {
                    System.out.println();
                    System.out.println("❌ 错误: " + failure.error());
                }
            }
        } catch (Exception e) {
            System.out.print("\r" + " ".repeat(20) + "\r");
            System.out.println();
            System.out.println("❌ 执行失败: " + e.getMessage());
            Log.errorf(e, "Agent execution failed");
        }

        System.out.println();
    }

    /**
     * 清理资源
     */
    private void cleanup() {
        userConfirmation.close();
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                Log.errorf(e, "Failed to close terminal");
            }
        }
    }
}