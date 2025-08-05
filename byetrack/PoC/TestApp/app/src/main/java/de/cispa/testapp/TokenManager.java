package de.cispa.testapp;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TokenManager {
    private static final String LOGTAG = "TestApp-TokenManager";
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
        SharedPreferences prefs = mContext.getSharedPreferences("cap_storage", MODE_PRIVATE);
        prefs.edit().clear().apply(); // Wipe data => no "old" tokens remain after update

        try {
            JSONObject tokens = new JSONObject(tokenJson);

            // Process tokens by domain
            Iterator<String> domains = tokens.keys();
            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                storeTokensForDomain(domain, domainTokens, prefs);
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
    private void storeTokensForDomain(String domain, JSONArray tokens, SharedPreferences prefs) throws JSONException {
        // Get existing tokens for this domain (if any)
        Set<String> tokenSet = new HashSet<>(prefs.getStringSet(domain, new HashSet<>()));
        // Set<String> tokenSet = new HashSet<>();

        // Add all new tokens from the JSONArray
        for (int i = 0; i < tokens.length(); i++) {
            tokenSet.add(tokens.getString(i));
        }

        // Store the updated set back into SharedPreferences
        prefs.edit().putStringSet(domain, tokenSet).apply();
        Log.d(LOGTAG, "Stored " + tokenSet + " for " + domain);
    }


    /**
     * Launches a regular CustomTab if no Capabilities exist for domain, else attaches them to to the intent
     * @param uri to launch CustomTab for
     */
    public void launchUrlMod(Context context, Uri uri) {
        // Build CustomTabsIntent
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        // customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); -> Make Firefox (GeckoView Example) default browser

        attachCapsToIntent(customTabsIntent, uri); // getDomainName broken for name like 10.0.2.2
        customTabsIntent.launchUrl(context, uri);
    }

    /**
     * Attaches Capabilities to Intent if available
     * @param customTabsIntent Intent to include Capabilities to
     * @param uri to launch CustomTab for
     */
    private void attachCapsToIntent(CustomTabsIntent customTabsIntent, Uri uri) {
        SharedPreferences prefs = mContext.getSharedPreferences("cap_storage", MODE_PRIVATE);
        String domainName = getDomainName(uri);
        String caps = prefs.getString(domainName, null);
        if (caps != null && !caps.isEmpty()) {
            customTabsIntent.intent.putExtra("browser_capability", caps);
            Log.v(LOGTAG, "Capability found for " + domainName);
        } else {
            Log.v(LOGTAG, "No capability found for " + domainName);
        }
    }
    @SuppressLint("SetTextI18n")
    private String getDomainName(Uri uri) {
        String host = uri.getHost();
        assert host != null;
        return host;
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
