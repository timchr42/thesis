package de.cispa.testapp;

import de.cispa.byetrack.DebugHelp;
import de.cispa.byetrack.ByetrackClient;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "TestApp";
    public TextView wildcardTokensStored;
    public TextView finalTokensStored;
    private Context mContext;
    public static SharedPreferences wildcardPrefs;
    public static SharedPreferences finalPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener wildcard_sharedPrefsListener;
    private SharedPreferences.OnSharedPreferenceChangeListener final_sharedPrefsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button storeButton = findViewById(R.id.btnStoreCap);
        Button launchTrustedUrl = findViewById(R.id.btnLaunchTrusted);
        Button launchUntrustedUrl = findViewById(R.id.btnLaunchUntrusted);
        wildcardTokensStored = findViewById(R.id.wildcardTokensStored);
        finalTokensStored = findViewById(R.id.finalTokensStored);

        mContext = MainActivity.this;

        String CAPSTORAGE_BUILDER = "wildcard_token";
        wildcardPrefs = mContext.getSharedPreferences(CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
        String CAPSTORAGE_FINAL = "final_token";
        finalPrefs = mContext.getSharedPreferences(CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

        wildcard_sharedPrefsListener = (sharedPrefs, key) -> wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(mContext));
        final_sharedPrefsListener = (sharedPrefs, key) -> finalTokensStored.setText(DebugHelp.displayFinalTokens(mContext));

        // Browser Packages
        String GECKOVIEW_EXAMPLE = "org.mozilla.geckoview_example";
        String FIREFOX_FENIX = "org.mozilla.fenix.debug";
        String CHROME = "com.android.chrome";

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(v -> {
            DebugHelp.clearTokenStorage(finalPrefs);
            //DebugHelp.clearTokenStorage(wildcardPrefs);
            //wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(mContext));
            //finalTokensStored.setText(DebugHelp.displayFinalTokens(mContext));
        });

        // Simulate launching CT with capability (Note: Firefox does not support TWA)
        launchUntrustedUrl.setOnClickListener(v -> {
            String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
            //String url = "http://10.0.2.2:8082/"; // mitmproxy url

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(mContext, R.color.my_purple))
                    .build();
            builder.setDefaultColorSchemeParams(default_colors);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage(FIREFOX_FENIX); // determine in what browser CT is launched

            ByetrackClient.attachTokens(customTabsIntent.intent, MainActivity.this, Uri.parse(url));
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
            Log.d(LOGTAG, "CT to untrusted domain launched");
        });

        launchTrustedUrl.setOnClickListener(v -> {
            String url = "https://royaleapi.com";

            // Build CustomTabsIntent (Let user do all his modification before applying defense)
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabColorSchemeParams default_colors = new CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(ContextCompat.getColor(mContext, R.color.my_purple))
                    .build();
            builder.setDefaultColorSchemeParams(default_colors);
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.intent.setPackage(FIREFOX_FENIX); // -> Use if Firefox (Geckoview_Example) not default browser

            ByetrackClient.attachTokens(customTabsIntent.intent, MainActivity.this, Uri.parse(url));
            customTabsIntent.launchUrl(MainActivity.this, Uri.parse(url));
            Log.d(LOGTAG, "CT to trusted domain launched");
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        wildcardPrefs.registerOnSharedPreferenceChangeListener(wildcard_sharedPrefsListener);
        finalPrefs.registerOnSharedPreferenceChangeListener(final_sharedPrefsListener);
        wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(mContext));
        finalTokensStored.setText(DebugHelp.displayFinalTokens(mContext));
    }

    @Override
    protected void onStop() {
        super.onStop();
        wildcardPrefs.unregisterOnSharedPreferenceChangeListener(wildcard_sharedPrefsListener);
        finalPrefs.unregisterOnSharedPreferenceChangeListener(final_sharedPrefsListener);
    }

}