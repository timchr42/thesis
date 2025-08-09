package de.cispa.testapp;

import static de.cispa.testapp.TokenManager.storeTokens;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements MyCallback {
    private static final String LOGTAG = "TestApp";
    private TextView statusText;
    public TextView capStorage;
    private Context mContext;
    private TokenManager mTokenManager;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button storeButton = findViewById(R.id.btnStoreCap);
        Button launchTrustedUrl = findViewById(R.id.btnLaunchTrusted);
        Button launchUntrustedUrl = findViewById(R.id.btnLaunchUntrusted);
        statusText = findViewById(R.id.debugOutput);
        capStorage = findViewById(R.id.capStorage);

        mContext = getApplicationContext();
        mTokenManager = new TokenManager(mContext, this);

        prefs = getSharedPreferences("cap_storage", MODE_PRIVATE);

        prefListener = (sharedPrefs, key) -> {
            // key = domain that changed
            String allCaps = mTokenManager.displayCapabilities(sharedPrefs);
            runOnUiThread(() -> capStorage.setText("Current Capabilities:\n\n" + allCaps));
        };

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(new View.OnClickListener() {
           @SuppressLint("SetTextI18n")
           @Override
           public void onClick(View v) {
                String tokensJson = mTokenManager.getSampleTokenJson();
                storeTokens(v.getContext(), tokensJson);
           }
        });

        // Simulate launching CT with capability (Note: Firefox does not support TWA)
        launchUntrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
                mTokenManager.launchUrlMod(mContext, Uri.parse(url));

                Log.d(LOGTAG, "CT to untrusted domain launched");
            }
        });

        launchTrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://royaleapi.com";
                mTokenManager.launchUrlMod(mContext, Uri.parse(url));


                Log.d(LOGTAG, "CT to trusted domain launched");
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    public void updateMyText(String myString) {
        capStorage.setText(myString);
    }

}