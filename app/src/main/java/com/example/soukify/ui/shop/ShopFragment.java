package com.example.soukify.ui.shop;

import androidx.appcompat.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

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
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Main controller fragment for shop management that handles:
 * - Shop Status Detection: Checks if logged-in user has an existing shop
 * - Dialog Management: Shows create shop and edit shop dialogs
 * - Image Handling: Implements image picker for shop cover photos
 * - Location Integration: Manages region/city dropdowns
 * - Fragment Navigation: Switches between ShopHomeFragment and NoShopFragment
 */
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
        
        android.util.Log.d("ShopFragment", "=== ShopFragment.onViewCreated STARTED ===");
        
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        android.util.Log.d("ShopFragment", "ViewModel initialized with activity scope");
        
        locationRepository = new LocationRepository(requireActivity().getApplication());
        initializeImagePicker();
        
        // Check if we have an external shop ID from navigation (e.g., from favorites)
        String externalShopId = null;
        if (getArguments() != null) {
            externalShopId = getArguments().getString("shopId");
            String source = getArguments().getString("source");
            android.util.Log.d("ShopFragment", "External navigation - shopId: " + externalShopId + ", source: " + source);
        }
        
        if (externalShopId != null && !externalShopId.isEmpty()) {
            // Load external shop by ID
            android.util.Log.d("ShopFragment", "Loading external shop: " + externalShopId);
            loadExternalShop(externalShopId);
        } else {
            // Load current user's shop (normal flow)
            String currentUserId = shopViewModel.getCurrentUserId();
            android.util.Log.d("ShopFragment", "CurrentUserId: " + currentUserId);
            if (currentUserId != null && !currentUserId.isEmpty()) {
                android.util.Log.d("ShopFragment", "User is logged in, checking shop status");
                // Force refresh shop status to ensure latest data
                shopViewModel.checkShopStatus();
                
                // Add a delayed refresh to ensure Firebase has time to sync
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    android.util.Log.d("ShopFragment", "Delayed shop status refresh");
                    shopViewModel.checkShopStatus();
                }, 1000); // 1 second delay
            } else {
                android.util.Log.d("ShopFragment", "User not logged in or userId is null/empty");
                Toast.makeText(getContext(), getString(R.string.please_login_to_access_shop), Toast.LENGTH_SHORT).show();
            }
        }
        
        observeViewModel();
        android.util.Log.d("ShopFragment", "=== ShopFragment.onViewCreated COMPLETED ===");
    }
    
    private void loadExternalShop(String shopId) {
        android.util.Log.d("ShopFragment", "Loading external shop with ID: " + shopId);
        
        // Load shop from Firestore by ID
        FirebaseFirestore.getInstance()
            .collection("shops")
            .document(shopId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    ShopModel externalShop = documentSnapshot.toObject(ShopModel.class);
                    if (externalShop != null) {
                        externalShop.setShopId(documentSnapshot.getId());
                        android.util.Log.d("ShopFragment", "External shop loaded: " + externalShop.getName());
                        
                        // Show the external shop directly
                        showExternalShop(externalShop);
                    } else {
                        android.util.Log.e("ShopFragment", "Failed to parse external shop data");
                        Toast.makeText(getContext(), getString(R.string.shop_not_found), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    android.util.Log.e("ShopFragment", "External shop not found: " + shopId);
                    Toast.makeText(getContext(), getString(R.string.shop_not_found), Toast.LENGTH_SHORT).show();
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("ShopFragment", "Error loading external shop: " + e.getMessage(), e);
                Toast.makeText(getContext(), getString(R.string.error_loading_shop), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void showExternalShop(ShopModel shop) {
        android.util.Log.d("ShopFragment", "Showing external shop: " + shop.getName());
        
        // Create ShopHomeFragment with external shop data
        ShopHomeFragment shopHomeFragment = new ShopHomeFragment();
        Bundle args = new Bundle();
        args.putString("shopId", shop.getShopId() != null ? shop.getShopId() : "");
        args.putString("shopUserId", shop.getUserId() != null ? shop.getUserId() : ""); // Add owner ID
        args.putString("shopName", shop.getName() != null ? shop.getName() : "");
        args.putString("shopCategory", shop.getCategory() != null ? shop.getCategory() : "");
        args.putString("shopDescription", shop.getDescription() != null ? shop.getDescription() : "");
        args.putString("shopLocation", shop.getLocation() != null ? shop.getLocation() : "");
        args.putString("shopPhone", shop.getPhone() != null ? shop.getPhone() : "");
        args.putString("shopEmail", shop.getEmail() != null ? shop.getEmail() : "");
        args.putString("shopAddress", shop.getAddress() != null ? shop.getAddress() : "");
        args.putString("shopImageUrl", shop.getImageUrl() != null ? shop.getImageUrl() : "");
        args.putString("shopInstagram", shop.getInstagram() != null ? shop.getInstagram() : "");
        args.putString("shopFacebook", shop.getFacebook() != null ? shop.getFacebook() : "");
        args.putString("shopWebsite", shop.getWebsite() != null ? shop.getWebsite() : "");
        args.putString("shopRegionId", shop.getRegionId() != null ? shop.getRegionId() : "");
        args.putString("shopCityId", shop.getCityId() != null ? shop.getCityId() : "");
        args.putString("shopCreatedAt", shop.getCreatedAtString() != null ? shop.getCreatedAtString() : "");
        args.putLong("shopCreatedAtTimestamp", shop.getCreatedAtTimestamp() > 0 ? shop.getCreatedAtTimestamp() : System.currentTimeMillis());
        args.putBoolean("hideDialogs", true); // Hide dialogs for external shops by default
        
        // Pass source parameter from parent fragment arguments
        if (getArguments() != null) {
            String source = getArguments().getString("source");
            if (source != null) {
                args.putString("source", source);
                android.util.Log.d("ShopFragment", "Passing source parameter to ShopHomeFragment: " + source);
            }
        }
        
        shopHomeFragment.setArguments(args);
        
        // Replace the container with the external shop
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.shop_container, shopHomeFragment)
            .commit();
            
        android.util.Log.d("ShopFragment", "External ShopHomeFragment transaction committed");
    }

    private void observeViewModel() {
        ShopModel initialShop = shopViewModel.getShop().getValue();
        Boolean initialHasShop = shopViewModel.getHasShop().getValue();
        Boolean initialLoading = shopViewModel.getIsLoading().getValue();
        
        android.util.Log.d("ShopFragment", "Initial state - Shop: " + (initialShop != null ? "EXISTS" : "NULL") + 
                          ", HasShop: " + initialHasShop + ", Loading: " + initialLoading);
        
        if (initialShop != null) {
            android.util.Log.d("ShopFragment", "Initial shop data available, showing shop home immediately");
            showShopHomeView();
            setupObservers();
            return;
        }
        
        if (initialHasShop != null && !initialHasShop && (initialLoading == null || !initialLoading)) {
            android.util.Log.d("ShopFragment", "Confirmed no shop exists, showing no shop view");
            showNoShopView();
            setupObservers();
            return;
        }
        
        setupObservers();
    }
    
    private void setupObservers() {
        android.util.Log.d("ShopFragment", "Setting up observers");
        
        // Single observer to handle all shop state changes
        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopFragment", "Shop data changed: " + (shop != null ? "EXISTS" : "NULL"));
            Boolean isLoading = shopViewModel.getIsLoading().getValue();
            
            if (isLoading == null || !isLoading) {
                if (shop != null) {
                    android.util.Log.d("ShopFragment", "Shop exists, showing shop home view");
                    showShopHomeView();
                } else {
                    Boolean hasShop = shopViewModel.getHasShop().getValue();
                    if (hasShop != null && hasShop) {
                        android.util.Log.d("ShopFragment", "Shop is null but hasShop is true, showing shop home view");
                        showShopHomeView();
                    } else {
                        android.util.Log.d("ShopFragment", "No shop data, showing no shop view");
                        showNoShopView();
                    }
                }
            }
        });
        
        // Only observe hasShop for logging, don't trigger UI changes here
        shopViewModel.getHasShop().observe(getViewLifecycleOwner(), hasShop -> {
            android.util.Log.d("ShopFragment", "hasShop changed: " + hasShop);
            // Don't trigger UI changes here to avoid loops
        });
        
        // Only observe repository shop for logging, don't trigger UI changes here
        shopViewModel.getRepositoryShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopFragment", "Repository shop changed: " + (shop != null ? "YES" : "NO"));
            // Don't trigger UI changes here to avoid loops
        });
        
        shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShopHomeView() {
        android.util.Log.d("ShopFragment", "showShopHomeView called");
        shopViewModel.clearSuccessMessage();
        
        // Check if ShopHomeFragment is already displayed
        Fragment currentFragment = getChildFragmentManager().findFragmentById(R.id.shop_container);
        if (currentFragment instanceof ShopHomeFragment) {
            android.util.Log.d("ShopFragment", "ShopHomeFragment already displayed, skipping duplicate creation");
            return;
        }
        
        // Create ShopHomeFragment with shop data
        ShopHomeFragment shopHomeFragment = new ShopHomeFragment();
        ShopModel currentShop = shopViewModel.getShop().getValue();
        if (currentShop != null) {
            Bundle args = new Bundle();
            args.putString("shopId", currentShop.getShopId() != null ? currentShop.getShopId() : "");
            args.putString("shopName", currentShop.getName() != null ? currentShop.getName() : "");
            args.putString("shopCategory", currentShop.getCategory() != null ? currentShop.getCategory() : "");
            args.putString("shopDescription", currentShop.getDescription() != null ? currentShop.getDescription() : "");
            args.putString("shopLocation", currentShop.getLocation() != null ? currentShop.getLocation() : "");
            args.putString("shopPhone", currentShop.getPhone() != null ? currentShop.getPhone() : "");
            args.putString("shopEmail", currentShop.getEmail() != null ? currentShop.getEmail() : "");
            args.putString("shopAddress", currentShop.getAddress() != null ? currentShop.getAddress() : "");
            args.putString("shopImageUrl", currentShop.getImageUrl() != null ? currentShop.getImageUrl() : "");
            args.putString("shopInstagram", currentShop.getInstagram() != null ? currentShop.getInstagram() : "");
            args.putString("shopFacebook", currentShop.getFacebook() != null ? currentShop.getFacebook() : "");
            args.putString("shopWebsite", currentShop.getWebsite() != null ? currentShop.getWebsite() : "");
            args.putString("shopRegionId", currentShop.getRegionId() != null ? currentShop.getRegionId() : "");
            args.putString("shopCityId", currentShop.getCityId() != null ? currentShop.getCityId() : "");
            args.putString("shopUserId", currentShop.getUserId() != null ? currentShop.getUserId() : ""); // Pass owner ID
            args.putString("shopCreatedAt", currentShop.getCreatedAtString() != null ? currentShop.getCreatedAtString() : "");
            args.putLong("shopCreatedAtTimestamp", currentShop.getCreatedAtTimestamp() > 0 ? currentShop.getCreatedAtTimestamp() : System.currentTimeMillis());
            args.putBoolean("hideDialogs", false); // Show dialogs when opened from settings
            
            // Pass source parameter from parent fragment arguments
            if (getArguments() != null) {
                String source = getArguments().getString("source");
                if (source != null) {
                    args.putString("source", source);
                    android.util.Log.d("ShopFragment", "Passing source parameter to ShopHomeFragment: " + source);
                }
            }
            
            shopHomeFragment.setArguments(args);
            android.util.Log.d("ShopFragment", "ShopHomeFragment created with shop data: " + currentShop.getName());
        } else {
            android.util.Log.d("ShopFragment", "ShopHomeFragment created without shop data");
        }
        
        getChildFragmentManager()
            .beginTransaction()
            .replace(R.id.shop_container, shopHomeFragment)
            .commit();
            
        android.util.Log.d("ShopFragment", "ShopHomeFragment transaction committed");
    }

    private void showNoShopView() {
        android.util.Log.d("ShopFragment", "showNoShopView called");
        
        ShopModel currentShop = shopViewModel.getShop().getValue();
        if (currentShop != null) {
            android.util.Log.d("ShopFragment", "Shop data found, switching to shop home view instead");
            showShopHomeView();
            return;
        }
        
        getChildFragmentManager()
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
                    
                    android.util.Log.d("ShopFragment", "Image selected: " + selectedImageUri);
                    android.util.Log.d("ShopFragment", "currentImageView: " + (currentImageView != null ? "NOT NULL" : "NULL"));
                    android.util.Log.d("ShopFragment", "currentImageContainer: " + (currentImageContainer != null ? "NOT NULL" : "NULL"));
                    
                    if (currentImageView != null && currentImageContainer != null) {
                        try {
                            currentImageView.setImageURI(selectedImageUri);
                            currentImageView.setVisibility(View.VISIBLE);
                            currentImageContainer.setVisibility(View.GONE);
                            android.util.Log.d("ShopFragment", "Image set successfully, views updated");
                        } catch (Exception e) {
                            android.util.Log.e("ShopFragment", "Error setting image URI", e);
                            // Fallback: keep container visible if image fails to load
                            currentImageView.setVisibility(View.GONE);
                            currentImageContainer.setVisibility(View.VISIBLE);
                        }
                    } else {
                        android.util.Log.e("ShopFragment", "View references are null - cannot update image");
                    }
                }
            }
        );
    }

    /**
     * Shows the create shop dialog - called by NoShopFragment
     */
    public void showCreateShopDialog() {
        android.util.Log.d("ShopFragment", "showCreateShopDialog called");
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_create_shop, null);
        builder.setView(dialogView);
        
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
        
        ImageView ivShopPreview = dialogView.findViewById(R.id.ivShopPreview);
        LinearLayout shopImageContainer = dialogView.findViewById(R.id.imageContainer);
        
        SwitchMaterial switchPromotion = dialogView.findViewById(R.id.switchPromotion);
        SwitchMaterial switchDelivery = dialogView.findViewById(R.id.switchDelivery);
        
        final String[] selectedCategory = {""};
        
        setupCategoryDropdown(etShopCategory, selectedCategory);
        setupWorkingHoursSelector(dialogView);
        setupRegionCityDropdowns(etShopRegion, etShopCity);
        
        shopImageContainer.setOnClickListener(v -> {
            currentImageView = ivShopPreview;
            currentImageContainer = shopImageContainer;
            openGallery();
        });
        
        ivShopPreview.setOnClickListener(v -> {
            showImageEditDialog(ivShopPreview, shopImageContainer);
        });
        
        AlertDialog dialog = builder.setTitle(getString(R.string.create_shop_title))
            .setIcon(R.drawable.ic_store)
            .setPositiveButton(getString(R.string.create_shop_button), (dialogInterface, which) -> {
                if (etShopName.getText().toString().trim().isEmpty() || 
                    etShopDescription.getText().toString().trim().isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.fill_required_fields), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                String imageUrl = selectedImageUri != null ? selectedImageUri.toString() : "";
                String selectedCategoryText = selectedCategory[0] != null && !selectedCategory[0].isEmpty() ? selectedCategory[0] : "General";
                
                // Match the exact parameter order expected by ShopViewModel.createShop()
                shopViewModel.createShop(
                    etShopName.getText().toString(),
                    etShopDescription.getText().toString(),
                    etShopPhone.getText().toString(),
                    etShopEmail.getText().toString(),
                    etShopRegion.getText().toString(),
                    etShopCity.getText().toString(),
                    etShopAddress.getText().toString(),
                    imageUrl,
                    selectedCategoryText,
                    collectWorkingHoursData(dialogView),
                    collectWorkingDaysData(dialogView),
                    etShopInstagram.getText().toString(),
                    etShopFacebook.getText().toString(),
                    etShopWebsite.getText().toString(),
                    switchPromotion.isChecked(),
                    switchDelivery.isChecked()
                );
                
                Toast.makeText(requireContext(), getString(R.string.shop_creation_initiated), Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            })
            .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> {
                selectedImageUri = null;
            })
            .create();
        
        dialog.show();
        styleDialogButtons(dialog);
    }

    /**
     * Shows the edit shop dialog for an existing shop
     */
    public void showEditShopDialog(ShopModel shop) {
        // Force refresh shop data to get latest boolean values from Firestore
        android.util.Log.d("ShopFragment", "Refreshing shop data before edit dialog");
        shopViewModel.refreshShopData();
        android.util.Log.d("ShopFragment", "showEditShopDialog called for shop: " + shop.getName());
        
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_shop, null);
        builder.setView(dialogView);
        
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
        TextView tvShopAge = dialogView.findViewById(R.id.tvShopAge);
        TextView tvShopRating = dialogView.findViewById(R.id.tvShopRating);
        TextView tvCreationDate = dialogView.findViewById(R.id.tvCreationDate);
        
        ImageView ivShopPreview = dialogView.findViewById(R.id.ivShopPreview);
        LinearLayout imageContainer = dialogView.findViewById(R.id.imageContainer);
        
        // Switch components for promotion and delivery
        SwitchMaterial switchPromotion = dialogView.findViewById(R.id.switchPromotion);
        SwitchMaterial switchDelivery = dialogView.findViewById(R.id.switchDelivery);
        
        // Pre-fill data
        etShopName.setText(shop.getName());
        etShopDescription.setText(shop.getDescription());
        etShopPhone.setText(shop.getPhone());
        etShopEmail.setText(shop.getEmail());
        etShopAddress.setText(shop.getAddress());
        etShopInstagram.setText(shop.getInstagram());
        etShopFacebook.setText(shop.getFacebook());
        etShopWebsite.setText(shop.getWebsite());
        
        // Set switch values from existing shop data
        android.util.Log.d("ShopFragment", "=== TOGGLE DEBUG START ===");
        android.util.Log.d("ShopFragment", "Shop data received - Name: " + shop.getName());
        android.util.Log.d("ShopFragment", "Shop boolean values - hasPromotion: " + shop.isHasPromotion() + ", hasLivraison: " + shop.isHasLivraison());
        
        // TEST: Force set to true for debugging
        android.util.Log.d("ShopFragment", "TEST: Forcing toggles to true for debugging");
        switchPromotion.setChecked(true);
        switchDelivery.setChecked(true);
        android.util.Log.d("ShopFragment", "TEST: After forced true - Promotion: " + switchPromotion.isChecked() + ", Delivery: " + switchDelivery.isChecked());
        
        // REAL: Set actual values from shop data
        android.util.Log.d("ShopFragment", "REAL: Setting actual shop values");
        switchPromotion.setChecked(shop.isHasPromotion());
        switchDelivery.setChecked(shop.isHasLivraison());
        
        // Add listeners to immediately update badges when toggles change
        switchPromotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Find and update promotion badge in the current view
            View promotionBadge = getView().findViewById(R.id.promotionBadge);
            if (promotionBadge != null) {
                promotionBadge.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        
        switchDelivery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Find and update delivery badge in the current view
            View deliveryBadge = getView().findViewById(R.id.deliveryBadge);
            if (deliveryBadge != null) {
                deliveryBadge.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        
        // FALLBACK: Ensure toggles are set correctly with delay
        new android.os.Handler().postDelayed(() -> {
            android.util.Log.d("ShopFragment", "FALLBACK: Re-checking toggle states");
            switchPromotion.setChecked(shop.isHasPromotion());
            switchDelivery.setChecked(shop.isHasLivraison());
            android.util.Log.d("ShopFragment", "FALLBACK: Final states - Promotion: " + switchPromotion.isChecked() + ", Delivery: " + switchDelivery.isChecked());
        }, 500);
        android.util.Log.d("ShopFragment", "Setting toggle states - hasPromotion: " + shop.isHasPromotion() + ", hasLivraison: " + shop.isHasLivraison());
        switchPromotion.setChecked(shop.isHasPromotion());
        android.util.Log.d("ShopFragment", "Promotion switch set to: " + switchPromotion.isChecked());
        switchDelivery.setChecked(shop.isHasLivraison());
        
        // FALLBACK: Ensure toggles are set correctly with delay
        new android.os.Handler().postDelayed(() -> {
            android.util.Log.d("ShopFragment", "FALLBACK: Re-checking toggle states");
            switchPromotion.setChecked(shop.isHasPromotion());
            switchDelivery.setChecked(shop.isHasLivraison());
            android.util.Log.d("ShopFragment", "FALLBACK: Final states - Promotion: " + switchPromotion.isChecked() + ", Delivery: " + switchDelivery.isChecked());
        }, 500);
        android.util.Log.d("ShopFragment", "Delivery switch set to: " + switchDelivery.isChecked());
        
        android.util.Log.d("ShopFragment", "=== TOGGLE STATES SET ===");
        android.util.Log.d("ShopFragment", "Final toggle states - Promotion: " + switchPromotion.isChecked() + ", Delivery: " + switchDelivery.isChecked());
        android.util.Log.d("ShopFragment", "Dialog will now show with these toggle states");
        
        tvShopId.setText(getString(R.string.id_format, shop.getShopId().substring(0, Math.min(8, shop.getShopId().length()))));
        if (shop.getCreatedAtString() != null) {
            tvCreationDate.setText(formatDate(shop.getCreatedAtTimestamp()));
            tvShopAge.setText(calculateShopAge(shop.getCreatedAtTimestamp()));
        }
        tvProductsCount.setText("0");
        tvShopRating.setText("0.0");
        
        final String[] selectedCategory = {shop.getCategory()};
        
        setupCategoryDropdown(etShopCategory, selectedCategory);
        etShopCategory.setText(shop.getCategory(), false);
        
        setupRegionCityDropdowns(etShopRegion, etShopCity);
        
        // Set current region
        if (shop.getRegionId() != null) {
            shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
                if (regionsList != null) {
                    for (RegionModel region : regionsList) {
                        if (region.getRegionId().equals(shop.getRegionId())) {
                            etShopRegion.setText(region.getName(), false);
                            break;
                        }
                    }
                }
            });
        }
        
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
        
        // Handle existing image
        if (shop.getImageUrl() != null && !shop.getImageUrl().isEmpty()) {
            try {
                ivShopPreview.setVisibility(View.VISIBLE);
                imageContainer.setVisibility(View.GONE);
                
                // Check if it's a Cloudinary URL (http/https) and load with Glide
                Uri imageUri = Uri.parse(shop.getImageUrl());
                if (imageUri.getScheme() != null && (imageUri.getScheme().equals("http") || imageUri.getScheme().equals("https"))) {
                    Glide.with(requireContext())
                        .load(imageUri.toString())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_image_placeholder)
                        .into(ivShopPreview);
                } else {
                    // Handle local file URIs
                    ivShopPreview.setImageURI(imageUri);
                }
            } catch (Exception e) {
                ivShopPreview.setVisibility(View.GONE);
                imageContainer.setVisibility(View.VISIBLE);
            }
        }
        
        imageContainer.setOnClickListener(v -> {
            currentImageView = ivShopPreview;
            currentImageContainer = imageContainer;
            openGallery();
        });
        
        ivShopPreview.setOnClickListener(v -> {
            showImageEditDialog(ivShopPreview, imageContainer);
        });
        
        AlertDialog dialog = builder.setTitle(getString(R.string.edit_shop_title))
            .setIcon(R.drawable.ic_store)
            .setPositiveButton(getString(R.string.save_changes), (dialogInterface, which) -> {
                String imageUrl = selectedImageUri != null ? selectedImageUri.toString() : shop.getImageUrl();
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
                shop.setImageUrl(imageUrl);
                shop.setHasPromotion(switchPromotion.isChecked());
                shop.setHasLivraison(switchDelivery.isChecked());
                
                // Debug logging to verify toggle values before save
                android.util.Log.d("ShopFragment", "=== EDIT SAVE DEBUG ===");
                android.util.Log.d("ShopFragment", "Saving shop: " + shop.getName());
                android.util.Log.d("ShopFragment", "Toggle values being saved - Promotion: " + switchPromotion.isChecked() + ", Delivery: " + switchDelivery.isChecked());
                android.util.Log.d("ShopFragment", "Shop object values - hasPromotion: " + shop.isHasPromotion() + ", hasLivraison: " + shop.isHasLivraison());
                
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
                shop.setLocation(shop.getAddress() + ", " + etShopCity.getText().toString() + ", " + etShopRegion.getText().toString());
                
                shopViewModel.updateShop(shop);
                Toast.makeText(requireContext(), getString(R.string.shop_updated_success), Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            })
            .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> {
                selectedImageUri = null;
            })
            .create();
        
        dialog.show();
        styleDialogButtons(dialog);
    }

    // Helper Methods

    private void setupCategoryDropdown(AutoCompleteTextView etShopCategory, String[] selectedCategory) {
        List<String> categories = Arrays.asList(
            getString(R.string.cat_textile_tapestry),
            getString(R.string.cat_gourmet_foods),
            getString(R.string.cat_pottery_ceramics),
            getString(R.string.cat_traditional_wear),
            getString(R.string.cat_leather_crafts),
            getString(R.string.cat_wellness_products),
            getString(R.string.cat_jewelry_accessories),
            getString(R.string.cat_metal_brass),
            getString(R.string.cat_painting_calligraphy),
            getString(R.string.cat_woodwork)
        );
        
        etShopCategory.setThreshold(0);
        etShopCategory.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCategory.setOnClickListener(v -> etShopCategory.showDropDown());
        
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(requireContext(), 
            R.layout.dropdown_item, R.id.dropdown_text, categories);
        etShopCategory.setAdapter(categoryAdapter);
        
        etShopCategory.setOnItemClickListener((parent, view, position, id) -> {
            String selectedLocalizedName = (String) parent.getItemAtPosition(position);
            selectedCategory[0] = com.example.soukify.utils.CategoryUtils.getCategoryKey(requireContext(), selectedLocalizedName);
            etShopCategory.setText(selectedLocalizedName, false);
        });
    }

    private void setupRegionCityDropdowns(AutoCompleteTextView etShopRegion, AutoCompleteTextView etShopCity) {
        etShopRegion.setThreshold(0);
        etShopCity.setThreshold(0);
        etShopRegion.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        etShopCity.setDropDownHeight((int) (8 * 40 * getResources().getDisplayMetrics().density));
        
        etShopRegion.setOnClickListener(v -> etShopRegion.showDropDown());
        etShopCity.setOnClickListener(v -> etShopCity.showDropDown());
        
        shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
            if (regionsList != null) {
                List<String> regionNames = new ArrayList<>();
                for (RegionModel region : regionsList) {
                    regionNames.add(region.getLocalizedName());
                }
                ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(requireContext(), 
                    R.layout.dropdown_item, R.id.dropdown_text, regionNames);
                etShopRegion.setAdapter(regionAdapter);
            }
        });
        
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
                                        cityNames.add(city.getLocalizedName());
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
    }

    private void setupWorkingHoursSelector(View dialogView) {
        CheckBox[] dayCheckboxes = {
            dialogView.findViewById(R.id.cbMonday), dialogView.findViewById(R.id.cbTuesday),
            dialogView.findViewById(R.id.cbWednesday), dialogView.findViewById(R.id.cbThursday),
            dialogView.findViewById(R.id.cbFriday), dialogView.findViewById(R.id.cbSaturday),
            dialogView.findViewById(R.id.cbSunday)
        };
        
        LinearLayout[] hourLayouts = {
            dialogView.findViewById(R.id.llMondayHours), dialogView.findViewById(R.id.llTuesdayHours),
            dialogView.findViewById(R.id.llWednesdayHours), dialogView.findViewById(R.id.llThursdayHours),
            dialogView.findViewById(R.id.llFridayHours), dialogView.findViewById(R.id.llSaturdayHours),
            dialogView.findViewById(R.id.llSundayHours)
        };
        
        for (int i = 0; i < dayCheckboxes.length; i++) {
            final int index = i;
            dayCheckboxes[i].setOnCheckedChangeListener((buttonView, isChecked) -> {
                hourLayouts[index].setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });
        }
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

    private void showImageEditDialog(ImageView imageView, LinearLayout imageContainer) {
        currentImageView = imageView;
        currentImageContainer = imageContainer;
        String[] options = {
            getString(R.string.change_cover_photo_option),
            getString(R.string.remove_cover_photo_option),
            getString(R.string.cancel)
        };
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.edit_cover_photo_dialog_title))
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        openGallery();
                        break;
                    case 1:
                        imageView.setVisibility(View.GONE);
                        imageContainer.setVisibility(View.VISIBLE);
                        selectedImageUri = null;
                        Toast.makeText(requireContext(), getString(R.string.cover_photo_removed), Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
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

    private void styleDialogButtons(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        
        if (positiveButton != null && negativeButton != null) {
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
            // Title styling failed, continue with default
        }
        
        try {
            ImageView iconView = dialog.findViewById(android.R.id.icon);
            if (iconView != null) {
                iconView.setColorFilter(getResources().getColor(R.color.colorPrimary, null));
            }
        } catch (Exception e) {
            // Icon tinting failed, continue with default icon
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        shopViewModel = null;
        rootView = null;
    }
}
