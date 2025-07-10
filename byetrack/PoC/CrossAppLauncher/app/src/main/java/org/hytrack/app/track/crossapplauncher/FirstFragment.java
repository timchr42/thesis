package org.hytrack.app.track.crossapplauncher;

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
import androidx.fragment.app.Fragment;

import org.hytrack.app.track.crossapplauncher.databinding.FragmentFirstBinding;

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

    @SuppressLint("SetTextI18n")
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);


        Intent intent = getActivity().getIntent();
        Uri data = intent.getData();


        if (data != null && data.getPath() != null) {
            String payload = data.getPath().substring(1); // drop leading slash
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString("c", payload);
            editor.apply();

            Toast toast = Toast.makeText(getContext(), "Read " + payload.length() + " bytes", Toast.LENGTH_SHORT);
            //toast.show();

        }

        binding.toggleButtonLabel.setText(binding.toggleButtonLabel.getText() + "(" + sharedPref.getString("c", "").length() + " bytes)");

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchURL();

            }
        });

    }

    public void launchURL(){
        SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
        String user_url = binding.trackingWebsite.getText().toString();
        String appName = binding.appName.getText().toString();

        String storedCookie = sharedPref.getString("c", "");

        String url = user_url + "?hide=1&demo&app=" + appName + "&s=trackmeplslaunch";
        if (binding.toggle.isChecked()) url += "&c=" + storedCookie;
        Log.w("CTT", url);

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(BuildConfig.APPLICATION_ID);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}