package de.cispa.testapp;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static de.cispa.testapp.TokenManager.CAPSTORAGE;
import static de.cispa.testapp.TokenManager.storeTokens;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements MyCallback {
    private static final String LOGTAG = "TestApp";
    private TextView statusText;
    public TextView capStorage;
    private Context mContext;
    private TokenManager mTokenManager;
    private SharedPreferences sharedPrefs;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefsListener;

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

        mContext.getSharedPreferences("filled_cap_storage", MODE_PRIVATE); // init storage where filled in capabilities life

        sharedPrefs = getSharedPreferences(CAPSTORAGE, MODE_PRIVATE);
        sharedPrefsListener = (sharedPrefs, key) -> displayCapabilities();

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(new View.OnClickListener() {
           @SuppressLint("SetTextI18n")
           @Override
           public void onClick(View v) {
               String tokensJson = createSampleTokenJson();
               storeTokens(tokensJson);
               displayCapabilities();

               // Test lauching regular CT
               String url = "https://royaleapi.com";
               CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
               CustomTabsIntent customTabsIntent = builder.build();
               customTabsIntent.intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
               customTabsIntent.intent.setPackage("com.android.chrome");
               customTabsIntent.launchUrl(mContext, Uri.parse(url));
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
    public void updateMyText(String myString) {
        capStorage.setText(myString);
    }

    @Override
    protected void onStart() {
        super.onStart();
        sharedPrefs.registerOnSharedPreferenceChangeListener(sharedPrefsListener);
        displayCapabilities();
    }

    @Override
    protected void onStop() {
        super.onStop();
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(sharedPrefsListener);
    }


    public void displayCapabilities() {
        SharedPreferences sharedPrefs = mContext.getSharedPreferences(CAPSTORAGE, MODE_PRIVATE);
        Map<String, ?> allCaps = sharedPrefs.getAll();

        Log.d(LOGTAG, "App Context reading Tokens from: " + mContext);

        StringBuilder builder = new StringBuilder();

        if (allCaps.isEmpty()) {
            builder.append("(none stored)");
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
            updateMyText("(Parsing Error occured)");
        }

        updateMyText("Current Capabilities:\n\n" + builder);
    }


    public String createSampleTokenJson(){
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
}