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
 * DevMate CLI Entry Point
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
        // Initialize
        initializeTerminal();
        initializeProjectRoot(args);
        registerSkills();
        initializeFileReferenceParser();

        // Print welcome message
        printWelcome();

        // Main loop
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
     * Initialize terminal
     */
    private void initializeTerminal() throws IOException {
        terminal = TerminalBuilder.builder()
            .system(true)
            .build();

        // Create completer and auto-suggestion provider
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

        // Enable auto-completion options - key configuration for iFlow-like real-time completion
        reader.setOpt(LineReader.Option.AUTO_GROUP);        // Auto group
        reader.setOpt(LineReader.Option.AUTO_MENU);         // Auto show menu
        reader.setOpt(LineReader.Option.AUTO_MENU_LIST);    // Auto show menu list
        reader.setOpt(LineReader.Option.CASE_INSENSITIVE);  // Case insensitive
        
        // Setup auto-suggestion feature
        setupAutoSuggestions();
    }

    /**
     * Setup auto-suggestion - show completion suggestions in real-time
     */
    private void setupAutoSuggestions() {
        // Enable auto-suggestion, show completions while typing
        reader.setAutosuggestion(LineReader.SuggestionType.COMPLETER);
    }

    /**
     * Get current word at cursor position
     */
    private String getCurrentWord(String buffer, int cursor) {
        int start = cursor;
        while (start > 0 && !Character.isWhitespace(buffer.charAt(start - 1))) {
            start--;
        }
        return buffer.substring(start, cursor);
    }

    /**
     * Initialize project root directory
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
     * Initialize file reference parser
     */
    private void initializeFileReferenceParser() {
        fileReferenceParser = new FileReferenceParser(configLoader.getProjectRoot(), pathValidator);
    }

    /**
     * Register built-in Skills
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
     * Print welcome message
     */
    private void printWelcome() {
        System.out.println();
        // Colored ASCII Logo
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
        
        // Quick tips
        System.out.println("  " + CliStyle.command("/help") + "  Show help  " + CliStyle.command("/exit") + "  Exit program");
        System.out.println("  " + CliStyle.highlight("@filename") + "  Reference file  " + CliStyle.highlight("Tab") + "  Auto complete");
        System.out.println();
    }

    /**
     * Main loop
     */
    private int mainLoop() {
        // Colored prompt
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

            // Handle built-in commands
            if (trimmedInput.startsWith("/")) {
                if (handleBuiltinCommand(trimmedInput)) {
                    break;
                }
                continue;
            }

            // Parse file references
            String processedInput = processFileReferences(trimmedInput);

            // Execute Agent
            executeAgent(processedInput);
        }

        printExitMessage();
        return 0;
    }

    /**
     * Process file references
     */
    private String processFileReferences(String input) {
        if (!fileReferenceParser.hasFileReference(input)) {
            return input;
        }

        FileReferenceParser.ParseResult result = fileReferenceParser.parse(input);
        
        // Show parsed file references
        List<FileReferenceParser.FileReference> refs = result.references();
        if (!refs.isEmpty()) {
            System.out.println();
            System.out.println("  " + CliStyle.FOLDER + " " + CliStyle.title("Detected " + refs.size() + " file reference(s)"));
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
     * Format file size
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
     * Handle built-in commands
     * @return whether to exit
     */
    private boolean handleBuiltinCommand(String command) {
        return switch (command.toLowerCase()) {
            case "/exit", "/quit", "/q" -> {
                yield true;
            }
            case "/reset" -> {
                agent.reset();
                System.out.println();
                System.out.println("  " + CliStyle.success("Context cleared"));
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
                System.out.println("  " + CliStyle.error("Unknown command: " + command));
                System.out.println("  " + CliStyle.muted("Type /help for available commands"));
                System.out.println();
                yield false;
            }
        };
    }

    /**
     * Print exit message
     */
    private void printExitMessage() {
        System.out.println();
        System.out.println("  " + CliStyle.SPARKLE + " " + CliStyle.title("Thank you for using DevMate!"));
        System.out.println("  " + CliStyle.muted("Log file: ~/.devmate/logs/devmate.log"));
        System.out.println();
    }

    /**
     * Print help
     */
    private void printHelp() {
        System.out.println();
        System.out.println("  " + CliStyle.title("📖 Help"));
        System.out.println();
        System.out.println(CliStyle.CYAN + "  ┌─────────────────────────────────────────────────────┐" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + " " + CliStyle.BOLD + "Commands" + CliStyle.RESET + "                                          " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  ├─────────────────────────────────────────────────────┤" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/help") + "  Show help                               " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/exit") + "  Exit program                            " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/reset") + " Clear conversation context               " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/skills") + " List available skills                    " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/config") + " Show current configuration               " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.command("/clear") + " Clear screen                             " + CliStyle.CYAN + "│" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  └─────────────────────────────────────────────────────┘" + CliStyle.RESET);
        System.out.println();
        System.out.println("  " + CliStyle.title("📄 File References"));
        System.out.println();
        System.out.println("    " + CliStyle.highlight("@filename") + "     Reference file content");
        System.out.println("    " + CliStyle.highlight("@path/to/file") + " Reference relative path file");
        System.out.println("    " + CliStyle.highlight("@.") + "           Reference project structure");
        System.out.println();
        System.out.println("  " + CliStyle.title("💡 Examples"));
        System.out.println();
        System.out.println("    " + CliStyle.muted("Analyze @src/main/java/App.java"));
        System.out.println("    " + CliStyle.muted("Compare @file1.java and @file2.java"));
        System.out.println();
        System.out.println("  " + CliStyle.info("Press Tab to auto-complete commands and file paths"));
        System.out.println();
    }

    /**
     * Print available skills
     */
    private void printSkills() {
        System.out.println();
        System.out.println("  " + CliStyle.title("🔧 Available Skills (" + skillRegistry.size() + ")"));
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
     * Print configuration
     */
    private void printConfig() {
        System.out.println();
        System.out.println("  " + CliStyle.title("⚙️ Current Configuration"));
        System.out.println();
        System.out.println(CliStyle.CYAN + "  ┌─────────────────────────────────────────────────────┐" + CliStyle.RESET);
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "Project Root" + CliStyle.RESET + "   " + CliStyle.highlight(configLoader.getProjectRoot().toString()) + CliStyle.RESET);
        
        configLoader.loadClaudeConfig().ifPresent(config -> {
            System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "Project Name" + CliStyle.RESET + "   " + CliStyle.highlight(config.name()));
            System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "Project Type" + CliStyle.RESET + "   " + CliStyle.highlight(config.type()));
        });
        
        System.out.println(CliStyle.CYAN + "  │" + CliStyle.RESET + "  " + CliStyle.BOLD + "Skills" + CliStyle.RESET + "        " + CliStyle.highlight(String.valueOf(skillRegistry.size())));
        System.out.println(CliStyle.CYAN + "  └─────────────────────────────────────────────────────┘" + CliStyle.RESET);
        System.out.println();
    }
    
    /**
     * Truncate text
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    /**
     * Execute Agent
     */
    private void executeAgent(String input) {
        System.out.println();

        try {
            // Use streaming output for real-time progress
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
                    System.out.println("  " + CliStyle.error("Execution failed: " + t.getMessage()));
                    latch.countDown();
                }

                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });

            // Wait for completion
            latch.await(120, java.util.concurrent.TimeUnit.SECONDS);

            // Output final result
            String output = outputBuilder.toString();
            if (!output.isEmpty()) {
                System.out.println();
                System.out.println(formatOutput(output));
                System.out.println();
                System.out.println("  " + CliStyle.muted("───"));
                System.out.println("  " + CliStyle.badge("Done", CliStyle.BG_GREEN) + " " + 
                    CliStyle.muted("Steps: " + steps[0] + " • Tool calls: " + toolCalls[0]));
                System.out.println();
            }

        } catch (Exception e) {
            System.out.println();
            System.out.println("  " + CliStyle.error("Execution failed: " + e.getMessage()));
            Log.errorf(e, "Agent execution failed");
        }

        System.out.println();
    }

    /**
     * Format output content (highlight task plan)
     */
    private String formatOutput(String content) {
        // Highlight task plan format
        return content
            // Highlight task plan title
            .replaceAll("📋\\s*\\*\\*Task Plan\\*\\*", "\n📋 \u001B[1;36mTask Plan\u001B[0m\n")
            // Highlight pending items
            .replaceAll("-\\s*\\[\\s*\\]\\s*(\\d+\\.)", "  ⏳ \u001B[33m$1\u001B[0m")
            // Highlight completed items
            .replaceAll("-\\s*\\[x\\]\\s*(\\d+\\.)", "  ✅ \u001B[32m$1\u001B[0m")
            // Highlight in-progress
            .replaceAll("-\\s*\\[\\s*\\]\\s*\\*\\*(.+?)\\*\\*", "  🔄 \u001B[1;33m**$1**\u001B[0m");
    }

    /**
     * Truncate text
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        String singleLine = text.replace("\n", " ").trim();
        if (singleLine.length() <= maxLength) return singleLine;
        return singleLine.substring(0, maxLength - 3) + "...";
    }

    /**
     * Cleanup resources
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
