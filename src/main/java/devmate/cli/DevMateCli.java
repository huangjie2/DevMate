package devmate.cli;

import devmate.agent.Agent;
import devmate.agent.AgentOutput;
import devmate.config.ConfigLoader;
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
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.nio.file.Path;

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

    @Override
    public int run(String... args) throws Exception {
        // 初始化
        initializeTerminal();
        initializeProjectRoot(args);
        registerSkills();

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

    /**
     * 初始化终端
     */
    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();
        reader = LineReaderBuilder.builder()
            .terminal(terminal)
            .appName("DevMate")
            .build();
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

            // 执行 Agent
            executeAgent(trimmedInput);
        }

        System.out.println("\n再见!");
        return 0;
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
        System.out.println("直接输入问题或任务，Agent 会自动处理。");
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
