package de.cispa.byetrack;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.util.Log;

import java.util.Set;

public class TokenService extends Service {
    private static final String LOGTAG = "TokenService";
    private static final Set<String> BROWSER_WHITELIST = Set.of(
            "org.mozilla.fenix.debug",
            "org.mozilla.geckoview_example"
    );

    // Handler for incoming messages
    static class IncomingHandler extends Handler {
        private final Service service;

        IncomingHandler(Looper looper, Service svc) {
            super(looper);
            this.service = svc;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                Bundle data = msg.getData();
                String tokens = data.getString("tokens");

                int callingUid = Binder.getCallingUid();
                String callingPackage = service.getPackageManager().getNameForUid(callingUid);

                if (BROWSER_WHITELIST.contains(callingPackage)) {
                    Log.d(LOGTAG, "[Byetrack] Received tokens from browser: " + tokens);
                    TokenManager.storeFinalTokens(tokens);
                } else {
                    Log.w(LOGTAG, "[Byetrack] Rejected call from unexpected package: " + callingPackage);
                }
            } else {
                super.handleMessage(msg);
            }
        }
    }

    private Messenger mMessenger;

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("TokenServiceThread");
        thread.start();
        mMessenger = new Messenger(new IncomingHandler(thread.getLooper(), this));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }
}
