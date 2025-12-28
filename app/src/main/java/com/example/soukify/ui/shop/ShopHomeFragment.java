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
import android.widget.ImageButton;
import android.graphics.Color;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.RegionModel;
import com.example.soukify.data.models.CityModel;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.repositories.LocationRepository;
import com.example.soukify.data.repositories.FavoritesTableRepository;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.example.soukify.data.sync.ShopSync;

public class ShopHomeFragment extends Fragment implements ShopSync.SyncListener {

    private static final String TAG = "ShopHomeFragment";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 100;

    private ShopViewModel shopViewModel;
    private ProductViewModel productViewModel;
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

    // Visitor buttons
    private ImageButton btnLike;
    private ImageButton btnFavorite;
    private LinearLayout visitorActionsLayout;
    
    // Track loaded shop to prevent duplicate product loads
    private String lastLoadedShopId = null;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_shop_home, container, false);

        // Initialize components first
        initializeComponents();
        initializeProductsUI(); // Initialize product UI components early
        setupVisitorButtons();

        // Check if dialogs should be hidden (when opened from search)
        boolean hideDialogs = false;
        Bundle args = getArguments();
        if (args != null) {
            hideDialogs = args.getBoolean("hideDialogs", false);
            
            // Check if we should load the current user's shop (from Settings)
            boolean loadUserShop = args.getBoolean("loadUserShop", false);
            
            if (loadUserShop) {
                // Load current user's shop from repository
                android.util.Log.d("ShopHomeFragment", "Loading current user's shop from Settings");
                shopViewModel.loadUserShops();
                
                // Check if shop data already exists in ViewModel
                ShopModel existingShop = shopViewModel.getShop().getValue();
                if (existingShop != null) {
                    // Shop data already available, load products immediately
                    android.util.Log.d("ShopHomeFragment", "Shop data already exists, loading products immediately for: " + existingShop.getShopId());
                    if (productViewModel != null) {
                        productViewModel.loadProductsForShop(existingShop.getShopId());
                    }
                } else {
                    // Shop data not yet available, set up a one-time observer
                    android.util.Log.d("ShopHomeFragment", "Shop data not yet available, setting up observer");
                    shopViewModel.getShop().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<ShopModel>() {
                        @Override
                        public void onChanged(ShopModel shop) {
                            if (shop != null) {
                                android.util.Log.d("ShopHomeFragment", "User shop loaded via observer, loading products for: " + shop.getShopId());
                                // Remove this observer after first trigger
                                shopViewModel.getShop().removeObserver(this);
                                // Load products immediately
                                if (productViewModel != null) {
                                    productViewModel.loadProductsForShop(shop.getShopId());
                                }
                            }
                        }
                    });
                }
            } else if (args.containsKey("shopId")) {
                // If shop data is passed from search/favorites, load the specific shop
                String shopId = args.getString("shopId");
                if (shopId != null && !shopId.isEmpty()) {
                    android.util.Log.d("ShopHomeFragment", "Loading shop data from database for shopId: " + shopId);
                    
                    // Load shop data from database to ensure consistency
                    shopViewModel.loadShopById(shopId);
                    
                    // 🔥 OPTIMIZATION: Start loading products IMMEDIATELY in parallel
                    // Don't wait for shop details to load first
                    android.util.Log.d("ShopHomeFragment", "Optimistic product load for shopId: " + shopId);
                    productViewModel.loadProductsForShop(shopId);
                }
            }
        }

        loadShopRegionCities();
        observeViewModel();
        setupClickListeners();

        android.util.Log.d("ShopHomeFragment", "onViewCreated completed");
        return rootView;
    }

    private boolean isShopOwner() {
        String currentUserId = shopViewModel.getCurrentUserId();
        
        if (currentUserId == null) {
            return false;
        }
        
        // First check if we have external shop data from arguments
        Bundle args = getArguments();
        if (args != null && args.containsKey("shopId")) {
            String externalShopUserId = args.getString("shopUserId");
            if (externalShopUserId != null) {
                boolean isOwner = currentUserId.equals(externalShopUserId);
                android.util.Log.d("ShopHomeFragment", "External shop ownership check - Current user: " + currentUserId + 
                                  ", Shop owner: " + externalShopUserId + ", Is owner: " + isOwner);
                return isOwner;
            }
        }
        
        // Fallback to checking current shop from ViewModel (for user's own shop)
        ShopModel currentShop = shopViewModel.getShop().getValue();
        if (currentShop == null) {
            return false;
        }
        
        // Check if the current user is the owner of this shop
        boolean isOwner = currentUserId.equals(currentShop.getUserId());
        android.util.Log.d("ShopHomeFragment", "Ownership check - Current user: " + currentUserId + 
                          ", Shop owner: " + currentShop.getUserId() + ", Is owner: " + isOwner);
        return isOwner;
    }

    private ShopModel createShopFromArguments(Bundle args) {
        try {
            ShopModel shop = new ShopModel();
            shop.setShopId(args.getString("shopId"));
            shop.setName(args.getString("shopName"));
            shop.setCategory(args.getString("shopCategory"));
            shop.setDescription(args.getString("shopDescription"));
            shop.setLocation(args.getString("shopLocation"));
            shop.setPhone(args.getString("shopPhone"));
            shop.setEmail(args.getString("shopEmail"));
            shop.setAddress(args.getString("shopAddress"));
            shop.setImageUrl(args.getString("shopImageUrl"));
            shop.setInstagram(args.getString("shopInstagram"));
            shop.setFacebook(args.getString("shopFacebook"));
            shop.setWebsite(args.getString("shopWebsite"));
            shop.setRegionId(args.getString("shopRegionId"));
            shop.setCityId(args.getString("shopCityId"));
            shop.setCreatedAt(args.getString("shopCreatedAt"));
            return shop;
        } catch (Exception e) {
            android.util.Log.e("ShopHomeFragment", "Error creating shop from arguments", e);
            return null;
        }
    }

    private void initializeComponents() {
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity(), new ProductViewModel.Factory(requireActivity().getApplication())).get(ProductViewModel.class);
        productViewModel.setupObservers(getViewLifecycleOwner());
        locationRepository = new LocationRepository(requireActivity().getApplication());

        // Initialize toolbar with back button
        initializeToolbar();

        // Initialize product managers
        productManager = new ProductManager(requireActivity().getApplication());
        productsUIManager = new ProductsUIManager(this, productManager, productViewModel);

        // Initialize image picker
        initializeImagePicker();

        // Initialize product dialog helper
        productDialogHelper = new ProductDialogHelper(this, productManager, imagePickerLauncher);
        shopViewModel.loadRegions();
    }

    private void initializeToolbar() {
        androidx.appcompat.widget.Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Clear city filter before navigating back to search
                if (getActivity() != null) {
                    getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                            .edit()
                            .remove("selected_city")
                            .remove("pending_city_filter")
                            .apply();
                }
                
                // Use NavController for proper navigation
                try {
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigateUp();
                    Log.d("ShopHomeFragment", "Back navigation successful");
                } catch (Exception e) {
                    Log.e("ShopHomeFragment", "Error with NavController navigation", e);
                    // Fallback to fragment manager
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    } else {
                        // Final fallback - navigate to settings directly
                        try {
                            NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment_activity_main);
                            navController.navigate(R.id.navigation_settings);
                        } catch (Exception ex) {
                            Log.e("ShopHomeFragment", "All navigation methods failed", ex);
                        }
                    }
                }
            });
            
            // Add location button to toolbar if we have a selected city
            setupToolbarLocationButton(toolbar);
        }
    }
    
    private void setupToolbarLocationButton(androidx.appcompat.widget.Toolbar toolbar) {
        // Check if we have a selected city from SharedPreferences
        if (getActivity() == null || rootView == null) return;
        
        String selectedCity = getActivity()
                .getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                .getString("selected_city", null);
        
        // Find the location button from the toolbar (if it exists in XML)
        LinearLayout locationButton = rootView.findViewById(R.id.toolbar_location_button);
        TextView cityText = rootView.findViewById(R.id.toolbar_city_text);
        
        if (selectedCity != null && !selectedCity.isEmpty() && locationButton != null) {
            // Set city text
            if (cityText != null) {
                cityText.setText(selectedCity);
            }
            
            // Show the location button
            locationButton.setVisibility(View.VISIBLE);
            
            // Set click listener to navigate to search WITHOUT city filter
            locationButton.setOnClickListener(v -> {
                try {
                    // Clear the city filter before navigating
                    if (getActivity() != null) {
                        getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                                .edit()
                                .remove("selected_city")
                                .remove("pending_city_filter")
                                .apply();
                    }
                    
                    NavController navController = Navigation.findNavController(requireView());
                    navController.navigate(R.id.navigation_search);
                } catch (Exception e) {
                    Log.e("ShopHomeFragment", "Error navigating to search", e);
                }
            });
        }
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
                    android.util.Log.d("ShopHomeFragment", "Product clicked: " + product.getName());
                    navigateToProductDetail(product);
                }

                @Override
                public void onProductLongClick(ProductModel product) {
                    Toast.makeText(getContext(), getString(R.string.long_clicked_prefix) + product.getName(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onFavoriteClick(ProductModel product, int position) {
                    // Similar logic to SearchFragment/FavoritesFragment if we want confirmation
                    // For now, let's keep it consistent with the sync utility
                    boolean newFavorite = !product.isFavoriteByUser();
                    product.setFavoriteByUser(newFavorite);
                    com.example.soukify.data.sync.ProductSync.FavoriteSync.update(product.getProductId(), newFavorite);
                    if (productViewModel != null) {
                        productViewModel.toggleFavoriteProduct(product.getProductId());
                    }
                }
            });
        }
    }

    private void observeViewModel() {
        android.util.Log.d("ShopHomeFragment", "observeViewModel called");

        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            android.util.Log.d("ShopHomeFragment", "Shop data received: " + (shop != null ? "EXISTS" : "NULL"));
            if (shop != null) {
                // Ensure we only display the requested shop (unless we're loading user's shop from Settings)
                Bundle args = getArguments();
                boolean loadUserShop = args != null && args.getBoolean("loadUserShop", false);
                
                if (args != null && args.containsKey("shopId") && !loadUserShop) {
                    String requestedId = args.getString("shopId");
                    if (requestedId != null && !requestedId.isEmpty() && !requestedId.equals(shop.getShopId())) {
                        android.util.Log.w("ShopHomeFragment", "Ignoring shop update. Requested: " + requestedId + ", Received: " + shop.getShopId());
                        return;
                    }
                }

                android.util.Log.d("ShopHomeFragment", "Shop name: " + shop.getName());
                android.util.Log.d("ShopHomeFragment", "Shop image URL: " + shop.getImageUrl());
                
                // Load products using the verified shop ID (only if not already loaded)
                if (lastLoadedShopId == null || !lastLoadedShopId.equals(shop.getShopId())) {
                    android.util.Log.d("ShopHomeFragment", "Loading products for shop: " + shop.getShopId());
                    lastLoadedShopId = shop.getShopId();
                    if (productViewModel != null) {
                        productViewModel.loadProductsForShop(shop.getShopId());
                    }
                } else {
                    android.util.Log.d("ShopHomeFragment", "Products already loaded for shop: " + shop.getShopId());
                }

                // Initialize ProductManager with the current shop ID for product addition/editing
                if (productManager != null) {
                    productManager.loadProductsForShop(shop.getShopId());
                }

                updateShopUI(shop);
                refreshButtonVisibility(); // Re-evaluate ownership buttons when data arrives
                hasHandledDeletion = false;
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

    @Override
    public void onStart() {
        super.onStart();
        ShopSync.LikeSync.register(this);
        ShopSync.FavoriteSync.register(this);
    }

    @Override
    public void onStop() {
        ShopSync.LikeSync.unregister(this);
        ShopSync.FavoriteSync.unregister(this);
        super.onStop();
    }

    @Override
    public void onShopSyncUpdate(String shopId, Bundle payload) {
        ShopModel currentShop = shopViewModel.getShop().getValue();
        if (currentShop != null && shopId.equals(currentShop.getShopId())) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (payload.containsKey("isLiked")) {
                        boolean liked = payload.getBoolean("isLiked");
                        int count = payload.getInt("likesCount", currentShop.getLikesCount());
                        currentShop.setLiked(liked);
                        currentShop.setLikesCount(count);
                        updateLikeUI(liked, count);
                    }
                    if (payload.containsKey("isFavorite")) {
                        boolean fav = payload.getBoolean("isFavorite");
                        currentShop.setFavorite(fav);
                        updateFavoriteUI(fav);
                    }
                });
            }
        }
    }

    private void updateLikeUI(boolean liked, int count) {
        if (btnLike != null) {
            btnLike.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            btnLike.setColorFilter(liked ? Color.parseColor("#E8574D") : Color.GRAY);
        }
        
        TextView likesCountText = rootView != null ? rootView.findViewById(R.id.likesCount) : null;
        if (likesCountText != null) {
            likesCountText.setText(String.valueOf(count));
        }
    }

    private void updateFavoriteUI(boolean fav) {
        if (btnFavorite != null) {
            btnFavorite.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            btnFavorite.setColorFilter(fav ? Color.parseColor("#FFC107") : Color.GRAY);
        }
    }

    private void setupVisitorButtons() {
        if (rootView == null) return;
        btnLike = rootView.findViewById(R.id.btnLike);
        btnFavorite = rootView.findViewById(R.id.btnFavorite);
        visitorActionsLayout = rootView.findViewById(R.id.visitorActionsLayout);

        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                ShopModel shop = shopViewModel.getShop().getValue();
                String currentUserId = shopViewModel.getCurrentUserId();
                
                if (shop != null && currentUserId != null) {
                    boolean wasLiked = shop.isLiked();
                    boolean newLiked = !wasLiked;
                    int newCount = newLiked ? shop.getLikesCount() + 1 : Math.max(0, shop.getLikesCount() - 1);
                    
                    // Optimistic update
                    shop.setLiked(newLiked);
                    shop.setLikesCount(newCount);
                    ShopSync.LikeSync.update(shop.getShopId(), newLiked, newCount);
                    updateLikeUI(newLiked, newCount); // Force local update immediately
                    
                    // Update in Firestore
                    FirebaseFirestore.getInstance().collection("shops").document(shop.getShopId())
                        .update("likesCount", newCount, "likedByUserIds", 
                            newLiked ? com.google.firebase.firestore.FieldValue.arrayUnion(currentUserId) 
                                     : com.google.firebase.firestore.FieldValue.arrayRemove(currentUserId))
                        .addOnFailureListener(e -> {
                            // Rollback on failure
                            if (getContext() != null) {
                                Toast.makeText(getContext(), getString(R.string.error_update_like), Toast.LENGTH_SHORT).show();
                            }
                            shop.setLiked(wasLiked);
                            shop.setLikesCount(shop.getLikesCount() + (wasLiked ? 1 : -1));
                            ShopSync.LikeSync.update(shop.getShopId(), wasLiked, shop.getLikesCount());
                            updateLikeUI(wasLiked, shop.getLikesCount());
                        });
                } else if (currentUserId == null) {
                   Toast.makeText(requireContext(), getString(R.string.login_to_like_shops), Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                ShopModel shop = shopViewModel.getShop().getValue();
                if (shop != null) {
                    boolean isCurrentlyFavorite = shop.isFavorite();
                    if (isCurrentlyFavorite) {
                        // Show confirmation before unfavoriting
                        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.unfavorite_title)
                                .setMessage(getString(R.string.unfavorite_message_format, shop.getName()))
                                .setPositiveButton(R.string.unfavorite_positive, (dialog, which) -> {
                                    performFavoriteToggle(shop, false);
                                })
                                .setNegativeButton(R.string.cancel, null)
                                .show();
                    } else {
                        // Favorite immediately
                        performFavoriteToggle(shop, true);
                    }
                }
            });
        }
    }

    private void performFavoriteToggle(ShopModel shop, boolean newFav) {
        shop.setFavorite(newFav);
        ShopSync.FavoriteSync.update(shop.getShopId(), newFav);
        updateFavoriteUI(newFav);
        
        FavoritesTableRepository repo = FavoritesTableRepository.getInstance(requireActivity().getApplication());
        if (newFav) {
            repo.addShopToFavorites(shop);
        } else {
            repo.removeShopFromFavorites(shop.getShopId());
        }
    }

    private void setupClickListeners() {
        ImageView editShopButton = rootView.findViewById(R.id.editShopButton);
        if (editShopButton != null) {
            editShopButton.setOnClickListener(v -> {
                android.util.Log.d("ShopHomeFragment", "Edit shop button clicked - starting comprehensive data refresh");
                
                // Get current shop for reference
                ShopModel currentShop = shopViewModel.getShop().getValue();
                if (currentShop != null) {
                    android.util.Log.d("ShopHomeFragment", "Current shop data - hasPromotion: " + currentShop.isHasPromotion() + ", hasLivraison: " + currentShop.isHasLivraison());
                
                // Use direct fetch for guaranteed fresh data
                shopViewModel.fetchCurrentShopDirectly();
                android.util.Log.d("ShopHomeFragment", "Refreshing shop data before edit dialog");
                
                // Set up one-time observer for fresh data
                final boolean[] dialogShown = {false};
                shopViewModel.getShop().observe(getViewLifecycleOwner(), freshShop -> {
                    if (freshShop != null && !dialogShown[0]) {
                        android.util.Log.d("ShopHomeFragment", "Fresh shop data received - hasPromotion: " + freshShop.isHasPromotion() + ", hasLivraison: " + freshShop.isHasLivraison());
                        
                        // Only show dialog once
                        dialogShown[0] = true;
                        
                        // Show dialog with confirmed fresh data
                        showEditShopDialog(freshShop);
                    }
                });
                
                // Fallback: show dialog after 3 seconds even if fresh data not received
                new android.os.Handler().postDelayed(() -> {
                    if (!dialogShown[0]) {
                        android.util.Log.w("ShopHomeFragment", "Fallback triggered - showing dialog with current data");
                        dialogShown[0] = true;
                        if (currentShop != null) {
                            showEditShopDialog(currentShop);
                        }
                    }
                }, 3000);
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

        // Initial button visibility setup
        refreshButtonVisibility();
    }

    private void refreshButtonVisibility() {
        if (rootView == null) return;

        Bundle args = getArguments();
        boolean fromSearch = args != null && args.getBoolean("hideDialogs", false);
        boolean fromFavorites = args != null && "favorites".equals(args.getString("source"));
        boolean isOwner = isShopOwner();
        
        // If user is owner, ALWAYS show controls, regardless of where they came from (Search/Favorites)
        boolean shouldHideDialogs = !isOwner;
        
        android.util.Log.d("ShopHomeFragment", "refreshButtonVisibility - isOwner: " + isOwner + 
                          ", fromSearch: " + fromSearch + ", fromFavorites: " + fromFavorites + 
                          ", shouldHide: " + shouldHideDialogs);

        if (shouldHideDialogs) {
            hideDialogElements();
        } else {
            showDialogElements();
        }

        if (visitorActionsLayout != null) {
            // Show visitor actions (Like/Favorite) for everyone to allow owners to test liking
            visitorActionsLayout.setVisibility(View.VISIBLE);
            
            // Initial UI sync
            ShopModel shop = shopViewModel.getShop().getValue();
            if (shop != null) {
                android.app.Application app = requireActivity().getApplication();
                ShopSync.LikeSync.LikeState ls = ShopSync.LikeSync.getState(shop.getShopId(), app);
                boolean liked = ls != null ? ls.isLiked : shop.isLiked();
                int count = ls != null ? ls.count : shop.getLikesCount();
                
                ShopSync.FavoriteSync.FavoriteState fs = ShopSync.FavoriteSync.getState(shop.getShopId(), app);
                boolean fav = fs != null ? fs.isFavorite : shop.isFavorite();
                
                updateLikeUI(liked, count);
                updateFavoriteUI(fav);
            }
        }
    }

    private void showDialogElements() {
        // Show edit and delete buttons
        ImageView editShopButton = rootView.findViewById(R.id.editShopButton);
        ImageView deleteShopButton = rootView.findViewById(R.id.deleteShopButton);
        
        if (editShopButton != null) {
            editShopButton.setVisibility(View.VISIBLE);
        }
        if (deleteShopButton != null) {
            deleteShopButton.setVisibility(View.VISIBLE);
        }

        // Show FAB
        FloatingActionButton addProductFab = rootView.findViewById(R.id.addProductFab);
        if (addProductFab != null) {
            addProductFab.setVisibility(View.VISIBLE);
        }

        // Enable image container clicks
        LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);
        ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);
        
        if (imageContainer != null) {
            imageContainer.setClickable(true);
            imageContainer.setFocusable(true);
        }
        if (shopBannerImage != null) {
            shopBannerImage.setClickable(true);
            shopBannerImage.setFocusable(true);
        }
    }

    private void hideDialogElements() {
        // Hide edit and delete buttons
        ImageView editShopButton = rootView.findViewById(R.id.editShopButton);
        ImageView deleteShopButton = rootView.findViewById(R.id.deleteShopButton);
        
        if (editShopButton != null) {
            editShopButton.setVisibility(View.GONE);
        }
        if (deleteShopButton != null) {
            deleteShopButton.setVisibility(View.GONE);
        }

        // Hide FAB
        FloatingActionButton addProductFab = rootView.findViewById(R.id.addProductFab);
        if (addProductFab != null) {
            addProductFab.setVisibility(View.GONE);
        }

        // Disable image container clicks
        LinearLayout imageContainer = rootView.findViewById(R.id.imageContainer);
        ImageView shopBannerImage = rootView.findViewById(R.id.shopBannerImage);
        
        if (imageContainer != null) {
            imageContainer.setClickable(false);
            imageContainer.setFocusable(false);
        }
        if (shopBannerImage != null) {
            shopBannerImage.setClickable(false);
            shopBannerImage.setFocusable(false);
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

        // Set promotion and delivery badges visibility
        LinearLayout promotionBadge = rootView.findViewById(R.id.promotionBadge);
        LinearLayout deliveryBadge = rootView.findViewById(R.id.deliveryBadge);
        
        if (promotionBadge != null) {
            if (shop.isHasPromotion()) {
                promotionBadge.setVisibility(View.VISIBLE);
            } else {
                promotionBadge.setVisibility(View.GONE);
            }
        }
        
        if (deliveryBadge != null) {
            if (shop.isHasLivraison()) {
                deliveryBadge.setVisibility(View.VISIBLE);
            } else {
                deliveryBadge.setVisibility(View.GONE);
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
                String createdAtString = shop.getCreatedAtString();
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

        android.util.Log.d("ShopHomeFragment", "loadLocationData - cities available: " + (cities != null && !cities.isEmpty()) + 
                          ", regions available: " + (regions != null && !regions.isEmpty()));

        if (cities == null || cities.isEmpty() || regions == null || regions.isEmpty()) {
            // Set loading text while waiting for data
            shopLocation.setText("Loading location...");
            
            // Use single observers to avoid nested observer issues
            shopViewModel.getRegions().observe(getViewLifecycleOwner(), regionsList -> {
                if (regionsList != null && !regionsList.isEmpty()) {
                    shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                        if (citiesList != null && !citiesList.isEmpty()) {
                            android.util.Log.d("ShopHomeFragment", "Both regions and cities loaded, updating location");
                            updateLocationWithData(shopLocation, address, cityId, regionId, hasCityId, hasRegionId, citiesList, regionsList);
                        }
                    });
                }
            });
        } else {
            android.util.Log.d("ShopHomeFragment", "Regions and cities already available, updating location immediately");
            updateLocationWithData(shopLocation, address, cityId, regionId, hasCityId, hasRegionId, cities, regions);
        }
    }

    private void updateLocationWithData(TextView shopLocation, String address, String cityId, String regionId,
                                        boolean hasCityId, boolean hasRegionId, List<CityModel> cities, List<RegionModel> regions) {
        android.util.Log.d("ShopHomeFragment", "updateLocationWithData - cityId: " + cityId + ", regionId: " + regionId);
        
        StringBuilder locationBuilder = new StringBuilder();

        if (address != null && !address.isEmpty()) {
            locationBuilder.append(address);
        }

        String cityName = null;
        if (hasCityId && cities != null) {
            android.util.Log.d("ShopHomeFragment", "Searching for city with ID: " + cityId + " in " + cities.size() + " cities");
            for (CityModel city : cities) {
                if (city.getCityId().equals(cityId)) {
                    cityName = city.getName();
                    android.util.Log.d("ShopHomeFragment", "Found city: " + cityName);
                    break;
                }
            }
            if (cityName == null) {
                android.util.Log.w("ShopHomeFragment", "City not found for ID: " + cityId);
            }
        }

        String regionName = null;
        if (hasRegionId && regions != null) {
            android.util.Log.d("ShopHomeFragment", "Searching for region with ID: " + regionId + " in " + regions.size() + " regions");
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
                        android.util.Log.d("ShopHomeFragment", "Found region: " + regionName);
                        break;
                    }
                }
                
                if (regionName == null) {
                    android.util.Log.w("ShopHomeFragment", "Region not found for ID: " + regionId + " (parsed as: " + regionIntId + ")");
                }
            } catch (NumberFormatException e) {
                android.util.Log.e("ShopHomeFragment", "Error parsing region ID: " + regionId + " - " + e.getMessage());
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
        android.util.Log.d("ShopHomeFragment", "Final location: " + finalLocation);
        
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

        // Switch components for promotion and delivery
        SwitchMaterial switchPromotion = dialogView.findViewById(R.id.switchPromotion);
        SwitchMaterial switchDelivery = dialogView.findViewById(R.id.switchDelivery);

        // Pre-fill existing data
        etShopName.setText(shop.getName());
        etShopDescription.setText(shop.getDescription());
        etShopPhone.setText(shop.getPhone());
        etShopEmail.setText(shop.getEmail());
        etShopAddress.setText(shop.getAddress());
        etShopInstagram.setText(shop.getInstagram());
        etShopFacebook.setText(shop.getFacebook());
        etShopWebsite.setText(shop.getWebsite());

        // Set toggle switch values from shop data
        switchPromotion.setChecked(shop.isHasPromotion());
        switchDelivery.setChecked(shop.isHasLivraison());
        
        // Add listeners to immediately update badges when toggles change
        switchPromotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LinearLayout promotionBadge = rootView.findViewById(R.id.promotionBadge);
            if (promotionBadge != null) {
                promotionBadge.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });
        
        switchDelivery.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LinearLayout deliveryBadge = rootView.findViewById(R.id.deliveryBadge);
            if (deliveryBadge != null) {
                deliveryBadge.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            }
        });

        tvShopId.setText(getString(R.string.id_format, shop.getShopId().substring(0, Math.min(8, shop.getShopId().length()))));

        if (shop.getCreatedAtString() != null) {
            tvCreationDate.setText(formatDate(shop.getCreatedAtTimestamp()));
            tvShopAge.setText(calculateShopAge(shop.getCreatedAtTimestamp()));
        }

        // Populate statistics with real data
        tvProductsCount.setText(String.valueOf(productsUIManager != null ? productsUIManager.getCurrentProductsCount() : 0));
        tvLikesCount.setText(String.valueOf(shop.getLikesCount()));
        tvShopRating.setText(String.format("%.1f", shop.getRating()));

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

        final String[] selectedCategory = {shop.getCategory()};

        setupCategoryDropdown(etShopCategory, categories, selectedCategory);
        if (shop.getCategory() != null) {
            etShopCategory.setText(com.example.soukify.utils.CategoryUtils.getLocalizedCategory(requireContext(), shop.getCategory()), false);
        }

        // Setup working hours and PRE-FILL DATA
        prefillWorkingHoursData(dialogView, shop);

        // Setup checkbox listeners for working hours
        setupWorkingHoursCheckboxes(dialogView);

        setupRegionCityDropdowns(etShopRegion, etShopCity, shop);

        handleShopImage(shop, ivShopPreview, imageContainer);

        AlertDialog dialog = builder.setTitle(getString(R.string.edit_shop_title))
                .setIcon(R.drawable.ic_store)
                .setPositiveButton(getString(R.string.save_changes), (dialogInterface, which) -> {
                    saveShopChanges(dialogView, shop, etShopName, etShopDescription, etShopPhone, etShopEmail,
                            etShopAddress, etShopInstagram, etShopFacebook, etShopWebsite,
                            etShopCategory, etShopRegion, etShopCity, selectedCategory, switchPromotion, switchDelivery);
                })
                .setNegativeButton(getString(R.string.cancel), (dialogInterface, which) -> {
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
            String selectedLocalizedName = (String) parent.getItemAtPosition(position);
            selectedCategory[0] = com.example.soukify.utils.CategoryUtils.getCategoryKey(requireContext(), selectedLocalizedName);
            etShopCategory.setText(selectedLocalizedName, false);
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
                    regionNames.add(region.getLocalizedName());
                }
                ArrayAdapter<String> regionAdapter = new ArrayAdapter<>(requireContext(),
                        R.layout.dropdown_item, R.id.dropdown_text, regionNames);
                etShopRegion.setAdapter(regionAdapter);

                // Set current region
                if (shop.getRegionId() != null) {
                    for (RegionModel region : regionsList) {
                        if (region.getRegionId().equals(shop.getRegionId())) {
                            etShopRegion.setText(region.getLocalizedName(), false);
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

        // Set current city
        if (shop.getCityId() != null) {
            shopViewModel.getCities().observe(getViewLifecycleOwner(), citiesList -> {
                if (citiesList != null) {
                    for (CityModel city : citiesList) {
                        if (city.getCityId().equals(shop.getCityId())) {
                            etShopCity.setText(city.getLocalizedName(), false);
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
                                 AutoCompleteTextView etShopCity, String[] selectedCategory,
                                 SwitchMaterial switchPromotion, SwitchMaterial switchDelivery) {

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
        
        // Save toggle switch values
        shop.setHasPromotion(switchPromotion.isChecked());
        shop.setHasLivraison(switchDelivery.isChecked());

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
        
        // Update the combined location field for proper display in item_shop.xml
        String address = etShopAddress.getText().toString();
        String cityName = etShopCity.getText().toString();
        String regionName = etShopRegion.getText().toString();
        String location = address + ", " + cityName + ", " + regionName;
        shop.setLocation(location);

        shopViewModel.updateShop(shop);

        // Clear the preview URI after saving
        selectedShopImageUri = null;
        
        // Force immediate UI refresh with updated shop data
        updateShopUI(shop);
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
        String[] options = {
            getString(R.string.change_cover_photo_option),
            getString(R.string.remove_cover_photo_option),
            getString(R.string.cancel)
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.edit_cover_photo_dialog_title))
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

                            Toast.makeText(requireContext(), getString(R.string.cover_photo_removed), Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.delete_shop_title)
                .setMessage(getString(R.string.delete_shop_message, shop.getName()))
                .setView(dialogView)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(R.string.delete, null)
                .setNegativeButton(R.string.cancel, (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            shopViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMsg -> {
                if (errorMsg != null) {
                    if (errorMsg.contains("Incorrect password")) {
                        errorText.setText(R.string.error_incorrect_password);
                        errorText.setVisibility(View.VISIBLE);
                    } else if (errorMsg.contains("Shop deletion cancelled")) {
                        errorText.setText(R.string.error_auth_failed);
                        errorText.setVisibility(View.VISIBLE);
                    } else if (errorMsg.contains("No user logged in")) {
                        errorText.setText(R.string.error_not_logged_in);
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
                    errorText.setText(R.string.password_required_error);
                    errorText.setVisibility(View.VISIBLE);
                    return;
                }

                errorText.setVisibility(View.GONE);
                shopViewModel.deleteShop(shop.getShopId(), password);
                Toast.makeText(requireContext(), getString(R.string.verifying_password), Toast.LENGTH_SHORT).show();
            });
        });

        dialog.show();
    }

    private void showShopImageEditDialog(boolean isEditing) {
        String[] options;
        String title;

        if (isEditing) {
            options = new String[]{
                getString(R.string.change_cover_photo_option),
                getString(R.string.remove_cover_photo_option),
                getString(R.string.cancel)
            };
            title = getString(R.string.edit_shop_cover_photo_title);
        } else {
            options = new String[]{
                getString(R.string.add_cover_photo_option),
                getString(R.string.cancel)
            };
            title = getString(R.string.add_shop_cover_photo_title);
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
                            Toast.makeText(requireContext(), getString(R.string.cover_photo_removed), Toast.LENGTH_SHORT).show();
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
            Toast.makeText(requireContext(), getString(R.string.error_open_link), Toast.LENGTH_SHORT).show();
        }
    }

    private void openLinkWithLogging(String link, String platform) {
        Uri uri = buildSocialMediaUrl(link, platform);
        if (uri == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.error_open_link), Toast.LENGTH_SHORT).show();
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

    private void navigateToProductDetail(ProductModel product) {
        if (product == null) {
            android.util.Log.e("ShopHomeFragment", "Cannot navigate to product detail - product is null");
            return;
        }
        
        android.util.Log.d("ShopHomeFragment", "Navigating to product detail for: " + product.getName());
        
        try {
            Bundle args = new Bundle();
            args.putParcelable("product", product);
            
            // Check if we need to pass source and hideDialogs parameters
            Bundle fragmentArgs = getArguments();
            if (fragmentArgs != null) {
                boolean hideDialogs = fragmentArgs.getBoolean("hideDialogs", true);
                args.putBoolean("hideDialogs", hideDialogs);
                
                if (hideDialogs) {
                    args.putString("source", "search");
                    android.util.Log.d("ShopHomeFragment", "Passing hideDialogs=true to product detail");
                } else {
                    android.util.Log.d("ShopHomeFragment", "Passing hideDialogs=false to product detail (from settings)");
                }
            } else {
                // Default to hiding dialogs if no arguments
                args.putBoolean("hideDialogs", true);
                android.util.Log.d("ShopHomeFragment", "No arguments found, defaulting to hideDialogs=true");
            }
            
            // Use the activity's NavController for global navigation
            // This should work regardless of fragment hierarchy
            androidx.navigation.NavController navController = null;
            
            // Try to get NavController from the activity first
            if (getActivity() != null) {
                navController = androidx.navigation.Navigation.findNavController(getActivity(), R.id.nav_host_fragment_activity_main);
            }
            
            // Fallback to view-based navigation
            if (navController == null && getView() != null) {
                navController = androidx.navigation.Navigation.findNavController(getView());
            }
            
            if (navController != null) {
                navController.navigate(R.id.global_action_to_productDetail, args);
                android.util.Log.d("ShopHomeFragment", "Navigation to product detail successful");
            } else {
                android.util.Log.e("ShopHomeFragment", "Could not find NavController");
                Toast.makeText(getContext(), getString(R.string.error_open_product), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            android.util.Log.e("ShopHomeFragment", "Error navigating to product detail", e);
            Toast.makeText(getContext(), getString(R.string.error_open_product), Toast.LENGTH_SHORT).show();
        }
    }
}
