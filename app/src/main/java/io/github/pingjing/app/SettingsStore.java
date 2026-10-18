package io.github.pingjing.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsStore {
    private static final String FILE = "pingjing_local_settings";
    public static final int MAX_LOGS = 160;

    private SettingsStore() { }
    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    public static boolean isEnabled(Context c) { return prefs(c).getBoolean("enabled", false); }
    public static void setEnabled(Context c, boolean value) { prefs(c).edit().putBoolean("enabled", value).apply(); }
    public static boolean isAppEnabled(Context c, String pkg) {
        return !isProtected(pkg) && prefs(c).getBoolean("app_" + pkg, true);
    }
    public static void setAppEnabled(Context c, String pkg, boolean value) {
        if (!isProtected(pkg)) prefs(c).edit().putBoolean("app_" + pkg, value).apply();
    }
    /** Conservative default deny-list. Protected apps cannot be re-enabled in this beta. */
    public static boolean isProtected(String packageName) {
        if (packageName == null || packageName.isEmpty()) return true;
        String p = packageName.toLowerCase(Locale.ROOT);
        if (p.equals("io.github.pingjing.app") || p.equals("com.tencent.mm")
                || p.equals("com.eg.android.alipaygphone")
                || p.equals("com.android.vending")
                || p.equals("com.google.android.packageinstaller")
                || p.equals("com.android.packageinstaller")) return true;
        String[] prefixes = {
                "com.android.", "android", "com.google.android.permissioncontroller",
                "com.google.android.apps.wallet", "com.google.android.gms",
                "com.miui.security", "com.huawei.systemmanager", "com.coloros.safecenter",
                "com.vivo.permission", "com.samsung.android.permission",
                "com.icbc.", "com.chinamworld.", "com.cmbchina.", "com.bankcomm."
        };
        for (String prefix : prefixes) {
            if (p.startsWith(prefix)) return true;
        }
        return false;
    }

    public static final class CustomRule {
        public final String pkg;
        public final String text;
        public CustomRule(String pkg, String text) { this.pkg = pkg; this.text = text; }
    }
    public static List<CustomRule> rules(Context c) {
        List<CustomRule> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("rules", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.optJSONObject(i);
                if (obj != null && !obj.optString("pkg").isEmpty()
                        && !obj.optString("text").isEmpty()) {
                    out.add(new CustomRule(obj.optString("pkg"), obj.optString("text")));
                }
            }
        } catch (JSONException ignored) { }
        return out;
    }
    public static boolean addRule(Context c, String pkg, String label) {
        String text = RuleMatcher.normalize(label);
        if (pkg == null || pkg.isEmpty() || isProtected(pkg)
                || text.isEmpty() || text.length() > 50) return false;
        String[] unsafe = {"支付", "付款", "购买", "授权", "安装", "下载", "充值",
                "转账", "提现", "验证码", "密码", "确认", "确定", "提交", "允许", "同意",
                "删除", "付款", "下一步", "发送", "解锁"};
        for (String word : unsafe) if (text.contains(word)) return false;
        List<CustomRule> list = rules(c);
        for (CustomRule rule : list) {
            if (rule.pkg.equals(pkg) && rule.text.equalsIgnoreCase(text)) return false;
        }
        list.add(new CustomRule(pkg, text));
        writeRules(c, list);
        return true;
    }
    public static void removeRule(Context c, int index) {
        List<CustomRule> list = rules(c);
        if (index >= 0 && index < list.size()) {
            list.remove(index);
            writeRules(c, list);
        }
    }
    private static void writeRules(Context c, List<CustomRule> list) {
        JSONArray arr = new JSONArray();
        for (CustomRule rule : list) {
            JSONObject item = new JSONObject();
            try {
                item.put("pkg", rule.pkg);
                item.put("text", rule.text);
                arr.put(item);
            } catch (JSONException ignored) { }
        }
        prefs(c).edit().putString("rules", arr.toString()).apply();
    }
    public static final class LogEntry {
        public final String pkg;
        public final String rule;
        public final long timestamp;
        LogEntry(String pkg, String rule, long timestamp) {
            this.pkg = pkg; this.rule = rule; this.timestamp = timestamp;
        }
    }
    public static synchronized void log(Context c, String pkg, String rule) {
        JSONArray prev;
        try { prev = new JSONArray(prefs(c).getString("logs", "[]")); }
        catch (JSONException e) { prev = new JSONArray(); }
        JSONArray arr = new JSONArray();
        JSONObject item = new JSONObject();
        try {
            item.put("pkg", pkg);
            item.put("rule", rule);
            item.put("time", System.currentTimeMillis());
            arr.put(item);
            for (int i = 0; i < prev.length() && i < MAX_LOGS - 1; i++) {
                Object entry = prev.opt(i);
                if (entry != null) arr.put(entry);
            }
            prefs(c).edit().putString("logs", arr.toString()).apply();
        } catch (JSONException ignored) { }
    }
    public static List<LogEntry> logs(Context c) {
        List<LogEntry> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString("logs", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item != null) {
                    out.add(new LogEntry(item.optString("pkg"),
                            item.optString("rule"), item.optLong("time")));
                }
            }
        } catch (JSONException ignored) { }
        return out;
    }
    public static void clearLogs(Context c) {
        prefs(c).edit().putString("logs", "[]").apply();
    }
}
