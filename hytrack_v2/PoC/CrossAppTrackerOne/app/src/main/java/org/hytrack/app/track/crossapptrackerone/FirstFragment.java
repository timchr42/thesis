package org.hytrack.app.track.crossapptrackerone;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.trusted.TrustedWebActivityIntentBuilder;
import androidx.fragment.app.Fragment;

import com.google.androidbrowserhelper.trusted.TwaLauncher;

import java.util.Arrays;
import java.util.List;

import org.hytrack.app.track.crossapptrackerone.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onResume() {
        super.onResume();
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        binding.toggleButtonLabel.setText(getResources().getText(R.string.sendcookie) + "(" + sharedPref.getString("c", "").length() + " bytes)");
    }

    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);


        Intent intent = getActivity().getIntent();
        Uri data = intent.getData();

        binding.toggleButtonLabel.setText(binding.toggleButtonLabel.getText() + "(" + sharedPref.getString("c", "").length() + " bytes)");


        if (data != null && data.getPath() != null) {
            String payload = data.getPath().substring(1); // drop leading slash
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("c", payload);
            editor.apply();

            Toast toast = Toast.makeText(getContext(), "Read " + payload.length() + " bytes", Toast.LENGTH_SHORT);
            //toast.show();

        }

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String user_url = binding.trackingWebsite.getText().toString();
                String appName = binding.appName.getText().toString();
                String storedCookie = sharedPref.getString("c", "");
                String url = user_url + "?hide=1&demo=1&app=" + appName + "&s=trackmeplsone";
                if(binding.toggle.isChecked()) url += "&c=" + storedCookie;
                Log.w("CTT", url);
                Uri LAUNCH_URI = Uri.parse(url);
                //Uri LAUNCH_URI = Uri.parse("https://schnellnochraviolimachen.de");

                List<String> origins = Arrays.asList(
                        "https://schnellnochraviolimachen.de/"
                );

                TrustedWebActivityIntentBuilder builder = new TrustedWebActivityIntentBuilder(LAUNCH_URI)
                        .setAdditionalTrustedOrigins(origins);
                // set ourselves as target of the intent
                //intent.setPackage("org.hytrack.app.track.crossapptrackerone");
                new TwaLauncher(getContext()).launch(builder, null, null, null);

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}