package com.agritech.util;

import java.util.Scanner;

public final class InputUtil {
    private InputUtil() {}

    public static int readInt(Scanner sc, String message) {
        while (true) {
            try {
                ConsoleUI.prompt(message);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                ConsoleUI.warning("Valid integer enter kijiye.");
            }
        }
    }

    public static double readDouble(Scanner sc, String message) {
        while (true) {
            try {
                ConsoleUI.prompt(message);
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                ConsoleUI.warning("Valid number enter kijiye.");
            }
        }
    }

    public static boolean readYesNo(Scanner sc, String message) {
        while (true) {
            ConsoleUI.prompt(message + " (y/n): ");
            String value = sc.nextLine().trim();
            if (value.equalsIgnoreCase("y") || value.equalsIgnoreCase("yes")) return true;
            if (value.equalsIgnoreCase("n") || value.equalsIgnoreCase("no")) return false;
            ConsoleUI.warning("Please y ya n enter kijiye.");
        }
    }
}
