package de.cispa.byetrack;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

// Query with "content:// + $packagename + .tokens"
public class TokenProvider extends ContentProvider {
    private static final String LOGTAG = "TokenProvider";
    private static final Set<String> BROWSER_WHITELIST = Set.of(
            "org.mozilla.fenix.debug",
            "org.mozilla.geckoview_example"
    );

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        Context context = Objects.requireNonNull(getContext());

        int uid = Binder.getCallingUid();
        String callingPkg = Objects.requireNonNull(getContext()).getPackageManager().getNameForUid(uid);
        if (!BROWSER_WHITELIST.contains(callingPkg)) {
            Log.w(LOGTAG, "[Byetrack] Rejected tokens from unexpected package: " + callingPkg);
        }
        TokenManager.initPrefs(context);

        String wildcard_tokens_str = values.getAsString(Constants.EXTRA_WILDCARD);
        Optional.ofNullable(wildcard_tokens_str)
                .ifPresent(TokenManager::storeWildcardTokens);

        String final_tokens_str = values.getAsString(Constants.EXTRA_FINAL);
        Optional.ofNullable(final_tokens_str)
                .ifPresent(TokenManager::storeFinalTokens);

        Boolean isAmbient = values.getAsBoolean(Constants.EXTRA_ISAMBIENT);
        Optional.ofNullable(isAmbient)
                .ifPresent(TokenManager::storeIsAmbient);

        Log.d(LOGTAG, "[Byetrack] Received values from " + callingPkg + ":\n" + wildcard_tokens_str + "\n" + final_tokens_str + "\n" + isAmbient);

        return null;
    }

    // unused stub methods
    @Override public boolean onCreate() {
        return true;
    }
    @Override public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
    @Override public String getType(@NonNull Uri uri) { return null; }
}
