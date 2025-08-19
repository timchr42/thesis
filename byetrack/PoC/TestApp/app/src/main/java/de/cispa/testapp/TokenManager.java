package de.cispa.testapp;

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
    public static final String CAPSTORAGE_BUILDER = "wildcard_token";
    public static final String CAPSTORAGE_FINAL = "final_token";

    private static final String AUTH = "org.mozilla.geckoview_example.callerid";
    private static final String METHOD_GET_NONCE = "getNonce";
    private static final String EXTRA_PURPOSE = "purpose";
    private static final String OUT_NONCE = "nonce";


    public static void storeTokens(String tokenJson, SharedPreferences prefs) {

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
            Context appCtx = context.getApplicationContext();
            // Get prefs on demand (no statics)
            SharedPreferences wildcardPrefs =
                    appCtx.getSharedPreferences(TokenManager.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
            SharedPreferences finalPrefs =
                    appCtx.getSharedPreferences(TokenManager.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);
            String wildcardTokens = wildcardPrefs.getString(domainName, "");
            String finalTokens = finalPrefs.getString(domainName, "");
            String nonce = getNonce(context);

            customTabsIntent.intent.putExtra("capability_tokens", wildcardTokens);
            customTabsIntent.intent.putExtra("final_caps", finalTokens);
            customTabsIntent.intent.putExtra("cap_nonce", nonce);

            Log.d(LOGTAG, "wildcard Tokens: " + wildcardTokens);
            Log.d(LOGTAG, "final Tokens: " + finalTokens);

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

}
