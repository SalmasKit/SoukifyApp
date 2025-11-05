package com.example.soukify.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.appcompat.app.AppCompatDelegate;
import com.example.soukify.R;
import com.example.soukify.databinding.FragmentSettingsBinding;

/**
 * Fragment that displays the app settings and handles user interactions with setting items.
 */
public class SettingsFragment extends Fragment {
    // View binding instance
    private FragmentSettingsBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment using View Binding
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    private void showThemeDialog() {
        final String[] items = new String[]{
                getString(R.string.theme_system_default),
                getString(R.string.theme_light),
                getString(R.string.theme_dark)
        };

        int checked = getSavedThemeIndex();

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.appearance)
                .setSingleChoiceItems(items, checked, (dialog, which) -> {
                    applyTheme(which);
                    saveThemeIndex(which);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                .show();
    }

    private void applyTheme(int index) {
        switch (index) {
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 0:
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

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Set up click listeners for each setting item
        binding.accountSettings.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_account_settings);
            }
        });

        binding.notifications.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_notifications);
            }
        });

        binding.languageCurrency.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_language_currency);
            }
        });

        // Appearance (Theme)
        binding.themeAppearance.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showThemeDialog();
            }
        });

        binding.privacy.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_privacy);
            }
        });

        binding.helpSupport.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_help_support);
            }
        });

        binding.about.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_navigation_settings_to_navigation_about);
            }
        });

        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.logout_confirm_title)
                        .setMessage(R.string.logout_confirm_message)
                        .setNegativeButton(R.string.cancel, (d, w) -> d.dismiss())
                        .setPositiveButton(R.string.logout, (d, w) -> performLogout())
                        .show();
            }
        });
    }

    /**
     * Helper method to show toast messages
     * @param message The message to display in the toast
     */
    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    private void performLogout() {
        // TODO: Clear auth/session state if applicable (e.g., FirebaseAuth.getInstance().signOut())
        try {
            // Clear app task and restart launcher activity
            android.content.Intent launchIntent = requireContext().getPackageManager()
                    .getLaunchIntentForPackage(requireContext().getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(launchIntent);
            } else {
                requireActivity().finishAffinity();
            }
        } catch (Exception e) {
            showToast("Logged out");
            requireActivity().finishAffinity();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear the binding when the view is destroyed to prevent memory leaks
        binding = null;
    }
}