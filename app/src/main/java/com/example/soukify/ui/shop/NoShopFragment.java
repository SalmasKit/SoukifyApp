package com.example.soukify.ui.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.soukify.R;

/**
 * Simple placeholder fragment shown when user doesn't have a shop.
 * Displays "no shop" screen with a create shop button that delegates
 * to ShopFragment for the actual creation dialog.
 */
public class NoShopFragment extends Fragment {
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_no_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupClickListener();
    }

    private void setupClickListener() {
        Button createShopButton = getView().findViewById(R.id.createShopButton);
        Toolbar toolbar = getView().findViewById(R.id.toolbar);
        
        if (createShopButton != null) {
            createShopButton.setOnClickListener(v -> {
                android.util.Log.d("NoShopFragment", "Create Shop button clicked");
                
                // Find or create ShopFragment and call its dialog method
                Fragment parentFragment = getParentFragment();
                if (parentFragment instanceof ShopFragment) {
                    ((ShopFragment) parentFragment).showCreateShopDialog();
                } else {
                    android.util.Log.e("NoShopFragment", "Parent fragment is not ShopFragment!");
                }
            });
        } else {
            android.util.Log.e("NoShopFragment", "Create Shop button is null!");
        }
        
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                android.util.Log.d("NoShopFragment", "Toolbar back button clicked - navigating to settings");
                try {
                    // Navigate to settings using the main navigation controller
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main)
                        .navigate(R.id.navigation_settings);
                } catch (Exception e) {
                    android.util.Log.e("NoShopFragment", "Navigation failed: " + e.getMessage());
                    // Fallback: try alternative navigation method
                    try {
                        requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment_activity_main, new com.example.soukify.ui.settings.SettingsFragment())
                            .commit();
                    } catch (Exception ex) {
                        android.util.Log.e("NoShopFragment", "Fallback navigation also failed: " + ex.getMessage());
                    }
                }
            });
        } else {
            android.util.Log.e("NoShopFragment", "Toolbar is null!");
        }
    }
}