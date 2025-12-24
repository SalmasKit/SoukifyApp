package com.example.soukify.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.Button;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.example.soukify.R;
public class PrivacyFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // This will handle the back button press
            requireActivity().onBackPressed();
        });

        // TODO: Load existing privacy preferences

        Button openAppSettingsButton = view.findViewById(R.id.openAppSettingsButton);
        Button clearDataButton = view.findViewById(R.id.clearDataButton);
        
        openAppSettingsButton.setOnClickListener(v -> openAppSettings());
        clearDataButton.setOnClickListener(v -> confirmClearData());
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.fromParts("package", requireContext().getPackageName(), null));
            startActivity(intent);
        } catch (Exception e) {
            showToast("Unable to open app settings");
        }
    }

    private void confirmClearData() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.soukify.R.string.clear_app_data)
                .setMessage(com.example.soukify.R.string.clear_app_data_desc)
                .setNegativeButton(com.example.soukify.R.string.cancel, (d, w) -> d.dismiss())
                .setPositiveButton(com.example.soukify.R.string.clear, (d, w) -> performClearData())
                .show();
    }

    private void performClearData() {
        // TODO: Clear caches, prefs, databases as needed
        showToast(getString(com.example.soukify.R.string.data_cleared));
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
