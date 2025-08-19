package de.cispa.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import static de.cispa.testapp.TokenManager.storeTokens;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";
    private static final String EXTRA_WILDCARD = "capability_tokens";
    private static final String EXTRA_FINAL    = "final_tokens";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Tokens received from Browser");

        String wildcard_tokens_str = intent.getStringExtra(EXTRA_WILDCARD);
        String final_tokens_str = intent.getStringExtra(EXTRA_FINAL);
        if (wildcard_tokens_str != null) {
            SharedPreferences storage_wildcard =
                    context.getSharedPreferences(TokenManager.CAPSTORAGE_BUILDER, Context.MODE_PRIVATE);
            storeTokens(wildcard_tokens_str, storage_wildcard);
        } else {
            SharedPreferences storage_final =
                    context.getSharedPreferences(TokenManager.CAPSTORAGE_FINAL, Context.MODE_PRIVATE);
            storeTokens(final_tokens_str, storage_final);
        }

    }
}
