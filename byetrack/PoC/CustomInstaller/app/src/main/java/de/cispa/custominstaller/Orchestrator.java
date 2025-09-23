package de.cispa.custominstaller;

import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.Cipher;

public final class Orchestrator {
    private static final String LOGTAG = "Orchestrator";

    public static final String METHOD_GET_PIPE = "GET_PIPE";
    public static final String METHOD_GET_CHANNEL = "GET_CHANNEL";
    public static final String METHOD_GET_PUBKEY = "GET_PUBKEY";

    public static final String EXTRA_PIPE = "de.cispa.byetrack.EXTRA_PIPE";
    public static final String EXTRA_CHANNEL = "de.cispa.byetrack.EXTRA_CHANNEL";
    public static final String EXTRA_PUBKEY = "de.cispa.byetrack.EXTRA_PUBKEY";
    public static final String EXTRA_ALIAS = "de.cispa.byetrack.EXTRA_ALIAS";
    public static final String EXTRA_TRANSFORMATION = "de.cispa.byetrack.EXTRA_TRANSFORMATION";

    public static final String AUTH_BROWSER = "org.mozilla.geckoview_example.pipe";

    public static void deliverPolicy(Context ctx, String policyStr, String packageName) {
        String AUTH_APP = packageName + ".pipe";

        try {
            // Ask App for its PendingIntents
            PendingIntent appChannel = getPI(ctx, AUTH_APP, METHOD_GET_CHANNEL);
            if (appChannel == null) { Log.e(LOGTAG, "App channel is null"); return; }

            PendingIntent appPipe = getPI(ctx, AUTH_APP, METHOD_GET_PIPE);
            if (appPipe == null) { Log.e(LOGTAG, "App pipe is null"); return; }

            // Ask Browser for its pipe
            PendingIntent browserPipe = getPI(ctx, AUTH_BROWSER, METHOD_GET_PIPE);
            if (browserPipe == null) { Log.e(LOGTAG, "Browser pipe is null"); return; }

            // Generate symmetric AES key (256 bits)
            SecretKey symKey = generateSymmetricKey();

            // Get wrapping infos from App and Browser
            WrappingInfo appWI = getWrappingInfo(ctx, AUTH_APP);
            WrappingInfo browserWI = getWrappingInfo(ctx, AUTH_BROWSER);

            // Wrap key for App & Browser
            byte[] wrappedForApp = wrapKeyForRecipient(symKey, appWI.publicKey, appWI.transformation);
            byte[] wrappedForBrowser = wrapKeyForRecipient(symKey, browserWI.publicKey, browserWI.transformation);

            // Deliver policy and wrapped keys to Browser
            Intent fill = new Intent()
                    .putExtra("policy_json", policyStr)
                    .putExtra("app_pipe", appPipe)
                    .putExtra("app_channel", appChannel)
                    .putExtra("package_name", packageName)
                    .putExtra("version_name", getAppVersionName(packageName, ctx))

                    // send wrapped keys + metadata
                    .putExtra("wrapped_key_app", wrappedForApp)
                    .putExtra("wrapped_key_browser", wrappedForBrowser)
                    .putExtra("wrapping_alias_app", appWI.alias)
                    .putExtra("wrapping_transformation_app", appWI.transformation)
                    .putExtra("wrapping_alias_browser", browserWI.alias)
                    .putExtra("wrapping_transformation_browser", browserWI.transformation);

            browserPipe.send(ctx, 0, fill);
            Log.i(LOGTAG, "Policy + wrapped keys delivered to Browser");

        } catch (PendingIntent.CanceledException e) {
            Log.e(LOGTAG, "PI was canceled/one-shot already used", e);
        } catch (Exception ex) {
            Log.e(LOGTAG, "deliverPolicy error", ex);
        }
    }

    private static PendingIntent getPI(Context ctx, String AUTH, String METHOD) {
        Uri uri = Uri.parse("content://" + AUTH);
        Bundle b = ctx.getContentResolver().call(uri, METHOD, null, null);
        if (b == null) return null;
        String EXTRA = (METHOD.equals(METHOD_GET_CHANNEL)) ? EXTRA_CHANNEL : EXTRA_PIPE;
        return b.getParcelable(EXTRA, PendingIntent.class);
    }

    private static WrappingInfo getWrappingInfo(Context ctx, String AUTH) throws Exception {
        Uri uri = Uri.parse("content://" + AUTH);
        Bundle b = ctx.getContentResolver().call(uri, METHOD_GET_PUBKEY, null, null);
        if (b == null) throw new IllegalStateException("No wrapping info from " + AUTH);

        byte[] pubKeyBytes = b.getByteArray(EXTRA_PUBKEY);
        PublicKey pubKey = decodePublicKey(pubKeyBytes);
        String alias = b.getString(EXTRA_ALIAS);
        String transformation = b.getString(EXTRA_TRANSFORMATION);

        return new WrappingInfo(pubKey, alias, transformation, null);
    }

    public static PublicKey decodePublicKey(byte[] encoded) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(encoded);
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private static SecretKey generateSymmetricKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    private static byte[] wrapKeyForRecipient(SecretKey key, PublicKey recipientPub, String transformation) throws Exception {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.WRAP_MODE, recipientPub);
        return cipher.wrap(key);
    }

    private static String getAppVersionName(String packageName, Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOGTAG, "Package not found: " + packageName, e);
            return null;
        } catch (Exception e) {
            Log.e(LOGTAG, "Error retrieving package info for " + packageName, e);
            return null;
        }
    }

    // data class for easier processing
    private static class WrappingInfo {
        final PublicKey publicKey;
        final String alias;
        final String transformation;
        final AlgorithmParameterSpec params;
        WrappingInfo(PublicKey pk, String alias, String transf, AlgorithmParameterSpec spec) {
            this.publicKey = pk;
            this.alias = alias;
            this.transformation = transf;
            this.params = spec;
        }
    }
}
