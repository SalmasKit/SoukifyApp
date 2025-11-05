package com.example.soukify.ui.settings;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.soukify.databinding.FragmentAccountSettingsBinding;
import android.provider.MediaStore;

/**
 * Fragment for Account Settings where users can edit their profile info.
 */
public class AccountSettingsFragment extends Fragment {
    private FragmentAccountSettingsBinding binding;
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (binding != null) {
                        binding.profileImage.setImageURI(uri);
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void showChangePhotoDialog() {
        String[] items = new String[]{
                getString(com.example.soukify.R.string.from_gallery),
                getString(com.example.soukify.R.string.remove_photo)
        };
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.soukify.R.string.choose_photo)
                .setItems(items, (dialog, which) -> {
                    if (which == 0) {
                        openGallery();
                    } else if (which == 1) {
                        if (binding != null) {
                            binding.profileImage.setImageResource(com.example.soukify.R.drawable.ic_profile_placeholder);
                        }
                    }
                })
                .show();
    }

    private void openGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            pickImageLauncher.launch(intent);
        } catch (ActivityNotFoundException e) {
            showToast(getString(com.example.soukify.R.string.no_gallery_app));
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Click listeners
        binding.changePhotoButton.setOnClickListener(v -> showChangePhotoDialog());
        binding.changePasswordButton.setOnClickListener(v -> showChangePassword());
        binding.saveButton.setOnClickListener(v -> saveChanges());

        // TODO: Load existing user data into fields
        // loadUserData();
    }

    private void showChangePassword() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(com.example.soukify.R.layout.dialog_change_password, null, false);

        final TextInputLayout currentLayout = dialogView.findViewById(com.example.soukify.R.id.currentPasswordLayout);
        final TextInputLayout newLayout = dialogView.findViewById(com.example.soukify.R.id.newPasswordLayout);
        final TextInputLayout confirmLayout = dialogView.findViewById(com.example.soukify.R.id.confirmPasswordLayout);
        final TextInputEditText currentEt = dialogView.findViewById(com.example.soukify.R.id.currentPasswordEditText);
        final TextInputEditText newEt = dialogView.findViewById(com.example.soukify.R.id.newPasswordEditText);
        final TextInputEditText confirmEt = dialogView.findViewById(com.example.soukify.R.id.confirmPasswordEditText);

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(com.example.soukify.R.string.change_password)
                .setView(dialogView)
                .setNegativeButton(com.example.soukify.R.string.cancel, (d, which) -> d.dismiss())
                .setPositiveButton(com.example.soukify.R.string.update, null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                // Clear previous errors
                currentLayout.setError(null);
                newLayout.setError(null);
                confirmLayout.setError(null);

                String current = currentEt.getText() != null ? currentEt.getText().toString() : "";
                String newer = newEt.getText() != null ? newEt.getText().toString() : "";
                String confirm = confirmEt.getText() != null ? confirmEt.getText().toString() : "";

                boolean hasError = false;
                if (current.isEmpty()) {
                    currentLayout.setError(getString(com.example.soukify.R.string.current_password_required));
                    hasError = true;
                }
                if (newer.length() < 6) {
                    newLayout.setError(getString(com.example.soukify.R.string.password_too_short));
                    hasError = true;
                }
                if (!newer.equals(confirm)) {
                    confirmLayout.setError(getString(com.example.soukify.R.string.passwords_do_not_match));
                    hasError = true;
                }

                if (hasError) return;

                // TODO: Call backend to change password using `current` and `newer`
                showToast(getString(com.example.soukify.R.string.password_updated));
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void saveChanges() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty()) {
            showToast("Please fill in required fields");
            return;
        }

        // TODO: Persist changes
        showToast("Changes saved");
        requireActivity().onBackPressed();
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
