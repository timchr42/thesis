package de.cispa.testapp;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.Iterator;
import java.util.Map;

public class TokenManager {
    private static final String LOGTAG = "TokenManager";
    private final Context mContext;
    MyCallback myCallback;

    public TokenManager(Context mContext, MyCallback myCallback) {
        this.mContext = mContext;
        this.myCallback = myCallback;
    }

    public static void storeTokens(Context context, String tokenJson) {
        Context appContext = context.getApplicationContext();
        appContext.getSharedPreferences("filled_cap_storage", MODE_PRIVATE); // create storage where filled in capabilities life
        SharedPreferences prefs = appContext.getSharedPreferences("cap_storage", MODE_PRIVATE);
        prefs.edit().clear().apply(); // Wipe data => no "old" tokens remain after update

        Log.d(LOGTAG, "Context in which tokens stored in: " + appContext);
        try {
            JSONObject tokens = new JSONObject(tokenJson);

            // Process tokens by domain
            Iterator<String> domains = tokens.keys();

            while (domains.hasNext()) {
                String domain = domains.next();
                JSONArray domainTokens = tokens.getJSONArray(domain);
                prefs.edit().putString(domain, domainTokens.toString()).apply(); // Store tokens per domain
                Log.d(LOGTAG, "Stored for " + domain + ": " + domainTokens);
            }
            Log.d(LOGTAG, "Tokens successfully stored for all domains");

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
        // customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); -> Use if Firefox (Geckoview_Example) not default browser

        try {
            String versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            String domainName = getDomainName(uri);
            String builder_caps = getTokensForDomain(domainName, "cap_storage");
            String final_caps = getTokensForDomain(domainName, "final_caps");

            customTabsIntent.intent.putExtra("capability_tokens", builder_caps);
            customTabsIntent.intent.putExtra("final_caps", final_caps);
            customTabsIntent.intent.putExtra("domain_name", domainName);
            customTabsIntent.intent.putExtra("version_number", versionName);

            customTabsIntent.launchUrl(context, uri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get Tokens for domain from different storage
     *  cap_storage  -> "builder/fresh" tokens
     *  final_caps   -> filled in tokens ready to be sent to webserver
     *
     * @param domainName to get tokens for
     */
    private String getTokensForDomain(String domainName, String name) {
        SharedPreferences prefs = mContext.getSharedPreferences(name, MODE_PRIVATE);
        String caps = prefs.getString(domainName, null);

        if (caps == null || caps.isEmpty()) {
            Log.d(LOGTAG, "No capability found for " + domainName + " in " + name);
            return ""; // Check if .isEmpty() if caps exist or not
        }

        Log.d(LOGTAG, "Capability found for " + domainName + " in " + name);
        return caps;
    }

    @SuppressLint("SetTextI18n")
    private String getDomainName(Uri uri) {
        String host = uri.getHost();
        assert host != null;
        return host;
    }

    public String displayCapabilities(SharedPreferences sharedPrefs) {
        Map<String, ?> allCaps = sharedPrefs.getAll();

        StringBuilder builder = new StringBuilder();

        if (allCaps.isEmpty()) {
            builder.append("(none stored)");
            return builder.toString();
        }

        try {

            for (Map.Entry<String, ?> entry : allCaps.entrySet()) {
                builder.append("Domain: ").append(entry.getKey()).append("\n");
                String tokensJson = (String) entry.getValue();
                JSONArray tokens = new JSONArray(tokensJson);

                for (int i = 0; i < tokens.length(); i++) {
                    String token = tokens.getString(i);
                    String compressedToken = token.substring(0, Math.min(30, token.length()));
                    builder.append("\tToken ").append(i + 1).append(": ").append(compressedToken).append("...\n");
                }

                builder.append("\n");
            }

        } catch (Exception e) {
            return "Parsing Error occured";
        }

        return builder.toString();
    }

    public String getSampleTokenJson(){
        JSONObject tokensJsonObject = new JSONObject();

        try {
            // example.com tokens
            JSONArray exampleTokens = new JSONArray();
            exampleTokens.put("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example1.signature");
            exampleTokens.put("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6ImNhcCIsImlhdCI6MTUxNjIzOTAyMn0.example2.signature");
            tokensJsonObject.put("example.com", exampleTokens);

            // trusted.app.com tokens
            JSONArray trustedTokens = new JSONArray();
            trustedTokens.put("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.trusted1.signature");
            trustedTokens.put("eyJpYXQiOjE1MTYyMzkwMjIsImV4cCI6MTUxNjI0MDAyMn0.trusted2.signature");
            tokensJsonObject.put("trusted.app.com", trustedTokens);

            // analytics.thirdparty.net tokens
            JSONArray analyticsTokens = new JSONArray();
            analyticsTokens.put("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.analytics1.signature");
            tokensJsonObject.put("analytics.thirdparty.net", analyticsTokens);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return tokensJsonObject.toString();
    }
}
