package de.cispa.custominstaller;

import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONObject;

public final class Orchestrator {
    private static final String LOGTAG = "Orchestrator";

    public static final String METHOD_GET_PIPE = "GET_PIPE";
    public static final String METHOD_GET_CHANNEL = "GET_CHANNEL";
    public static final String EXTRA_PIPE = "de.cispa.byetrack.EXTRA_PIPE";
    public static final String EXTRA_CHANNEL = "de.cispa.byetrack.EXTRA_CHANNEL";

    // Authorities
    // Browser’s provider authority (in the Browser app):
    public static final String AUTH_BROWSER = "org.mozilla.geckoview_example.pipe";
    // App’s provider authority (in the App):
    public static final String AUTH_APP = "de.cispa.testapp.pipe";

    public static void deliverPolicy(Context ctx, JSONObject policy, String packageName) {
        try {
            // Ask App for its channel to deliver final tokens "later"
            PendingIntent appChannel = getChannelFromProvider(ctx);
            if (appChannel == null) { Log.e(LOGTAG, "App channel is null"); return; }

            // Ask App for its pipe to deliver init tokens "now"
            PendingIntent appPipe = getPipeFromProvider(ctx, AUTH_APP);
            if (appPipe == null) { Log.e(LOGTAG, "App pipe is null"); return; }

            // Ask Browser for its pipe (to deliver policy now)
            PendingIntent browserPipe = getPipeFromProvider(ctx, AUTH_BROWSER);
            if (browserPipe == null) { Log.e(LOGTAG, "Browser pipe is null"); return; }

            // Send policy to Browser via its private receiver; include the app’s pipe
            Intent fill = new Intent()
                    .putExtra("policy_json", policy.toString())
                    .putExtra("app_pipe", appPipe)
                    .putExtra("app_channel", appChannel)
                    .putExtra("package_name", packageName)
                    .putExtra("version_name", getAppVersionName(packageName, ctx));

            browserPipe.send(ctx, 0, fill);
            Log.i(LOGTAG, "Policy delivered to Browser via PI");

        } catch (PendingIntent.CanceledException e) {
            Log.e(LOGTAG, "PI was canceled/one-shot already used", e);
        } catch (Exception ex) {
            Log.e(LOGTAG, "deliverPolicy error", ex);
        }
    }

    private static PendingIntent getPipeFromProvider(Context ctx, String AUTH) {
        Uri uri = Uri.parse("content://" + AUTH);
        Bundle b = ctx.getContentResolver().call(uri, METHOD_GET_PIPE, null, null);
        if (b == null) return null;
        return b.getParcelable(EXTRA_PIPE, PendingIntent.class);
    }

    private static PendingIntent getChannelFromProvider(Context ctx) {
        Uri uri = Uri.parse("content://" + AUTH_APP);
        Bundle b = ctx.getContentResolver().call(uri, METHOD_GET_CHANNEL, null, null);
        if (b == null) {
            Log.e(LOGTAG, "called bundle containing channel is null");
            return null;
        }
        return b.getParcelable(EXTRA_CHANNEL, PendingIntent.class);
    }

    private static String getAppVersionName(String packageName, Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return info.versionName;  // e.g., "1.0.3"
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOGTAG, "Package not found: " + packageName, e);
            return null;
        } catch (Exception e) {
            Log.e(LOGTAG, "Error retrieving package info for " + packageName, e);
            return null;
        }
    }

}
