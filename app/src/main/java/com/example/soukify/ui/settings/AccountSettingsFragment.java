package com.example.soukify.ui.settings;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.databinding.FragmentAccountSettingsBinding;
import com.example.soukify.data.models.UserModel;
import android.util.Log;

public class AccountSettingsFragment extends Fragment {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 2;

    private FragmentAccountSettingsBinding binding;
    private SettingsViewModel viewModel;
    private UserModel currentUser;
    private String originalEmail;
    private ProgressBar loadingIndicator;
    private Uri selectedImageUri;
    private boolean isUploadingImage = false;
    private boolean isSaving = false;

    // Store pending values during save
    private String pendingName = null;
    private String pendingPhone = null;
    private String pendingEmail = null;

    // ✅ NEW: Track if we're waiting for image upload
    private boolean waitingForImageUpload = false;
    private String pendingNameAfterImage = null;
    private String pendingPhoneAfterImage = null;
    private String pendingEmailAfterImage = null;
    private String pendingPasswordAfterImage = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeToolbar(view);
        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        setupObservers();
        setupClickListeners();
        setupLoadingIndicator();
    }

    private void setupLoadingIndicator() {
        loadingIndicator = new ProgressBar(requireContext());
        loadingIndicator.setVisibility(View.GONE);
    }

    private void initializeToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                } else {
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
        }
    }

    private void setupObservers() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user == null) return;

            currentUser = user;
            originalEmail = user.getEmail();

            // Clear errors
            binding.nameLayout.setError(null);
            binding.emailLayout.setError(null);
            binding.phoneLayout.setError(null);

            // Only update fields if we're NOT in the middle of saving
            if (!isSaving && !isUploadingImage && !waitingForImageUpload) {
                String currentNameInField = binding.nameEditText.getText().toString();
                if (!currentNameInField.equals(user.getFullName()) && pendingName == null) {
                    binding.nameEditText.setText(user.getFullName());
                }

                String currentEmailInField = binding.emailEditText.getText().toString();
                if (!currentEmailInField.equals(user.getEmail()) && pendingEmail == null) {
                    binding.emailEditText.setText(user.getEmail());
                }

                String currentPhoneInField = binding.phoneEditText.getText().toString();
                if (!currentPhoneInField.equals(user.getPhoneNumber()) && pendingPhone == null) {
                    binding.phoneEditText.setText(user.getPhoneNumber());
                }
            }

            // ✅ FIX: Handle image upload completion
            if (isUploadingImage && user.getProfileImage() != null &&
                !user.getProfileImage().isEmpty()) {

                Log.d("AccountSettings", "Image upload complete, new URL: " + user.getProfileImage());

                // Reset image upload state
                isUploadingImage = false;
                binding.changePhotoButton.setEnabled(true);
                binding.changePhotoButton.setText("Change Photo");

                // Load the new image
                loadProfileImage(user.getProfileImage());
                selectedImageUri = null;

                // ✅ FIX: If we were waiting to update other fields, do it now
                if (waitingForImageUpload) {
                    Log.d("AccountSettings", "Image upload done, now updating profile fields");
                    waitingForImageUpload = false;

                    // Now update the profile with name, phone, email
                    if (pendingEmailAfterImage != null &&
                        !pendingEmailAfterImage.equals(originalEmail)) {
                        viewModel.updateUserProfile(
                            pendingNameAfterImage,
                            pendingEmailAfterImage,
                            pendingPhoneAfterImage,
                            pendingPasswordAfterImage
                        );
                    } else {
                        viewModel.updateUserProfile(
                            pendingNameAfterImage,
                            pendingEmailAfterImage,
                            pendingPhoneAfterImage,
                            null
                        );
                    }

                    // Clear pending values after image
                    pendingNameAfterImage = null;
                    pendingPhoneAfterImage = null;
                    pendingEmailAfterImage = null;
                    pendingPasswordAfterImage = null;
                }
            } else if (!isUploadingImage && selectedImageUri == null) {
                // Normal image load
                loadProfileImage(user.getProfileImage());
            }
        });

        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.startsWith("SUCCESS:")) {
                    String message = result.substring(8);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();

                    // ✅ FIX: Only clear pending values if NOT uploading image
                    if (!isUploadingImage && !waitingForImageUpload) {
                        clearPendingValues();
                    }
                } else {
                    Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();

                    // On error, keep user's input
                    if (pendingName != null) binding.nameEditText.setText(pendingName);
                    if (pendingPhone != null) binding.phoneEditText.setText(pendingPhone);
                    if (pendingEmail != null) binding.emailEditText.setText(pendingEmail);

                    clearPendingValues();
                }
                viewModel.clearOperationResult();
            }
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading) {
                showLoading();
            } else {
                hideLoading();

                // Reset save state only if not uploading image
                if (!isUploadingImage && !waitingForImageUpload) {
                    isSaving = false;
                }
            }
        });
    }

    private void clearPendingValues() {
        pendingName = null;
        pendingPhone = null;
        pendingEmail = null;
        isSaving = false;
    }

    private void loadProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.profileImage);
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void setupClickListeners() {
        binding.saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveProfile();
            }
        });

        binding.changePasswordButton.setOnClickListener(v -> {
            showChangePasswordDialog();
        });

        binding.changePhotoButton.setOnClickListener(v -> {
            checkPermissionsAndOpenGallery();
        });
    }

    private boolean validateForm() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();

        boolean isValid = true;

        if (name.isEmpty()) {
            binding.nameLayout.setError("Name is required");
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }

        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Please enter a valid email");
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }

        if (phone.isEmpty()) {
            binding.phoneLayout.setError("Phone number is required");
            isValid = false;
        } else if (phone.length() < 8) {
            binding.phoneLayout.setError("Please enter a valid phone number");
            isValid = false;
        } else {
            binding.phoneLayout.setError(null);
        }

        return isValid;
    }

    private void saveProfile() {
        String name = binding.nameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String phone = binding.phoneEditText.getText().toString().trim();

        // Check if anything changed
        boolean hasChanges = !name.equals(currentUser.getFullName()) ||
                            !email.equals(currentUser.getEmail()) ||
                            !phone.equals(currentUser.getPhoneNumber()) ||
                            selectedImageUri != null;

        if (!hasChanges) {
            Toast.makeText(getContext(), "No changes to save", Toast.LENGTH_SHORT).show();
            return;
        }

        showSaveConfirmationDialog(name, email, phone);
    }

    private void showSaveConfirmationDialog(String name, String email, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirm Changes");

        StringBuilder message = new StringBuilder("Are you sure you want to save these changes?\n\n");

        if (!name.equals(currentUser.getFullName())) {
            message.append("• Name: ").append(currentUser.getFullName()).append(" → ").append(name).append("\n");
        }
        if (!email.equals(currentUser.getEmail())) {
            message.append("• Email: ").append(currentUser.getEmail()).append(" → ").append(email).append("\n");
        }
        if (!phone.equals(currentUser.getPhoneNumber())) {
            message.append("• Phone: ").append(currentUser.getPhoneNumber()).append(" → ").append(phone).append("\n");
        }
        if (selectedImageUri != null) {
            message.append("• Profile photo: New photo selected\n");
        }

        builder.setMessage(message.toString());

        builder.setPositiveButton("Save Changes", (dialog, which) -> {
            if (!email.equals(originalEmail)) {
                showEmailChangeDialog(name, email, phone);
            } else {
                applyProfileChanges(name, email, phone);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            resetFieldsToOriginal();
        });

        builder.show();
    }

    private void showEmailChangeDialog(String name, String email, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Email Change Verification");
        builder.setMessage("Changing your email requires verification. Please enter your current password to continue.");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        builder.setView(view);

        builder.setPositiveButton("Verify", (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), "Password is required", Toast.LENGTH_SHORT).show();
            } else {
                applyProfileChangesWithEmail(name, email, phone, password);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> {
            binding.emailEditText.setText(originalEmail);
        });

        builder.show();
    }

    /**
     * ✅ FIXED: Proper sequence - Image FIRST, then profile
     */
    private void applyProfileChanges(String name, String email, String phone) {
        if (isSaving) return;

        // Store pending values
        pendingName = name;
        pendingPhone = phone;
        pendingEmail = email;

        isSaving = true;
        showLoading();

        // ✅ FIX: If image selected, upload it FIRST
        if (selectedImageUri != null) {
            Log.d("AccountSettings", "Starting image upload first");

            waitingForImageUpload = true;
            isUploadingImage = true;

            // Store values to apply AFTER image upload completes
            pendingNameAfterImage = name;
            pendingPhoneAfterImage = phone;
            pendingEmailAfterImage = email;
            pendingPasswordAfterImage = null;

            binding.changePhotoButton.setText("Uploading...");
            binding.changePhotoButton.setEnabled(false);

            // Upload image - the observer will detect completion and continue
            viewModel.updateProfileImage(selectedImageUri.toString());
        } else {
            // No image, just update profile directly
            Log.d("AccountSettings", "No image, updating profile directly");
            viewModel.updateUserProfile(name, email, phone, null);
        }
    }

    /**
     * ✅ FIXED: Proper sequence with email change
     */
    private void applyProfileChangesWithEmail(String name, String email, String phone, String password) {
        if (isSaving) return;

        // Store pending values
        pendingName = name;
        pendingPhone = phone;
        pendingEmail = email;

        isSaving = true;
        showLoading();

        // ✅ FIX: If image selected, upload it FIRST
        if (selectedImageUri != null) {
            Log.d("AccountSettings", "Starting image upload first (with email change)");

            waitingForImageUpload = true;
            isUploadingImage = true;

            // Store values to apply AFTER image upload completes
            pendingNameAfterImage = name;
            pendingPhoneAfterImage = phone;
            pendingEmailAfterImage = email;
            pendingPasswordAfterImage = password;

            binding.changePhotoButton.setText("Uploading...");
            binding.changePhotoButton.setEnabled(false);

            // Upload image - the observer will detect completion and continue
            viewModel.updateProfileImage(selectedImageUri.toString());
        } else {
            // No image, just update profile with email
            Log.d("AccountSettings", "No image, updating profile with email directly");
            viewModel.updateUserProfile(name, email, phone, password);
        }
    }

    private void resetFieldsToOriginal() {
        if (currentUser != null) {
            binding.nameEditText.setText(currentUser.getFullName());
            binding.emailEditText.setText(currentUser.getEmail());
            binding.phoneEditText.setText(currentUser.getPhoneNumber());
            loadProfileImage(currentUser.getProfileImage());

            selectedImageUri = null;
            binding.changePhotoButton.setText("Change Photo");
            clearPendingValues();
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Password");

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = view.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordInput = view.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordEditText);

        builder.setView(view);

        builder.setPositiveButton("Change Password", null);
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set click listener after dialog is shown to prevent auto-dismiss
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPassword = currentPasswordInput.getText().toString().trim();
            String newPassword = newPasswordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Clear previous errors
            if (view.findViewById(R.id.currentPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.currentPasswordLayout)).setError(null);
            }
            if (view.findViewById(R.id.newPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.newPasswordLayout)).setError(null);
            }
            if (view.findViewById(R.id.confirmPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.confirmPasswordLayout)).setError(null);
            }

            boolean hasError = false;

            if (currentPassword.isEmpty()) {
                if (view.findViewById(R.id.currentPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.currentPasswordLayout)).setError(getString(R.string.current_password_required));
                }
                hasError = true;
            }

            if (newPassword.isEmpty()) {
                if (view.findViewById(R.id.newPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.newPasswordLayout)).setError(getString(R.string.new_password_required));
                }
                hasError = true;
            } else if (newPassword.length() < 6) {
                if (view.findViewById(R.id.newPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.newPasswordLayout)).setError(getString(R.string.password_too_short));
                }
                hasError = true;
            } else if (newPassword.equals(currentPassword)) {
                if (view.findViewById(R.id.newPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.newPasswordLayout)).setError(getString(R.string.new_password_different));
                }
                hasError = true;
            }

            if (confirmPassword.isEmpty()) {
                if (view.findViewById(R.id.confirmPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.confirmPasswordLayout)).setError(getString(R.string.confirm_new_password));
                }
                hasError = true;
            } else if (!newPassword.equals(confirmPassword)) {
                if (view.findViewById(R.id.confirmPasswordLayout) instanceof com.google.android.material.textfield.TextInputLayout) {
                    ((com.google.android.material.textfield.TextInputLayout) view.findViewById(R.id.confirmPasswordLayout)).setError(getString(R.string.passwords_do_not_match));
                }
                hasError = true;
            }

            if (!hasError) {
                // Show loading state
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Updating...");
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                
                // Update password
                viewModel.updateUserPassword(currentPassword, newPassword);
                
                // Observe the result and handle dialog dismissal
                viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
                    if (result != null && !result.startsWith("SUCCESS:")) {
                        // Error occurred - re-enable dialog buttons
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("Change Password");
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                    } else if (result != null && result.startsWith("SUCCESS:")) {
                        // Success - dismiss dialog
                        dialog.dismiss();
                    }
                });
                
                // Close dialog after a delay to allow user to see any success/error messages
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).postDelayed(() -> {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                    }
                }, 3000);
            }
        });
    }

    private void checkPermissionsAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
        } else {
            openGallery();
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(getContext(), "Permission denied to access gallery", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == getActivity().RESULT_OK && data != null) {
            Uri selectedUri = data.getData();
            if (selectedUri != null) {
                selectedImageUri = selectedUri;

                // Preview the image
                Glide.with(this)
                    .load(selectedUri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(binding.profileImage);

                binding.changePhotoButton.setText("Photo Selected");
            }
        }
    }

    private void showLoading() {
        binding.saveButton.setEnabled(false);
        binding.changePasswordButton.setEnabled(false);
        if (!isUploadingImage) {
            binding.changePhotoButton.setEnabled(false);
        }
        binding.saveButton.setText("Saving...");
    }

    private void hideLoading() {
        binding.saveButton.setEnabled(true);
        binding.changePasswordButton.setEnabled(true);
        if (!isUploadingImage) {
            binding.changePhotoButton.setEnabled(true);
            binding.changePhotoButton.setText("Change Photo");
        }
        binding.saveButton.setText("Save Changes");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        loadingIndicator = null;
    }
}

