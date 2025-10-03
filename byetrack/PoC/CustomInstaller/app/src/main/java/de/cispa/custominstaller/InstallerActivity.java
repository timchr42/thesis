package de.cispa.custominstaller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;

public class InstallerActivity extends AppCompatActivity {

    private static final String LOGTAG = "CustomInstaller";
    private TextView statusText;
    private ActivityResultLauncher<Intent> installLauncher;
    private String currentInstallingPackageName;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installer);

        statusText = findViewById(R.id.statusText);

        installLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.i(LOGTAG, "Install succeeded!");
                        Toast.makeText(this, "Install succeeded", Toast.LENGTH_LONG).show();

                        String policy = extractPolicy(currentInstallingPackageName);
                        deliverPolicy(policy, currentInstallingPackageName);

                        statusText.setText(displayPolicy(policy));
                    } else {
                        Log.e(LOGTAG, "Install failed or was canceled. Code=" + result.getResultCode());
                        Toast.makeText(this, "Install failed/canceled", Toast.LENGTH_LONG).show();
                    }
                }
        );

        findViewById(R.id.installTestAppButton).setOnClickListener(v ->
            installWithLauncher("testapp.apk", "de.cispa.testapp")

        );

        findViewById(R.id.installApp1Button).setOnClickListener(v ->
                installWithLauncher("trackerone_nopolicy.apk", "org.hytrack.app.track.crossapptrackerone.instrumented")
        );

        findViewById(R.id.installApp2Button).setOnClickListener(v ->
                //statusText.setText("Not yet implemented")
                installWithLauncher("trackerone_policy.apk", "org.hytrack.app.track.crossapptrackerone.policy")

        );

        //findViewById(R.id.debugButton).setOnClickListener(v -> });
    }

    private Uri copyApkFromAssetsToFiles(String assetName) throws IOException {
        File outFile = new File(getFilesDir(), assetName);
        try (InputStream in = getAssets().open(assetName);
             OutputStream out = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
        return FileProvider.getUriForFile(this, getPackageName() + ".provider", outFile);
    }

    private void installWithLauncher(String assetName, String packageName) {
        currentInstallingPackageName = packageName;

        try {
            Uri apkUri = copyApkFromAssetsToFiles(assetName);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);

            installLauncher.launch(intent);
            Log.i(LOGTAG, "Started install flow with launcher for: " + assetName);
        } catch (IOException e) {
            Log.e(LOGTAG, "Failed to copy asset " + assetName, e);
            Toast.makeText(this, "Copy failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void deliverPolicy(String policyStr, String packageName) {
        try {
            String AUTH = "content://org.mozilla.provider.policy";

            ContentValues values = new ContentValues();
            values.put("policy_json", policyStr);
            values.put("package_name", packageName);
            values.put("version_name", getAppVersionName(packageName));

            Log.d(LOGTAG, "[Byetrack] Sending tokens to " + packageName + " : " + policyStr);
            getApplicationContext().getContentResolver().insert(Uri.parse(AUTH), values);
        } catch (Exception e) {
            Log.e(LOGTAG, "Failed delivering policy of " + packageName, e);
        }
    }

    public String extractPolicy(String packageName) {
        try {
            Context targetContext = createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            return getJsonObject(targetContext).toString();
        } catch (Exception e) {
            Log.i(LOGTAG, "No Policy found; returning null signalising Ambient Mode");
            return null; // Ambient mode
        }
    }

    @NonNull
    private static JSONObject getJsonObject(Context targetContext) throws IOException, JSONException {
        AssetManager assetManager = targetContext.getAssets();
        InputStream input = assetManager.open("policy.json");

        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        reader.close();

        return new JSONObject(jsonBuilder.toString());
    }

    private String getAppVersionName(String packageName) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(packageName, 0);
            return info.versionName;  // e.g., "1.0.3"
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(LOGTAG, "Package not found: " + packageName, e);
            return null;
        } catch (Exception e) {
            Log.e(LOGTAG, "Error retrieving package info for " + packageName, e);
            return null;
        }
    }

    @SuppressLint("SetTextI18n")
    private String displayPolicy(String policyStr) {
        if (policyStr == null) return "(no policy => Enable Ambient Mode)";

        try {
            JSONObject policy = new JSONObject(policyStr);
            StringBuilder output = new StringBuilder();

            JSONObject predefined = policy.getJSONObject("predefined");
            output.append("Predefined Domains:\n");
            formatPredefined(output, "Global Jar", predefined.getJSONObject("global"));
            formatPredefined(output, "Private Jar", predefined.getJSONObject("private"));

            JSONObject wildcard = policy.getJSONObject("wildcard");
            output.append("\nWildcard Capabilities:\n");
            formatWildcard(output, "Global Jar", wildcard.getJSONArray("global"));
            formatWildcard(output, "Private Jar", wildcard.getJSONArray("private"));

            return output.toString();
        } catch (Exception e) {
            return "Displaying Policy failed";
        }
    }

    private void formatPredefined(StringBuilder builder, String label, JSONObject domainMap) throws JSONException {
        builder.append("- ").append(label).append(":\n");
        if (domainMap.length() == 0) {
            builder.append("  (none)\n");
            return;
        }
        for (Iterator<String> it = domainMap.keys(); it.hasNext(); ) {
            String domain = it.next();
            JSONArray cookies = domainMap.optJSONArray(domain);
            builder.append("  • ").append(domain);
            if (cookies != null && cookies.length() > 0) {
                builder.append(" [");
                for (int i = 0; i < cookies.length(); i++) {
                    builder.append(cookies.getString(i));
                    if (i < cookies.length() - 1) builder.append(", ");
                }
                builder.append("]");
            }
            builder.append("\n");
        }
    }

    private void formatWildcard(StringBuilder builder, String label, JSONArray domains) throws JSONException {
        builder.append("- ").append(label).append(":\n");
        if (domains.length() == 0) {
            builder.append("  (none)\n");
            return;
        }
        for (int i = 0; i < domains.length(); i++) {
            builder.append("\t- ").append(domains.getString(i)).append("\n");
        }
    }

}
