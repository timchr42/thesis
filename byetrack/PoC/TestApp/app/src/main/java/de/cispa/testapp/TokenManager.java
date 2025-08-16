package de.cispa.testapp;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Iterator;

public class TokenManager {
    private static final String LOGTAG = "TokenManager";
    public static final String CAPSTORAGE = "cap_storage";

    private static final String AUTH = "org.mozilla.geckoview_example.callerid";
    private static final String METHOD_GET_NONCE = "getNonce";
    private static final String EXTRA_PURPOSE = "purpose";
    private static final String OUT_NONCE = "nonce";
    private static Context appContext;
    static MyCallback myCallback;

    public TokenManager(Context appContext, MyCallback myCallback) {
        TokenManager.appContext = appContext;
        TokenManager.myCallback = myCallback;
    }

    public static void storeTokens(String tokenJson) {
        SharedPreferences prefs = appContext.getSharedPreferences(CAPSTORAGE, MODE_PRIVATE);

        try {
            JSONObject tokens = new JSONObject(tokenJson);

            SharedPreferences.Editor editor = prefs.edit();
            editor.clear(); // wipe old set

            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                editor.putString(domain, domainTokens.toString());
                Log.d(LOGTAG, "Queued for " + domain + ": " + domainTokens);
            }

            // IMPORTANT in BroadcastReceiver: block until written
            boolean ok = editor.commit();
            Log.d(LOGTAG, "Tokens stored commit=" + ok);

        } catch (Exception e) {
            Log.d(LOGTAG, "Failed to parse tokens", e);
        }
    }

    /**
     * Launches a regular CustomTab if no Capabilities exist for domain, else attaches them to to the intent
     * @param uri to launch CustomTab for
     */
    public void launchUrlMod(Context context, Uri uri) {
        // Build CustomTabsIntent
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); // -> Use if Firefox (Geckoview_Example) not default browser

        try {
            String domainName = uri.getHost();
            String builder_caps = getTokensForDomain(domainName, CAPSTORAGE);
            String final_caps = getTokensForDomain(domainName, "final_caps");
            String nonce = getNonce(context);

            customTabsIntent.intent.putExtra("capability_tokens", builder_caps);
            customTabsIntent.intent.putExtra("final_caps", final_caps);
            customTabsIntent.intent.putExtra("cap_nonce", nonce);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        customTabsIntent.launchUrl(context, uri);
    }

    /**
     * Asks Browser for a short-lived nonce that Browser mapped to Apps uid/packagename via ContentProvider
     * @param ctx Context to get ContentResolver for
     * @return A nonce, of which ContentProvider can infer app's identity
     */
    private String getNonce(Context ctx) {
        Bundle args = new Bundle();
        args.putString(EXTRA_PURPOSE, "customtab");
        Bundle out = ctx.getContentResolver().call(
                Uri.parse("content://" + AUTH),
                METHOD_GET_NONCE,
                null,
                args
        );
        assert out != null;
        String nonce = out.getString(OUT_NONCE);
        if (nonce == null) {
            // abort/fallback?
            Log.e(LOGTAG, "Something went wrong retrieving Nonce!");
            return null;
        }
        Log.d(LOGTAG, "Nonce: " + nonce);
        return nonce;
    }

    /**
     * Get Tokens for domain from different storage
     *  cap_storage  -> "builder/fresh" tokens
     *  final_caps   -> filled in tokens ready to be sent to webserver
     *
     * @param domainName to get tokens for
     */
    private String getTokensForDomain(String domainName, String name) {
        SharedPreferences prefs = appContext.getSharedPreferences(name, MODE_PRIVATE);
        String caps = prefs.getString(domainName, null);

        if (caps == null || caps.isEmpty()) {
            Log.d(LOGTAG, "No capability found for " + domainName + " in " + name);
            return ""; // Check if .isEmpty() if caps exist or not
        } else {
            Log.d(LOGTAG, "Capability found for " + domainName + " in " + name);
        }

        return caps;
    }

}
