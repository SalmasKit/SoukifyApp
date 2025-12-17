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
        
        observeViewModel();
        android.util.Log.d("ShopFragment", "=== ShopFragment.onViewCreated COMPLETED ===");
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
            args.putString("shopCreatedAt", currentShop.getCreatedAt() != null ? currentShop.getCreatedAt() : "");
            args.putLong("shopCreatedAtTimestamp", currentShop.getCreatedAtTimestamp() > 0 ? currentShop.getCreatedAtTimestamp() : System.currentTimeMillis());
            args.putBoolean("hideDialogs", false); // Show dialogs when opened from settings
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
        
        AlertDialog dialog = builder.setTitle("Create Your Shop")
            .setIcon(R.drawable.ic_store)
            .setPositiveButton("Create Shop", (dialogInterface, which) -> {
                if (etShopName.getText().toString().trim().isEmpty() || 
                    etShopDescription.getText().toString().trim().isEmpty()) {
                    Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
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
                    etShopWebsite.getText().toString()
                );
                
                Toast.makeText(requireContext(), "Shop creation initiated!", Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            })
            .setNegativeButton("Cancel", (dialogInterface, which) -> {
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
        
        // Pre-fill data
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
                ivShopPreview.setImageURI(Uri.parse(shop.getImageUrl()));
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
        
        AlertDialog dialog = builder.setTitle("Edit Your Shop")
            .setIcon(R.drawable.ic_store)
            .setPositiveButton("Save Changes", (dialogInterface, which) -> {
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
                Toast.makeText(requireContext(), "Shop updated successfully!", Toast.LENGTH_SHORT).show();
                selectedImageUri = null;
            })
            .setNegativeButton("Cancel", (dialogInterface, which) -> {
                selectedImageUri = null;
            })
            .create();
        
        dialog.show();
        styleDialogButtons(dialog);
    }

    // Helper Methods

    private void setupCategoryDropdown(AutoCompleteTextView etShopCategory, String[] selectedCategory) {
        List<String> categories = Arrays.asList(
            "Textile & Tapestry", "Gourmet & Local Foods", "Pottery & Ceramics","Traditional Wear", "Leather Crafts",
            "Natural Wellness Products", "Jewelry & Accessories", "Metal & Brass Crafts",
            "Painting & Calligraphy", "Woodwork"
        );
        
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
                    regionNames.add(region.getName());
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
        String[] options = {"Change Cover Photo", "Remove Cover Photo", "Cancel"};
        
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle("Edit Cover Photo")
            .setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        openGallery();
                        break;
                    case 1:
                        imageView.setVisibility(View.GONE);
                        imageContainer.setVisibility(View.VISIBLE);
                        selectedImageUri = null;
                        Toast.makeText(requireContext(), "Cover photo removed", Toast.LENGTH_SHORT).show();
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