package devmate.security;

import io.quarkus.logging.Log;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import jakarta.enterprise.context.ApplicationScoped;
import java.io.IOException;

/**
 * User Confirmation Tool
 * 
 * Used for interactive confirmation of dangerous operations
 */
@ApplicationScoped
public class UserConfirmation {

    private Terminal terminal;
    private LineReader reader;

    /**
     * Initialize terminal
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
     * Request user confirmation
     * 
     * @param message Confirmation message
     * @return Whether user confirmed
     */
    public boolean ask(String message) {
        return ask(message, false);
    }

    /**
     * Request user confirmation
     * 
     * @param message Confirmation message
     * @param defaultYes Default to Yes
     * @return Whether user confirmed
     */
    public boolean ask(String message, boolean defaultYes) {
        // Non-interactive mode, default behavior
        if (!isInteractive()) {
            Log.infof("Non-interactive mode: %s", defaultYes ? "auto-confirming" : "auto-rejecting");
            return defaultYes;
        }

        initTerminal();

        String options = defaultYes ? "[Y/n]" : "[y/N]";
        String prompt = String.format("\n⚠️  %s\nConfirm execution? %s ", message, options);

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
     * Request user text input
     * 
     * @param prompt Prompt message
     * @return User input
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
     * Request user to select from options
     * 
     * @param message Message
     * @param options Options list
     * @param defaultIndex Default option index
     * @return Selected option index, -1 means cancelled
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
                sb.append(" (default)");
            }
            sb.append("\n");
        }
        sb.append("Select [1-").append(options.length).append("]: ");

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
     * Check if terminal is interactive
     */
    public boolean isInteractive() {
        return System.console() != null;
    }

    /**
     * Show warning message
     */
    public void showWarning(String message) {
        System.err.println("\n⚠️  Warning: " + message);
    }

    /**
     * Show error message
     */
    public void showError(String message) {
        System.err.println("\n❌ Error: " + message);
    }

    /**
     * Show success message
     */
    public void showSuccess(String message) {
        System.out.println("\n✅ " + message);
    }

    /**
     * Show info message
     */
    public void showInfo(String message) {
        System.out.println("\nℹ️  " + message);
    }

    /**
     * Close terminal
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