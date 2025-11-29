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
import android.widget.Button;
import android.widget.ImageView;
import android.provider.MediaStore;

import com.example.soukify.R;
import com.example.soukify.data.models.UserModel;

/**
 * Fragment for Account Settings where users can edit their profile info.
 */
public class AccountSettingsFragment extends Fragment {
    private ImageView profileImageView;
    private SettingsViewModel viewModel;
    private UserModel currentUser;
    
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (profileImageView != null) {
                        profileImageView.setImageURI(uri);
                        // Save image URI using ViewModel
                        if (viewModel != null) {
                            viewModel.updateProfileImage(uri.toString());
                        }
                    }
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account_settings, container, false);
    }

    private void showChangePhotoDialog(View view) {
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
                        ImageView profileImage = view.findViewById(R.id.profileImage);
                        profileImage.setImageResource(com.example.soukify.R.drawable.ic_profile_placeholder);
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
        
        profileImageView = view.findViewById(R.id.profileImage);
        
        // Initialize ViewModel
        viewModel = new androidx.lifecycle.ViewModelProvider(this).get(SettingsViewModel.class);
        
        // Observe LiveData
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            currentUser = user;
            updateUIWithUserData(view, user);
        });
        
        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                android.util.Log.d("AccountSettings", "Operation result: " + result);
                showToast(result);
            }
        });

        // Click listeners
        Button changePhotoButton = view.findViewById(R.id.changePhotoButton);
        Button changePasswordButton = view.findViewById(R.id.changePasswordButton);
        Button saveButton = view.findViewById(R.id.saveButton);
        
        changePhotoButton.setOnClickListener(v -> showChangePhotoDialog(view));
        changePasswordButton.setOnClickListener(v -> showChangePassword());
        saveButton.setOnClickListener(v -> saveChanges(view));
    }

    private void showChangePassword() {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(com.example.soukify.R.layout.dialog_change_password, null, false);

        final TextInputLayout currentLayout = dialogView.findViewById(R.id.currentPasswordLayout);
        final TextInputLayout newLayout = dialogView.findViewById(R.id.newPasswordLayout);
        final TextInputLayout confirmLayout = dialogView.findViewById(R.id.confirmPasswordLayout);
        final TextInputEditText currentEt = dialogView.findViewById(R.id.currentPasswordEditText);
        final TextInputEditText newEt = dialogView.findViewById(R.id.newPasswordEditText);
        final TextInputEditText confirmEt = dialogView.findViewById(R.id.confirmPasswordEditText);

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

                // Update password using ViewModel
                if (viewModel != null) {
                    viewModel.updateUserPassword(current, newer);
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void saveChanges(View view) {
        View rootView = getView();
        if (rootView == null) return;
        
        TextInputEditText nameEditText = rootView.findViewById(R.id.nameEditText);
        TextInputEditText emailEditText = rootView.findViewById(R.id.emailEditText);
        TextInputEditText phoneEditText = rootView.findViewById(R.id.phoneEditText);
        
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();

        android.util.Log.d("AccountSettings", "Saving changes - Name: " + name + ", Email: " + email + ", Phone: " + phone);
        android.util.Log.d("AccountSettings", "Current user object: " + (currentUser != null ? "exists" : "null"));
        android.util.Log.d("AccountSettings", "Current user email: " + (currentUser != null && currentUser.getEmail() != null ? currentUser.getEmail() : "null"));

        if (name.isEmpty() || email.isEmpty()) {
            showToast("Please fill in required fields");
            return;
        }
        
        // Get current user to check if email changed
        String currentEmail = (currentUser != null && currentUser.getEmail() != null) ? currentUser.getEmail().trim() : "";
        String newEmail = email.trim();
        
        android.util.Log.d("AccountSettings", "Email comparison - Current: '" + currentEmail + "', New: '" + newEmail + "'");
        android.util.Log.d("AccountSettings", "Are emails equal? " + currentEmail.equalsIgnoreCase(newEmail));
        android.util.Log.d("AccountSettings", "Current email hash: " + currentEmail.hashCode() + ", New email hash: " + newEmail.hashCode());
        
        if (currentUser != null && currentUser.getEmail() != null && !currentEmail.equalsIgnoreCase(newEmail)) {
            android.util.Log.d("AccountSettings", "Email changed from " + currentEmail + " to " + newEmail);
            // Email changed, ask for password
            showPasswordDialogForEmailChange(name, newEmail, phone);
        } else {
            android.util.Log.d("AccountSettings", "No email change detected");
            // No email change, update normally
            if (viewModel != null) {
                viewModel.updateUserProfile(name, newEmail, phone);
            }
        }
    }
    
    private void showPasswordDialogForEmailChange(String name, String email, String phone) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_change_password, null, false);

        final TextInputLayout passwordLayout = dialogView.findViewById(R.id.currentPasswordLayout);
        final TextInputEditText passwordEditText = dialogView.findViewById(R.id.currentPasswordEditText);
        
        // Hide new and confirm password fields - only current password needed for email change
        TextInputLayout newPasswordLayout = dialogView.findViewById(R.id.newPasswordLayout);
        TextInputLayout confirmPasswordLayout = dialogView.findViewById(R.id.confirmPasswordLayout);
        if (newPasswordLayout != null) {
            newPasswordLayout.setVisibility(View.GONE);
        }
        if (confirmPasswordLayout != null) {
            confirmPasswordLayout.setVisibility(View.GONE);
        }
        
        // Update dialog for email change
        passwordLayout.setHint("Current Password");
        passwordEditText.setHint("Enter your current password");

        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Confirm Email Change")
                .setMessage("To change your email, please enter your current password for security.")
                .setView(dialogView)
                .setNegativeButton("Cancel", (d, w) -> d.dismiss())
                .setPositiveButton("Update Email", null);

        final AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            android.widget.Button positive = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                String password = passwordEditText.getText() != null ? passwordEditText.getText().toString() : "";
                
                if (password.isEmpty()) {
                    passwordLayout.setError("Password required");
                    return;
                }
                
                // Update profile with email change (requires password)
                if (viewModel != null) {
                    android.util.Log.d("AccountSettings", "Calling updateUserProfileWithEmail with password");
                    viewModel.updateUserProfileWithEmail(name, email, phone, password);
                }
                dialog.dismiss();
            });
        });
        dialog.show();
    }
    
    private void updateUIWithUserData(final View view, UserModel user) {
        if (view == null || user == null) {
            showToast("Error updating UI");
            return;
        }
        
        try {
            TextInputEditText nameEditText = view.findViewById(R.id.nameEditText);
            TextInputEditText emailEditText = view.findViewById(R.id.emailEditText);
            TextInputEditText phoneEditText = view.findViewById(R.id.phoneEditText);
            
            if (nameEditText != null) {
                nameEditText.setText(user.getFullName() != null ? user.getFullName() : "");
            }
            
            if (emailEditText != null) {
                emailEditText.setText(user.getEmail() != null ? user.getEmail() : "");
            }
            
            if (phoneEditText != null) {
                phoneEditText.setText(user.getPhoneNumber() != null ? user.getPhoneNumber() : "");
            }
            
            // Load profile image if available
            if (user.getProfileImage() != null && !user.getProfileImage().isEmpty() && profileImageView != null) {
                try {
                    Uri imageUri = Uri.parse(user.getProfileImage());
                    profileImageView.setImageURI(imageUri);
                } catch (Exception e) {
                    // If image loading fails, use placeholder
                    profileImageView.setImageResource(com.example.soukify.R.drawable.ic_profile_placeholder);
                }
            } else if (profileImageView != null) {
                profileImageView.setImageResource(com.example.soukify.R.drawable.ic_profile_placeholder);
            }
        } catch (Exception e) {
            showToast("Error updating user interface: " + e.getMessage());
        }
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
