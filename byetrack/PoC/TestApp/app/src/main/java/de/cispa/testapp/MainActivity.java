package de.cispa.testapp;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements MyCallback {
    private static final String LOGTAG = "TestApp";
    private TextView statusText;
    public TextView capStorage;
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

        mTokenManager = new TokenManager(mContext, this);

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
                mTokenManager.launchUrlMod(mContext, Uri.parse(url));
                //Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                //i.setPackage("org.mozilla.geckoview_example");
                //startActivity(i);
            }
        });

        launchTrustedUrl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "https://royaleapi.com";
                mTokenManager.launchUrlMod(mContext, Uri.parse(url));
            }
        });
    }

    @Override
    public void updateMyText(String myString) {
        capStorage.setText(myString);
    }
}