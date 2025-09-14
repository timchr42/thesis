package de.cispa.byetrack;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import java.util.Map;
public final class DebugHelp {

    public static String displayFinalTokens(Context context) {
        SharedPreferences finalPrefs = context.getSharedPreferences(TokenManager.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);
        String out = displayCapabilities(finalPrefs);
        return "Current Final Tokens:\n\n" + out;
    }


    public static String displayWildcardTokens(Context context) {
        SharedPreferences wildcardPrefs = context.getSharedPreferences(TokenManager.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
        String out = displayCapabilities(wildcardPrefs);
        return "Current Wildcard Tokens:\n\n" + out;
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

    public static void clearTokenStorage(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear().apply();
    }

}
