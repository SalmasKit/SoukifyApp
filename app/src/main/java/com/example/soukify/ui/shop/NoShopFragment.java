package com.example.soukify.ui.shop;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
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
    }
}