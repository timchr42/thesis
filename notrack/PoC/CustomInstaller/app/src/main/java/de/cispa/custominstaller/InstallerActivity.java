package de.cispa.custominstaller;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import android.content.pm.ApplicationInfo;

public class InstallerActivity extends AppCompatActivity {

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
        findViewById(R.id.installApp1Button).setOnClickListener(v ->
                installApk("testapp.apk", "de.cispa.testapp")
        );

        findViewById(R.id.installApp2Button).setOnClickListener(v ->
                installApk("app2.apk", "com.example.app2")
        );

        findViewById(R.id.debugButton).setOnClickListener(v -> {
            if (isPackageInstalled("de.cispa.testapp")) {
                statusText.setText("✅ Detected installed app");
                extractAndShowPolicy("de.cispa.testapp");
            } else {
                statusText.setText("❌ App not installed");
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

            statusText.setText(getString(R.string.installing_asset, assetName));
        } catch (IOException e) {
            statusText.setText(getString(R.string.apk_error, e.getMessage()));
        }
    }

    /**
     * Polls the PackageManager until the given package is installed or a timeout is reached.
     * @param packageName the package to check
     * @param attempt current polling attempt
     */
    private void waitForPackageInstall(String packageName, int attempt) {
        if (isPackageInstalled(packageName)) {
            statusText.setText(getString(R.string.install_success, packageName));
            extractAndShowPolicy(packageName);
            return;
        }

        if (attempt >= 10) {
            statusText.setText(getString(R.string.install_fail, packageName));
            return;
        }

        handler.postDelayed(() -> waitForPackageInstall(packageName, attempt + 1), 500);
    }

    /**
     * Helper to check if a package is currently installed on the device.
     */
    private boolean isPackageInstalled(String packageName) {
        try {
            getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
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

    // extracts and shows policy from manifest
    private void extractAndShowPolicy(String packageName) {
        try {
            PackageManager pm = getPackageManager();

            // Get application info including meta-data
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            Bundle metaData = appInfo.metaData;

            if (metaData == null || !metaData.containsKey("policy")) {
                statusText.setText(getString(R.string.metadata_not_found, packageName));
                return;
                // Ambient Capability Case here (no policy existent) -> for Backwards Compatibility!
            }

            int resId = metaData.getInt("policy");
            Resources res = pm.getResourcesForApplication(appInfo);

            InputStream inputStream = res.openRawResource(resId);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            // Display actual policy
            statusText.setText(getString(R.string.policy_display, packageName, builder.toString()));

            reader.close();
            inputStream.close();

        } catch (Exception e) {
            statusText.setText(getString(R.string.policy_not_found, packageName, e.getMessage()));
        }
    }
}
