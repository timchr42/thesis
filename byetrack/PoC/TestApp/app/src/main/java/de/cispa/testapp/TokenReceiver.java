package de.cispa.testapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static de.cispa.testapp.TokenManager.storeFinalTokens;
import static de.cispa.testapp.TokenManager.storeWildcardTokens;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";
    private static final String EXTRA_WILDCARD = "capability_tokens";
    private static final String EXTRA_FINAL = "final_tokens";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Tokens received from Browser");

        String wildcard_tokens_str = intent.getStringExtra(EXTRA_WILDCARD);
        String final_tokens_str = intent.getStringExtra(EXTRA_FINAL);
        if (wildcard_tokens_str != null)
            storeWildcardTokens(wildcard_tokens_str, context);
        else
            storeFinalTokens(final_tokens_str, context);
    }
}
