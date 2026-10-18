import io.github.pingjing.app.RuleMatcher;

public class RuleMatcherTest {
    private static int count = 0;
    private static void yes(String input) {
        if (RuleMatcher.builtInRule(input) == null) throw new AssertionError("Expected match: " + input);
        count++;
    }
    private static void no(String input) {
        if (RuleMatcher.builtInRule(input) != null) throw new AssertionError("Unexpected match: " + input);
        count++;
    }
    public static void main(String[] args) {
        yes("跳过"); yes("跳过广告"); yes("跳过 5s"); yes("跳过 (3秒)");
        yes("Skip"); yes("skip ad"); yes("Skip 3s"); yes("关闭广告");
        yes("关闭此广告"); yes("不再显示此广告"); yes("略过 ›");
        no("确定"); no("关闭"); no("×"); no("允许"); no("立即下载");
        no("跳过支付"); no("立即购买"); no("广告"); no("3s 后跳过");
        if (!RuleMatcher.customExact("  关闭  ", "关闭")) throw new AssertionError("custom match failed");
        if (RuleMatcher.customExact("付款", "确认")) throw new AssertionError("custom mismatch failed");
        count += 2;
        System.out.println("PASS: " + count + " safe matcher checks");
    }
}
