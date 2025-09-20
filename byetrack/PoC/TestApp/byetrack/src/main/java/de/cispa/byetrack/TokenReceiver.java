package de.cispa.byetrack;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static de.cispa.byetrack.TokenManager.storeFinalTokens;
import static de.cispa.byetrack.TokenManager.storeIsAmbient;
import static de.cispa.byetrack.TokenManager.storeWildcardTokens;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Tokens received from Browser");

        String wildcard_tokens_str = intent.getStringExtra(Constants.EXTRA_WILDCARD);
        String final_tokens_str = intent.getStringExtra(Constants.EXTRA_FINAL);
        boolean isAmbient = intent.getBooleanExtra(Constants.EXTRA_ISAMBIENT, false);
        Log.d(LOGTAG, isAmbient ? "isAmbient: true" : "isAmbient: false");

        if (wildcard_tokens_str != null) storeWildcardTokens(wildcard_tokens_str, context);
        if (final_tokens_str != null) storeFinalTokens(final_tokens_str, context);
        storeIsAmbient(isAmbient, context);
    }
}
