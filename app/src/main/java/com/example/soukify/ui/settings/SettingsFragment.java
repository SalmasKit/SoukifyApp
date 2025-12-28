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
import com.example.soukify.data.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {
    
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    private SettingsViewModel settingsViewModel;
    private ShopViewModel shopViewModel;
    private ChatRepository chatRepository;
    private int currentThemeIndex;
    private int buyerUnreadCount = 0;
    private int sellerUnreadCount = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        chatRepository = new ChatRepository();
        
        currentThemeIndex = getSavedThemeIndex();
        // Apply theme locally without attempting to update Firebase yet
        applyThemeLocally(currentThemeIndex);
        
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        
        displayUserInfo(view);
        observeViewModel();
        setupShopButton(view);
        
        SettingItemView messagesButton = view.findViewById(R.id.messagesButton);
        if (messagesButton != null) {
            messagesButton.setOnSettingClickListener(v -> {
                Intent intent = new Intent(requireActivity(), com.example.soukify.ui.conversations.ConversationsListActivity.class);
                // Smart navigation: If we have unread seller messages, prioritize showing those.
                // Otherwise default to buyer view (typical user Messages).
                boolean isSellerView = sellerUnreadCount > 0;
                intent.putExtra(com.example.soukify.ui.conversations.ConversationsListActivity.EXTRA_IS_SELLER_VIEW, isSellerView);
                startActivity(intent);
            });
            
            // Observe unread counts
            chatRepository.getBuyerUnreadCount().observe(getViewLifecycleOwner(), count -> {
                buyerUnreadCount = count != null ? count : 0;
                updateMessagesBadge(messagesButton);
            });

            chatRepository.getSellerUnreadCount().observe(getViewLifecycleOwner(), count -> {
                sellerUnreadCount = count != null ? count : 0;
                updateMessagesBadge(messagesButton);
            });
        }
        
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
                        FirebaseAuth.getInstance().signOut();
                        performLogout();
                    })
                    .show();
        });

        Log.d("SettingsFragment", "SettingsFragment onViewCreated completed");
    }

    private void setupShopButton(View view) {
        SettingItemView openShopButton = view.findViewById(R.id.openShopButton);
        if (openShopButton == null) return;

        openShopButton.setOnSettingClickListener(v -> {
            Log.d("SettingsFragment", "Open Shop button clicked!");
            Bundle args = new Bundle();
            args.putBoolean("hideDialogs", false); // Show dialogs from settings
            
            // Pass the current shop ID if available to speed up loading in ShopHomeFragment
            if (shopViewModel.getShop().getValue() != null) {
                args.putString("shopId", shopViewModel.getShop().getValue().getShopId());
            }
            
            Navigation.findNavController(v).navigate(R.id.action_navigation_settings_to_navigation_shop, args);
        });

        // Observe shop status to enable/disable button
        shopViewModel.getHasShop().observe(getViewLifecycleOwner(), hasShop -> {
            boolean enabled = hasShop != null && hasShop;
            Log.d("SettingsFragment", "Shop status updated: hasShop=" + enabled);
            openShopButton.setEnabled(enabled);
            
            // Optionally update title if no shop
            if (!enabled) {
                openShopButton.setAlpha(0.5f);
            } else {
                openShopButton.setAlpha(1.0f);
            }
        });
    }

    private void updateMessagesBadge(SettingItemView messagesButton) {
        if (messagesButton != null) {
            messagesButton.setBadgeCount(buyerUnreadCount + sellerUnreadCount);
        }
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
                    currentThemeIndex = which;
                    applyThemeLocally(which);
                    saveThemeIndex(which);
                    
                    // Sync with Firebase if user is logged in
                    if (settingsViewModel.isLoggedIn()) {
                        settingsViewModel.setThemeIndex(which);
                    }
                    
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }
    
    /**
     * Apply theme locally without updating Firebase
     */
    private void applyThemeLocally(int index) {
        switch (index) {
            case 1: // Light
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2: // Dark
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 0: // System
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
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
            showToast(getString(R.string.logged_out));
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
                showToast(getString(R.string.storage_permission_msg));
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
