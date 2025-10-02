package de.cispa.byetrack;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

public class ByetrackClient {
    private static final String LOGTAG = "ByetrackClient";

    /**
     * Launches a regular CustomTab if no Capabilities exist for domain, else attaches them to to the intent
     * @param uri to launch CustomTab for
     */
    public static void launchUrl(CustomTabsIntent customTabsIntent, Context context, Uri uri) {
        TokenManager.initPrefs(context);
        String domainName = uri.getHost();

        // Get prefs on demand (no statics)
        String wildcardTokens = TokenManager.getWildcardTokens(domainName);
        String finalTokens = TokenManager.getFinalTokens(domainName);
        String packageName = context.getPackageName();

        Bundle byetrackData = new Bundle();
        byetrackData.putString("wildcard_tokens", wildcardTokens);
        byetrackData.putString("final_tokens", finalTokens);
        byetrackData.putString("package_name", packageName);
        customTabsIntent.intent.putExtra(Constants.BYETRACK_DATA, byetrackData);

        Log.d(LOGTAG, "wildcard Tokens: " + wildcardTokens);
        Log.d(LOGTAG, "final Tokens: " + finalTokens);

        customTabsIntent.launchUrl(context, uri);
    }

    /**
     * Attaches Tokens and extra data to an intent if existent, otherwise Intent remains clean
     * @param intent The intent to attach the tokens and additional data to.
     * @param context The context of the app to fetch tokens from.
     * @param uri The Uri to lauch and get the host for.
     */
    public static void attachTokens(Intent intent, Context context, Uri uri) {
        TokenManager.initPrefs(context);
        String domainName = uri.getHost();

        // Get prefs on demand (no statics)
        String wildcardTokens = TokenManager.getWildcardTokens(domainName);
        String finalTokens = TokenManager.getFinalTokens(domainName);
        String packageName = context.getPackageName();

        Bundle byetrackData = new Bundle();
        byetrackData.putString("wildcard_tokens", wildcardTokens);
        byetrackData.putString("final_tokens", finalTokens);
        byetrackData.putString("package_name", packageName);
        intent.putExtra(Constants.BYETRACK_DATA, byetrackData);
    }
}
