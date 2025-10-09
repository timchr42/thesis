package de.cispa.testapp;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.byetrack.*;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;


public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "TestApp";
    public TextView wildcardTokensStored;
    public TextView finalTokensStored;
    public static SharedPreferences wildcardPrefs;
    public static SharedPreferences finalPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener wildcard_sharedPrefsListener;
    private SharedPreferences.OnSharedPreferenceChangeListener final_sharedPrefsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button storeButton = findViewById(R.id.btnStoreCap);
        Button launch1 = findViewById(R.id.btnLaunch1);
        Button launch2 = findViewById(R.id.btnLaunch2);
        Button launch3 = findViewById(R.id.btnLaunch3);
        Button tokenInfo1 = findViewById(R.id.btnTokenInfo1);
        Button tokenInfo2 = findViewById(R.id.btnTokenInfo2);
        Button tokenInfo3 = findViewById(R.id.btnTokenInfo3);

        wildcardTokensStored = findViewById(R.id.wildcardTokensStored);
        finalTokensStored = findViewById(R.id.finalTokensStored);

        String CAPSTORAGE_BUILDER = "wildcard_token";
        wildcardPrefs = this.getSharedPreferences(CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
        String CAPSTORAGE_FINAL = "final_token";
        finalPrefs = this.getSharedPreferences(CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

        wildcard_sharedPrefsListener = (sharedPrefs, key) -> wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(this));
        final_sharedPrefsListener = (sharedPrefs, key) -> finalTokensStored.setText(DebugHelp.displayFinalTokens(this));

        // Browser Packages
        String GECKOVIEW_EXAMPLE = "org.mozilla.geckoview_example";
        String FIREFOX_FENIX = "org.mozilla.fenix.debug";
        String CHROME = "com.android.chrome";

        launch3.setOnClickListener(v -> {
            String url = "https://royaleapi.com";
            Set<String> additonalHosts = new HashSet<>();
            additonalHosts.add("nr-data.net");


            // Build CustomTabsIntent (Let user do all his modification before applying defense)
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(this, R.color.my_purple))
                    .build();
            builder.setDefaultColorSchemeParams(default_colors);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage(FIREFOX_FENIX); // -> Use if Firefox (Geckoview_Example) not default browser

            customTabsIntent.launchUrl(this, Uri.parse(url), additonalHosts);
            Log.d(LOGTAG, "CT to trusted domain launched");
        });

        // Simulate launching CT with capability (Note: Firefox does not support TWA)
        launch2.setOnClickListener(v -> {
            String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
            //String url = "http://10.0.2.2:8082/"; // mitmproxy url

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(this, R.color.my_purple))
                    .build();
            builder.setDefaultColorSchemeParams(default_colors);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage(FIREFOX_FENIX); // determine in what browser CT is launched

            customTabsIntent.launchUrl(this, Uri.parse(url));
            Log.d(LOGTAG, "CT to untrusted domain launched");
        });

        launch1.setOnClickListener(v -> {
            String url = "https://royaleapi.com";

            // Build CustomTabsIntent (Let user do all his modification before applying defense)
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(this, R.color.my_purple))
                    .build();
            builder.setDefaultColorSchemeParams(default_colors);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage(FIREFOX_FENIX); // -> Use if Firefox (Geckoview_Example) not default browser

            customTabsIntent.launchUrl(this, Uri.parse(url));
            Log.d(LOGTAG, "CT to trusted domain launched");
        });

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(v -> {
            DebugHelp.clearTokenStorage(finalPrefs);
            //DebugHelp.clearTokenStorage(wildcardPrefs);
            //EvilClient.shareByetrackData(mContext, "10.0.2.2");
        });

        tokenInfo1.setOnClickListener(v -> {
            Bundle tokenToCookieName = Client.getTokenCookieNames(this, "royaleapi.com"); // -> Uri.parse("https://royaleapi.com");
            Log.d(LOGTAG, "size cookieNames: " + tokenToCookieName.size());

            for (String token : tokenToCookieName.keySet()) {
                Log.d(LOGTAG, "Token '" + token + "' encapsulates Name '" + tokenToCookieName.getString(token) + "'");
            }
        });

        tokenInfo2.setOnClickListener(v -> {
            Bundle tokenCookieNames = Client.getTokenCookieNames(this, "royaleapi.com"); // -> Uri.parse("https://royaleapi.com");

            for (String token : tokenCookieNames.keySet()) {
                String cookieName = tokenCookieNames.getString(token);
                String cookieValue = Client.getTokenCookieValue(this, token);
                Log.d(LOGTAG, "Cookie: '" + cookieName + "=" + cookieValue + "'");
            }
        });

        tokenInfo3.setOnClickListener(v -> {
            Bundle tokenCookieNames = Client.getTokenCookieNames(this, "royaleapi.com"); // -> Uri.parse("https://royaleapi.com");
            String tokenToWrite = "Not yet found";
            for (String token : tokenCookieNames.keySet()) {
                String cookieName = tokenCookieNames.getString(token);
                assert cookieName != null;
                if (cookieName.equals("__royaleapi_session_v2")) {
                    tokenToWrite = token;
                    break;
                }
            }
            Log.d(LOGTAG, "Writing cookie '" + tokenToWrite + "' with value 'test_value'");
            Client.writeTokenCookieValue(this, tokenToWrite, "test_value");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        wildcardPrefs.registerOnSharedPreferenceChangeListener(wildcard_sharedPrefsListener);
        finalPrefs.registerOnSharedPreferenceChangeListener(final_sharedPrefsListener);
        wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(this));
        finalTokensStored.setText(DebugHelp.displayFinalTokens(this));
    }

    @Override
    protected void onStop() {
        super.onStop();
        wildcardPrefs.unregisterOnSharedPreferenceChangeListener(wildcard_sharedPrefsListener);
        finalPrefs.unregisterOnSharedPreferenceChangeListener(final_sharedPrefsListener);
    }

}