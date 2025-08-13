package de.cispa.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static de.cispa.testapp.TokenManager.storeTokens;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Intent received " + intent.getPackage());

        String status = intent.getStringExtra("status");
        if ("success".equals(status)) {
            Log.d(LOGTAG, "Tokens received from Browser");

            String tokenJson = intent.getStringExtra("capability_tokens");
            // String timestamp = intent.getStringExtra("timestamp");
            storeTokens(tokenJson);

        } else {
            String error = intent.getStringExtra("error_message");
            Log.e(LOGTAG, "Token generation failed: " + error);
        }
    }
}
