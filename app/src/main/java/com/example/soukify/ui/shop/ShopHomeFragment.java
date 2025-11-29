package com.example.soukify.ui.shop;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.repositories.LocationRepository;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShopHomeFragment extends Fragment {
    
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;
    
    private ShopViewModel shopViewModel;
    private LocationRepository locationRepository;
    private View rootView;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        android.util.Log.d("ShopHomeFragment", "onCreateView called");
        rootView = inflater.inflate(R.layout.fragment_shop_home, container, false);
        android.util.Log.d("ShopHomeFragment", "Layout inflated successfully");
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        android.util.Log.d("ShopHomeFragment", "onViewCreated called");
        
        // Initialize ViewModel using activity scope to share with ShopFragment and NoShopFragment
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        android.util.Log.d("ShopHomeFragment", "ViewModel initialized with activity scope");
        
        // Initialize LocationRepository
        locationRepository = new LocationRepository(requireActivity().getApplication());
        
        // Observe LiveData
        observeViewModel();
        
        // Set up click listeners
        setupClickListeners();
        
        android.util.Log.d("ShopHomeFragment", "onViewCompleted completed");
    }

    private void observeViewModel() {
        android.util.Log.d("ShopHomeFragment", "observeViewModel called");
        
        // Observe shop data
        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopHomeFragment", "Shop data received: " + (shop != null ? "EXISTS" : "NULL"));
            if (shop != null) {
                android.util.Log.d("ShopHomeFragment", "Shop name: " + shop.getName());
                android.util.Log.d("ShopHomeFragment", "Shop ID: " + shop.getShopId());
                updateShopUI(shop);
            } else {
                android.util.Log.d("ShopHomeFragment", "Shop data is null, UI not updated");
            }
        });
        
        // Also check current value immediately
        ShopModel currentShop = shopViewModel.getShop().getValue();
        android.util.Log.d("ShopHomeFragment", "Current shop value on init: " + (currentShop != null ? "EXISTS" : "NULL"));
        if (currentShop != null) {
            android.util.Log.d("ShopHomeFragment", "Updating UI immediately with current shop");
            updateShopUI(currentShop);
        }
        
        // Observe error messages
        shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupClickListeners() {
        // Set up clear shops button
        Button clearShopsButton = rootView.findViewById(R.id.clearShopsButton);
        if (clearShopsButton != null) {
            clearShopsButton.setOnClickListener(v -> {
                // Show confirmation dialog before clearing
                new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear Shop")
                    .setMessage("Are you sure you want to delete your shop? This action cannot be undone.")
                    .setPositiveButton("Clear Shop", (dialog, which) -> {
                        shopViewModel.clearAllShops();
                        Toast.makeText(requireContext(), getString(R.string.shop_cleared_database), Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
        
        // Set up image container click listener for shop banner
        LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);
        ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);
        
        if (imageContainer != null) {
            imageContainer.setOnClickListener(v -> {
                // Show dialog to add/edit shop banner image
                showShopImageEditDialog(false); // false = adding new image
            });
        }
        
        if (shopBannerImage != null) {
            shopBannerImage.setOnClickListener(v -> {
                // Show dialog to edit existing shop banner image
                showShopImageEditDialog(true); // true = editing existing image
            });
        }
    }

    private void updateShopUI(com.example.soukify.data.models.ShopModel shop) {
        android.util.Log.d("ShopHomeFragment", "updateShopUI called with shop: " + (shop != null ? shop.getName() : "NULL"));
        
        if (rootView != null) {
            android.util.Log.d("ShopHomeFragment", "RootView exists, updating UI fields");
            
            // Update shop name
            TextView shopName = rootView.findViewById(R.id.shopName);
            if (shopName != null) {
                shopName.setText(shop.getName());
                android.util.Log.d("ShopHomeFragment", "Shop name updated: " + shop.getName());
            } else {
                android.util.Log.e("ShopHomeFragment", "shopName TextView is null");
            }
            
            // Update shop location
            TextView shopLocation = rootView.findViewById(R.id.shopLocation);
            if (shopLocation != null) {
                String location = shop.getLocation();
                if (location != null && !location.isEmpty()) {
                    shopLocation.setText(location);
                } else {
                    // Safely get city and region names using IDs with null checks
                    try {
                        if (shop.getCityId() != null && shop.getRegionId() != null && 
                            !shop.getCityId().isEmpty() && !shop.getRegionId().isEmpty()) {
                            try {
                                int cityId = Integer.parseInt(shop.getCityId());
                                int regionId = Integer.parseInt(shop.getRegionId());
                                setLocationTextFromIds(shopLocation, cityId, regionId);
                            } catch (NumberFormatException e) {
                                shopLocation.setText(getString(R.string.location_ids_invalid));
                            }
                        } else {
                            shopLocation.setText(getString(R.string.location_not_specified));
                        }
                    } catch (Exception e) {
                        shopLocation.setText(getString(R.string.location_unavailable));
                    }
                }
            }
            
            // Update shop description
            TextView shopDescription = rootView.findViewById(R.id.shopDescription);
            if (shopDescription != null) {
                String description = shop.getCategory(); // Using category as description since no description field exists
                if (description != null && !description.isEmpty()) {
                    shopDescription.setText(description);
                    shopDescription.setVisibility(View.VISIBLE);
                } else {
                    shopDescription.setVisibility(View.GONE);
                }
            }
            
            // Update shop contact information
            TextView shopContact = rootView.findViewById(R.id.shopContact);
            if (shopContact != null) {
                StringBuilder contactInfo = new StringBuilder();
                if (shop.getPhone() != null && !shop.getPhone().isEmpty()) {
                    contactInfo.append("📞 ").append(shop.getPhone()).append("\n");
                }
                if (shop.getEmail() != null && !shop.getEmail().isEmpty()) {
                    contactInfo.append("✉️ ").append(shop.getEmail());
                }
                
                if (contactInfo.length() > 0) {
                    shopContact.setText(contactInfo.toString());
                    shopContact.setVisibility(View.VISIBLE);
                } else {
                    shopContact.setVisibility(View.GONE);
                }
            }
            
            // Update shop creation date
            TextView shopCreatedAt = rootView.findViewById(R.id.shopCreatedAt);
            android.util.Log.d("ShopHomeFragment", "shopCreatedAt TextView found: " + (shopCreatedAt != null));
            if (shopCreatedAt != null) {
                try {
                    String createdAtString = shop.getCreatedAt();
                    android.util.Log.d("ShopHomeFragment", "Created at string: " + createdAtString);
                    
                    // Check if createdAt string is valid
                    if (createdAtString == null || createdAtString.isEmpty()) {
                        android.util.Log.w("ShopHomeFragment", "Invalid createdAt string: " + createdAtString);
                        shopCreatedAt.setText(getString(R.string.created_just_now));
                        shopCreatedAt.setVisibility(View.VISIBLE);
                        return;
                    }
                    
                    // createdAt is already formatted as "dd/MM/yyyy HH:mm"
                    shopCreatedAt.setText("Created: " + createdAtString);
                    android.util.Log.d("ShopHomeFragment", "Displayed date: " + createdAtString);
                    shopCreatedAt.setVisibility(View.VISIBLE);
                    android.util.Log.d("ShopHomeFragment", "Final shopCreatedAt text: " + shopCreatedAt.getText().toString());
                } catch (Exception e) {
                    android.util.Log.e("ShopHomeFragment", "Error formatting date: " + e.getMessage(), e);
                    // Fallback: show current time as placeholder
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    String now = dateFormat.format(new Date());
                    shopCreatedAt.setText("Created: " + now);
                    shopCreatedAt.setVisibility(View.VISIBLE);
                }
            } else {
                android.util.Log.e("ShopHomeFragment", "shopCreatedAt TextView is null!");
            }
            
            // Update shop banner image if available
            ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);
            LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);
            if (shopBannerImage != null && imageContainer != null) {
                if (shop.getImageUrl() != null && !shop.getImageUrl().isEmpty()) {
                    try {
                        // Load image from URI stored in database
                        Uri imageUri = Uri.parse(shop.getImageUrl());
                        
                        // Check if this is an internal storage URI (file://) or external storage URI (content://)
                        if (imageUri.getScheme().equals("file")) {
                            // Internal storage - no permission needed
                            shopBannerImage.setImageURI(imageUri);
                            shopBannerImage.setVisibility(View.VISIBLE);
                            imageContainer.setVisibility(View.GONE);
                        } else if (imageUri.getScheme().equals("content")) {
                            // External storage - need permission
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) 
                                    == PackageManager.PERMISSION_GRANTED) {
                                shopBannerImage.setImageURI(imageUri);
                                shopBannerImage.setVisibility(View.VISIBLE);
                                imageContainer.setVisibility(View.GONE);
                            } else {
                                // Request permission and show default for now
                                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 
                                        REQUEST_READ_EXTERNAL_STORAGE);
                                imageContainer.setVisibility(View.VISIBLE);
                                shopBannerImage.setVisibility(View.GONE);
                            }
                        } else {
                            // Unknown URI scheme, show default
                            imageContainer.setVisibility(View.VISIBLE);
                            shopBannerImage.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        // If loading fails, show default container
                        android.util.Log.e("ShopHomeFragment", "Error loading shop image", e);
                        imageContainer.setVisibility(View.VISIBLE);
                        shopBannerImage.setVisibility(View.GONE);
                    }
                } else {
                    // Show default container when no image is available
                    imageContainer.setVisibility(View.VISIBLE);
                    shopBannerImage.setVisibility(View.GONE);
                }
            }
        }
    }

    private void setLocationTextFromIds(TextView locationTextView, int cityId, int regionId) {
        if (locationRepository != null && locationTextView != null) {
            try {
                // Get region name
                locationRepository.getRegionById(regionId).observe(getViewLifecycleOwner(), region -> {
                    if (region != null) {
                        // Get city name
                        locationRepository.getCityById(cityId).observe(getViewLifecycleOwner(), city -> {
                            if (city != null && city.getName() != null && region.getName() != null) {
                                String locationText = city.getName() + ", " + region.getName();
                                locationTextView.setText(locationText);
                            } else {
                                locationTextView.setText(getString(R.string.unknown_location));
                            }
                        });
                    } else {
                        locationTextView.setText(getString(R.string.unknown_region));
                    }
                });
            } catch (Exception e) {
                locationTextView.setText(getString(R.string.location_unavailable));
            }
        } else if (locationTextView != null) {
            locationTextView.setText(getString(R.string.location_unavailable));
        }
    }

    private void showShopImageEditDialog(boolean isEditing) {
        String[] options;
        String title;
        
        if (isEditing) {
            options = new String[]{"Change Cover Photo", "Remove Cover Photo", "Cancel"};
            title = "Edit Shop Cover Photo";
        } else {
            options = new String[]{"Add Cover Photo", "Cancel"};
            title = "Add Shop Cover Photo";
        }
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Add/Change Image
                        // Navigate back to main ShopFragment to handle image selection
                        requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.shop_container, new ShopFragment())
                            .commit();
                        break;
                    case 1: // Remove Image (only for editing)
                        if (isEditing) {
                            // Remove image from database
                            shopViewModel.updateShopImage("");
                            
                            // Update UI to show default container
                            ImageView bannerImage = rootView.findViewById(R.id.shopBannerImage);
                            LinearLayout container = rootView.findViewById(R.id.imageContainer);
                            if (bannerImage != null && container != null) {
                                bannerImage.setVisibility(View.GONE);
                                container.setVisibility(View.VISIBLE);
                            }
                            
                            Toast.makeText(requireContext(), getString(R.string.cover_photo_removed), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case 2: // Cancel
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, refresh shop data to load image
                if (shopViewModel != null && shopViewModel.getShop().getValue() != null) {
                    updateShopUI(shopViewModel.getShop().getValue());
                }
            } else {
                // Permission denied, show toast
                Toast.makeText(requireContext(), getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        shopViewModel = null;
        rootView = null;
    }
}
