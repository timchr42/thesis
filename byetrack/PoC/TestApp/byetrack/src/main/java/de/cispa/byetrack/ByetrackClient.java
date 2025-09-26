package de.cispa.byetrack;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

/**
 * Minimal helper to launch a Custom Tab with a session and capability.
 */
public class ByetrackClient {
    private static final String LOGTAG = "ByetrackClient";
    private final Context context;
    private final String browserPackage = "org.mozilla.geckoview_example";
    private CustomTabsServiceConnection connection;
    private CustomTabsClient client;
    private CustomTabsSession session;

    public ByetrackClient(Context ctx) {
        this.context = ctx.getApplicationContext();
    }

    /**
     * Launch a URL in a session-bound Custom Tab. Non-blocking.
     * capabilityToken: installer-issued token (e.g., JWT) or null.
     */
    @MainThread
    public void launchUrl(Context context, Uri uri) {
        connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(@NonNull ComponentName name, @NonNull CustomTabsClient customTabsClient) {
                client = customTabsClient;
                client.warmup(0L);
                session = client.newSession(null);

                if (session == null) {
                    return;
                }

                // send tokens over session-scoped binder call
                Bundle byetrack_tokens = new Bundle();
                try {
                    String domainName = uri.getHost();
                    Context appCtx = context.getApplicationContext();

                    // Get prefs on demand (no statics)
                    boolean isAmbient = appCtx.getSharedPreferences(Constants.STORAGE_ISAMBIENT, Context.MODE_PRIVATE).getBoolean(Constants.ISAMBIENT, false);
                    SharedPreferences wildcardPrefs =
                            appCtx.getSharedPreferences(Constants.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
                    SharedPreferences finalPrefs =
                            appCtx.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

                    String wildcardTokens = isAmbient? wildcardPrefs.getString("*", "error retrieving ambient token") : wildcardPrefs.getString(domainName, "");
                    String finalTokens = finalPrefs.getString(domainName, "");

                    byetrack_tokens.putString("wildcard_tokens", wildcardTokens);
                    byetrack_tokens.putString("final_tokens", finalTokens);

                    session.mayLaunchUrl(uri, byetrack_tokens, null);

                    CustomTabsIntent cti = new CustomTabsIntent.Builder(session).build();
                    cti.intent.setPackage(browserPackage);
                    cti.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                    cti.intent.putExtra("wildcard_tokens", wildcardTokens);
                    cti.intent.putExtra("final_tokens", finalTokens);
                    cti.launchUrl(context, uri);

                    Log.d(LOGTAG, isAmbient ? "Ambient: true" : "Ambient: false");
                    Log.d(LOGTAG, "wildcard Tokens: " + wildcardTokens);
                    Log.d(LOGTAG, "final Tokens: " + finalTokens);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }


                // optionally unbind now or keep bound for reuse
                // context.unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                client = null;
                session = null;
            }
        };

        // bind (this call returns immediately; callback invoked asynchronously)
        CustomTabsClient.bindCustomTabsService(context, browserPackage, connection);
    }

    /**
     * Call when you want to clean up (e.g., Activity.onDestroy())
     */
    public void destroy() {
        if (connection != null) {
            try {
                context.unbindService(connection);
            } catch (Exception ignored) {}
            connection = null;
            client = null;
            session = null;
        }
    }
}
