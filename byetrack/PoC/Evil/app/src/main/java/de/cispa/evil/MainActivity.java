package de.cispa.evil;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button launchButton = findViewById(R.id.btnLaunch);
        launchButton.setOnClickListener(v -> {

            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            Bundle byetrackData = SharedData.byetrackBundle;
            Log.d("byetrackData", byetrackData.toString());

            customTabsIntent.intent.putExtra("byetrack_data", byetrackData);
            customTabsIntent.launchUrl(this, Uri.parse("http://10.0.2.2"));
        });
    }

}