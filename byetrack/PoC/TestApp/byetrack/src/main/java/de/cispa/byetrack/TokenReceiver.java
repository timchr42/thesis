package de.cispa.byetrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TokenReceiver extends BroadcastReceiver {
    private static final String LOGTAG = "TokenReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(LOGTAG, "Tokens received from Browser");

        String wildcard_tokens_str = intent.getStringExtra(Constants.EXTRA_WILDCARD);
        String final_tokens_str = intent.getStringExtra(Constants.EXTRA_FINAL);
        boolean isAmbient = intent.getBooleanExtra(Constants.EXTRA_ISAMBIENT, false);
        Log.d(LOGTAG, isAmbient ? "isAmbient: true" : "isAmbient: false");

        //if (wildcard_tokens_str != null) storeWildcardTokens(wildcard_tokens_str, context);
        //if (final_tokens_str != null) storeFinalTokens(final_tokens_str, context);
        //storeIsAmbient(isAmbient, context);
        Log.i(LOGTAG, "Final Tokens String should be empty: " + final_tokens_str);
        if (wildcard_tokens_str == null) return;
        TokenManager.initPrefs(context);
        TokenManager.storeWildcardTokens(wildcard_tokens_str);
        TokenManager.storeIsAmbient(isAmbient);
    }
}
