package de.cispa.custominstaller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class InstallerActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> apkInstallLauncher;
    private String currentInstallingPackage;
    private TextView statusText;
    // private static final String Filename1 = "app1";
    // private static final String Filename2 = "app2";

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_installer);

        statusText = findViewById(R.id.statusText);

        // You can do the assignment inside onAttach or onCreate, i.e, before the activity is displayed
        apkInstallLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        statusText.setText("Installed: " + currentInstallingPackage);
                        extractAndShowPolicy(currentInstallingPackage);
                    } else {
                        statusText.setText("Install canceled or failed for " + currentInstallingPackage);
                    }
                }
        );

        // Handle button clicks
        findViewById(R.id.installApp1Button).setOnClickListener(v ->
                installApk("app1.apk", "com.example.app1")
        );

        findViewById(R.id.installApp2Button).setOnClickListener(v ->
                installApk("app2.apk", "com.example.app2")
        );
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

    private void extractAndShowPolicy(String packageName) {
        try {
            // Create a context for the installed app
            Context targetContext = createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);

            // Access its assets
            AssetManager assetManager = targetContext.getAssets();
            InputStream inputStream = assetManager.open("policy.xml");

            // Read file to String
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }

            // Display it
            statusText.setText(getString(R.string.policy_origin_content, packageName, builder.toString()));

            reader.close();
            inputStream.close();

        } catch (PackageManager.NameNotFoundException e) {
            statusText.setText(getString(R.string.app_not_found, packageName));
        } catch (FileNotFoundException e) {
            statusText.setText(getString(R.string.policy_not_found, packageName));
        } catch (IOException e) {
            statusText.setText(getString(R.string.policy_reading_error, packageName, e.getMessage()));
        }
    }
}
