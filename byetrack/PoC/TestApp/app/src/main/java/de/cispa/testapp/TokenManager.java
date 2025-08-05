package de.cispa.testapp;

import static android.content.Context.MODE_PRIVATE;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TokenManager {
    private static final String LOGTAG = "TokenManager";
    private static final String ACTION = "org.mozilla.geckoview.CAPABILITY_TOKENS";
    private final Context mContext;
    private boolean mReceiverRegistered = false;

    public TokenManager(Context mContext) {
        this.mContext = mContext;
        registerReceiver();
    }

    private void registerReceiver() {
        if (!mReceiverRegistered) {
            IntentFilter filter = new IntentFilter(ACTION);
            mContext.registerReceiver(TokenReceiver, filter, Context.RECEIVER_EXPORTED);

            mReceiverRegistered = true;
            Log.d(LOGTAG, "TokenReceiver registered for action: " + ACTION);
        }
    }

    private final BroadcastReceiver TokenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");

            if ("success".equals(status)) {
                Log.d(LOGTAG, "Tokens received from Browser");

                String tokenJson = intent.getStringExtra("capability_tokens");
                // String timestamp = intent.getStringExtra("timestamp");
                storeTokens(tokenJson);

            } else {
                String error = intent.getStringExtra("error_message");
                Log.e(LOGTAG, "Token generation failed: " + error);
            }
        }
    };

    private void storeTokens(String tokenJson) {
        try {
            JSONObject tokens = new JSONObject(tokenJson);

            // Process tokens by domain
            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                storeTokensForDomain(domain, domainTokens);
            }
            Log.d(LOGTAG, "Tokens successfully stored for all domains");
        } catch (Exception e) {
            Log.d(LOGTAG, "Failed to parse tokens", e);
        }
    }

    /**
     * Stores the tokens associated to their domain in the apps shared preferences
     *
     * @param tokens The token to be stored for domain
     * @param domain The domain tokens are associated to
     */
    private void storeTokensForDomain(String domain, JSONArray tokens) throws JSONException {
        SharedPreferences prefs = mContext.getSharedPreferences("cap_storage", MODE_PRIVATE);
        prefs.edit().clear().apply(); // Wipe data => no "old" tokens remain after update
        // Get existing tokens for this domain (if any)
       //  Set<String> tokenSet = new HashSet<>(prefs.getStringSet(domain, new HashSet<>()));
        Set<String> tokenSet = new HashSet<>();

        // Add all new tokens from the JSONArray
        for (int i = 0; i < tokens.length(); i++) {
            tokenSet.add(tokens.getString(i)); // Set ensures no duplicates
        }

        // Store the updated set back into SharedPreferences
        prefs.edit().putStringSet(domain, tokenSet).apply();
    }

    public String displayCapabilities() {
        SharedPreferences prefs = mContext.getSharedPreferences("cap_storage", MODE_PRIVATE);
        Map<String, ?> allCaps = prefs.getAll();

        StringBuilder builder = new StringBuilder();

        if (allCaps.isEmpty()) {
            builder.append("(none stored)");
            return builder.toString();
        }

        for (Map.Entry<String, ?> entry : allCaps.entrySet()) {
            builder.append("Domain: ").append(entry.getKey()).append("\n");
            //noinspection unchecked
            Set<String> tokens = (Set<String>) entry.getValue();
            int count = 1;
            for (String token : tokens) {
                String compressedToken = token.substring(0, Math.min(40, token.length()));
                builder.append("  Token ").append(count++).append(": ").append(compressedToken).append("...\n");
            }

            builder.append("\n");
        }

        return builder.toString();
    }
}
