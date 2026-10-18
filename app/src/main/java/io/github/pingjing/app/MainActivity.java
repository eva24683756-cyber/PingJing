package io.github.pingjing.app;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.content.Context;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/** Four plain-language tabs. Uses Android framework Views; no external UI libraries. */
public final class MainActivity extends Activity {
    private static final int BG = Color.rgb(246, 248, 252);
    private static final int BLUE = Color.rgb(37, 99, 235);
    private static final int DARK = Color.rgb(24, 35, 55);
    private static final int MUTED = Color.rgb(107, 119, 140);
    private static final int GREEN = Color.rgb(18, 141, 98);
    private static final int BORDER = Color.rgb(227, 233, 243);
    private static final int WHITE = Color.WHITE;

    private LinearLayout root;
    private LinearLayout content;
    private LinearLayout navbar;
    private int page = 0;
    private List<AppRepository.InstalledApp> apps = new ArrayList<>();
    private boolean loadingApps = true;
    private String appQuery = "";
    private LinearLayout appList;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        root = column();
        root.setBackgroundColor(BG);
        if (Build.VERSION.SDK_INT >= 35) {
            // targetSdk 35 enforces edge-to-edge on Android 15+.
            root.setOnApplyWindowInsetsListener((v, insets) -> {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                root.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }
        setContentView(root);
        LinearLayout header = column();
        header.setPadding(dp(22), dp(20), dp(22), dp(12));
        LinearLayout branding = row();
        branding.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("净", 20, WHITE, true);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(BLUE, 14));
        branding.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout titleArea = column();
        titleArea.setPadding(dp(12), 0, 0, 0);
        titleArea.addView(text("屏净", 22, DARK, true));
        titleArea.addView(text("让手机少一点广告干扰", 12, MUTED, false));
        branding.addView(titleArea, new LinearLayout.LayoutParams(0, -2, 1));
        TextView version = pill("测试版 0.1", BLUE, Color.rgb(228, 237, 255));
        branding.addView(version);
        header.addView(branding);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = column();
        content.setPadding(dp(20), dp(6), dp(20), dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        navbar = row();
        navbar.setBackgroundColor(WHITE);
        navbar.setPadding(dp(8), dp(10), dp(8), dp(8));
        root.addView(navbar, new LinearLayout.LayoutParams(-1, dp(68)));
        render();

        new Thread(() -> {
            List<AppRepository.InstalledApp> result = AppRepository.load(getApplicationContext());
            runOnUiThread(() -> {
                apps = result;
                loadingApps = false;
                if (page == 1) render();
            });
        }, "pingjing-app-list").start();
    }

    @Override protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    private void render() {
        if (content == null) return;
        content.removeAllViews();
        navbar.removeAllViews();
        if (page == 0) overview();
        if (page == 1) appPage();
        if (page == 2) historyPage();
        if (page == 3) settingsPage();
        String[] icons = {"⌂", "▦", "≡", "⚙"};
        String[] labels = {"总览", "应用", "记录", "设置"};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = column();
            item.setGravity(Gravity.CENTER);
            item.setBackground(ripple(i == page ? Color.rgb(235, 242, 255) : WHITE, 14));
            TextView glyph = text(icons[i], 20, i == page ? BLUE : MUTED, true);
            glyph.setGravity(Gravity.CENTER);
            item.addView(glyph);
            TextView label = text(labels[i], 11, i == page ? BLUE : MUTED, i == page);
            label.setGravity(Gravity.CENTER);
            item.addView(label);
            item.setOnClickListener(v -> { page = index; render(); });
            LinearLayout.LayoutParams navParams = new LinearLayout.LayoutParams(0, -1, 1);
            navParams.setMargins(dp(3), 0, dp(3), 0);
            navbar.addView(item, navParams);
        }
    }

