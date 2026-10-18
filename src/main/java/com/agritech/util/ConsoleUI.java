package com.agritech.util;

public final class ConsoleUI {
    private ConsoleUI() {}

    private static final boolean COLOR =
            System.getenv("NO_COLOR") == null
                    && !"true".equalsIgnoreCase(System.getenv("AGRITECH_NO_COLOR"));

    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String GREEN = "\u001B[92m";
    private static final String YELLOW = "\u001B[93m";
    private static final String RED = "\u001B[91m";
    private static final String BLUE = "\u001B[94m";
    private static final String CYAN = "\u001B[96m";
    private static final String WHITE = "\u001B[97m";
    private static final String GRAY = "\u001B[90m";

    private static String c(String code, String text) {
        return COLOR ? code + text + RESET : text;
    }

    public static String green(String text) { return c(GREEN, text); }
    public static String yellow(String text) { return c(YELLOW, text); }
    public static String red(String text) { return c(RED, text); }
    public static String blue(String text) { return c(BLUE, text); }
    public static String cyan(String text) { return c(CYAN, text); }
    public static String white(String text) { return c(WHITE, text); }
    public static String gray(String text) { return c(GRAY, text); }
    public static String bold(String text) { return c(BOLD, text); }

    public static void banner() {
        System.out.println();
        System.out.println(green("=============================================================="));
        System.out.println(green("                  AGRITECH INTELLIGENCE"));
        System.out.println(cyan("               Smart Farming Decision Assistant"));
        System.out.println(green("=============================================================="));
    }

    public static void title(String text) {
        System.out.println();
        System.out.println(cyan("--------------------------------------------------------------"));
        System.out.println(cyan("  " + text));
        System.out.println(cyan("--------------------------------------------------------------"));
    }

    public static void menuItem(String no, String text) {
        System.out.println(yellow(no + ".") + " " + white(text));
    }

    public static void prompt(String text) {
        System.out.print(blue(text));
    }

    public static void success(String text) {
        System.out.println(green("[SUCCESS] " + text));
    }

    public static void info(String text) {
        System.out.println(cyan("[INFO] " + text));
    }

    public static void warning(String text) {
        System.out.println(yellow("[WARNING] " + text));
    }

    public static void error(String text) {
        System.out.println(red("[ERROR] " + text));
    }

    public static void label(String label, Object value) {
        System.out.println(cyan(label + ": ") + white(String.valueOf(value)));
    }

    public static void money(String label, Object value) {
        System.out.println(cyan(label + ": ") + green("Rs " + value));
    }

    public static String status(String value) {
        if (value == null) return gray("UNKNOWN");
        return switch (value.trim().toUpperCase()) {
            case "RECOMMENDED", "GOOD CHOICE", "ONLINE", "READY",
                 "CONFIGURED", "AVAILABLE", "COMFORTABLE", "CONNECTED" -> green(value);
            case "WAIT", "POSSIBLE WITH CAUTION", "CAUTION",
                 "CACHED", "LOCAL MODE", "FALLBACK READY" -> yellow(value);
            case "AVOID", "NOT RECOMMENDED", "OFFLINE",
                 "UNAVAILABLE", "NOT CONFIGURED", "HIGH PRESSURE" -> red(value);
            default -> white(value);
        };
    }

    public static String risk(Object risk) {
        if (risk == null) return gray("UNKNOWN");
        String value = risk.toString().toUpperCase();
        return switch (value) {
            case "LOW" -> green(value);
            case "MEDIUM" -> yellow(value);
            case "HIGH" -> red(value);
            default -> white(value);
        };
    }

    public static String scoreBar(double score) {
        int width = 20;
        int filled = (int) Math.round(Math.max(0, Math.min(100, score)) / 100.0 * width);
        return "[" + "#".repeat(filled) + "-".repeat(width - filled) + "] "
                + String.format("%.0f/100", score);
    }
}
