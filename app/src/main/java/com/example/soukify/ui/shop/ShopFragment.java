package com.example.soukify.ui.shop;

import androidx.appcompat.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.R;
import com.example.soukify.data.repositories.LocationRepository;
import com.example.soukify.data.models.RegionModel;
import com.example.soukify.data.models.CityModel;
import com.example.soukify.data.models.ShopModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShopFragment extends Fragment {
    
    private ShopViewModel shopViewModel;
    private LocationRepository locationRepository;
    private View rootView;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    // References to current dialog views for image picker callback
    private ImageView currentImageView;
    private LinearLayout currentImageContainer;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_shop_container, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel using activity scope to share with NoShopFragment
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        android.util.Log.d("ShopFragment", "ViewModel initialized with activity scope");
        
        // Initialize LocationRepository
        locationRepository = new LocationRepository(requireActivity().getApplication());
        
        // Initialize image picker launcher
        initializeImagePicker();
        
        // Only check shop status if user is logged in
        String currentUserId = shopViewModel.getCurrentUserId();
        if (currentUserId != null && !currentUserId.isEmpty()) {
            shopViewModel.checkShopStatus();
        } else {
            // User not logged in - you can redirect to login or show login UI
            Toast.makeText(getContext(), getString(R.string.please_login_to_access_shop), Toast.LENGTH_SHORT).show();
        }
        
        observeViewModel();
    }

    private void observeViewModel() {
        // Observe hasShop LiveData
        shopViewModel.getHasShop().observe(getViewLifecycleOwner(), hasShop -> {
            android.util.Log.d("ShopFragment", "hasShop changed: " + hasShop);
            if (hasShop != null && hasShop) {
                android.util.Log.d("ShopFragment", "hasShop is true, showing shop home view");
                // Show shop home view
                showShopHomeView();
            } else {
                android.util.Log.d("ShopFragment", "hasShop is false or null, showing no shop view");
                // Show no shop view
                showNoShopView();
            }
        });
        
        // Also observe the repository's current shop to detect changes
        shopViewModel.getRepositoryShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopFragment", "Repository shop changed: " + (shop != null ? "YES" : "NO"));
            if (shop != null) {
                android.util.Log.d("ShopFragment", "Shop detected, showing shop home view");
                android.util.Log.d("ShopFragment", "Shop name: " + shop.getName());
                android.util.Log.d("ShopFragment", "Shop ID: " + shop.getShopId());
                // Show shop home view
                showShopHomeView();
            }
        });
        
        // Observe error messages
        shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShopHomeView() {
        android.util.Log.d("ShopFragment", "showShopHomeView called");
        android.util.Log.d("ShopFragment", "Current shop data: " + (shopViewModel.getShop().getValue() != null ? "EXISTS" : "NULL"));
        android.util.Log.d("ShopFragment", "hasShop value: " + (shopViewModel.getHasShop().getValue()));
        
        // Force check repository shop directly
        ShopModel repositoryShop = shopViewModel.getRepositoryShop().getValue();
        android.util.Log.d("ShopFragment", "Repository shop: " + (repositoryShop != null ? "EXISTS - " + repositoryShop.getName() : "NULL"));
        
        // If repository has shop but hasShop is false, force update
        if (repositoryShop != null && (shopViewModel.getHasShop().getValue() == null || !shopViewModel.getHasShop().getValue())) {
            android.util.Log.d("ShopFragment", "Forcing hasShop update based on repository shop");
            shopViewModel.setHasShop(true);
        }
        
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.shop_container, new ShopHomeFragment())
            .commit();
            
        android.util.Log.d("ShopFragment", "ShopHomeFragment transaction committed");
    }

    private void showNoShopView() {
        android.util.Log.d("ShopFragment", "showNoShopView called");
        requireActivity().getSupportFragmentManager()
            .beginTransaction()
            .replace(R.id.shop_container, new NoShopFragment())
            .commit();
        android.util.Log.d("ShopFragment", "NoShopFragment transaction committed");
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    
                    // Show validation dialog before updating the shop
                    showImageValidationDialog(selectedImageUri);
                }
            }
        );
    }
    
    private void showImageValidationDialog(Uri imageUri) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Confirm Cover Image")
            .setMessage("Are you sure you want to set this as your shop's cover image?")
            .setPositiveButton("Confirm", (dialog, which) -> {
                // Convert URI to string URL and update shop
                String imageUrl = imageUri.toString();
                shopViewModel.updateShopImage(imageUrl);
                
                Toast.makeText(requireContext(), getString(R.string.cover_image_updated), Toast.LENGTH_SHORT).show();
                
                // Navigate back to shop home view
                showShopHomeView();
            })
            .setNegativeButton("Cancel", (dialog, which) -> {
                // Reset selection
                selectedImageUri = null;
                Toast.makeText(requireContext(), getString(R.string.image_selection_cancelled), Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    public void showCreateShopDialog() {
        android.util.Log.d("ShopFragment", "showCreateShopDialog called");
        Toast.makeText(requireContext(), getString(R.string.debug_shop_fragment_called), Toast.LENGTH_SHORT).show();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_shop, null);
        builder.setView(dialogView);
        
        // Initialize views
        TextInputEditText etShopName = dialogView.findViewById(R.id.etShopName);
        TextInputEditText etShopDescription = dialogView.findViewById(R.id.etShopDescription);
        TextInputEditText etShopPhone = dialogView.findViewById(R.id.etShopPhone);
        TextInputEditText etShopEmail = dialogView.findViewById(R.id.etShopEmail);
        TextInputEditText etShopAddress = dialogView.findViewById(R.id.etShopAddress);
        
        // Region and City dropdowns
        AutoCompleteTextView etShopRegion = dialogView.findViewById(R.id.etShopRegion);
        AutoCompleteTextView etShopCity = dialogView.findViewById(R.id.etShopCity);
        
        // Category dropdown
        AutoCompleteTextView etShopCategory = dialogView.findViewById(R.id.etShopCategory);
        
        // Initialize categories list
        List<String> categories = Arrays.asList(
            "Textile & Tapestry",
            "Gourmet & Local Foods",
            "Pottery & Ceramics",
            "Natural Wellness Products",
            "Jewelry & Accessories",
            "Metal & Brass Crafts",
            "Painting & Calligraphy",
            "Woodwork"
        );
        
        // Store selected category
        final String[] selectedCategory = {""};
        
        // Set threshold to 0 so dropdown appears immediately
        etShopRegion.setThreshold(0);
        etShopCity.setThreshold(0);
        etShopCategory.setThreshold(0);
        
        // Set dropdown height for smaller items (40dp each)
        etShopRegion.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCity.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCategory.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        
        // Show dropdown when field is clicked
        etShopRegion.setOnClickListener(v -> etShopRegion.showDropDown());
        etShopCity.setOnClickListener(v -> etShopCity.showDropDown());
        etShopCategory.setOnClickListener(v -> etShopCategory.showDropDown());
        
        // Setup Category dropdown
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), 
            R.layout.dropdown_item, R.id.dropdown_text, categories);
        etShopCategory.setAdapter(categoryAdapter);
        
        // Handle category selection (single select)
        etShopCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory[0] = (String) parent.getItemAtPosition(position);
            etShopCategory.setText(selectedCategory[0]);
        });
        
        // Setup Region dropdown from database
        shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
            if (regionsList != null) {
                List<String> regionNames = new ArrayList<>();
                for (RegionModel region : regionsList) {
                    regionNames.add(region.getName());
                }
                ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(requireContext(), 
                    R.layout.dropdown_item, R.id.dropdown_text, regionNames);
                etShopRegion.setAdapter(regionAdapter);
            }
        });
        
        // Setup City dropdown based on selected region
        etShopRegion.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRegionName = (String) parent.getItemAtPosition(position);
            // Find the region ID from the list
            shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
                if (regionsList != null) {
                    for (RegionModel region : regionsList) {
                        if (region.getName().equals(selectedRegionName)) {
                            // Load cities for this region
                            shopViewModel.loadCitiesByRegion(selectedRegionName);
                            // Observe the cities LiveData
                            shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                                if (citiesList != null) {
                                    List<String> cityNames = new ArrayList<>();
                                    for (CityModel city : citiesList) {
                                        cityNames.add(city.getName());
                                    }
                                    // Sort cities alphabetically from A to Z
                                    java.util.Collections.sort(cityNames);
                                    ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                                        R.layout.dropdown_item, R.id.dropdown_text, cityNames);
                                    etShopCity.setAdapter(cityAdapter);
                                    etShopCity.setText(""); // Clear city when region changes
                                }
                            });
                            break;
                        }
                    }
                }
            });
        });
        
        // Image upload components
        ImageView ivShopPreview = dialogView.findViewById(R.id.ivShopPreview);
        LinearLayout shopImageContainer = dialogView.findViewById(R.id.imageContainer);
        
        // Handle image upload
        shopImageContainer.setOnClickListener(v -> {
            currentImageView = ivShopPreview;
            currentImageContainer = shopImageContainer;
            openGallery();
        });
        
        // Handle image editing when image is shown
        ivShopPreview.setOnClickListener(v -> {
            // Show edit options dialog
            showImageEditDialog(ivShopPreview, shopImageContainer);
        });
        
        AlertDialog dialog = builder.setTitle("Create Your Shop")
            .setIcon(R.drawable.ic_store)
            .setPositiveButton("Create Shop", (dialogInterface, which) -> {
                String imageUrl = selectedImageUri != null ? selectedImageUri.toString() : "";
                String selectedCategoryText = selectedCategory[0] != null && !selectedCategory[0].isEmpty() ? selectedCategory[0] : "General";
                shopViewModel.createShop(
                    etShopName.getText().toString(),
                    etShopDescription.getText().toString(),
                    etShopPhone.getText().toString(),
                    etShopEmail.getText().toString(),
                    etShopRegion.getText().toString(),
                    etShopCity.getText().toString(),
                    etShopAddress.getText().toString(),
                    imageUrl,
                    selectedCategoryText
                );
            })
            .setNeutralButton("Reset Cities", (dialogInterface, which) -> {
                shopViewModel.clearAndRepopulateCities();
                Toast.makeText(requireContext(), "Cities table cleared and repopulated", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .create();
        
        dialog.show();
        
        // Get the title TextView and set orange color
        try {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            }
        } catch (Exception e) {
            // Title styling failed, continue with default
        }
        
        // Set icon color to primary orange color
        try {
            ImageView iconView = dialog.findViewById(android.R.id.icon);
            if (iconView != null) {
                iconView.setColorFilter(getResources().getColor(R.color.colorPrimary, null));
            }
        } catch (Exception e) {
            // Icon tinting failed, continue with default icon
        }
    }

    private void showImageEditDialog(ImageView imageView, LinearLayout imageContainer) {
        currentImageView = imageView;
        currentImageContainer = imageContainer;
        String[] options = {"Change Cover Photo", "Remove Cover Photo", "Cancel"};
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Cover Photo")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0: // Change Image
                        openGallery();
                        break;
                    case 1: // Remove Image
                        imageView.setVisibility(View.GONE);
                        imageContainer.setVisibility(View.VISIBLE);
                        selectedImageUri = null;
                        Toast.makeText(requireContext(), "Cover photo removed", Toast.LENGTH_SHORT).show();
                        break;
                    case 2: // Cancel
                        dialog.dismiss();
                        break;
                }
            })
            .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        shopViewModel = null;
        rootView = null;
    }
}