    private void overview() {
        boolean access = accessibilityReady();
        boolean enabled = SettingsStore.isEnabled(this);
        heading("一键开启，清爽使用", "自动跳过开屏和应用内的常见广告按钮");

        LinearLayout hero = card(Color.rgb(233, 241, 255));
        space(hero, 4);
        LinearLayout header = row();
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout left = column();
        TextView badge = pill(enabled && access ? "● 防护运行中" : enabled ? "● 等待系统授权" : "● 当前已暂停",
                enabled && access ? GREEN : MUTED, enabled && access ? Color.rgb(217, 246, 234) : WHITE);
        left.addView(badge);
        space(left, 14);
        left.addView(text(enabled && access ? "已为你开启基础防护" :
                enabled ? "还差一步：开启无障碍服务" : "开启后自动处理广告", 19, DARK, true));
        space(left, 6);
        left.addView(text("本地识别广告按钮，不截图、不上传。", 12, MUTED, false));
        header.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
        hero.addView(header);
        space(hero, 20);
        LinearLayout toggle = row();
        toggle.setGravity(Gravity.CENTER_VERTICAL);
        toggle.addView(text("自动跳广告总开关", 15, DARK, true), new LinearLayout.LayoutParams(0, -2, 1));
        Switch mainSwitch = new Switch(this);
        mainSwitch.setChecked(enabled);
        mainSwitch.setContentDescription("自动跳广告总开关");
        mainSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (checked && !accessibilityReady()) {
                discloseAndOpenSettings();
            } else {
                SettingsStore.setEnabled(this, checked);
                render();
            }
        });
        toggle.addView(mainSwitch);
        hero.addView(toggle);
        if (!access) {
            space(hero, 12);
            hero.addView(action("开启系统无障碍权限 →", true, this::discloseAndOpenSettings));
        }
        content.addView(hero);
        space(content, 16);

        List<SettingsStore.LogEntry> logs = SettingsStore.logs(this);
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 0); midnight.set(Calendar.MINUTE, 0);
        midnight.set(Calendar.SECOND, 0); midnight.set(Calendar.MILLISECOND, 0);
        int today = 0;
        for (SettingsStore.LogEntry entry : logs) if (entry.timestamp >= midnight.getTimeInMillis()) today++;
        LinearLayout stats = row();
        addStat(stats, String.valueOf(today), "今日触发", "次");
        addStat(stats, String.valueOf(logs.size()), "最近记录", "条");
        content.addView(stats);
        space(content, 24);

        sectionTitle("防护功能", "目前可用的功能会明确标注");
        feature("↗", "自动跳过开屏广告", "识别跳过、Skip 等按钮", "已开放", true);
        feature("✕", "自动关闭应用内广告", "识别明确标注“关闭广告”的按钮", "已开放", true);
        feature("▧", "网络广告过滤", "过滤广告域名与部分横幅广告", "后续版本", false);
        space(content, 20);
        sectionTitle("首次使用，只需三步", null);
        numbered("01", "开启防护开关并授权", "进入系统无障碍设置，找到“屏净 · 自动跳广告”。");
        numbered("02", "选择想管理的应用", "默认对大部分普通应用开启，支持逐个关闭。");
        numbered("03", "遇到特殊广告时添加规则", "在“应用”中点“添加规则”，输入关闭按钮原文。");
        space(content, 16);
        note("安全提示", "为避免误操作，支付、银行、系统设置及权限管理等敏感应用会默认受到保护，不能在本测试版启用。");
    }

    private void appPage() {
        heading("应用管理", "逐个决定哪些 APP 允许屏净自动操作");
        note("默认规则", "大多数已安装的普通应用默认参与基础识别；关闭某个开关后，屏净就不会操作它。系统及支付相关应用始终受到保护。");
        space(content, 14);
        EditText search = new EditText(this);
        search.setSingleLine(true);
        search.setTextSize(14);
        search.setHint("搜索应用名称或包名");
        search.setPadding(dp(16), dp(12), dp(16), dp(12));
        search.setBackground(round(WHITE, 14));
        search.setText(appQuery);
        search.setSelection(search.getText().length());
        content.addView(search, new LinearLayout.LayoutParams(-1, dp(50)));
        space(content, 14);
        appList = column();
        content.addView(appList);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                appQuery = s.toString(); populateApps();
            }
            public void afterTextChanged(Editable e) { }
        });
        populateApps();
    }

    private void populateApps() {
        if (appList == null) return;
        appList.removeAllViews();
        if (loadingApps) {
            appList.addView(text("正在读取已安装应用…", 14, MUTED, false));
            return;
        }
        int shown = 0;
        String filter = appQuery.trim().toLowerCase(Locale.ROOT);
        for (AppRepository.InstalledApp app : apps) {
            if (!filter.isEmpty() && !app.name.toLowerCase(Locale.ROOT).contains(filter)
                    && !app.pkg.toLowerCase(Locale.ROOT).contains(filter)) continue;
            shown++;
            boolean protectedApp = SettingsStore.isProtected(app.pkg);
            LinearLayout c = card(WHITE);
            LinearLayout top = row(); top.setGravity(Gravity.CENTER_VERTICAL);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(42), dp(42));
            iconParams.rightMargin = dp(12);
            top.addView(icon, iconParams);
            LinearLayout meta = column();
            meta.addView(text(app.name, 15, DARK, true));
            TextView pkg = text(app.pkg, 10, MUTED, false);
            pkg.setSingleLine(true); pkg.setEllipsize(android.text.TextUtils.TruncateAt.END);
            meta.addView(pkg);
            top.addView(meta, new LinearLayout.LayoutParams(0, -2, 1));
            Switch toggle = new Switch(this);
            toggle.setChecked(SettingsStore.isAppEnabled(this, app.pkg));
            toggle.setEnabled(!protectedApp);
            toggle.setContentDescription(app.name + "广告跳过开关");
            toggle.setOnCheckedChangeListener((b, checked) -> SettingsStore.setAppEnabled(this, app.pkg, checked));
            top.addView(toggle);
            c.addView(top);
            space(c, 7);
            if (protectedApp) {
                c.addView(text("受安全保护 · 禁止自动操作", 11, MUTED, false));
            } else {
                LinearLayout footer = row(); footer.setGravity(Gravity.CENTER_VERTICAL);
                footer.addView(text("基础规则 + 专属规则", 11, MUTED, false),
                        new LinearLayout.LayoutParams(0, -2, 1));
                TextView add = text("＋ 添加规则", 12, BLUE, true);
                add.setPadding(dp(6), dp(6), 0, dp(6));
                add.setOnClickListener(v -> addRuleDialog(app));
                footer.addView(add);
                c.addView(footer);
            }
            appList.addView(c);
            space(appList, 10);
        }
        if (shown == 0) appList.addView(text("没有找到匹配的应用", 14, MUTED, false));
        appList.addView(text("已显示 " + shown + " 个应用 · 只列出有桌面入口的应用", 12, MUTED, false));
    }

    private void addRuleDialog(AppRepository.InstalledApp app) {
        EditText entry = new EditText(this);
        entry.setSingleLine(true);
        entry.setTextSize(16);
        entry.setHint("例如：关闭、跳过广告");
        LinearLayout layout = column(); layout.setPadding(dp(20), dp(6), dp(20), 0);
        layout.addView(text("只在「" + app.name + "」中生效。输入广告按钮上实际显示的文字，最多 50 字。请不要把支付、授权等按钮加入规则。", 13, MUTED, false));
        space(layout, 10);
        layout.addView(entry);
        new AlertDialog.Builder(this).setTitle("为应用添加专属规则")
                .setView(layout).setNegativeButton("取消", null)
                .setPositiveButton("保存规则", (d, which) -> {
                    String value = entry.getText().toString().trim();
                    if (SettingsStore.addRule(this, app.pkg, value)) {
                        Toast.makeText(this, "规则已保存到本机", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "内容无效、涉及敏感操作或规则已存在", Toast.LENGTH_LONG).show();
                    }
                }).show();
    }

    private void historyPage() {
        heading("拦截记录", "这里只记录执行过的广告按钮点击尝试");
        LinearLayout header = row(); header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("最近操作", 16, DARK, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView clear = text("清空记录", 13, BLUE, true);
        clear.setPadding(dp(8), dp(8), dp(8), dp(8));
        clear.setOnClickListener(v -> new AlertDialog.Builder(this).setTitle("清空拦截记录？")
                .setMessage("只删除保存在本机的操作日志，不影响防护规则。")
                .setNegativeButton("取消", null).setPositiveButton("清空", (dialog, which) -> {
                    SettingsStore.clearLogs(this); render();
                }).show());
        header.addView(clear);
        content.addView(header);
        space(content, 10);
        List<SettingsStore.LogEntry> list = SettingsStore.logs(this);
        if (list.isEmpty()) {
            LinearLayout empty = card(WHITE);
            empty.setGravity(Gravity.CENTER);
            space(empty, 20);
            TextView symbol = text("◎", 35, BLUE, true); symbol.setGravity(Gravity.CENTER);
            empty.addView(symbol);
            space(empty, 8);
            TextView message = text("还没有拦截记录", 16, DARK, true);
            message.setGravity(Gravity.CENTER); empty.addView(message);
            TextView sub = text("开启服务后，识别到广告按钮就会显示在这里。", 12, MUTED, false);
            sub.setGravity(Gravity.CENTER); empty.addView(sub);
            space(empty, 20);
            content.addView(empty);
        } else {
            SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm:ss", Locale.CHINA);
            for (SettingsStore.LogEntry log : list) {
                LinearLayout c = card(WHITE);
                c.addView(text(log.rule, 14, DARK, true));
                space(c, 4);
                c.addView(text(log.pkg, 11, MUTED, false));
                space(c, 6);
                c.addView(text(format.format(log.timestamp) + " · 已尝试点击", 11, GREEN, false));
                content.addView(c);
                space(content, 8);
            }
        }
        space(content, 14);
        note("关于记录", "日志不保存屏幕内容，也不代表广告一定被成功关闭。为控制手机存储量，只保留最近 160 条。");
    }

    private void settingsPage() {
        heading("设置与规则", "功能清晰、数据留在你的手机里");
        sectionTitle("权限与运行状态", null);
        LinearLayout p = card(WHITE);
        p.addView(statusRow("无障碍权限", accessibilityReady() ? "已授权" : "未授权",
                accessibilityReady() ? GREEN : MUTED));
        space(p, 10);
        p.addView(action("打开系统无障碍设置", false, this::discloseAndOpenSettings));
        space(p, 12);
        p.addView(text("设备识别：" + Build.MANUFACTURER + " · Android " + Build.VERSION.RELEASE, 12, MUTED, false));
        space(p, 10);
        p.addView(action("打开电池后台管理设置", false, () -> {
            try { startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        }));
        content.addView(p);
        space(content, 12);
        note("本机适配建议", deviceGuide());
        space(content, 24);
        sectionTitle("自带规则", "识别明确的广告按钮，不会随意点击所有“×”");
        smallList("跳过广告", "支持：跳过、跳过广告、Skip、Skip 5s 等");
        smallList("关闭广告", "支持：关闭广告、关闭此广告等");
        space(content, 20);
        sectionTitle("我的专属规则", "请前往“应用”页面添加规则");
        List<SettingsStore.CustomRule> rules = SettingsStore.rules(this);
        if (rules.isEmpty()) {
            note("当前没有自定义规则", "遇到无法自动关闭的广告时，可以在“应用”页面为指定 APP 添加按钮文字规则。");
        } else {
            for (int i = 0; i < rules.size(); i++) {
                final int index = i;
                SettingsStore.CustomRule rule = rules.get(i);
                LinearLayout card = card(WHITE);
                LinearLayout row = row(); row.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout words = column();
                words.addView(text("按钮文字：" + rule.text, 14, DARK, true));
                words.addView(text(rule.pkg, 11, MUTED, false));
                row.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
                TextView delete = text("删除", 12, BLUE, true);
                delete.setPadding(dp(8), dp(8), 0, dp(8));
                delete.setOnClickListener(v -> new AlertDialog.Builder(this)
                        .setTitle("删除这条规则？")
                        .setMessage(rule.text + "\n" + rule.pkg)
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除", (d, which) -> {
                            SettingsStore.removeRule(this, index); render();
                        }).show());
                row.addView(delete); card.addView(row); content.addView(card);
                space(content, 8);
            }
        }
        space(content, 24);
        sectionTitle("隐私与关于", null);
        smallList("完全本地处理", "测试版不申请互联网权限，不上传屏幕内容或规则记录。");
        smallList("敏感应用保护", "系统设置、安装、支付及部分银行应用不可自动操作。");
        smallList("网络广告过滤", "此功能尚未开发完成，不会冒充已拦截。");
        smallList("开源测试版本", "屏净 PingJing v0.1.0 · Android 8.0+");
        space(content, 16);
    }

    private String deviceGuide() {
        String name = Build.MANUFACTURER.toLowerCase(Locale.ROOT);
        if (name.contains("xiaomi") || name.contains("redmi") || name.contains("poco"))
            return "小米/Redmi：留意 HyperOS 的无障碍服务、后台运行与电池优化设置；不同版本入口可能不同。";
        if (name.contains("honor"))
            return "荣耀：留意 MagicOS 的应用启动管理与电池优化，确认无障碍服务没有被关闭。";
        if (name.contains("oppo") || name.contains("oneplus") || name.contains("realme"))
            return "OPPO/一加/真我：留意 ColorOS/OxygenOS 的自启动与后台耗电管理。";
        if (name.contains("vivo") || name.contains("iqoo"))
            return "vivo/iQOO：留意 OriginOS 的后台高耗电限制及无障碍权限状态。";
        if (name.contains("samsung"))
            return "三星：检查 One UI 的无障碍授权和电池后台使用限制/休眠应用设置。";
        if (name.contains("huawei"))
            return "华为：Android 兼容系统可测试；HarmonyOS NEXT 等不兼容 APK 的系统不能安装此版本。";
        return "如自动跳过突然失效，请先检查无障碍服务是否仍已授权，以及系统是否限制了后台运行。";
    }

    private boolean accessibilityReady() {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager == null) return false;
        String target = new ComponentName(this, AdSkipService.class).flattenToString();
        for (AccessibilityServiceInfo service : manager.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            if (target.equalsIgnoreCase(service.getId())) return true;
        }
        return false;
    }

    private void discloseAndOpenSettings() {
        new AlertDialog.Builder(this)
                .setTitle("开启屏净无障碍服务")
                .setMessage("屏净需要读取其他应用界面上可访问的按钮文字，并在符合广告规则时自动点击。\n\n所有检测都在手机本地完成，不录屏、不上传，且会跳过密码输入框；支付、权限和系统设置等敏感应用默认禁用。\n\n授权后也可以随时通过屏净总开关暂停，或到系统设置中撤销权限。")
                .setNegativeButton("暂不开启", (d, w) -> render())
                .setPositiveButton("同意并前往设置", (d, w) -> {
                    SettingsStore.setEnabled(this, true);
                    try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
                    catch (Exception e) { Toast.makeText(this, "请到系统设置手动开启无障碍权限", Toast.LENGTH_LONG).show(); }
                }).show();
    }

    // --- Reusable UI primitives (dp based, readable Chinese labels). ---
    private int dp(float n) { return (int) (getResources().getDisplayMetrics().density * n + 0.5f); }
    private LinearLayout column() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private LinearLayout row() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.HORIZONTAL); return l; }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color);
        t.setTextSize(size); t.setIncludeFontPadding(true);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private GradientDrawable round(int color, int radius) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color); gd.setCornerRadius(dp(radius));
        return gd;
    }
    private android.graphics.drawable.Drawable ripple(int color, int radius) {
        return new RippleDrawable(ColorStateList.valueOf(Color.rgb(211, 226, 252)),
                round(color, radius), null);
    }
    private LinearLayout card(int background) {
        LinearLayout c = column(); c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(round(background, 18)); return c;
    }
    private void space(LinearLayout c, int height) {
        c.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }
    private void heading(String title, String subtitle) {
        content.addView(text(title, 23, DARK, true));
        space(content, 5);
        content.addView(text(subtitle, 13, MUTED, false));
        space(content, 20);
    }
    private void sectionTitle(String title, String sub) {
        content.addView(text(title, 17, DARK, true));
        if (sub != null) { space(content, 4); content.addView(text(sub, 12, MUTED, false)); }
        space(content, 12);
    }
    private TextView pill(String name, int foreground, int background) {
        TextView t = text(name, 11, foreground, true);
        t.setPadding(dp(10), dp(5), dp(10), dp(5));
        t.setBackground(round(background, 12)); return t;
    }
    private TextView action(String name, boolean primary, Runnable task) {
        TextView t = text(name, 14, primary ? WHITE : BLUE, true);
        t.setGravity(Gravity.CENTER); t.setPadding(dp(12), dp(13), dp(12), dp(13));
        t.setBackground(ripple(primary ? BLUE : Color.rgb(231, 239, 255), 12));
        t.setOnClickListener(v -> task.run()); return t;
    }
    private void addStat(LinearLayout parent, String value, String title, String unit) {
        LinearLayout box = card(WHITE);
        LinearLayout count = row(); count.setGravity(Gravity.BOTTOM);
        count.addView(text(value, 28, DARK, true));
        TextView units = text(unit, 12, MUTED, false);
        units.setPadding(dp(5), dp(9), 0, 0); count.addView(units);
        box.addView(count);
        space(box, 4); box.addView(text(title, 12, MUTED, false));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
        lp.setMargins(dp(3), 0, dp(3), 0); parent.addView(box, lp);
    }
    private void feature(String icon, String title, String desc, String state, boolean active) {
        LinearLayout c = card(WHITE);
        LinearLayout r = row(); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView symbol = text(icon, 20, active ? BLUE : MUTED, true);
        symbol.setGravity(Gravity.CENTER); symbol.setBackground(round(
                active ? Color.rgb(232, 240, 255) : BG, 12));
        r.addView(symbol, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout middle = column(); middle.setPadding(dp(12), 0, dp(6), 0);
        middle.addView(text(title, 14, DARK, true));
        space(middle, 3); middle.addView(text(desc, 11, MUTED, false));
        r.addView(middle, new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(pill(state, active ? GREEN : MUTED,
                active ? Color.rgb(229, 247, 238) : BG));
        c.addView(r); content.addView(c); space(content, 9);
    }
    private void numbered(String number, String title, String desc) {
        LinearLayout c = card(WHITE);
        LinearLayout r = row(); r.setGravity(Gravity.CENTER_VERTICAL);
        TextView n = text(number, 16, BLUE, true); n.setPadding(0, 0, dp(15), 0);
        r.addView(n);
        LinearLayout t = column(); t.addView(text(title, 14, DARK, true));
        space(t, 3); t.addView(text(desc, 12, MUTED, false));
        r.addView(t); c.addView(r); content.addView(c); space(content, 8);
    }
    private void note(String label, String details) {
        LinearLayout n = card(Color.rgb(237, 243, 254));
        n.addView(text(label, 13, BLUE, true)); space(n, 5);
        n.addView(text(details, 12, MUTED, false)); content.addView(n);
    }
    private void smallList(String title, String description) {
        LinearLayout c = card(WHITE); c.addView(text(title, 14, DARK, true));
        space(c, 5); c.addView(text(description, 12, MUTED, false));
        content.addView(c); space(content, 9);
    }
    private LinearLayout statusRow(String title, String status, int color) {
        LinearLayout r = row();
        r.addView(text(title, 14, DARK, true), new LinearLayout.LayoutParams(0, -2, 1));
        r.addView(text(status, 13, color, true)); return r;
    }
}
