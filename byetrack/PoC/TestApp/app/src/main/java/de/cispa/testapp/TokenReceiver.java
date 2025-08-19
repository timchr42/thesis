package de.cispa.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static de.cispa.testapp.TokenManager.storeTokens;
import static de.cispa.testapp.TokenManager.storage_wildcard_tokens;
import static de.cispa.testapp.TokenManager.storage_final_tokens;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Tokens received from Browser");

        String wildcard_tokens_str = intent.getStringExtra("capability_tokens");
        String final_tokens_str = intent.getStringExtra("final_tokens");
        if (wildcard_tokens_str != null) {
            storeTokens(wildcard_tokens_str, storage_wildcard_tokens);
        } else {
            storeTokens(final_tokens_str, storage_final_tokens);
        }

    }
}
