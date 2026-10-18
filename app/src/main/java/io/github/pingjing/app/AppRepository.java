package io.github.pingjing.app;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AppRepository {
    private AppRepository() { }
    public static final class InstalledApp {
        public final String name;
        public final String pkg;
        public final Drawable icon;
        InstalledApp(String name, String pkg, Drawable icon) {
            this.name = name; this.pkg = pkg; this.icon = icon;
        }
    }
    public static List<InstalledApp> load(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos = pm.queryIntentActivities(launch, 0);
        List<InstalledApp> apps = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (ResolveInfo info : infos) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (!seen.add(pkg) || pkg.equals(ctx.getPackageName())) continue;
            String title = String.valueOf(info.loadLabel(pm));
            Drawable icon = info.loadIcon(pm);
            apps.add(new InstalledApp(title, pkg, icon));
        }
        Collections.sort(apps, (a, b) -> {
            if (SettingsStore.isProtected(a.pkg) != SettingsStore.isProtected(b.pkg))
                return SettingsStore.isProtected(a.pkg) ? 1 : -1;
            return a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT));
        });
        return apps;
    }
}
