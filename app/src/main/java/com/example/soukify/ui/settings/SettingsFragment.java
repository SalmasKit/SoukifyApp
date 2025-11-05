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
                showToast("Notifications");
                // TODO: Navigate to notifications screen
            }
        });

        binding.languageCurrency.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Language & Currency");
                // TODO: Navigate to language & currency screen
            }
        });

        binding.privacy.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Privacy");
                // TODO: Navigate to privacy screen
            }
        });

        binding.helpSupport.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Help & Support");
                // TODO: Navigate to help & support screen
            }
        });

        binding.about.setOnSettingClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("About");
                // TODO: Navigate to about screen
            }
        });

        binding.logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToast("Logging out...");
                // TODO: Implement logout logic
                // Example:
                // FirebaseAuth.getInstance().signOut();
                // Navigation.findNavController(v).navigate(R.id.action_global_login);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear the binding when the view is destroyed to prevent memory leaks
        binding = null;
    }
}