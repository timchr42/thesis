package de.cispa.testapp;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import de.cispa.byetrack.DebugHelp;
import de.cispa.byetrack.TokenManager;


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


public class MainActivity extends AppCompatActivity implements MyCallback {
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

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(v -> {
            DebugHelp.clearTokenStorage(finalPrefs);
            //DebugHelp.clearTokenStorage(wildcardPrefs);
            //wildcardTokensStored.setText(DebugHelp.displayWildcardTokens(mContext));
            //finalTokensStored.setText(DebugHelp.displayFinalTokens(mContext));

            // Test lauching regular CT
            //String url = "https://royaleapi.com";
            //CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            //CustomTabsIntent customTabsIntent = builder.build();
            //customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            //customTabsIntent.intent.setPackage("com.android.chrome");
            //customTabsIntent.launchUrl(mContext, Uri.parse(url));
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
            customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); // -> Use if Firefox (Geckoview_Example) not default browser

            TokenManager.launchUrl(customTabsIntent, MainActivity.this, Uri.parse(url)); // hook this later in androidx lib st. customTabsIntent.launchUrl(...) already entails my modifcations
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
            //customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); // -> Use if Firefox (Geckoview_Example) not default browser

            TokenManager.launchUrl(customTabsIntent, MainActivity.this, Uri.parse(url)); // hook this later in androidx lib st. customTabsIntent.launchUrl(...) already entails my modifcations
            Log.d(LOGTAG, "CT to trusted domain launched");
        });
    }

    @Override
    public void updateWildcardTokens(String myString) {
        wildcardTokensStored.setText(myString);
    }

    @Override
    public void updateFinalTokens(String myString) {
        finalTokensStored.setText(myString);
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