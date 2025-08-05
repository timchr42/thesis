package de.cispa.testapp;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static final String LOGTAG = "TestApp";
    private TextView statusText;
    private TextView capStorage;
    private final Context mContext = MainActivity.this;
    private TokenManager mTokenManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button storeButton = findViewById(R.id.btnStoreCap);
        Button launchTrustedUrl = findViewById(R.id.btnLaunchTrusted);
        Button launchUntrustedUrl = findViewById(R.id.btnLaunchUntrusted);
        statusText = findViewById(R.id.debugOutput);
        capStorage = findViewById(R.id.capStorage);

        mTokenManager = new TokenManager(mContext);

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String currentTokens = mTokenManager.displayCapabilities();
                capStorage.setText("Current Capabilities:\n\n" + currentTokens);
            }
        });

        // Simulate launching CT with capability (Note: Firefox does not support TWA)
        launchUntrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
                launchUrlMod(mContext, Uri.parse(url));
                //Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                //i.setPackage("org.mozilla.geckoview_example");
                //startActivity(i);
            }
        });

        launchTrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://royaleapi.com";
                launchUrlMod(mContext, Uri.parse(url));
            }
        });
    }


    /**
     * Launches a regular CustomTab if no Capabilities exist for domain, else attaches them to to the intent
     * @param uri to launch CustomTab for
     */
    private void launchUrlMod(Context context, Uri uri) {
        // Build CustomTabsIntent
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        // customTabsIntent.intent.setPackage("org.mozilla.geckoview_example"); -> Make Firefox (GeckoView Example) default browser

        attachCapsToIntent(customTabsIntent, uri); // getDomainName broken for name like 10.0.2.2
        customTabsIntent.launchUrl(context, uri);
    }

    /**
     * Attaches Capabilities to Intent if available
     * @param customTabsIntent Intent to include Capabilities to
     * @param uri to launch CustomTab for
     */
    private void attachCapsToIntent(CustomTabsIntent customTabsIntent, Uri uri) {
        SharedPreferences prefs = getSharedPreferences("cap_storage", MODE_PRIVATE);
        String domainName = getDomainName(uri);
        String caps = prefs.getString(domainName, null);
        if (caps != null && !caps.isEmpty()) {
            customTabsIntent.intent.putExtra("browser_capability", caps);
            Log.v(LOGTAG, "Capability found for " + domainName);
        } else {
            Log.v(LOGTAG, "No capability found for " + domainName);
        }
    }
    @SuppressLint("SetTextI18n")
    private String getDomainName(Uri uri) {
        String host = uri.getHost();
        assert host != null;
        statusText.setText("Domain: " + host);
        return host;
    }

}