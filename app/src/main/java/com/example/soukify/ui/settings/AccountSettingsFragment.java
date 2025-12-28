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

            // Check if image URL changed or if it's the first load
            String oldImageUrl = (currentUser != null) ? currentUser.getProfileImage() : null;
            String newImageUrl = user.getProfileImage();
            
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

            // ✅ FIX: Load image if URL changed or first load
            boolean imageChanged = (oldImageUrl == null && newImageUrl != null) || 
                                   (oldImageUrl != null && !oldImageUrl.equals(newImageUrl));
            
            if (isUploadingImage && newImageUrl != null && !newImageUrl.isEmpty()) {
                 // Upload flow handling (kept same)
                 Log.d("AccountSettings", "Image upload complete, new URL: " + newImageUrl);
                 isUploadingImage = false;
                 binding.changePhotoButton.setEnabled(true);
                 binding.changePhotoButton.setText(R.string.change_photo_btn);
                 loadProfileImage(newImageUrl);
                 selectedImageUri = null;
                 
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
                 if (imageChanged) {
                     loadProfileImage(newImageUrl);
                 }
            }
        });

        viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
            if (result != null) {
                if (result.startsWith("SUCCESS:")) {
                    String message = result.substring(8);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show(); // Note: message is from result substring, might need check if it needs translation or if it is already translated from ViewModel

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
            binding.nameLayout.setError(getString(R.string.name_required_error));
            isValid = false;
        } else {
            binding.nameLayout.setError(null);
        }

        if (email.isEmpty()) {
            binding.emailLayout.setError(getString(R.string.email_required_error));
            isValid = false;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.email_invalid_error));
            isValid = false;
        } else {
            binding.emailLayout.setError(null);
        }

        if (!phone.isEmpty() && phone.length() < 8) {
            binding.phoneLayout.setError(getString(R.string.phone_invalid_error));
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
            Toast.makeText(getContext(), R.string.no_changes_to_save, Toast.LENGTH_SHORT).show();
            return;
        }

        showSaveConfirmationDialog(name, email, phone);
    }

    private void showSaveConfirmationDialog(String name, String email, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.confirm_changes_title);

        StringBuilder message = new StringBuilder(getString(R.string.confirm_changes_message));

        if (!name.equals(currentUser.getFullName())) {
            message.append(getString(R.string.change_summary_name)).append(currentUser.getFullName()).append(" → ").append(name).append("\n");
        }
        if (!email.equals(currentUser.getEmail())) {
            message.append(getString(R.string.change_summary_email)).append(currentUser.getEmail()).append(" → ").append(email).append("\n");
        }
        if (!phone.equals(currentUser.getPhoneNumber())) {
            message.append(getString(R.string.change_summary_phone)).append(currentUser.getPhoneNumber()).append(" → ").append(phone).append("\n");
        }
        if (selectedImageUri != null) {
            message.append(getString(R.string.change_summary_photo));
        }

        builder.setMessage(message.toString());

        builder.setPositiveButton(R.string.save_changes, (dialog, which) -> {
            if (!email.equals(originalEmail)) {
                showEmailChangeDialog(name, email, phone);
            } else {
                applyProfileChanges(name, email, phone);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
            resetFieldsToOriginal();
        });

        builder.show();
    }

    private void showEmailChangeDialog(String name, String email, String phone) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.email_change_verification_title);
        builder.setMessage(R.string.email_change_verification_message);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_password_input, null);
        EditText passwordInput = view.findViewById(R.id.passwordInput);
        builder.setView(view);

        builder.setPositiveButton(R.string.verify_btn, (dialog, which) -> {
            String password = passwordInput.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(getContext(), R.string.password_required_error, Toast.LENGTH_SHORT).show();
            } else {
                applyProfileChangesWithEmail(name, email, phone, password);
            }
        });

        builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
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

            binding.changePhotoButton.setText(R.string.uploading_status);
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

            binding.changePhotoButton.setText(R.string.uploading_status);
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
            binding.changePhotoButton.setText(R.string.change_photo_btn);
            clearPendingValues();
        }
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.change_password_title);

        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_change_password, null);
        EditText currentPasswordInput = view.findViewById(R.id.currentPasswordEditText);
        EditText newPasswordInput = view.findViewById(R.id.newPasswordEditText);
        EditText confirmPasswordInput = view.findViewById(R.id.confirmPasswordEditText);

        builder.setView(view);

        builder.setPositiveButton(R.string.change_password_btn, null);
        builder.setNegativeButton(R.string.cancel, null);

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
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.updating_status);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                
                // Update password
                viewModel.updateUserPassword(currentPassword, newPassword);
                
                // Observe the result and handle dialog dismissal
                viewModel.getOperationResult().observe(getViewLifecycleOwner(), result -> {
                    if (result != null && !result.startsWith("SUCCESS:")) {
                        // Error occurred - re-enable dialog buttons
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(R.string.change_password_btn);
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
                Toast.makeText(getContext(), R.string.permission_denied_gallery, Toast.LENGTH_SHORT).show();
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

                binding.changePhotoButton.setText(R.string.photo_selected);
            }
        }
    }

    private void showLoading() {
        binding.saveButton.setEnabled(false);
        binding.changePasswordButton.setEnabled(false);
        if (!isUploadingImage) {
            binding.changePhotoButton.setEnabled(false);
        }
        binding.saveButton.setText(R.string.saving_status);
    }

    private void hideLoading() {
        binding.saveButton.setEnabled(true);
        binding.changePasswordButton.setEnabled(true);
        if (!isUploadingImage) {
            binding.changePhotoButton.setEnabled(true);
            binding.changePhotoButton.setText(R.string.change_photo_btn);
        }
        binding.saveButton.setText(R.string.save_changes);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        loadingIndicator = null;
    }
}

