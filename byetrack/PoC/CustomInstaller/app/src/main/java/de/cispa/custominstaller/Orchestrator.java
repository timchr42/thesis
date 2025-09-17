package de.cispa.custominstaller;

import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import org.json.JSONObject;

public final class Orchestrator {
    private static final String LOGTAG = "Orchestrator";

    public static final String METHOD_GET_PIPE = "GET_PIPE";
    public static final String EXTRA_PIPE = "de.cispa.byetrack.EXTRA_PIPE";

    // Authorities
    // Browser’s provider authority (in the Browser app):
    public static final String AUTH_BROWSER = "org.mozilla.geckoview_example.pipe";
    // App’s provider authority (in the App):
    public static final String AUTH_APP = "de.cispa.byetrack.pipe";

    public static void deliverPolicy(Context ctx, JSONObject policy, String packageName) {
        try {
            // Ask App for its pipe (to deliver tokens later)
            PendingIntent appPipe = getPipeFromProvider(ctx, AUTH_APP);
            if (appPipe == null) { Log.e(LOGTAG, "App pipe is null"); return; }

            // Ask Browser for its pipe (to deliver policy now)
            PendingIntent browserPipe = getPipeFromProvider(ctx, AUTH_BROWSER);
            if (browserPipe == null) { Log.e(LOGTAG, "Browser pipe is null"); return; }

            // Send policy to Browser via its private receiver; include the app’s pipe
            Intent fill = new Intent()
                    .putExtra("policy_json", policy.toString())
                    .putExtra("app_pipe", appPipe)
                    .putExtra("package_name", packageName);

            browserPipe.send(ctx, 0, fill);
            Log.i(LOGTAG, "Policy delivered to Browser via PI");

        } catch (PendingIntent.CanceledException e) {
            Log.e(LOGTAG, "PI was canceled/one-shot already used", e);
        } catch (Exception ex) {
            Log.e(LOGTAG, "deliverPolicy error", ex);
        }
    }

    private static PendingIntent getPipeFromProvider(Context ctx, String AUTH) {
        // Uri uri = new Uri.Builder().scheme("content").authority(authority).path("pipe").build();
        Uri uri = Uri.parse("content://" + AUTH);
        Bundle b = ctx.getContentResolver().call(uri, METHOD_GET_PIPE, null, null);
        if (b == null) return null;
        return b.getParcelable(EXTRA_PIPE, PendingIntent.class);
    }

}
