package com.tea.util;

import java.io.Console;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * 控制台输入工具：所有"读一个数字 / 金额 / 非空字符串"的兜底逻辑集中在这里，
 * 保证任何非法输入（空回车、字母、负数、超长）都只提示重输，不会让程序崩溃。
 */
public final class ConsoleUtil {

    private static final Scanner SC = new Scanner(System.in, detectCharset());

    private ConsoleUtil() {
    }

    /** Windows 命令行默认 GBK，IDEA 默认 UTF-8：优先使用控制台自身的编码，避免中文输入乱码。 */
    private static Charset detectCharset() {
        Console console = System.console();
        return console != null ? console.charset() : Charset.defaultCharset();
    }

    /** 读一行（已 trim）；输入流结束（Ctrl+D / Ctrl+Z）时直接退出程序。 */
    public static String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return SC.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("输入已结束，程序退出。");
            System.exit(0);
            return "";
        }
    }

    /** 读非空字符串，且长度不超过 maxLen。 */
    public static String readNonBlank(String prompt, int maxLen) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                System.out.println("✗ 输入不能为空");
            } else if (s.length() > maxLen) {
                System.out.println("✗ 输入过长，最多 " + maxLen + " 个字符");
            } else {
                return s;
            }
        }
    }

    /** 读一个落在 [min, max] 区间的整数。 */
    public static int readInt(String prompt, int min, int max) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                System.out.println("✗ 输入不能为空");
                continue;
            }
            try {
                int v = Integer.parseInt(s);
                if (v < min || v > max) {
                    System.out.printf("✗ 请输入 %d ~ %d 之间的数字%n", min, max);
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("✗ 请输入数字");
            }
        }
    }

    /** 读一个正整数 ID（长整型）。 */
    public static long readId(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                System.out.println("✗ 输入不能为空");
                continue;
            }
            try {
                long v = Long.parseLong(s);
                if (v <= 0) {
                    System.out.println("✗ ID 必须是正整数");
                    continue;
                }
                return v;
            } catch (NumberFormatException e) {
                System.out.println("✗ 请输入数字");
            }
        }
    }

    /** 读一个金额：大于 0，最多两位小数，不超过 max。 */
    public static BigDecimal readMoney(String prompt, BigDecimal max) {
        while (true) {
            String s = readLine(prompt);
            if (s.isEmpty()) {
                System.out.println("✗ 输入不能为空");
                continue;
            }
            try {
                BigDecimal v = new BigDecimal(s);
                if (v.signum() <= 0) {
                    System.out.println("✗ 金额必须大于 0");
                } else if (v.scale() > 2) {
                    System.out.println("✗ 金额最多保留两位小数");
                } else if (v.compareTo(max) > 0) {
                    System.out.println("✗ 金额不能超过 " + max.toPlainString());
                } else {
                    return v;
                }
            } catch (NumberFormatException e) {
                System.out.println("✗ 请输入正确的金额，例如 12 或 12.50");
            }
        }
    }

    /** 读 y/n，二次确认关键操作。 */
    public static boolean confirm(String prompt) {
        while (true) {
            String s = readLine(prompt);
            if ("y".equalsIgnoreCase(s)) {
                return true;
            }
            if ("n".equalsIgnoreCase(s)) {
                return false;
            }
            System.out.println("✗ 请输入 y 或 n");
        }
    }

    public static void pause() {
        readLine("按回车返回...");
    }

    /** 按显示宽度右侧补空格（中文、全角字符算 2 格），让中文名称列对齐。 */
    public static String padRight(String s, int width) {
        int w = displayWidth(s);
        StringBuilder sb = new StringBuilder(s);
        for (int i = w; i < width; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static int displayWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            w += (cp >= 0x1100 && (cp <= 0x115F || (cp >= 0x2E80 && cp <= 0xA4CF)
                    || (cp >= 0xAC00 && cp <= 0xD7A3) || (cp >= 0xF900 && cp <= 0xFAFF)
                    || (cp >= 0xFE30 && cp <= 0xFE4F) || (cp >= 0xFF00 && cp <= 0xFF60)
                    || (cp >= 0xFFE0 && cp <= 0xFFE6))) ? 2 : 1;
            i += Character.charCount(cp);
        }
        return w;
    }
}
