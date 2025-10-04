package de.cispa.evil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class EvilReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedData.byetrackBundle = intent.getBundleExtra("testAppsByetrackData");
    }
}
