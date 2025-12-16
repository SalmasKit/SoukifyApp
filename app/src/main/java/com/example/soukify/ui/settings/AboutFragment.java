package com.example.soukify.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;

public class AboutFragment extends Fragment {

    private LinearLayout privacyHeader, termsHeader, privacyDetails, termsDetails;
    private TextView privacyContent, termsContent;
    private ImageView privacyIcon, termsIcon;

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

        // Initialize expandable sections
        initExpandableSections(view);
        setupClickListeners();
        
        // Rate and Share buttons
        View rateButton = view.findViewById(R.id.rateButton);
        View shareButton = view.findViewById(R.id.shareButton);
        
        rateButton.setOnClickListener(v -> rateApp());
        shareButton.setOnClickListener(v -> shareApp());
    }

    private void initExpandableSections(View view) {
        // Privacy Policy section
        privacyHeader = view.findViewById(R.id.privacyHeader);
        privacyContent = view.findViewById(R.id.privacyContent);
        privacyDetails = view.findViewById(R.id.privacyDetails);
        privacyIcon = view.findViewById(R.id.privacyIcon);

        // Terms of Service section
        termsHeader = view.findViewById(R.id.termsHeader);
        termsContent = view.findViewById(R.id.termsContent);
        termsDetails = view.findViewById(R.id.termsDetails);
        termsIcon = view.findViewById(R.id.termsIcon);
    }

    private void setupClickListeners() {
        privacyHeader.setOnClickListener(v -> toggleSection("privacy"));
        termsHeader.setOnClickListener(v -> toggleSection("terms"));
    }

    private void toggleSection(String section) {
        switch (section) {
            case "privacy":
                toggleVisibility(privacyContent, privacyDetails, privacyIcon);
                break;
            case "terms":
                toggleVisibility(termsContent, termsDetails, termsIcon);
                break;
        }
    }

    private void toggleVisibility(TextView contentView, LinearLayout detailsView, ImageView iconView) {
        if (contentView.getVisibility() == View.GONE) {
            contentView.setVisibility(View.VISIBLE);
            detailsView.setVisibility(View.VISIBLE);
            iconView.setImageResource(R.drawable.ic_expand_less);
        } else {
            contentView.setVisibility(View.GONE);
            detailsView.setVisibility(View.GONE);
            iconView.setImageResource(R.drawable.ic_expand_more);
        }
    }

    private String getAppVersion() {
        try {
            return requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "unknown";
        }
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
