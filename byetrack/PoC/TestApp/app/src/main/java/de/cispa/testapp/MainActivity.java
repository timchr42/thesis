package de.cispa.testapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.net.URISyntaxException;
import java.util.Map;

import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.net.InternetDomainName;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "TEST-App";
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button storeButton = findViewById(R.id.btnStoreCap);
        Button launchTrustedUrl = findViewById(R.id.btnLaunchTrusted);
        Button launchUntrustedUrl = findViewById(R.id.btnLaunchUntrusted);
        statusText = findViewById(R.id.debugOutput);

        // Simulate storing a received capability from browser
        storeButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View v) {
                String domain = "example.com";
                String opaqueCapability = "eyJhbGciOi..."; // placeholder: simulated signed token
                saveCapability(MainActivity.this, domain, opaqueCapability);
                displayCapabilities();
            }
        });

        // Simulate launching CT with capability (Note: Firefox does not support TWA)
        launchUntrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://10.0.2.2/"; // examplecorp.de -> 10.0.2.2 on emulator
                launchUrlMod(v.getContext(), Uri.parse(url));
                //Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                //i.setPackage("org.mozilla.geckoview_example");
                //startActivity(i);
            }
        });

        launchTrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://royaleapi.com";
                launchUrlMod(v.getContext(), Uri.parse(url));
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void saveCapability(Context context, String domain, String opaqueToken) {
        SharedPreferences prefs = context.getSharedPreferences("cap_storage", MODE_PRIVATE);
        prefs.edit().putString(domain, opaqueToken).apply();
        statusText.setText("Stored capability for: " + domain);
        Log.d(TAG, "Capability stored for " + domain);
    }

    private void displayCapabilities() {
        SharedPreferences prefs = getSharedPreferences("cap_storage", MODE_PRIVATE);
        Map<String, ?> allCaps = prefs.getAll();

        StringBuilder builder = new StringBuilder();
        builder.append("\n\nCurrent Capability Tokens:\n");

        if (allCaps.isEmpty()) {
            builder.append("(none stored)");
        } else {
            for (Map.Entry<String, ?> entry : allCaps.entrySet()) {
                builder.append("Domain: ").append(entry.getKey())
                        .append("\nToken: ").append(entry.getValue())
                        .append("\n\n");
            }
        }

        statusText.append(builder.toString());
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

        // attachCapsToIntent(customTabsIntent, uri); // getDomainName broken for name like 10.0.2.2

        customTabsIntent.launchUrl(context, uri);
    }

    /**
     * Attaches Capabilities to Intent if available
     * @param customTabsIntent Intent to include Capabilities to
     * @param uri to launch CustomTab for
     */
    private void attachCapsToIntent(CustomTabsIntent customTabsIntent, Uri uri) {
        SharedPreferences prefs = getSharedPreferences("cap_storage", MODE_PRIVATE);
        String domainName = null;
        try {
            domainName = getDomainName(uri);
        } catch (URISyntaxException e) {
            Log.d(TAG, "Failed to parse Domain");
        }
        String caps = prefs.getString(domainName, null);
        if (caps != null && !caps.isEmpty()) {
            customTabsIntent.intent.putExtra("browser_capability", caps);
        }
        Log.e(TAG, "No capability found for " + domainName);
    }
    private String getDomainName(Uri uri) throws URISyntaxException {
        String host = uri.getHost();
        assert host != null;
        InternetDomainName internetDomainName = InternetDomainName.from(host).topPrivateDomain();
        return internetDomainName.toString();
    }

}