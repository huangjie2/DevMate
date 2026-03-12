package devmate.security;

import io.quarkus.logging.Log;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;

/**
 * 用户确认工具
 * 
 * 用于危险操作的交互式确认
 */
@ApplicationScoped
public class UserConfirmation {

    private Terminal terminal;
    private LineReader reader;

    /**
     * 初始化终端
     */
    private void initTerminal() {
        if (terminal == null) {
            try {
                terminal = TerminalBuilder.builder()
                    .system(true)
                    .build();
                reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .build();
            } catch (IOException e) {
                Log.errorf(e, "Failed to initialize terminal");
                throw new RuntimeException("Failed to initialize terminal", e);
            }
        }
    }

    /**
     * 请求用户确认
     * 
     * @param message 确认消息
     * @return 用户是否确认
     */
    public boolean ask(String message) {
        return ask(message, false);
    }

    /**
     * 请求用户确认
     * 
     * @param message 确认消息
     * @param defaultYes 默认是否为 Yes
     * @return 用户是否确认
     */
    public boolean ask(String message, boolean defaultYes) {
        // 非交互模式下，默认行为
        if (!isInteractive()) {
            Log.infof("Non-interactive mode: %s", defaultYes ? "auto-confirming" : "auto-rejecting");
            return defaultYes;
        }

        initTerminal();

        String options = defaultYes ? "[Y/n]" : "[y/N]";
        String prompt = String.format("\n⚠️  %s\n确认执行? %s ", message, options);

        try {
            String input = reader.readLine(prompt).trim().toLowerCase();
            
            if (input.isEmpty()) {
                return defaultYes;
            }
            
            return input.equals("y") || input.equals("yes");
        } catch (Exception e) {
            Log.errorf(e, "Error reading user input");
            return false;
        }
    }

    /**
     * 请求用户输入文本
     * 
     * @param prompt 提示信息
     * @return 用户输入
     */
    public String askInput(String prompt) {
        if (!isInteractive()) {
            Log.info("Non-interactive mode: cannot ask for input");
            return "";
        }

        initTerminal();

        try {
            return reader.readLine(prompt + ": ").trim();
        } catch (Exception e) {
            Log.errorf(e, "Error reading user input");
            return "";
        }
    }

    /**
     * 请求用户从选项中选择
     * 
     * @param message 消息
     * @param options 选项列表
     * @param defaultIndex 默认选项索引
     * @return 选择的选项索引，-1 表示取消
     */
    public int askChoice(String message, String[] options, int defaultIndex) {
        if (!isInteractive()) {
            Log.infof("Non-interactive mode: returning default option %d", defaultIndex);
            return defaultIndex;
        }

        initTerminal();

        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(message).append("\n");
        for (int i = 0; i < options.length; i++) {
            sb.append(String.format("  %d. %s", i + 1, options[i]));
            if (i == defaultIndex) {
                sb.append(" (默认)");
            }
            sb.append("\n");
        }
        sb.append("请选择 [1-").append(options.length).append("]: ");

        try {
            String input = reader.readLine(sb.toString()).trim();
            
            if (input.isEmpty()) {
                return defaultIndex;
            }
            
            int choice = Integer.parseInt(input) - 1;
            if (choice >= 0 && choice < options.length) {
                return choice;
            }
            
            return -1;
        } catch (NumberFormatException e) {
            return -1;
        } catch (Exception e) {
            Log.errorf(e, "Error reading user choice");
            return -1;
        }
    }

    /**
     * 检查是否为交互式终端
     */
    public boolean isInteractive() {
        return System.console() != null;
    }

    /**
     * 显示警告消息
     */
    public void showWarning(String message) {
        System.err.println("\n⚠️  警告: " + message);
    }

    /**
     * 显示错误消息
     */
    public void showError(String message) {
        System.err.println("\n❌ 错误: " + message);
    }

    /**
     * 显示成功消息
     */
    public void showSuccess(String message) {
        System.out.println("\n✅ " + message);
    }

    /**
     * 显示信息消息
     */
    public void showInfo(String message) {
        System.out.println("\nℹ️  " + message);
    }

    /**
     * 关闭终端
     */
    public void close() {
        if (terminal != null) {
            try {
                terminal.close();
            } catch (IOException e) {
                Log.errorf(e, "Failed to close terminal");
            }
        }
    }
}
