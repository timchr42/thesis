package de.cispa.custominstaller;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import android.content.pm.ApplicationInfo;

public class InstallerActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> apkInstallLauncher;
    private String currentInstallingPackage;
    private TextView statusText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String PACKAGENAME1 = "de.cispa.testapp";

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

        if (attempt >= 40) {
            statusText.setText(getString(R.string.install_fail, packageName));
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

    @SuppressLint("SetTextI18n")
    private void extractAndShowPolicy(String packageName) {
        try {
            Context target = createPackageContext(packageName, Context.CONTEXT_IGNORE_SECURITY);
            AssetManager am = target.getAssets();
            InputStream input = am.open("policy.xml");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(input);
            doc.getDocumentElement().normalize();

            Map<String, List<String>> predefinedGlobal = new LinkedHashMap<>();
            Map<String, List<String>> predefinedPrivate = new LinkedHashMap<>();
            List<String> wildcardGlobal = new ArrayList<>();
            List<String> wildcardPrivate = new ArrayList<>();

            // Parse <predefined>
            NodeList predefinedNodes = doc.getElementsByTagName("predefined");
            for (int i = 0; i < predefinedNodes.getLength(); i++) {
                Element predefined = (Element) predefinedNodes.item(i);
                boolean isGlobal = Boolean.parseBoolean(predefined.getAttribute("global"));

                NodeList domainNodes = predefined.getElementsByTagName("domain");
                for (int j = 0; j < domainNodes.getLength(); j++) {
                    Element domain = (Element) domainNodes.item(j);
                    String domainName = domain.getAttribute("name");

                    NodeList cookieNodes = domain.getElementsByTagName("cookie");
                    List<String> cookieList = new ArrayList<>();
                    for (int k = 0; k < cookieNodes.getLength(); k++) {
                        Element cookie = (Element) cookieNodes.item(k);
                        cookieList.add(cookie.getAttribute("name"));
                    }

                    Map<String, List<String>> targetMap = isGlobal ? predefinedGlobal : predefinedPrivate;
                    targetMap.put(domainName, cookieList);
                }
            }

            // Parse <wildcard>
            NodeList wildcardNodes = doc.getElementsByTagName("wildcard");
            for (int i = 0; i < wildcardNodes.getLength(); i++) {
                Element wildcard = (Element) wildcardNodes.item(i);
                boolean isGlobal = Boolean.parseBoolean(wildcard.getAttribute("global"));

                NodeList domainNodes = wildcard.getElementsByTagName("domain");
                for (int j = 0; j < domainNodes.getLength(); j++) {
                    Element domain = (Element) domainNodes.item(j);
                    String domainName = domain.getAttribute("name");
                    if (isGlobal) wildcardGlobal.add(domainName);
                    else wildcardPrivate.add(domainName);
                }
            }

            StringBuilder out = new StringBuilder();
            formatOut(out, predefinedGlobal, predefinedPrivate, wildcardGlobal, wildcardPrivate);
            statusText.setText(out.toString());

        } catch (PackageManager.NameNotFoundException e) {
            statusText.setText("Target app not installed.");
        } catch (FileNotFoundException e) {
            statusText.setText("policy.xml not found in target app's assets.");
        } catch (Exception e) {
            statusText.setText(getString(R.string.policy_not_found, packageName, e.getMessage()));
        }
    }

    private void formatOut(StringBuilder builder, Map<String, List<String>> predefinedGlobal, Map<String, List<String>> predefinedLocal, List<String> wildcardGlobal, List<String> wildcardLocal) {
        builder.append("Developer-defined Policy:\n");

        // Predefined
        builder.append("\nPredefined Capabilities:\n")
                        .append("\n- Global:\n");
        appendMapBlock(builder, predefinedGlobal);
        builder.append("\n-Local:\n");
        appendMapBlock(builder, predefinedLocal);

        //Wildcard
        builder.append("\nWildcard Capabilities:\n")
                .append("\n- Global:\n");
        appendListBlock(builder, wildcardGlobal);
        builder.append("\n- Local:\n");
        appendListBlock(builder, wildcardLocal);
    }

    private void appendMapBlock(StringBuilder builder, Map<String, List<String>> map) {
        if (map.isEmpty()) {
            builder.append("  (none)\n");
            return;
        }
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            builder.append("\t- ").append(entry.getKey());
            if (!entry.getValue().isEmpty()) {
                builder.append(" [").append(String.join(", ", entry.getValue())).append("]");
            }
            builder.append("\n");
        }
    }

    private void appendListBlock(StringBuilder builder, List<String> list) {
        if (list.isEmpty()) {
            builder.append("  (none)\n");
            return;
        }
        for (String item : list) {
            builder.append("\t- ").append(item).append("\n");
        }
    }

}
