package de.cispa.evil;

import android.os.Binder;
import android.os.Parcel;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

class BinderToken extends Binder {
    private static final String LOGTAG = "BinderToken";
    @Override
    protected boolean onTransact(int code, @NonNull Parcel data, Parcel reply, int flags) {
        int uid = Process.myUid();
        assert reply != null;
        reply.writeInt(uid);
        Log.d(LOGTAG, "[Byetrack] Process.myUid: " + uid);
        return true;
    }
}
