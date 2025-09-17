package de.cispa.byetrack;

import static de.cispa.byetrack.TokenManager.storeFinalTokens;
import static de.cispa.byetrack.TokenManager.storeWildcardTokens;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class CapabilityService extends Service {
    private static final String LOGTAG = "CapabilityService";
    private static final String EXTRA_WILDCARD = "capability_tokens";
    private static final String EXTRA_FINAL = "final_tokens";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        assert intent != null;
        Log.i(LOGTAG, "Tokens received!");

        String wildcard_tokens_str = intent.getStringExtra(EXTRA_WILDCARD);
        String final_tokens_str = intent.getStringExtra(EXTRA_FINAL);
        if (wildcard_tokens_str != null)
            storeWildcardTokens(wildcard_tokens_str, this);
        else
            storeFinalTokens(final_tokens_str, this);

        // Stop service after handling this request
        stopSelf(startId);
        return START_NOT_STICKY; // Don’t restart automatically
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // No binding, just one-shot messages
    }
}
