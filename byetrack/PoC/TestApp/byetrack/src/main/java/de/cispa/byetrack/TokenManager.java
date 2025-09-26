package de.cispa.byetrack;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Iterator;

public final class TokenManager {
    private static final String LOGTAG = "TokenManager";

    public static boolean storeIsAmbient(boolean isAmbient, Context context) {
        SharedPreferences storage_isAmbient =
                context.getSharedPreferences(Constants.STORAGE_ISAMBIENT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage_isAmbient.edit();
        editor.clear().apply();
        editor.putBoolean(Constants.ISAMBIENT, isAmbient);
        return editor.commit();
    }

    private static boolean storeTokens(String tokenJson, SharedPreferences.Editor editor) {
        try {
            JSONObject tokens = new JSONObject(tokenJson);

            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                editor.putString(domain, domainTokens.toString());
                Log.d(LOGTAG, "Queued for " + domain + ": " + domainTokens);
            }

            return editor.commit();

        } catch (Exception e) {
            Log.d(LOGTAG, "Failed to parse tokens", e);
            return false;
        }
    }

    public static void storeWildcardTokens(String tokenJson, Context context) {
        SharedPreferences storage_wildcard =
                context.getSharedPreferences(Constants.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor_wildcard = storage_wildcard.edit();
        editor_wildcard.clear().apply(); // clear tokens, so in case of policy change, only new tokens will  be stored!
        context.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE).edit().clear().apply(); // also clear finals

        boolean success = storeTokens(tokenJson, editor_wildcard);
        Log.i(LOGTAG, "Wildcard Tokens stored commit=" + success);
    }

    public static void storeFinalTokens(String tokenJson, Context context) {
        SharedPreferences storage_final =
                context.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = storage_final.edit();

        final JSONObject tokens;
        try {
            tokens = new JSONObject(tokenJson);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to parse tokenJson as JSONObject", e);
            return;
        }

        try {
            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();

                // incoming list for this domain
                JSONArray incomingTokens = tokens.optJSONArray(domain);
                if (incomingTokens == null) {
                    Log.w(LOGTAG, "Value for domain '" + domain + "' is not a JSON array; skipping");
                    continue;
                }

                String existingStr = storage_final.getString(domain, "[]");
                if (existingStr.trim().isEmpty()) existingStr = "[]";

                JSONArray existingTokens;
                try {
                    existingTokens = new JSONArray(existingStr);
                } catch (Exception ex) {
                    Log.w(LOGTAG, "Corrupt stored tokens for " + domain + " -> resetting to empty array", ex);
                    existingTokens = new JSONArray();
                }

                for (int i = 0; i < incomingTokens.length(); i++) {
                    Object v = incomingTokens.get(i);
                    existingTokens.put(v);
                }

                // Persist merged array
                editor.putString(domain, existingTokens.toString());
                Log.d(LOGTAG, "Queued for " + domain + ": " + existingTokens);
            }

            boolean success = editor.commit(); // or editor.apply();
            Log.i(LOGTAG, "Final Tokens stored commit=" + success);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to merge/store tokens", e);
        }
    }

    /**
     * Launches a regular CustomTab if no Capabilities exist for domain, else attaches them to to the intent
     * @param uri to launch CustomTab for
     */
    public static void launchUrlMod(Context context, Uri uri) {
        // Build CustomTabsIntent
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                .setToolbarColor(ContextCompat.getColor(context, androidx.cardview.R.color.cardview_dark_background))
                .build();
        builder.setDefaultColorSchemeParams(default_colors);

        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
        customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); // -> Use if Firefox (Geckoview_Example) not default browser

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
            String nonce = getNonce(context);

            customTabsIntent.intent.putExtra("wildcard_tokens", wildcardTokens);
            customTabsIntent.intent.putExtra("final_tokens", finalTokens);
            customTabsIntent.intent.putExtra("cap_nonce", nonce);

            Log.d(LOGTAG, isAmbient ? "Ambient: true" : "Ambient: false");
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
    private static String getNonce(Context ctx) {
        Bundle args = new Bundle();
        args.putString(Constants.EXTRA_PURPOSE, "customtab");
        Bundle out = ctx.getContentResolver().call(
                Uri.parse("content://" + Constants.AUTH),
                Constants.METHOD_GET_NONCE,
                null,
                args
        );
        assert out != null;
        String nonce = out.getString(Constants.OUT_NONCE);
        if (nonce == null) {
            // abort/fallback?
            Log.e(LOGTAG, "Something went wrong retrieving Nonce!");
            return null;
        }
        Log.d(LOGTAG, "Nonce: " + nonce);
        return nonce;
    }

}
