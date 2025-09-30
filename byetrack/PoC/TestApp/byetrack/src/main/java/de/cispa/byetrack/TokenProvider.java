package de.cispa.byetrack;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Set;

public class TokenProvider extends ContentProvider {
    private static final String LOGTAG = "TokenProvider";
    private static final Set<String> BROWSER_WHITELIST = Set.of(
            "org.mozilla.fenix.debug",
            "org.mozilla.geckoview_example"
    );

    public static final String AUTHORITY = "de.cispa.byetrack.tokenprovider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + ".tokens");

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        String tokens = values.getAsString("tokens");

        int uid = Binder.getCallingUid();
        String callingPkg = getContext().getPackageManager().getNameForUid(uid);

        if (BROWSER_WHITELIST.contains(callingPkg)) {
            Log.d(LOGTAG, "[Byetrack] Received tokens from " + callingPkg + ": " + tokens);
            TokenManager.storeFinalTokens(tokens);
        } else {
            Log.w(LOGTAG, "[Byetrack] Rejected tokens from unexpected package: " + callingPkg);
        }

        return null;
    }

    // unused stub methods
    @Override public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public String getType(@NonNull Uri uri) { return null; }
}
