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
import com.example.soukify.databinding.FragmentPrivacyBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PrivacyFragment extends Fragment {
    private FragmentPrivacyBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPrivacyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Load existing privacy preferences

        binding.openAppSettingsButton.setOnClickListener(v -> openAppSettings());
        binding.clearDataButton.setOnClickListener(v -> confirmClearData());
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
        binding = null;
    }
}
