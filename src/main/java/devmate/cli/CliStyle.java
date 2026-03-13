package devmate.cli;

/**
 * CLI 样式工具 - ANSI 颜色和格式化
 */
public final class CliStyle {

    // ANSI 颜色代码
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String BLINK = "\u001B[5m";
    public static final String REVERSE = "\u001B[7m";
    
    // 前景色
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // 亮色
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";
    
    // 背景色
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";

    // 图标
    public static final String CHECK = "✓";
    public static final String CROSS = "✗";
    public static final String ARROW = "→";
    public static final String BULLET = "•";
    public static final String GEAR = "⚙";
    public static final String FOLDER = "📁";
    public static final String FILE = "📄";
    public static final String ROCKET = "🚀";
    public static final String SPARKLE = "✨";
    public static final String WRENCH = "🔧";
    public static final String LIGHTNING = "⚡";
    public static final String INFO = "ℹ";
    public static final String WARNING = "⚠";
    public static final String ERROR = "✖";
    public static final String SUCCESS = "✔";
    public static final String CLOCK = "⏳";
    public static final String THINKING = "💭";
    public static final String CLIPBOARD = "📋";
    
    // 语义化样式方法
    
    public static String success(String text) {
        return GREEN + BOLD + CHECK + " " + text + RESET;
    }
    
    public static String error(String text) {
        return RED + BOLD + CROSS + " " + text + RESET;
    }
    
    public static String warning(String text) {
        return YELLOW + WARNING + " " + text + RESET;
    }
    
    public static String info(String text) {
        return CYAN + INFO + " " + text + RESET;
    }
    
    public static String highlight(String text) {
        return BOLD + CYAN + text + RESET;
    }
    
    public static String muted(String text) {
        return DIM + BRIGHT_BLACK + text + RESET;
    }
    
    public static String command(String text) {
        return BOLD + YELLOW + text + RESET;
    }
    
    public static String skill(String name, String desc) {
        return BOLD + CYAN + name + RESET + " " + muted(desc);
    }
    
    public static String title(String text) {
        return BOLD + MAGENTA + text + RESET;
    }
    
    public static String badge(String text, String color) {
        return color + BOLD + " " + text + " " + RESET;
    }
    
    // 盒子绘制
    
    public static String box(String title, String content) {
        int width = Math.max(title.length(), maxWidth(content)) + 4;
        String top = CYAN + "╔" + "═".repeat(width) + "╗" + RESET;
        String bottom = CYAN + "╚" + "═".repeat(width) + "╝" + RESET;
        String separator = CYAN + "╠" + "═".repeat(width) + "╣" + RESET;
        
        StringBuilder sb = new StringBuilder();
        sb.append(top).append("\n");
        sb.append(CYAN + "║" + RESET).append(" ").append(BOLD).append(title).append(RESET);
        sb.append(" ".repeat(width - title.length() - 1)).append(CYAN + "║" + RESET).append("\n");
        sb.append(separator).append("\n");
        
        for (String line : content.split("\n")) {
            sb.append(CYAN + "║" + RESET).append(" ").append(line);
            sb.append(" ".repeat(width - line.length() - 1)).append(CYAN + "║" + RESET).append("\n");
        }
        
        sb.append(bottom);
        return sb.toString();
    }
    
    public static String progressBar(int current, int total, int width) {
        int filled = (int) ((double) current / total * width);
        String bar = GREEN + "█".repeat(filled) + DIM + "░".repeat(width - filled) + RESET;
        return String.format("[%s] %d/%d", bar, current, total);
    }
    
    public static String table(String[] headers, String[][] rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
            for (String[] row : rows) {
                if (i < row.length) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
        }
        
        StringBuilder sb = new StringBuilder();
        
        // 表头
        sb.append(CYAN + "┌" + RESET);
        for (int i = 0; i < widths.length; i++) {
            sb.append(CYAN + "─".repeat(widths[i] + 2) + (i < widths.length - 1 ? "┬" : "┐") + RESET);
        }
        sb.append("\n");
        
        sb.append(CYAN + "│" + RESET);
        for (int i = 0; i < headers.length; i++) {
            sb.append(" ").append(BOLD).append(headers[i]).append(RESET);
            sb.append(" ".repeat(widths[i] - headers[i].length()));
            sb.append(" ").append(CYAN + "│" + RESET);
        }
        sb.append("\n");
        
        // 分隔线
        sb.append(CYAN + "├" + RESET);
        for (int i = 0; i < widths.length; i++) {
            sb.append(CYAN + "─".repeat(widths[i] + 2) + (i < widths.length - 1 ? "┼" : "┤") + RESET);
        }
        sb.append("\n");
        
        // 数据行
        for (String[] row : rows) {
            sb.append(CYAN + "│" + RESET);
            for (int i = 0; i < widths.length; i++) {
                String cell = i < row.length ? row[i] : "";
                sb.append(" ").append(cell);
                sb.append(" ".repeat(widths[i] - cell.length()));
                sb.append(" ").append(CYAN + "│" + RESET);
            }
            sb.append("\n");
        }
        
        // 底部
        sb.append(CYAN + "└" + RESET);
        for (int i = 0; i < widths.length; i++) {
            sb.append(CYAN + "─".repeat(widths[i] + 2) + (i < widths.length - 1 ? "┴" : "┘") + RESET);
        }
        
        return sb.toString();
    }
    
    private static int maxWidth(String text) {
        int max = 0;
        for (String line : text.split("\n")) {
            max = Math.max(max, line.length());
        }
        return max;
    }
    
    // 清屏
    public static void clearScreen() {
        System.out.print("\u001B[2J\u001B[H");
        System.out.flush();
    }
    
    // 移动光标
    public static void moveCursor(int row, int col) {
        System.out.printf("\u001B[%d;%dH", row, col);
    }
    
    // 保存/恢复光标
    public static void saveCursor() {
        System.out.print("\u001B[s");
    }
    
    public static void restoreCursor() {
        System.out.print("\u001B[u");
    }
    
    // 隐藏/显示光标
    public static void hideCursor() {
        System.out.print("\u001B[?25l");
    }
    
    public static void showCursor() {
        System.out.print("\u001B[?25h");
    }
    
    // 清除行
    public static void clearLine() {
        System.out.print("\u001B[2K");
    }
    
    public static void clearLineFromCursor() {
        System.out.print("\u001B[0K");
    }
    
    // 加载动画帧
    public static final String[] SPINNER = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
    public static final String[] DOTS = {"⢀", "⢠", "⢰", "⢸", "⡸", "⣸", "⣴", "⣤"};
}
