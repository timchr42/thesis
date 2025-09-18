package de.cispa.custominstaller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class InstallerActivity extends AppCompatActivity {

    private static final String LOGTAG = "CustomInstaller";
    private ActivityResultLauncher<Intent> apkInstallLauncher;
    private String currentInstallingPackage;
    private TextView statusText;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installer);

        statusText = findViewById(R.id.statusText);

        apkInstallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    // The package installer returns before the actual installation
                    // finishes. Poll until the package appears on the device.
                    waitForPackageInstall(currentInstallingPackage, 0);
                }
        );

        // Handle button clicks
        findViewById(R.id.installTestAppButton).setOnClickListener(v ->
                installApk("testapp.apk", "de.cispa.testapp")
        );

        findViewById(R.id.installApp1Button).setOnClickListener(v ->
                installApk("app1.apk", "com.example.app1")
        );

        findViewById(R.id.installApp2Button).setOnClickListener(v ->
                installApk("app2.apk", "com.example.app2")
        );

        findViewById(R.id.debugButton).setOnClickListener(v -> {
            if (isPackageInstalled("de.cispa.testapp")) {
                statusText.setText("Detected installed app");
            } else {
                statusText.setText("App not installed");
            }
        });
    }


    private void installApk(String assetName, String packageName) {
        try {
            File apkFile = copyAssetToInternalStorage(assetName);
            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);

            currentInstallingPackage = packageName;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);

            apkInstallLauncher.launch(intent);

            Log.i(LOGTAG, getString(R.string.installing_asset, assetName));
        } catch (IOException e) {
            Log.e(LOGTAG, getString(R.string.apk_error, e.getMessage()));
        }
    }

    /**
     * Polls the PackageManager until the given package is installed or a timeout is reached.
     * @param packageName the package to check
     * @param attempt current polling attempt
     */
    private void waitForPackageInstall(String packageName, int attempt) {
        if (isPackageInstalled(packageName)) {
            Log.i(LOGTAG, "Successfully installed app!");

            JSONObject policy = extractPolicy(packageName);
            Orchestrator.deliverPolicy(this, policy, packageName);
            // Only Debug purpose
            statusText.setText(displayPolicy(policy));
            return;
        }

        if (attempt >= 40) {
            Log.e(LOGTAG, getString(R.string.install_fail, packageName));
            return;
        }

        handler.postDelayed(() -> waitForPackageInstall(packageName, attempt + 1), 500);
    }

    /**
     * Helper to check if a package is currently installed on the device.
     */
    private boolean isPackageInstalled(String packageName) {
        PackageManager pm=getPackageManager();
        try {
            PackageInfo info=pm.getPackageInfo(packageName,PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * Copies Asset (.sdk's in src/main/assets folder to internal storage)
     * @param assetName .skd to install
     * @return apk File used by install logic
     * @throws IOException if error occurred
     */
    private File copyAssetToInternalStorage(String assetName) throws IOException {
        File outFile = new File(getFilesDir(), assetName);
        try (InputStream is = getAssets().open(assetName);
             OutputStream os = new FileOutputStream(outFile)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        return outFile;
    }

    private JSONObject extractPolicy(String packageName) {
        try {
            Context targetContext = createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            return getJsonObject(targetContext);
        } catch (Exception e) {
            Log.e(LOGTAG, getString(R.string.policy_not_found, packageName, e.getMessage()));
            return null; // Signals no policy exist and Ambient Mode to be accessed
        }
    }

    @NonNull
    private static JSONObject getJsonObject(Context targetContext) throws IOException, JSONException {
        AssetManager assetManager = targetContext.getAssets();
        InputStream input = assetManager.open("policy.json");

        // Read the JSON file into a String
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        StringBuilder jsonBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jsonBuilder.append(line);
        }
        reader.close();

        return new JSONObject(jsonBuilder.toString());
    }

    @SuppressLint("SetTextI18n")
    private String displayPolicy(JSONObject policy) {
        try {
            StringBuilder output = new StringBuilder();

            // Predefined Capabilities
            JSONObject predefined = policy.getJSONObject("predefined");
            output.append("Predefined Domains:\n");

            formatPredefined(output, "Global Jar", predefined.getJSONObject("global"));
            formatPredefined(output, "Private Jar", predefined.getJSONObject("private"));

            // Wildcard Capabilities
            JSONObject wildcard = policy.getJSONObject("wildcard");
            output.append("\nWildcard Capabilities:\n");
            formatWildcard(output, "Global Jar", wildcard.getJSONArray("global"));
            formatWildcard(output, "Private Jar", wildcard.getJSONArray("private"));

            statusText.setText(output.toString());
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
            builder.append("  • ").append(domains.getString(i)).append("\n");
        }
    }

}
