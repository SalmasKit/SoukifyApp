package com.example.soukify.ui.shop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.R;
import com.example.soukify.data.models.CityModel;
import com.example.soukify.data.models.RegionModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NoShopFragment extends Fragment {
    
    private ShopViewModel shopViewModel;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    
    // References to current dialog views for image picker callback
    private ImageView currentImageView;
    private LinearLayout currentImageContainer;
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_no_shop, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize ViewModel
        shopViewModel = new ViewModelProvider(this).get(ShopViewModel.class);
        
        // Initialize image picker launcher
        initializeImagePicker();
        
        // Set up click listener
        setupClickListener();
    }

    private void setupClickListener() {
        Button createShopButton = getView().findViewById(R.id.createShopButton);
        Button clearShopsButton = getView().findViewById(R.id.clearShopsButton);
        
        if (createShopButton != null) {
            createShopButton.setOnClickListener(v -> {
                android.util.Log.d("NoShopFragment", "Create Shop button clicked");
                Toast.makeText(requireContext(), "Debug: Create Shop button clicked", Toast.LENGTH_SHORT).show();
                // Try to get parent ShopFragment
                Fragment parentFragment = getParentFragment();
                if (parentFragment instanceof ShopFragment) {
                    android.util.Log.d("NoShopFragment", "Found parent ShopFragment");
                    ShopFragment parentShopFragment = (ShopFragment) parentFragment;
                    parentShopFragment.showCreateShopDialog();
                } else {
                    android.util.Log.d("NoShopFragment", "No parent ShopFragment found, trying fragment manager");
                    // Fallback: get ShopFragment from parent fragment manager
                    Fragment shopFragment = requireActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.shop_container);
                    if (shopFragment instanceof ShopFragment) {
                        android.util.Log.d("NoShopFragment", "Found ShopFragment in container");
                        ((ShopFragment) shopFragment).showCreateShopDialog();
                    } else {
                        android.util.Log.d("NoShopFragment", "No ShopFragment found, showing dialog directly");
                        // Last resort: show the dialog directly from this fragment
                        showCreateShopDialogDirectly();
                    }
                }
            });
        } else {
            android.util.Log.e("NoShopFragment", "Create Shop button is null!");
        }
        
        if (clearShopsButton != null) {
            clearShopsButton.setOnClickListener(v -> {
                // Show confirmation dialog before clearing
                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Clear All Shops")
                    .setMessage("Are you sure you want to delete all shops? This action cannot be undone.")
                    .setPositiveButton("Clear All", (dialog, which) -> {
                        shopViewModel.clearAllShops();
                        Toast.makeText(requireContext(), "All shops cleared from database", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        }
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    
                    // Update the image view
                    if (currentImageView != null && currentImageContainer != null) {
                        currentImageView.setImageURI(selectedImageUri);
                        currentImageView.setVisibility(View.VISIBLE);
                        currentImageContainer.setVisibility(View.GONE);
                    }
                }
            }
        );
    }

    private void showCreateShopDialogDirectly() {
        android.util.Log.d("NoShopFragment", "showCreateShopDialogDirectly called");
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_shop, null);
        builder.setView(dialogView);
        
        // Get the parent activity's ViewModel to ensure we're using the same instance as ShopFragment
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        android.util.Log.d("NoShopFragment", "Using shared ViewModel from activity");
        
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
        
        androidx.appcompat.app.AlertDialog dialog = builder.setTitle("Create Your Shop")
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
            .setNegativeButton("Cancel", null)
            .create();
        
        dialog.show();
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
        currentImageView = null;
        currentImageContainer = null;
    }
}
