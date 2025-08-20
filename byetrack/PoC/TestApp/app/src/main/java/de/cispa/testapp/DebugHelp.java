package de.cispa.testapp;

import static de.cispa.testapp.MainActivity.finalPrefs;
import static de.cispa.testapp.MainActivity.wildcardPrefs;

import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
public final class DebugHelp {

    private static final String LOGTAG = "DebugHelp";
    private static final String SECRET_KEY = "super-secret-key";

    public static void displayFinalTokens(MyCallback myCallback) {
        String out = displayCapabilities(finalPrefs);
        myCallback.updateFinalTokens("Current Final Tokens:\n\n" + out);
    }


    public static void displayWildcardTokens(MyCallback myCallback) {
        String out = displayCapabilities(wildcardPrefs);
        myCallback.updateWildcardTokens("Current Wildcard Tokens:\n\n" + out);
    }
    private static String displayCapabilities(SharedPreferences sharedPrefs) {
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
            return null;
        }

        return builder.toString();
    }


    public static String createSampleTokenJson(){
        JSONObject tokensJsonObject = new JSONObject();

        try {
            // example.com tokens
            JSONArray exampleTokens = new JSONArray();
            exampleTokens.put("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example1.signature");
            exampleTokens.put("eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6ImNhcCIsImlhdCI6MTUxNjIzOTAyMn0.example2.signature");
            tokensJsonObject.put("royaleapi.com", exampleTokens);

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
            throw new RuntimeException(e);
        }

        return tokensJsonObject.toString();
    }
    /**
     * ONLY FOR TESTING!!!
     * Generate a single capability token for a specific domain and cookie
     */
    private static String generateSingleToken(String domain, String cookieName, String cookieValue,
                                       String jarType, String packageName, String versionName, String rights) {
        try {
            // Create token payload
            JSONObject tokenPayload = new JSONObject();
            tokenPayload.put("cookie_name", cookieName);
            tokenPayload.put("cookie_value", cookieValue != null ? cookieValue : "wildcard");
            tokenPayload.put("application_id", packageName);
            tokenPayload.put("version_name", versionName);
            tokenPayload.put("destination_domain", domain);
            tokenPayload.put("access_rights", rights);
            tokenPayload.put("global_jar", jarType);

            tokenPayload.put("timestamp", System.currentTimeMillis());

            String payload = tokenPayload.toString();
            String signature = createSimpleSignature(payload);

            JSONObject token = new JSONObject();
            token.put("payload", payload);
            token.put("signature", signature);

            String tokenString = token.toString();
            String encodedToken = Base64.encodeToString(
                    tokenString.getBytes(StandardCharsets.UTF_8),
                    Base64.NO_WRAP
            );

            Log.d(LOGTAG, "Generated token for domain: " + domain + ", cookie: " + cookieName + ", jar: " + jarType);

            return encodedToken;

        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to generate token for domain: " + domain, e);
            return null;
        }
    }


    private static String createSimpleSignature(String payload) {
        // Use HMAC-SHA256 for proper cryptographic signature
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                    SECRET_KEY.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKeySpec);

            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(signatureBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed to create token signature", e);
            return null;
        }
    }

    private static JSONObject tokensMapToJson(Map<String, List<String>> tokensByDomain) throws JSONException {
        JSONObject json = new JSONObject();
        for (Map.Entry<String, List<String>> entry : tokensByDomain.entrySet()) {
            String domain = entry.getKey();
            List<String> tokens = entry.getValue();
            JSONArray tokenArray = new JSONArray(tokens); // for (String token : tokens) { tokenArray.put(token); }
            json.put(domain, tokenArray);
        }
        return json;
    }

    public static JSONObject generateExampleToken(String domainName) {
        try {
            String cname = "login";
            String cval = "abctest123";
            String jarType = "private";
            String packageName = "de.cispa.testapp";
            String versionName = "1.0";
            String rights = "R";
            String token = generateSingleToken(domainName, cname, cval, jarType, packageName, versionName, rights);
            Map<String, List<String>> tokensByDomain = new HashMap<>();
            tokensByDomain.putIfAbsent(domainName, new ArrayList<>());
            Objects.requireNonNull(tokensByDomain.get(domainName)).add(token);
            return tokensMapToJson(tokensByDomain);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
