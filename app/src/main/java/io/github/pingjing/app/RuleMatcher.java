package io.github.pingjing.app;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Small pure-Java core: only deliberate, ad-specific button labels match by default. */
public final class RuleMatcher {
    private static final Pattern SKIP = Pattern.compile(
            "^(?:跳过(?:广告)?|略过|skip(?:[\\s_-]*ad)?)(?:[\\s:：›>»→]*[（(]?[0-9]{1,2}(?:\\s*(?:s|秒))?[)）]?)?[\\s›>»→]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern AD_CLOSE = Pattern.compile(
            "^(?:关闭广告|关闭此广告|关闭该广告|跳过广告|不再显示此广告)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private RuleMatcher() { }

    public static String normalize(String raw) {
        if (raw == null) return "";
        return Normalizer.normalize(raw, Normalizer.Form.NFKC)
                .replace('\u200b', ' ')
                .replace('\u00a0', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Return a human-readable rule id, or null. This method must never match generic '关闭' or '×'. */
    public static String builtInRule(String raw) {
        String text = normalize(raw);
        if (text.isEmpty() || text.length() > 30) return null;
        if (SKIP.matcher(text).matches()) return "跳过广告按钮";
        if (AD_CLOSE.matcher(text).matches()) return "关闭广告按钮";
        return null;
    }

    public static boolean customExact(String buttonText, String ruleText) {
        String a = normalize(buttonText);
        String b = normalize(ruleText);
        return !a.isEmpty() && !b.isEmpty() && a.equalsIgnoreCase(b) && b.length() <= 50;
    }
}
