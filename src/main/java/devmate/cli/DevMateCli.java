package devmate.cli;

import devmate.agent.Agent;
import devmate.agent.AgentEvent;
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
        // 彩色 ASCII Logo
        System.out.println(CliStyle.CYAN + "╔═══════════════════════════════════════════════════════════╗" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "                                                           " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "██████╗ ███████╗██████╗ ███╗   ███╗" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "██╔══██╗██╔════╝██╔══██╗████╗ ████║" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "██║  ██║█████╗  ██║  ██║██╔████╔██║" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "██║  ██║██╔══╝  ██║  ██║██║╚██╔╝██║" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "██████╔╝███████╗██████╔╝██║ ╚═╝ ██║" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BRIGHT_MAGENTA + "╚═════╝ ╚══════╝╚═════╝ ╚═╝     ╚═╝" + CliStyle.RESET + "                    " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "                                                           " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.BOLD + CliStyle.BRIGHT_CYAN + "✨ AI-Powered Development Assistant" + CliStyle.RESET + "                       " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "   " + CliStyle.muted("Version 1.0.0 • Quarkus + LangChain4j") + "                              " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "║" + CliStyle.RESET + "                                                           " + CliStyle.CYAN + "║" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "╚═══════════════════════════════════════════════════════════╝" + CliStyle.RESET);
        System.out.println();
        
        // 快捷提示
        System.out.println("  " + CliStyle.command("/help") + "  查看帮助  " + CliStyle.command("/exit") + "  退出程序");
        System.out.println("  " + CliStyle.highlight("@filename") + "  引用文件  " + CliStyle.highlight("Tab") + "  自动补全");
        System.out.println();
    }

    /**
     * 主循环
     */
    private int mainLoop() {
        // 彩色提示符
        String prompt = CliStyle.BOLD + CliStyle.BRIGHT_GREEN + "devmate" + CliStyle.RESET + 
                        CliStyle.BRIGHT_CYAN + ">" + CliStyle.RESET + " ";
        
        while (true) {
            String input;
            try {
                input = reader.readLine(prompt);
            } catch (UserInterruptException e) {
                // Ctrl+C
                System.out.println();
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

        printExitMessage();
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
            System.out.println("  " + CliStyle.FOLDER + " " + CliStyle.title("检测到 " + refs.size() + " 个文件引用"));
            for (FileReferenceParser.FileReference ref : refs) {
                if (ref.success()) {
                    String size = formatSize(ref.content().length());
                    System.out.println("     " + CliStyle.success(ref.reference()) + " " + CliStyle.muted("(" + size + ")"));
                } else {
                    System.out.println("     " + CliStyle.error(ref.reference()) + " " + CliStyle.muted(ref.error()));
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
                yield true;
            }
            case "/reset" -> {
                agent.reset();
                System.out.println();
                System.out.println("  " + CliStyle.success("上下文已清空"));
                System.out.println();
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
                CliStyle.clearScreen();
                yield false;
            }
            default -> {
                System.out.println();
                System.out.println("  " + CliStyle.error("未知命令: " + command));
                System.out.println("  " + CliStyle.muted("输入 /help 查看可用命令"));
                System.out.println();
                yield false;
            }
        };
    }

    /**
     * 打印退出信息
     */
    private void printExitMessage() {
        System.out.println();
        System.out.println("  " + CliStyle.SPARKLE + " " + CliStyle.title("感谢使用 DevMate!"));
        System.out.println("  " + CliStyle.muted("日志文件: ~/.devmate/logs/devmate.log"));
        System.out.println();
    }

    /**
     * 打印帮助
     */
    private void printHelp() {
        System.out.println();
        System.out.println("  " + CliStyle.title("📖 帮助信息"));
        System.out.println();
        System.out.println(CliStyle.CYAN + "  ┌─────────────────────────────────────────────────────┐" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + " " + CliStyle.BOLD + "命令" + CliStyle.RESET + "                                             " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  ├─────────────────────────────────────────────────────┤" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/help") + "  显示帮助信息                            " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/exit") + "  退出程序                                " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/reset") + " 清空对话上下文                          " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/skills") + " 列出可用的技能                          " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/config") + " 显示当前配置                            " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/clear") + " 清屏                                    " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  └─────────────────────────────────────────────────────┘" + CliStyle.RESET);
        System.out.println();
        System.out.println("  " + CliStyle.title("📄 文件引用"));
        System.out.println();
        System.out.println("    " + CliStyle.highlight("@filename") + "     引用文件内容");
        System.out.println("    " + CliStyle.highlight("@path/to/file") + " 引用相对路径文件");
        System.out.println("    " + CliStyle.highlight("@.") + "           引用项目结构");
        System.out.println();
        System.out.println("  " + CliStyle.title("💡 示例"));
        System.out.println();
        System.out.println("    " + CliStyle.muted("分析 @src/main/java/App.java 这个文件"));
        System.out.println("    " + CliStyle.muted("对比 @file1.java 和 @file2.java"));
        System.out.println();
        System.out.println("  " + CliStyle.info("按 Tab 键可以自动补全命令和文件路径"));
        System.out.println();
    }

    /**
     * 打印可用技能
     */
    private void printSkills() {
        System.out.println();
        System.out.println("  " + CliStyle.title("🔧 可用技能 (" + skillRegistry.size() + " 个)"));
        System.out.println();
        System.out.println(CliStyle.CYAN + "  ┌──────────────────────────────────────────────────────────────┐" + CliStyle.RESET);
        
        skillRegistry.allSkills().forEach(skill -> {
            System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + CliStyle.CYAN + 
                String.format("%-18s", skill.name()) + CliStyle.RESET + " " + 
                CliStyle.muted(String.format("%-38s", truncateText(skill.description(), 38))) + " " + CliStyle.CYAN + "│" + CliStyle.RESET);
        });
        
        System.out.println(CliStyle.CYAN + "  └──────────────────────────────────────────────────────────────┘" + CliStyle.RESET);
        System.out.println();
    }

    /**
     * 打印配置
     */
    private void printConfig() {
        System.out.println();
        System.out.println("  " + CliStyle.title("⚙️ 当前配置"));
        System.out.println();
        System.out.println(CliStyle.CYAN + "  ┌─────────────────────────────────────────────────────┐" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "项目根目录" + CliStyle.RESET + "    " + CliStyle.highlight(configLoader.getProjectRoot().toString()) + CliStyle.RESET);
        
        configLoader.loadClaudeConfig().ifPresent(config -> {
            System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "项目名称" + CliStyle.RESET + "      " + CliStyle.highlight(config.name()));
            System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "项目类型" + CliStyle.RESET + "      " + CliStyle.highlight(config.type()));
        });
        
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "注册技能数" + CliStyle.RESET + "    " + CliStyle.highlight(String.valueOf(skillRegistry.size())));
        System.out.println(CliStyle.CYAN + "  └─────────────────────────────────────────────────────┘" + CliStyle.RESET);
        System.out.println();
    }
    
    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * 执行 Agent
     */
    private void executeAgent(String input) {
        System.out.println();

        try {
            // 使用流式输出，实时显示进度
            var publisher = agent.runStream(input);
            var latch = new java.util.concurrent.CountDownLatch(1);
            var outputBuilder = new StringBuilder();
            var steps = new int[]{0};
            var toolCalls = new int[]{0};
            var spinnerIndex = new int[]{0};

            publisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
                private java.util.concurrent.Flow.Subscription subscription;

                @Override
                public void onSubscribe(java.util.concurrent.Flow.Subscription s) {
                    this.subscription = s;
                    s.request(1);
                }

                @Override
                public void onNext(AgentEvent event) {
                    switch (event) {
                        case AgentEvent.Thinking thinking -> {
                            String spinner = CliStyle.SPINNER[spinnerIndex[0] % CliStyle.SPINNER.length];
                            spinnerIndex[0]++;
                            System.out.print("\r" + " ".repeat(50) + "\r");
                            System.out.print("  " + CliStyle.BRIGHT_CYAN + spinner + CliStyle.RESET + " " + 
                                CliStyle.muted(truncate(thinking.content(), 45)));
                        }
                        case AgentEvent.ToolCall toolCall -> {
                            toolCalls[0]++;
                            System.out.print("\r" + " ".repeat(50) + "\r");
                            System.out.println("  " + CliStyle.WRENCH + " " + CliStyle.highlight(toolCall.skillName()));
                        }
                        case AgentEvent.ToolResult toolResult -> {
                            steps[0]++;
                            System.out.print("\r" + " ".repeat(50) + "\r");
                            System.out.println("  " + CliStyle.SUCCESS + " " + CliStyle.success(toolResult.skillName()));
                        }
                        case AgentEvent.FinalAnswer answer -> {
                            outputBuilder.append(answer.content());
                        }
                        case AgentEvent.Error error -> {
                            System.out.println();
                            System.out.println("  " + CliStyle.error(error.message()));
                        }
                    }
                    subscription.request(1);
                }

                @Override
                public void onError(Throwable t) {
                    System.out.println();
                    System.out.println("  " + CliStyle.error("执行失败: " + t.getMessage()));
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            // 等待完成
            latch.await(120, java.util.concurrent.TimeUnit.SECONDS);

            // 输出最终结果
            String output = outputBuilder.toString();
            if (!output.isEmpty()) {
                System.out.println();
                System.out.println(formatOutput(output));
                System.out.println();
                System.out.println("  " + CliStyle.muted("───"));
                System.out.println("  " + CliStyle.badge("完成", CliStyle.BG_GREEN) + " " + 
                    CliStyle.muted("步骤: " + steps[0] + " • 工具调用: " + toolCalls[0]));
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println();
            System.out.println("  " + CliStyle.error("执行失败: " + e.getMessage()));
            Log.errorf(e, "Agent execution failed");
        }

        System.out.println();
    }

    /**
     * 格式化输出内容（高亮任务计划）
     */
    private String formatOutput(String content) {
        // 高亮任务计划格式
        return content
            // 高亮任务计划标题
            .replaceAll("📋\\s*\\*\\*任务计划\\*\\*", "\n📋 \u001B[1;36m任务计划\u001B[0m\n")
            // 高亮待办项
            .replaceAll("-\\s*\\[\\s*\\]\\s*(\\d+\\.)", "  ⏳ \u001B[33m$1\u001B[0m")
            // 高亮已完成项
            .replaceAll("-\\s*\\[x\\]\\s*(\\d+\\.)", "  ✅ \u001B[32m$1\u001B[0m")
            // 高亮正在执行
            .replaceAll("-\\s*\\[\\s*\\]\\s*\\*\\*(.+?)\\*\\*", "  🔄 \u001B[1;33m**$1**\u001B[0m");
    }

    /**
     * 截断文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        String singleLine = text.replace("\n", " ").trim();
        if (singleLine.length() <= maxLength) return singleLine;
        return singleLine.substring(0, maxLength - 3) + "...";
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