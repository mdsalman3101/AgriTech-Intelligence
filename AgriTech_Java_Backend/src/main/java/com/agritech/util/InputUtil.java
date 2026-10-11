package com.agritech.util;

import java.util.Scanner;

public final class InputUtil {
    private InputUtil() {}

    public static int readInt(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter valid integer.");
            }
        }
    }

    public static double readDouble(Scanner sc, String message) {
        while (true) {
            try {
                System.out.print(message);
                return Double.parseDouble(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter valid number.");
            }
        }
    }
}
