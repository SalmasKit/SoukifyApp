package com.example.soukify.ui.shop;

import android.content.ClipData;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import java.io.File;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.RegionModel;
import com.example.soukify.data.models.CityModel;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.repositories.LocationRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ShopHomeFragment extends Fragment {

    private static final String TAG = "ShopHomeFragment";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;

    private ShopViewModel shopViewModel;
    private LocationRepository locationRepository;
    private View rootView;
    
    // Shop image handling
    private Uri selectedShopImageUri;
    private boolean isPreviewingShopImage = false;
    private boolean isSelectingShopImage = false;
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean hasHandledDeletion = false;

    // Product management (delegated to managers)
    private ProductManager productManager;
    private ProductsUIManager productsUIManager;
    private ProductDialogHelper productDialogHelper;

    // Current dialog views for shop image picker
    private ImageView currentImageView;
    private LinearLayout currentImageContainer;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_shop_home, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initializeComponents();
        loadShopRegionCities();
        observeViewModel();
        setupClickListeners();
        initializeProductsUI();

        android.util.Log.d("ShopHomeFragment", "onViewCreated completed");
    }

    private void initializeComponents() {
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        locationRepository = new LocationRepository(requireActivity().getApplication());
        
        // Initialize product managers
        productManager = new ProductManager(requireActivity().getApplication());
        productsUIManager = new ProductsUIManager(this, productManager);
        
        // Initialize image picker
        initializeImagePicker();
        
        // Initialize product dialog helper
        productDialogHelper = new ProductDialogHelper(this, productManager, imagePickerLauncher);
        
        shopViewModel.loadRegions();
    }

    private void initializeImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                        ClipData clipData = result.getData().getClipData();
                        
                        if (isSelectingShopImage && currentImageView != null && currentImageContainer != null) {
                            isSelectingShopImage = false;
                            Uri selectedUri = null;
                            if (clipData != null) {
                                selectedUri = clipData.getItemAt(0).getUri();
                            } else {
                                selectedUri = result.getData().getData();
                            }
                            
                            if (selectedUri != null) {
                                try {
                                    selectedShopImageUri = selectedUri;
                                    isPreviewingShopImage = true;
                                    
                                    currentImageView.setImageURI(selectedUri);
                                    currentImageView.setVisibility(View.VISIBLE);
                                    currentImageContainer.setVisibility(View.GONE);
                                    
                                    ShopModel currentShop = shopViewModel.getShop().getValue();
                                    if (currentShop != null && currentImageView.getId() == R.id.shopBannerImage) {
                                        try {
                                            requireContext().getContentResolver().takePersistableUriPermission(
                                                selectedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                        } catch (SecurityException e) {
                                            Log.w("ShopHomeFragment", "Could not take persistent permission", e);
                                        }
                                        
                                        shopViewModel.updateShopImage(selectedUri.toString());
                                        
                                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                                            isPreviewingShopImage = false;
                                        }, 1000);
                                    }
                                } catch (Exception e) {
                                    isPreviewingShopImage = false;
                                    currentImageView.setVisibility(View.GONE);
                                    currentImageContainer.setVisibility(View.VISIBLE);
                                }
                            }
                            return;
                        }
                        
                        // Handle product image picker through ProductDialogHelper
                        if (productDialogHelper != null) {
                            productDialogHelper.handleImagePickerResult(result.getData());
                        }
                    }
                }
        );
    }

    private void loadShopRegionCities() {
        ShopModel currentShop = shopViewModel.getShop().getValue();
        if (currentShop != null && currentShop.getRegionId() != null) {
            String regionId = currentShop.getRegionId();
            android.util.Log.d("ShopHomeFragment", "Loading cities for shop's region ID: " + regionId);

            shopViewModel.getRegions().observe(getViewLifecycleOwner(), regions -> {
                if (regions != null && !regions.isEmpty()) {
                    for (RegionModel region : regions) {
                        if (region.getRegionId().equals(regionId) ||
                                region.getRegionId().equals(String.valueOf(regionId))) {
                            android.util.Log.d("ShopHomeFragment", "Found region: " + region.getName() + ", loading cities");
                            shopViewModel.loadCitiesByRegion(region.getName());
                            break;
                        }
                    }
                }
            });
        }
    }

    private void initializeProductsUI() {
        if (productsUIManager != null) {
            productsUIManager.initializeUI(rootView);
            productsUIManager.setOnProductClickListener(new ProductsUIManager.OnProductClickListener() {
                @Override
                public void onProductClick(ProductModel product) {
                    Toast.makeText(getContext(), "Clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onProductLongClick(ProductModel product) {
                    Toast.makeText(getContext(), "Long clicked: " + product.getName(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void observeViewModel() {
        android.util.Log.d("ShopHomeFragment", "observeViewModel called");

        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopHomeFragment", "Shop data received: " + (shop != null ? "EXISTS" : "NULL"));
            if (shop != null) {
                android.util.Log.d("ShopHomeFragment", "Shop name: " + shop.getName());
                android.util.Log.d("ShopHomeFragment", "Shop image URL: " + shop.getImageUrl());
                updateShopUI(shop);
                hasHandledDeletion = false;

                if (productManager != null) {
                    productManager.loadProductsForShop(shop.getShopId());
                }
            }
        });

        shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        shopViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMessage -> {
            if (successMessage != null && !successMessage.isEmpty()) {
                Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show();

                if (successMessage.contains("deleted successfully") && !hasHandledDeletion) {
                    hasHandledDeletion = true;
                    android.util.Log.d("ShopHomeFragment", "Shop deletion successful, navigating to shop creation");

                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.shop_container, new ShopFragment())
                            .commit();
                }
            }
        });
    }

    private void setupClickListeners() {
        ImageView editShopButton = rootView.findViewById(R.id.editShopButton);
        if (editShopButton != null) {
            editShopButton.setOnClickListener(v -> {
                ShopModel currentShop = shopViewModel.getShop().getValue();
                if (currentShop != null) {
                    showEditShopDialog(currentShop);
                }
            });
        }

        ImageView deleteShopButton = rootView.findViewById(R.id.deleteShopButton);
        if (deleteShopButton != null) {
            deleteShopButton.setOnClickListener(v -> {
                ShopModel currentShop = shopViewModel.getShop().getValue();
                if (currentShop != null) {
                    showDeleteShopDialog(currentShop);
                }
            });
        }

        LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);
        ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);

        if (imageContainer != null) {
            imageContainer.setOnClickListener(v -> showShopImageEditDialog(false));
        }

        if (shopBannerImage != null) {
            shopBannerImage.setOnClickListener(v -> showShopImageEditDialog(true));
        }

        setupCollapsibleInfoSection();

        FloatingActionButton addProductFab = rootView.findViewById(R.id.addProductFab);
        if (addProductFab != null) {
            addProductFab.setOnClickListener(v -> {
                if (productDialogHelper != null) {
                    productDialogHelper.showAddProductDialog();
                }
            });
        }
    }

    private void setupCollapsibleInfoSection() {
        View infoHeader = rootView.findViewById(R.id.infoHeader);
        View infoContent = rootView.findViewById(R.id.infoContent);
        ImageView expandCollapseIcon = rootView.findViewById(R.id.expandCollapseIcon);

        if (infoHeader != null && infoContent != null && expandCollapseIcon != null) {
            infoHeader.setOnClickListener(v -> {
                if (infoContent.getVisibility() == View.GONE) {
                    infoContent.setVisibility(View.VISIBLE);
                    expandCollapseIcon.setRotation(180f);
                } else {
                    infoContent.setVisibility(View.GONE);
                    expandCollapseIcon.setRotation(0f);
                }
            });
        }
    }

    private void updateShopUI(ShopModel shop) {
        android.util.Log.d("ShopHomeFragment", "updateShopUI called with shop: " + shop.getName());

        if (rootView == null) return;

        updateShopBasicInfo(shop);
        updateShopLocation(shop);
        updateShopContactAndSocial(shop);
        updateShopWorkingHours(shop);
        updateShopImage(shop);
    }

    private void updateShopBasicInfo(ShopModel shop) {
        TextView shopName = rootView.findViewById(R.id.shopName);
        if (shopName != null) {
            shopName.setText(shop.getName());
        }

        TextView shopDescription = rootView.findViewById(R.id.shopDescription);
        if (shopDescription != null) {
            String description = shop.getDescription();
            if (description != null && !description.isEmpty()) {
                shopDescription.setText(description);
                shopDescription.setVisibility(View.VISIBLE);
            } else {
                shopDescription.setVisibility(View.GONE);
            }
        }

        TextView shopCategoryInline = rootView.findViewById(R.id.shopCategoryInline);
        if (shopCategoryInline != null) {
            String category = shop.getCategory();
            if (category != null && !category.isEmpty()) {
                shopCategoryInline.setText(category);
                shopCategoryInline.setVisibility(View.VISIBLE);
            } else {
                shopCategoryInline.setVisibility(View.GONE);
            }
        }

        TextView likesCount = rootView.findViewById(R.id.likesCount);
        if (likesCount != null) {
            likesCount.setText(String.valueOf(shop.getLikesCount()));
        }

        TextView ratingValue = rootView.findViewById(R.id.ratingValue);
        if (ratingValue != null) {
            ratingValue.setText(String.format("%.1f", shop.getRating()));
        }

        TextView shopCreatedAt = rootView.findViewById(R.id.shopCreatedAt);
        if (shopCreatedAt != null) {
            try {
                String createdAtString = shop.getCreatedAt();
                if (createdAtString != null && !createdAtString.isEmpty()) {
                    shopCreatedAt.setText("Created: " + createdAtString);
                    shopCreatedAt.setVisibility(View.VISIBLE);
                } else {
                    shopCreatedAt.setText(getString(R.string.created_just_now));
                    shopCreatedAt.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                android.util.Log.e("ShopHomeFragment", "Error formatting creation date", e);
                shopCreatedAt.setText("Created: Recently");
                shopCreatedAt.setVisibility(View.VISIBLE);
            }
        }
    }

    private void updateShopLocation(ShopModel shop) {
        TextView shopLocation = rootView.findViewById(R.id.shopLocation);
        if (shopLocation == null) return;

        String address = shop.getAddress();
        String cityId = shop.getCityId();
        String regionId = shop.getRegionId();

        shopLocation.setText("Loading location...");
        shopLocation.setVisibility(View.VISIBLE);

        boolean hasCityId = cityId != null && !cityId.isEmpty();
        boolean hasRegionId = regionId != null && !regionId.isEmpty();

        if (hasCityId || hasRegionId) {
            loadLocationData(shopLocation, address, cityId, regionId, hasCityId, hasRegionId);
        } else {
            if (address != null && !address.isEmpty()) {
                shopLocation.setText(address);
            } else {
                shopLocation.setText("Location not specified");
            }
        }
    }

    private void updateShopContactAndSocial(ShopModel shop) {
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

        updateSocialMediaLinks(shop);
    }

    private void updateSocialMediaLinks(ShopModel shop) {
        TextView followUsTitle = rootView.findViewById(R.id.followUsTitle);
        LinearLayout socialMediaIconsLayout = rootView.findViewById(R.id.socialMediaIconsLayout);
        ImageView facebookIcon = rootView.findViewById(R.id.facebookLink);
        ImageView instagramIcon = rootView.findViewById(R.id.instagramLink);
        ImageView websiteIcon = rootView.findViewById(R.id.websiteLink);

        boolean hasSocialMedia = false;

        if (facebookIcon != null) {
            String facebook = shop.getFacebook();
            if (facebook != null && !facebook.isEmpty()) {
                facebookIcon.setVisibility(View.VISIBLE);
                facebookIcon.setOnClickListener(v -> openFacebookLink(facebook));
                hasSocialMedia = true;
            } else {
                facebookIcon.setVisibility(View.GONE);
            }
        }

        if (instagramIcon != null) {
            String instagram = shop.getInstagram();
            if (instagram != null && !instagram.isEmpty()) {
                instagramIcon.setVisibility(View.VISIBLE);
                instagramIcon.setOnClickListener(v -> openInstagramLink(instagram));
                hasSocialMedia = true;
            } else {
                instagramIcon.setVisibility(View.GONE);
            }
        }

        if (websiteIcon != null) {
            String website = shop.getWebsite();
            if (website != null && !website.isEmpty()) {
                websiteIcon.setVisibility(View.VISIBLE);
                websiteIcon.setOnClickListener(v -> openWebsiteLink(website));
                hasSocialMedia = true;
            } else {
                websiteIcon.setVisibility(View.GONE);
            }
        }

        if (followUsTitle != null && socialMediaIconsLayout != null) {
            if (hasSocialMedia) {
                followUsTitle.setVisibility(View.VISIBLE);
                socialMediaIconsLayout.setVisibility(View.VISIBLE);
            } else {
                followUsTitle.setVisibility(View.GONE);
                socialMediaIconsLayout.setVisibility(View.GONE);
            }
        }
    }

    private void updateShopWorkingHours(ShopModel shop) {
        TextView shopWorkingHours = rootView.findViewById(R.id.shop_working_hours);
        if (shopWorkingHours != null) {
            String workingHours = shop.getWorkingHours();
            String workingDays = shop.getWorkingDays();

            if ((workingHours != null && !workingHours.isEmpty()) || (workingDays != null && !workingDays.isEmpty())) {
                String formattedHours = formatWorkingHours(workingDays, workingHours);
                shopWorkingHours.setText(formattedHours);
                shopWorkingHours.setVisibility(View.VISIBLE);
            } else {
                shopWorkingHours.setVisibility(View.GONE);
            }
        }
    }

    private void updateShopImage(ShopModel shop) {
        ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);
        LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);

        if (shopBannerImage == null || imageContainer == null) return;

        android.util.Log.d("ShopHomeFragment", "updateShopImage called");
        android.util.Log.d("ShopHomeFragment", "isPreviewingShopImage: " + isPreviewingShopImage);
        android.util.Log.d("ShopHomeFragment", "shop.getImageUrl(): " + shop.getImageUrl());

        // Only skip update if we're previewing a LOCAL image (file/content scheme)
        // Allow Firebase Storage URLs to update normally
        if (isPreviewingShopImage && shop.getImageUrl() != null && 
            (shop.getImageUrl().startsWith("file:") || shop.getImageUrl().startsWith("content:"))) {
            android.util.Log.d("ShopHomeFragment", "Skipping image update - preview in progress for local image");
            return;
        }

        if (shop.getImageUrl() != null && !shop.getImageUrl().isEmpty()) {
            try {
                Uri imageUri = Uri.parse(shop.getImageUrl());
                
                if (imageUri.getScheme() != null && imageUri.getScheme().equals("file")) {
                    String imagePath = imageUri.getPath();
                    if (imagePath != null && new File(imagePath).exists()) {
                        shopBannerImage.setImageURI(imageUri);
                        shopBannerImage.setVisibility(View.VISIBLE);
                        imageContainer.setVisibility(View.GONE);
                    } else {
                        imageContainer.setVisibility(View.VISIBLE);
                        shopBannerImage.setVisibility(View.GONE);
                    }
                } else if (imageUri.getScheme() != null && imageUri.getScheme().equals("content")) {
                    try {
                        Log.d("ShopHomeFragment", "Content URI detected: " + imageUri);
                        requireContext().getContentResolver().takePersistableUriPermission(
                            imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shopBannerImage.setImageURI(imageUri);
                        shopBannerImage.setVisibility(View.VISIBLE);
                        imageContainer.setVisibility(View.GONE);
                        Log.d("ShopHomeFragment", "Content URI loaded successfully");
                    } catch (SecurityException e) {
                        Log.w("ShopHomeFragment", "No permission to access URI: " + imageUri + ", showing placeholder", e);
                        imageContainer.setVisibility(View.VISIBLE);
                        shopBannerImage.setVisibility(View.GONE);
                    } catch (Exception e) {
                        Log.e("ShopHomeFragment", "Error loading content URI: " + imageUri, e);
                        imageContainer.setVisibility(View.VISIBLE);
                        shopBannerImage.setVisibility(View.GONE);
                    }
                } else if (imageUri.getScheme() != null && (imageUri.getScheme().equals("http") || imageUri.getScheme().equals("https"))) {
                    Log.d("ShopHomeFragment", "Firebase Storage URL detected: " + imageUri + " - loading with Glide");
                    Glide.with(requireContext())
                        .load(imageUri.toString())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(shopBannerImage);
                    shopBannerImage.setVisibility(View.VISIBLE);
                    imageContainer.setVisibility(View.GONE);
                } else {
                    Log.w("ShopHomeFragment", "Unknown URI scheme: " + (imageUri.getScheme() != null ? imageUri.getScheme() : "null") + ", showing placeholder");
                    imageContainer.setVisibility(View.VISIBLE);
                    shopBannerImage.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Log.e("ShopHomeFragment", "Error parsing image URI: " + e.getMessage());
                imageContainer.setVisibility(View.VISIBLE);
                shopBannerImage.setVisibility(View.GONE);
            }
        } else {
            imageContainer.setVisibility(View.VISIBLE);
            shopBannerImage.setVisibility(View.GONE);
        }
    }

    private void loadLocationData(TextView shopLocation, String address, String cityId, String regionId,
                              boolean hasCityId, boolean hasRegionId) {
        List<CityModel> cities = shopViewModel.getCities().getValue();
        List<RegionModel> regions = shopViewModel.getRegions().getValue();

        if (cities == null || cities.isEmpty() || regions == null || regions.isEmpty()) {
            shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
                    if (citiesList != null && regionsList != null && !citiesList.isEmpty() && !regionsList.isEmpty()) {
                        updateLocationWithData(shopLocation, address, cityId, regionId, hasCityId, hasRegionId, citiesList, regionsList);
                    }
                });
            });
        } else {
            updateLocationWithData(shopLocation, address, cityId, regionId, hasCityId, hasRegionId, cities, regions);
        }
    }

    private void updateLocationWithData(TextView shopLocation, String address, String cityId, String regionId,
                                        boolean hasCityId, boolean hasRegionId, List<CityModel> cities, List<RegionModel> regions) {
        StringBuilder locationBuilder = new StringBuilder();

        if (address != null && !address.isEmpty()) {
            locationBuilder.append(address);
        }

        String cityName = null;
        if (hasCityId && cities != null) {
            for (CityModel city : cities) {
                if (city.getCityId().equals(cityId)) {
                    cityName = city.getName();
                    break;
                }
            }
        }

        String regionName = null;
        if (hasRegionId && regions != null) {
            try {
                int regionIntId;
                if (regionId.startsWith("region_")) {
                    regionIntId = Integer.parseInt(regionId.substring(7));
                } else {
                    regionIntId = Integer.parseInt(regionId);
                }

                for (RegionModel region : regions) {
                    if (region.getRegionId().equals(String.valueOf(regionIntId)) ||
                            region.getRegionId().equals(regionId)) {
                        regionName = region.getName();
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                android.util.Log.e("ShopHomeFragment", "Error parsing region ID: " + e.getMessage());
            }
        }

        if (cityName != null) {
            if (locationBuilder.length() > 0) locationBuilder.append(", ");
            locationBuilder.append(cityName);
        }

        if (regionName != null) {
            if (locationBuilder.length() > 0) locationBuilder.append(", ");
            locationBuilder.append(regionName);
        }

        String finalLocation = locationBuilder.toString();
        if (!finalLocation.isEmpty()) {
            shopLocation.setText(finalLocation);
        } else {
            shopLocation.setText("Location not available");
        }
        shopLocation.setVisibility(View.VISIBLE);
    }

    // ==================== EDIT SHOP DIALOG ====================

    public void showEditShopDialog(ShopModel shop) {
        android.util.Log.d("ShopHomeFragment", "showEditShopDialog called for shop: " + shop.getName());

        shopViewModel.loadRegions();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_shop, null);
        builder.setView(dialogView);

        // Initialize all views
        TextInputEditText etShopName = dialogView.findViewById(R.id.etShopName);
        TextInputEditText etShopDescription = dialogView.findViewById(R.id.etShopDescription);
        TextInputEditText etShopPhone = dialogView.findViewById(R.id.etShopPhone);
        TextInputEditText etShopEmail = dialogView.findViewById(R.id.etShopEmail);
        TextInputEditText etShopAddress = dialogView.findViewById(R.id.etShopAddress);
        TextInputEditText etShopInstagram = dialogView.findViewById(R.id.etShopInstagram);
        TextInputEditText etShopFacebook = dialogView.findViewById(R.id.etShopFacebook);
        TextInputEditText etShopWebsite = dialogView.findViewById(R.id.etShopWebsite);

        AutoCompleteTextView etShopRegion = dialogView.findViewById(R.id.etShopRegion);
        AutoCompleteTextView etShopCity = dialogView.findViewById(R.id.etShopCity);
        AutoCompleteTextView etShopCategory = dialogView.findViewById(R.id.etShopCategory);

        TextView tvShopId = dialogView.findViewById(R.id.tvShopId);
        TextView tvProductsCount = dialogView.findViewById(R.id.tvProductsCount);
        TextView tvLikesCount = dialogView.findViewById(R.id.tvLikesCount);
        TextView tvShopAge = dialogView.findViewById(R.id.tvShopAge);
        TextView tvShopRating = dialogView.findViewById(R.id.tvShopRating);
        TextView tvCreationDate = dialogView.findViewById(R.id.tvCreationDate);

        ImageView ivShopPreview = dialogView.findViewById(R.id.ivShopPreview);
        LinearLayout imageContainer = dialogView.findViewById(R.id.imageContainer);

        // Pre-fill existing data
        etShopName.setText(shop.getName());
        etShopDescription.setText(shop.getDescription());
        etShopPhone.setText(shop.getPhone());
        etShopEmail.setText(shop.getEmail());
        etShopAddress.setText(shop.getAddress());
        etShopInstagram.setText(shop.getInstagram());
        etShopFacebook.setText(shop.getFacebook());
        etShopWebsite.setText(shop.getWebsite());

        tvShopId.setText("ID: #" + shop.getShopId().substring(0, Math.min(8, shop.getShopId().length())));

        if (shop.getCreatedAt() != null) {
            tvCreationDate.setText(formatDate(shop.getCreatedAtTimestamp()));
            tvShopAge.setText(calculateShopAge(shop.getCreatedAtTimestamp()));
        }

        // Populate statistics with real data
        tvProductsCount.setText(String.valueOf(productsUIManager != null ? productsUIManager.getCurrentProductsCount() : 0));
        tvLikesCount.setText(String.valueOf(shop.getLikesCount()));
        tvShopRating.setText(String.format("%.1f", shop.getRating()));

        List<String> categories = Arrays.asList(
                "Textile & Tapestry", "Gourmet & Local Foods", "Pottery & Ceramics",
                "Natural Wellness Products", "Jewelry & Accessories", "Metal & Brass Crafts",
                "Painting & Calligraphy", "Woodwork"
        );

        final String[] selectedCategory = {shop.getCategory()};

        setupCategoryDropdown(etShopCategory, categories, selectedCategory);
        etShopCategory.setText(shop.getCategory(), false);

        // Setup working hours and PRE-FILL DATA
        prefillWorkingHoursData(dialogView, shop);
        
        // Setup checkbox listeners for working hours
        setupWorkingHoursCheckboxes(dialogView);

        setupRegionCityDropdowns(etShopRegion, etShopCity, shop);

        handleShopImage(shop, ivShopPreview, imageContainer);

        AlertDialog dialog = builder.setTitle("Edit Your Shop")
                .setIcon(R.drawable.ic_store)
                .setPositiveButton("Save Changes", (dialogInterface, which) -> {
                    saveShopChanges(dialogView, shop, etShopName, etShopDescription, etShopPhone, etShopEmail,
                            etShopAddress, etShopInstagram, etShopFacebook, etShopWebsite,
                            etShopCategory, etShopRegion, etShopCity, selectedCategory);
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    selectedShopImageUri = null;
                })
                .create();

        dialog.show();
        styleDialog(dialog);
    }

    // ==================== HELPER METHODS ====================

    private void setupWorkingHoursCheckboxes(View dialogView) {
        // Array of day checkboxes and their corresponding hour layouts
        CheckBox[] dayCheckboxes = {
            dialogView.findViewById(R.id.cbMonday),
            dialogView.findViewById(R.id.cbTuesday),
            dialogView.findViewById(R.id.cbWednesday),
            dialogView.findViewById(R.id.cbThursday),
            dialogView.findViewById(R.id.cbFriday),
            dialogView.findViewById(R.id.cbSaturday),
            dialogView.findViewById(R.id.cbSunday)
        };

        LinearLayout[] hourLayouts = {
            dialogView.findViewById(R.id.llMondayHours),
            dialogView.findViewById(R.id.llTuesdayHours),
            dialogView.findViewById(R.id.llWednesdayHours),
            dialogView.findViewById(R.id.llThursdayHours),
            dialogView.findViewById(R.id.llFridayHours),
            dialogView.findViewById(R.id.llSaturdayHours),
            dialogView.findViewById(R.id.llSundayHours)
        };

        // Setup listeners for each checkbox
        for (int i = 0; i < dayCheckboxes.length; i++) {
            final int index = i;
            final CheckBox checkBox = dayCheckboxes[i];
            final LinearLayout hourLayout = hourLayouts[i];

            if (checkBox != null && hourLayout != null) {
                checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        hourLayout.setVisibility(View.VISIBLE);
                    } else {
                        hourLayout.setVisibility(View.GONE);
                    }
                });
            }
        }
    }

    private void setupCategoryDropdown(AutoCompleteTextView etShopCategory, List<String> categories, String[] selectedCategory) {
        etShopCategory.setThreshold(0);
        etShopCategory.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCategory.setOnClickListener(v -> etShopCategory.showDropDown());

        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.dropdown_item, R.id.dropdown_text, categories);
        etShopCategory.setAdapter(categoryAdapter);

        etShopCategory.setOnItemClickListener((parent, view, position, id) -> {
            selectedCategory[0] = (String) parent.getItemAtPosition(position);
            etShopCategory.setText(selectedCategory[0], false);
        });
    }

    private void setupRegionCityDropdowns(AutoCompleteTextView etShopRegion, AutoCompleteTextView etShopCity, ShopModel shop) {
        etShopRegion.setThreshold(0);
        etShopCity.setThreshold(0);
        etShopRegion.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCity.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));

        etShopRegion.setOnClickListener(v -> etShopRegion.showDropDown());
        etShopCity.setOnClickListener(v -> etShopCity.showDropDown());

        // Setup regions
        shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
            if (regionsList != null) {
                List<String> regionNames = new ArrayList<>();
                for (RegionModel region : regionsList) {
                    regionNames.add(region.getName());
                }
                ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(requireContext(),
                        R.layout.dropdown_item, R.id.dropdown_text, regionNames);
                etShopRegion.setAdapter(regionAdapter);

                // Set current region
                if (shop.getRegionId() != null) {
                    for (RegionModel region : regionsList) {
                        if (region.getRegionId().equals(shop.getRegionId())) {
                            etShopRegion.setText(region.getName(), false);
                            break;
                        }
                    }
                }
            }
        });

        // Setup region change listener
        etShopRegion.setOnItemClickListener((parent, view, position, id) -> {
            String selectedRegionName = (String) parent.getItemAtPosition(position);
            shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
                if (regionsList != null) {
                    for (RegionModel region : regionsList) {
                        if (region.getName().equals(selectedRegionName)) {
                            shopViewModel.loadCitiesByRegion(selectedRegionName);
                            shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                                if (citiesList != null) {
                                    List<String> cityNames = new ArrayList<>();
                                    for (CityModel city : citiesList) {
                                        cityNames.add(city.getName());
                                    }
                                    java.util.Collections.sort(cityNames);
                                    ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(requireContext(),
                                            R.layout.dropdown_item, R.id.dropdown_text, cityNames);
                                    etShopCity.setAdapter(cityAdapter);
                                    etShopCity.setText("");
                                }
                            });
                            break;
                        }
                    }
                }
            });
        });

        // Set current city
        if (shop.getCityId() != null) {
            shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                if (citiesList != null) {
                    for (CityModel city : citiesList) {
                        if (city.getCityId().equals(shop.getCityId())) {
                            etShopCity.setText(city.getName(), false);
                            break;
                        }
                    }
                }
            });
        }
    }

    private void handleShopImage(ShopModel shop, ImageView ivShopPreview, LinearLayout imageContainer) {
        // Handle existing image
        if (shop.getImageUrl() != null && !shop.getImageUrl().isEmpty()) {
            try {
                ivShopPreview.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.GONE);
                ivShopPreview.setImageURI(Uri.parse(shop.getImageUrl()));
            } catch (Exception e) {
                ivShopPreview.setVisibility(View.GONE);
                imageContainer.setVisibility(View.VISIBLE);
            }
        }

        // Setup image click listeners
        imageContainer.setOnClickListener(v -> {
            currentImageView = ivShopPreview;
            currentImageContainer = imageContainer;
            isSelectingShopImage = true;
            openGallery();
        });

        ivShopPreview.setOnClickListener(v -> {
            showImageEditDialog(ivShopPreview, imageContainer);
        });
    }

    private void saveShopChanges(View dialogView, ShopModel shop,
                                 TextInputEditText etShopName, TextInputEditText etShopDescription,
                                 TextInputEditText etShopPhone, TextInputEditText etShopEmail,
                                 TextInputEditText etShopAddress, TextInputEditText etShopInstagram,
                                 TextInputEditText etShopFacebook, TextInputEditText etShopWebsite,
                                 AutoCompleteTextView etShopCategory, AutoCompleteTextView etShopRegion,
                                 AutoCompleteTextView etShopCity, String[] selectedCategory) {

        // Handle shop image upload if a new image was selected
        if (selectedShopImageUri != null) {
            // Upload the selected image to get a proper file:// URI like during creation
            shopViewModel.updateShopImage(selectedShopImageUri.toString());
            
            // Reset the preview flag after initiating the upload
            isPreviewingShopImage = false;
        }

        String selectedCategoryText = selectedCategory[0] != null && !selectedCategory[0].isEmpty() ? selectedCategory[0] : shop.getCategory();

        shop.setName(etShopName.getText().toString());
        shop.setDescription(etShopDescription.getText().toString());
        shop.setPhone(etShopPhone.getText().toString());
        shop.setEmail(etShopEmail.getText().toString());
        shop.setAddress(etShopAddress.getText().toString());
        shop.setWorkingHours(collectWorkingHoursData(dialogView));
        shop.setWorkingDays(collectWorkingDaysData(dialogView));
        shop.setInstagram(etShopInstagram.getText().toString());
        shop.setFacebook(etShopFacebook.getText().toString());
        shop.setWebsite(etShopWebsite.getText().toString());
        shop.setCategory(selectedCategoryText);

        // Get IDs from names
        String regionId = null;
        String cityId = null;

        List<RegionModel> regions = shopViewModel.getRegions().getValue();
        if (regions != null) {
            for (RegionModel region : regions) {
                if (region.getName().equals(etShopRegion.getText().toString())) {
                    regionId = region.getRegionId();
                    break;
                }
            }
        }

        List<CityModel> cities = shopViewModel.getCities().getValue();
        if (cities != null) {
            for (CityModel city : cities) {
                if (city.getName().equals(etShopCity.getText().toString())) {
                    cityId = city.getCityId();
                    break;
                }
            }
        }

        shop.setRegionId(regionId);
        shop.setCityId(cityId);

        shopViewModel.updateShop(shop);
        
        // Clear the preview URI after saving
        selectedShopImageUri = null;
    }

    private void styleDialog(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        if (positiveButton != null && negativeButton != null) {
            positiveButton.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            negativeButton.setTextColor(getResources().getColor(R.color.text_secondary, null));

            LinearLayout.LayoutParams positiveParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            LinearLayout.LayoutParams negativeParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);

            positiveParams.setMargins(0, 0, 8, 0);
            negativeParams.setMargins(8, 0, 0, 0);

            positiveButton.setLayoutParams(positiveParams);
            negativeButton.setLayoutParams(negativeParams);
        }

        try {
            TextView titleView = dialog.findViewById(android.R.id.title);
            if (titleView != null) {
                titleView.setTextColor(getResources().getColor(R.color.colorPrimary, null));
            }
        } catch (Exception e) {
            // Title styling failed
        }

        try {
            ImageView iconView = dialog.findViewById(android.R.id.icon);
            if (iconView != null) {
                iconView.setColorFilter(getResources().getColor(R.color.colorPrimary, null));
            }
        } catch (Exception e) {
            // Icon tinting failed
        }
    }

    private void showImageEditDialog(ImageView imageView, LinearLayout imageContainer) {
        currentImageView = imageView;
        currentImageContainer = imageContainer;
        isSelectingShopImage = true;
        String[] options = {"Change Cover Photo", "Remove Cover Photo", "Cancel"};

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Edit Cover Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Change Photo
                            openGallery();
                            break;
                        case 1: // Remove Photo
                            imageView.setVisibility(View.GONE);
                            imageContainer.setVisibility(View.VISIBLE);
                            selectedShopImageUri = null;

                            ShopModel currentShop = shopViewModel.getShop().getValue();
                            if (currentShop != null) {
                                shopViewModel.updateShopImage("");
                            }

                            Toast.makeText(requireContext(), "Cover photo removed", Toast.LENGTH_SHORT).show();
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void showDeleteShopDialog(ShopModel shop) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_delete_shop_confirmation, null);
        EditText passwordInput = dialogView.findViewById(R.id.passwordInput);
        TextView errorText = dialogView.findViewById(R.id.errorText);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Shop")
                .setMessage("Are you sure you want to delete '" + shop.getName() + "'? This action cannot be undone and will also delete all products associated with this shop.\n\nPlease enter your password to confirm:")
                .setView(dialogView)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Delete", null)
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
                if (errorMsg != null) {
                    if (errorMsg.contains("Incorrect password")) {
                        errorText.setText("Incorrect password. Please try again.");
                        errorText.setVisibility(View.VISIBLE);
                    } else if (errorMsg.contains("Shop deletion cancelled")) {
                        errorText.setText("Authentication failed. Please try again.");
                        errorText.setVisibility(View.VISIBLE);
                    } else if (errorMsg.contains("No user logged in")) {
                        errorText.setText("User not logged in. Please sign in again.");
                        errorText.setVisibility(View.VISIBLE);
                    }
                    shopViewModel.clearErrorMessage();
                }
            });
            
            shopViewModel.getSuccessMessage().observe(getViewLifecycleOwner(), successMsg -> {
                if (successMsg != null && successMsg.contains("deleted successfully")) {
                    dialog.dismiss();
                    Toast.makeText(requireContext(), successMsg, Toast.LENGTH_LONG).show();
                    shopViewModel.clearSuccessMessage();
                }
            });
            
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(view -> {
                String password = passwordInput.getText().toString().trim();
                
                if (password.isEmpty()) {
                    errorText.setText("Password is required");
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }
                
                errorText.setVisibility(View.GONE);
                shopViewModel.deleteShop(shop.getShopId(), password);
                Toast.makeText(requireContext(), "Verifying password...", Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
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
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setItems(options, (dialog, which) -> {
                if (!isEditing) {
                    switch (which) {
                        case 0: // Add Cover Photo
                            currentImageView = rootView.findViewById(R.id.shopBannerImage);
                            currentImageContainer = rootView.findViewById(R.id.imageContainer);
                            isSelectingShopImage = true;
                            openGallery();
                            break;
                        case 1: // Cancel
                            dialog.dismiss();
                            break;
                    }
                } else {
                    switch (which) {
                        case 0: // Change Cover Photo
                            currentImageView = rootView.findViewById(R.id.shopBannerImage);
                            currentImageContainer = rootView.findViewById(R.id.imageContainer);
                            isSelectingShopImage = true;
                            openGallery();
                            break;
                        case 1: // Remove Cover Photo
                            shopViewModel.updateShopImage("");
                            ImageView bannerImage = rootView.findViewById(R.id.shopBannerImage);
                            LinearLayout container = rootView.findViewById(R.id.imageContainer);
                            if (bannerImage != null && container != null) {
                                bannerImage.setVisibility(View.GONE);
                                container.setVisibility(View.VISIBLE);
                            }
                            selectedShopImageUri = null;
                            Toast.makeText(requireContext(), "Cover photo removed", Toast.LENGTH_SHORT).show();
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                }
            })
            .show();
    }

    private String collectWorkingDaysData(View dialogView) {
        StringBuilder days = new StringBuilder();
        CheckBox[] dayCheckboxes = {
                dialogView.findViewById(R.id.cbMonday), dialogView.findViewById(R.id.cbTuesday),
                dialogView.findViewById(R.id.cbWednesday), dialogView.findViewById(R.id.cbThursday),
                dialogView.findViewById(R.id.cbFriday), dialogView.findViewById(R.id.cbSaturday),
                dialogView.findViewById(R.id.cbSunday)
        };

        String[] dayNames = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (int i = 0; i < dayCheckboxes.length; i++) {
            if (dayCheckboxes[i].isChecked()) {
                if (days.length() > 0) days.append(", ");
                days.append(dayNames[i]);
            }
        }

        return days.toString();
    }

    private String collectWorkingHoursData(View dialogView) {
        StringBuilder hours = new StringBuilder();
        CheckBox[] dayCheckboxes = {
                dialogView.findViewById(R.id.cbMonday), dialogView.findViewById(R.id.cbTuesday),
                dialogView.findViewById(R.id.cbWednesday), dialogView.findViewById(R.id.cbThursday),
                dialogView.findViewById(R.id.cbFriday), dialogView.findViewById(R.id.cbSaturday),
                dialogView.findViewById(R.id.cbSunday)
        };

        EditText[] fromTimeFields = {
                dialogView.findViewById(R.id.etMondayFrom), dialogView.findViewById(R.id.etTuesdayFrom),
                dialogView.findViewById(R.id.etWednesdayFrom), dialogView.findViewById(R.id.etThursdayFrom),
                dialogView.findViewById(R.id.etFridayFrom), dialogView.findViewById(R.id.etSaturdayFrom),
                dialogView.findViewById(R.id.etSundayFrom)
        };

        EditText[] toTimeFields = {
                dialogView.findViewById(R.id.etMondayTo), dialogView.findViewById(R.id.etTuesdayTo),
                dialogView.findViewById(R.id.etWednesdayTo), dialogView.findViewById(R.id.etThursdayTo),
                dialogView.findViewById(R.id.etFridayTo), dialogView.findViewById(R.id.etSaturdayTo),
                dialogView.findViewById(R.id.etSundayTo)
        };

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

        for (int i = 0; i < dayCheckboxes.length; i++) {
            if (dayCheckboxes[i].isChecked()) {
                if (hours.length() > 0) hours.append(" | ");
                String fromTime = fromTimeFields[i].getText().toString();
                String toTime = toTimeFields[i].getText().toString();
                hours.append(dayNames[i]).append(": ")
                        .append(fromTime.isEmpty() ? "9:00" : fromTime)
                        .append("-")
                        .append(toTime.isEmpty() ? "17:00" : toTime);
            }
        }

        return hours.toString();
    }

    private String formatDate(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    private String calculateShopAge(long timestamp) {
        long currentTime = System.currentTimeMillis();
        long diff = currentTime - timestamp;
        long days = diff / (1000 * 60 * 60 * 24);

        if (days < 30) {
            return days + "d";
        } else if (days < 365) {
            long months = days / 30;
            return months + "m";
        } else {
            long years = days / 365;
            return years + "y";
        }
    }

    private String formatWorkingHours(String workingDays, String workingHours) {
        StringBuilder formatted = new StringBuilder();

        if (workingHours != null && !workingHours.isEmpty()) {
            String[] dayHours = workingHours.split("\\|");

            for (String dayHour : dayHours) {
                String trimmed = dayHour.trim();
                if (!trimmed.isEmpty()) {
                    if (formatted.length() > 0) {
                        formatted.append("\n");
                    }

                    String formattedDay = trimmed
                            .replaceAll("Mon:", "Monday:")
                            .replaceAll("Tue:", "Tuesday:")
                            .replaceAll("Wed:", "Wednesday:")
                            .replaceAll("Thu:", "Thursday:")
                            .replaceAll("Fri:", "Friday:")
                            .replaceAll("Sat:", "Saturday:")
                            .replaceAll("Sun:", "Sunday:")
                            .replaceAll("-", "->");

                    formatted.append(formattedDay);
                }
            }
        }

        return formatted.toString();
    }

    private void openFacebookLink(String link) {
        openLinkWithLogging(link, "facebook");
    }

    private void openInstagramLink(String link) {
        openLinkWithLogging(link, "instagram");
    }

    private void openWebsiteLink(String link) {
        if (link == null || link.isEmpty()) return;

        Uri uri;
        if (link.startsWith("http://") || link.startsWith("https://")) {
            uri = Uri.parse(link);
        } else {
            uri = Uri.parse("https://" + link);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLinkWithLogging(String link, String platform) {
        Uri uri = buildSocialMediaUrl(link, platform);
        if (uri == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    private Uri buildSocialMediaUrl(String link, String platform) {
        if (link == null || link.isEmpty()) return null;

        if (link.startsWith("http://") || link.startsWith("https://")) {
            return Uri.parse(link);
        }

        String baseUrl = "https://" + platform + ".com/";
        String platformDomain = platform + ".com";

        if (link.contains(platformDomain)) {
            if (link.contains(platformDomain + "/")) {
                String username = link.substring(link.indexOf(platformDomain + "/") + platformDomain.length() + 1);
                if (username.startsWith("/")) {
                    username = username.substring(1);
                }
                return Uri.parse(baseUrl + username);
            } else {
                return Uri.parse(baseUrl + link);
            }
        } else if (link.startsWith("@")) {
            return Uri.parse(baseUrl + link.substring(1));
        } else {
            return Uri.parse(baseUrl + link);
        }
    }

    private void openGallery() {
        if (!isAdded() || getContext() == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_EXTERNAL_STORAGE);
            return;
        }

        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Shop Cover Photo"));
    }

    private void prefillWorkingHoursData(View dialogView, ShopModel shop) {
        CheckBox[] dayCheckboxes = {
            dialogView.findViewById(R.id.cbMonday),
            dialogView.findViewById(R.id.cbTuesday),
            dialogView.findViewById(R.id.cbWednesday),
            dialogView.findViewById(R.id.cbThursday),
            dialogView.findViewById(R.id.cbFriday),
            dialogView.findViewById(R.id.cbSaturday),
            dialogView.findViewById(R.id.cbSunday)
        };

        EditText[] fromTimeFields = {
            dialogView.findViewById(R.id.etMondayFrom),
            dialogView.findViewById(R.id.etTuesdayFrom),
            dialogView.findViewById(R.id.etWednesdayFrom),
            dialogView.findViewById(R.id.etThursdayFrom),
            dialogView.findViewById(R.id.etFridayFrom),
            dialogView.findViewById(R.id.etSaturdayFrom),
            dialogView.findViewById(R.id.etSundayFrom)
        };

        EditText[] toTimeFields = {
            dialogView.findViewById(R.id.etMondayTo),
            dialogView.findViewById(R.id.etTuesdayTo),
            dialogView.findViewById(R.id.etWednesdayTo),
            dialogView.findViewById(R.id.etThursdayTo),
            dialogView.findViewById(R.id.etFridayTo),
            dialogView.findViewById(R.id.etSaturdayTo),
            dialogView.findViewById(R.id.etSundayTo)
        };

        LinearLayout[] hourLayouts = {
            dialogView.findViewById(R.id.llMondayHours),
            dialogView.findViewById(R.id.llTuesdayHours),
            dialogView.findViewById(R.id.llWednesdayHours),
            dialogView.findViewById(R.id.llThursdayHours),
            dialogView.findViewById(R.id.llFridayHours),
            dialogView.findViewById(R.id.llSaturdayHours),
            dialogView.findViewById(R.id.llSundayHours)
        };

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        String workingHours = shop.getWorkingHours();

        if (workingHours != null && !workingHours.isEmpty()) {
            String[] dayHours = workingHours.split("\\|");

            for (String dayHour : dayHours) {
                String trimmed = dayHour.trim();

                if (trimmed.contains(":")) {
                    String[] parts = trimmed.split(":", 2);
                    if (parts.length >= 2) {
                        String day = parts[0].trim();
                        String hours = parts[1].trim();

                        for (int i = 0; i < dayNames.length; i++) {
                            if (dayNames[i].equals(day)) {
                                dayCheckboxes[i].setChecked(true);
                                hourLayouts[i].setVisibility(View.VISIBLE);

                                if (hours.contains("-")) {
                                    String[] times = hours.split("-");
                                    if (times.length == 2) {
                                        fromTimeFields[i].setText(times[0].trim());
                                        toTimeFields[i].setText(times[1].trim());
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void copyFile(java.io.File source, java.io.File dest) throws java.io.IOException {
        try (java.io.FileInputStream in = new java.io.FileInputStream(source);
             java.io.FileOutputStream out = new java.io.FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }
}