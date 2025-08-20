package de.cispa.testapp;

import static de.cispa.testapp.DebugHelp.createSampleTokenJson;
import static de.cispa.testapp.DebugHelp.displayFinalTokens;
import static de.cispa.testapp.DebugHelp.displayWildcardTokens;
import static de.cispa.testapp.DebugHelp.generateExampleToken;
import static de.cispa.testapp.TokenManager.storeTokens;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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
    private TokenManager mTokenManager;
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

        mContext = getApplicationContext();
        mTokenManager = new TokenManager();

        wildcardPrefs = mContext.getSharedPreferences(TokenManager.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
        finalPrefs = mContext.getSharedPreferences(TokenManager.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);

        wildcard_sharedPrefsListener = (sharedPrefs, key) -> displayWildcardTokens(this);
        final_sharedPrefsListener = (sharedPrefs, key) -> displayFinalTokens(this);

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(v -> {
            String tokensJson = createSampleTokenJson();
            storeTokens(tokensJson, wildcardPrefs);

            String example_final_in_app_token = generateExampleToken("10.0.2.2").toString();
            storeTokens(example_final_in_app_token, finalPrefs);

            displayWildcardTokens(this);
            displayFinalTokens(this);

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
            // String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
            String url = "http://10.0.2.2:8082/"; // mitmproxy url
            mTokenManager.launchUrlMod(mContext, Uri.parse(url));

            Log.d(LOGTAG, "CT to untrusted domain launched");
        });

        launchTrustedUrl.setOnClickListener(v -> {
            String url = "https://royaleapi.com";
            mTokenManager.launchUrlMod(mContext, Uri.parse(url));


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
        displayWildcardTokens(this);
        displayFinalTokens(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        wildcardPrefs.unregisterOnSharedPreferenceChangeListener(wildcard_sharedPrefsListener);
        finalPrefs.unregisterOnSharedPreferenceChangeListener(final_sharedPrefsListener);
    }

}