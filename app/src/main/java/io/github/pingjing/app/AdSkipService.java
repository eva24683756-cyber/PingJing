package io.github.pingjing.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

/** Local-only text-node ad skipper. No screenshots, no Internet, no general-purpose clicker. */
public final class AdSkipService extends AccessibilityService {
    private long lastScanTime;
    private long lastActionTime;
    private String lastSignature = "";

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !SettingsStore.isEnabled(this)) return;
        final int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
                && type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return;
        long now = SystemClock.uptimeMillis();
        if (now - lastScanTime < 280) return;
        lastScanTime = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return;
        final String pkg = root.getPackageName().toString();
        if (!SettingsStore.isAppEnabled(this, pkg)) return;
        List<SettingsStore.CustomRule> rules = SettingsStore.rules(this);
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        final int screenW = getResources().getDisplayMetrics().widthPixels;
        final int screenH = getResources().getDisplayMetrics().heightPixels;

        while (!queue.isEmpty() && visited++ < 220) {
            AccessibilityNodeInfo node = queue.removeFirst();
            if (node == null || !node.isVisibleToUser() || node.isPassword() || node.isEditable()) continue;
            CharSequence nodeText = node.getText();
            CharSequence nodeDesc = node.getContentDescription();
            if (maybeClick(node, nodeText == null ? null : nodeText.toString(),
                    pkg, rules, screenW, screenH, now)) return;
            if (maybeClick(node, nodeDesc == null ? null : nodeDesc.toString(),
                    pkg, rules, screenW, screenH, now)) return;
            int children = Math.min(node.getChildCount(), 24);
            for (int i = 0; i < children; i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
    }

    private boolean maybeClick(AccessibilityNodeInfo node, String label, String pkg,
                               List<SettingsStore.CustomRule> rules,
                               int width, int height, long now) {
        String matchName = RuleMatcher.builtInRule(label);
        boolean custom = false;
        if (matchName == null) {
            for (SettingsStore.CustomRule rule : rules) {
                if (rule.pkg.equals(pkg) && RuleMatcher.customExact(label, rule.text)) {
                    matchName = "自定义：" + rule.text;
                    custom = true;
                    break;
                }
            }
        }
        if (matchName == null) return false;
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        if (bounds.isEmpty() || bounds.centerX() < 0 || bounds.centerX() > width
                || bounds.centerY() < 0 || bounds.centerY() > height
                || bounds.width() > width * 0.90 || bounds.height() > height * 0.45) return false;

        // Generic “Skip” is ambiguous. Restrict to the typical upper-right ad corner.
        String normalized = RuleMatcher.normalize(label).toLowerCase(Locale.ROOT);
        boolean genericSkip = normalized.matches("^(跳过|略过|skip)[\\s›>»→]*$");
        if (!custom && genericSkip
                && (bounds.centerX() < width * 0.50 || bounds.centerY() > height * 0.58)) return false;

        String signature = pkg + ":" + matchName;
        if (signature.equals(lastSignature) && now - lastActionTime < 5000) return false;
        if (now - lastActionTime < 1200) return false;

        AccessibilityNodeInfo clickable = node;
        int parentDepth = 0;
        while (clickable != null && !clickable.isClickable() && parentDepth++ < 3) {
            clickable = clickable.getParent();
        }
        boolean triggered = false;
        if (clickable != null && clickable.isClickable()) {
            Rect clickArea = new Rect();
            clickable.getBoundsInScreen(clickArea);
            // Do not activate a giant clickable container surrounding the label.
            if (!clickArea.isEmpty() && clickArea.width() < width * 0.92
                    && clickArea.height() < height * 0.50) {
                triggered = clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
        if (triggered) {
            lastSignature = signature;
            lastActionTime = now;
            SettingsStore.log(this, pkg, matchName);
            return true;
        }

        // Gesture fallback only for high-confidence explicit ad labels, not generic 'Skip'.
        boolean explicitAd = !custom && (normalized.contains("广告") || normalized.contains("ad"));
        if (explicitAd) {
            Path path = new Path();
            path.moveTo(bounds.centerX(), bounds.centerY());
            GestureDescription gesture = new GestureDescription.Builder()
                    .addStroke(new GestureDescription.StrokeDescription(path, 0, 60)).build();
            String capturedRule = matchName;
            boolean dispatched = dispatchGesture(gesture, new GestureResultCallback() {
                @Override public void onCompleted(GestureDescription description) {
                    SettingsStore.log(AdSkipService.this, pkg, capturedRule);
                }
            }, null);
            if (dispatched) {
                lastSignature = signature;
                lastActionTime = now;
                return true;
            }
        }
        return false;
    }

    @Override public void onInterrupt() { /* System interrupted accessibility processing. */ }
}
