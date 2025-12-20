package com.example.soukify.ui.settings;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.soukify.R;
import com.example.soukify.ui.settings.SettingItemView;
import com.example.soukify.ui.shop.ShopViewModel;
import de.hdodenhof.circleimageview.CircleImageView;
import com.bumptech.glide.Glide;

public class SettingsFragment extends Fragment {
    
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    private SettingsViewModel settingsViewModel;
    private ShopViewModel shopViewModel;
    private int currentThemeIndex;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        
        currentThemeIndex = getSavedThemeIndex();
        settingsViewModel.setThemeIndex(currentThemeIndex);
        
        displayUserInfo(view);
        observeViewModel();
        
        SettingItemView accountSettings = view.findViewById(R.id.accountSettings);
        accountSettings.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_account_settings));

        SettingItemView notifications = view.findViewById(R.id.notifications);
        notifications.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_notifications));

        SettingItemView languageCurrency = view.findViewById(R.id.languageCurrency);
        languageCurrency.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_language_currency));

        SettingItemView themeAppearance = view.findViewById(R.id.themeAppearance);
        themeAppearance.setOnSettingClickListener(v -> showThemeDialog());

        SettingItemView privacy = view.findViewById(R.id.privacy);
        privacy.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_privacy));

        SettingItemView helpSupport = view.findViewById(R.id.helpSupport);
        helpSupport.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_help_support));

        SettingItemView about = view.findViewById(R.id.about);
        about.setOnSettingClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_about));

        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.logout_confirm_title)
                    .setMessage(R.string.logout_confirm_message)
                    .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                    .setPositiveButton(R.string.logout, (d, w) -> {
                        settingsViewModel.logout();
                        performLogout();
                    })
                    .show();
        });

        SettingItemView openShopButton = view.findViewById(R.id.openShopButton);
        openShopButton.setEnabled(false);
        openShopButton.setOnSettingClickListener(v -> {
            Log.d("SettingsFragment", "Open Shop button clicked!");
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_shop);
        });
        
        Log.d("SettingsFragment", "SettingsFragment onViewCreated completed");
    }

    private void showThemeDialog() {
        final String[] items = new String[]{
                getString(R.string.theme_system_default),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.appearance)
                .setSingleChoiceItems(items, currentThemeIndex, (dialog, which) -> {
                    settingsViewModel.setThemeIndex(which);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private int getSavedThemeIndex() {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        return prefs.getInt("theme_index", 0);
    }

    private void saveThemeIndex(int index) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE);
        prefs.edit().putInt("theme_index", index).apply();
    }

    private void displayUserInfo(View view) {
        TextView userNameTextView = view.findViewById(R.id.userName);
        TextView userEmailTextView = view.findViewById(R.id.userEmail);
        CircleImageView profileImageView = view.findViewById(R.id.profileImage);
        
        if (settingsViewModel.isLoggedIn()) {
            // Set initial values from SessionRepository
            String userName = settingsViewModel.getUserName();
            String userEmail = settingsViewModel.getUserEmail();
            
            userNameTextView.setText(userName != null && !userName.isEmpty() ? userName : "User");
            userEmailTextView.setText(userEmail != null && !userEmail.isEmpty() ? userEmail : "");
            
            // Observe currentUser LiveData for real-time updates
            settingsViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
                Log.d("SettingsFragment", "User observer called: " + (user != null ? user.getFullName() : "null"));
                if (user != null) {
                    // Update UI with latest user data from Firebase
                    userNameTextView.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : "User");
                    userEmailTextView.setText(user.getEmail() != null && !user.getEmail().isEmpty() ? user.getEmail() : "");
                    
                    // Load profile image if available
                    if (user.getProfileImage() != null && !user.getProfileImage().isEmpty()) {
                        loadProfileImage(user.getProfileImage(), profileImageView);
                    } else {
                        Log.d("SettingsFragment", "No profile image found, using placeholder");
                        profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                    }
                } else {
                    // Fallback to SessionRepository if no user data
                    userNameTextView.setText(settingsViewModel.getUserName() != null && !settingsViewModel.getUserName().isEmpty() ? settingsViewModel.getUserName() : "User");
                    userEmailTextView.setText(settingsViewModel.getUserEmail() != null && !settingsViewModel.getUserEmail().isEmpty() ? settingsViewModel.getUserEmail() : "");
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                }
            });
        } else {
            userNameTextView.setText("");
            userEmailTextView.setText("");
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    private void observeViewModel() {
        settingsViewModel.getThemeIndex().observe(getViewLifecycleOwner(), themeIndex -> {
            currentThemeIndex = themeIndex;
            saveThemeIndex(themeIndex);
        });
        
        settingsViewModel.getCurrentUserId().observe(getViewLifecycleOwner(), userId -> {
            if (getView() != null) {
                displayUserInfo(getView());
            }
        });
    }
    
    private void performLogout() {
        try {
            Intent intent = new Intent(requireContext(), com.example.soukify.ui.login.LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        } catch (Exception e) {
            showToast("Logged out");
            requireActivity().finishAffinity();
        }
    }

    private void loadProfileImage(String imageUri, CircleImageView profileImageView) {
        try {
            Log.d("SettingsFragment", "Loading profile image: " + imageUri);
            
            if (imageUri == null || imageUri.isEmpty()) {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                return;
            }
            
            android.net.Uri uri = android.net.Uri.parse(imageUri);
            
            // Check if it's a Cloudinary URL (http/https) or local file
            if (uri.getScheme() != null && (uri.getScheme().equals("http") || uri.getScheme().equals("https"))) {
                // Load Cloudinary URL with Glide
                Glide.with(this)
                    .load(imageUri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(profileImageView);
            } else if (uri.getScheme().equals("file")) {
                profileImageView.setImageURI(uri);
            } else if (uri.getScheme().equals("content")) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                        == PackageManager.PERMISSION_GRANTED) {
                    profileImageView.setImageURI(uri);
                } else {
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                            REQUEST_READ_EXTERNAL_STORAGE);
                    profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
                }
            } else {
                profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
            }
        } catch (Exception e) {
            Log.e("SettingsFragment", "Error loading profile image", e);
            profileImageView.setImageResource(R.drawable.ic_profile_placeholder);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (getView() != null) {
                    displayUserInfo(getView());
                }
            } else {
                showToast("Storage permission required to load profile images");
            }
        }
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (settingsViewModel != null) {
            settingsViewModel.refreshCurrentUser();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        settingsViewModel = null;
    }
}
