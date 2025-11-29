package com.example.soukify.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;
import android.widget.TextView;

import com.example.soukify.R;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set version text
        TextView versionText = view.findViewById(R.id.versionText);
        versionText.setText(getString(com.example.soukify.R.string.app_version_placeholder,
                getAppVersion()));

        Button privacyButton = view.findViewById(R.id.privacyButton);
        Button termsButton = view.findViewById(R.id.termsButton);
        Button rateButton = view.findViewById(R.id.rateButton);
        Button shareButton = view.findViewById(R.id.shareButton);
        
        privacyButton.setOnClickListener(v -> openUrl("https://example.com/privacy"));
        termsButton.setOnClickListener(v -> openUrl("https://example.com/terms"));
        rateButton.setOnClickListener(v -> rateApp());
        shareButton.setOnClickListener(v -> shareApp());
    }

    private String getAppVersion() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void openUrl(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {}
    }

    private void rateApp() {
        final String pkg = requireContext().getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        }
    }

    private void shareApp() {
        final String pkg = requireContext().getPackageName();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(com.example.soukify.R.string.app_name));
        intent.putExtra(Intent.EXTRA_TEXT, "Check out this app: https://play.google.com/store/apps/details?id=" + pkg);
        startActivity(Intent.createChooser(intent, getString(com.example.soukify.R.string.share_app)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
