package de.cispa.byetrack;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Iterator;

final class TokenManager {
    private static final String LOGTAG = "TokenManager";

    // Store in field to prevent read from disk every time
    private static SharedPreferences storage_isAmbient;
    private static SharedPreferences storage_wildcard;
    private static SharedPreferences storage_final;

    static void initPrefs(Context context) {
        if (storage_wildcard == null) {
            storage_wildcard = context.getSharedPreferences(Constants.CAPSTORAGE_WILDCARD, Context.MODE_PRIVATE);
        }
        if (storage_final == null)
            storage_final = context.getSharedPreferences(Constants.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

        if (storage_isAmbient == null)
            storage_isAmbient = context.getSharedPreferences(Constants.ISAMBIENT, Context.MODE_PRIVATE);
    }

    static void storeIsAmbient(boolean isAmbient) {

        SharedPreferences.Editor editor = storage_isAmbient.edit();
        editor.clear().apply();
        editor.putBoolean(Constants.ISAMBIENT, isAmbient);
        editor.apply();
    }

    static void storeWildcardTokens(String tokenJson) {
        SharedPreferences.Editor editor_wildcard = storage_wildcard.edit();
        editor_wildcard.clear().apply(); // clear tokens, so in case of policy change, only new tokens will  be stored!
        storage_final.edit().clear().apply(); // also clear finals

        try {
            JSONObject tokens = new JSONObject(tokenJson);

            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                editor_wildcard.putString(domain, domainTokens.toString());
                Log.d(LOGTAG, "Queued for " + domain + ": " + domainTokens);
            }

            editor_wildcard.apply(); // apply instead of commit for async write (StrictMode policy violation)
            Log.d(LOGTAG, "Wildcard Tokens stored");
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to parse tokens", e);
        }
    }

    static void storeFinalTokens(String tokenJson) {
        final JSONObject parsed;
        try {
            parsed = new JSONObject(tokenJson);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to parse tokenJson", e);
            return;
        }

        // Expect exactly one domain key
        String domain = parsed.keys().hasNext() ? parsed.keys().next() : null;
        if (domain == null) {
            Log.w(LOGTAG, "No domain key in tokenJson");
            return;
        }

        JSONArray incoming = parsed.optJSONArray(domain);
        if (incoming == null) {
            Log.w(LOGTAG, "Value for domain " + domain + " is not a JSON array");
            return;
        }

        // Load existing tokens for that domain
        JSONArray existing;
        try {
            String existingStr = storage_final.getString(domain, "[]");
            existing = new JSONArray(existingStr);
        } catch (Exception e) {
            Log.w(LOGTAG, "Corrupt stored tokens for " + domain + " -> resetting", e);
            existing = new JSONArray();
        }

        // Merge existing with incoming tokens
        for (int i = 0; i < incoming.length(); i++) {
            existing.put(incoming.opt(i));
        }

        // Persist asynchronously (due to Firefox StrictMode policy)
        storage_final.edit().putString(domain, existing.toString()).apply();

        Log.d(LOGTAG, "Stored tokens for " + domain + ": " + existing);
    }

    static String getWildcardTokens(String domainName) {
        boolean isAmbient = storage_isAmbient.getBoolean(Constants.ISAMBIENT, false);
        Log.d(LOGTAG, isAmbient ? "Ambient: true" : "Ambient: false");

        return isAmbient? storage_wildcard.getString("*", "error retrieving ambient token") : storage_wildcard.getString(domainName, "");

    }

    static String getFinalTokens(String domainName) {
        return storage_final.getString(domainName, "");
    }

}
