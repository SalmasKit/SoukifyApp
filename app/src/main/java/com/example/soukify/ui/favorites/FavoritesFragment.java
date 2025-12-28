package com.example.soukify.ui.favorites;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.ui.chat.ChatActivity;
import com.example.soukify.ui.search.ShopAdapter;
import com.example.soukify.ui.shop.CleanProductsAdapter;
import com.example.soukify.ui.shop.ProductViewModel;
import com.example.soukify.data.repositories.FavoritesTableRepository;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.ProductModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.lifecycle.ViewModelProvider;
import android.os.Handler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.example.soukify.data.sync.ShopSync;
import com.example.soukify.data.sync.ProductSync;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class FavoritesFragment extends Fragment implements ShopAdapter.OnShopClickListener, ShopSync.SyncListener, ProductSync.SyncListener {

    private static final String TAG = "FavoritesFragment";

    // Views
    private RecyclerView recyclerViewBoutiques;
    private RecyclerView recyclerViewProducts;
    private ProgressBar progressBar;
    private MaterialButton btnBoutiques;
    private MaterialButton btnProducts;

    // Adapters
    private ShopAdapter shopAdapter;
    private CleanProductsAdapter productAdapter;
    private List<ShopModel> favoriteShops = new ArrayList<>();
    private List<ProductModel> favoriteProducts = new ArrayList<>();

    // Repository
    private FavoritesTableRepository favoritesTableRepository;
    private UserProductPreferencesRepository userPreferences;
    private ProductViewModel productViewModel;

    // Current selection
    private boolean showingBoutiques = true;
    
    // Flag to prevent state loading during user interactions
    private boolean isUserInteracting = false;

    // Realtime listeners for like updates per shop
    private final Map<String, ListenerRegistration> shopLikeListeners = new HashMap<>();
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        try {
            // Initialize views
            initViews(view);

            // Setup RecyclerViews first (important)
            setupRecyclerViews();

            // Setup buttons AFTER RecyclerViews to avoid NPE in selectBoutiques()
            setupButtons();

            // Initialize FavoritesRepository
            if (getContext() != null && isAdded()) {
                favoritesTableRepository = FavoritesTableRepository.getInstance(requireActivity().getApplication());
                currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
                userPreferences = new UserProductPreferencesRepository(requireContext());
                productViewModel = new ViewModelProvider(requireActivity(), new ProductViewModel.Factory(requireActivity().getApplication())).get(ProductViewModel.class);
                productViewModel.setupObservers(getViewLifecycleOwner());
                observeViewModel();
                loadFavoriteShops();
            } else {
                Log.e(TAG, "Context is null or fragment not added in onCreateView");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            if (getActivity() != null && getContext() != null) {
                Toast.makeText(getContext(), R.string.init_error, Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    private void initViews(View view) {
        try {
            recyclerViewBoutiques = view.findViewById(R.id.recycler_favorites);
            recyclerViewProducts = view.findViewById(R.id.recycler_favorites_products);
            progressBar = view.findViewById(R.id.progress_bar_favorites);
            btnBoutiques = view.findViewById(R.id.btn_boutiques);
            btnProducts = view.findViewById(R.id.btn_products);

            // Vérifier que toutes les vues sont trouvées
            if (recyclerViewBoutiques == null) {
                Log.e(TAG, "recycler_favorites not found (check fragment_favorites.xml)");
            }
            if (recyclerViewProducts == null) {
                Log.e(TAG, "recycler_favorites_products not found (check fragment_favorites.xml)");
            }
            if (btnBoutiques == null) {
                Log.e(TAG, "btn_boutiques not found (check fragment_favorites.xml)");
            }
            if (btnProducts == null) {
                Log.e(TAG, "btn_products not found (check fragment_favorites.xml)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
        }
    }

    private void setupButtons() {
        if (btnBoutiques == null || btnProducts == null) {
            Log.e(TAG, "Buttons are null, cannot setup");
            return;
        }

        // Par défaut, boutiques est sélectionné
        // Use the safer select method which checks views
        selectBoutiques();

        btnBoutiques.setOnClickListener(v -> selectBoutiques());
        btnProducts.setOnClickListener(v -> selectProducts());
    }

    @Override
    public void onStart() {
        super.onStart();
        ShopSync.LikeSync.register(this);
        ShopSync.FavoriteSync.register(this);
        ProductSync.LikeSync.register(this);
        ProductSync.FavoriteSync.register(this);
    }

    @Override
    public void onStop() {
        ShopSync.LikeSync.unregister(this);
        ShopSync.FavoriteSync.unregister(this);
        ProductSync.LikeSync.unregister(this);
        ProductSync.FavoriteSync.unregister(this);
        super.onStop();
    }

    @Override
    public void onShopSyncUpdate(String shopId, Bundle payload) {
        if (shopId == null || favoriteShops == null || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            for (int i = 0; i < favoriteShops.size(); i++) {
                if (shopId.equals(favoriteShops.get(i).getShopId())) {
                    if (payload.containsKey("isFavorite") && !payload.getBoolean("isFavorite")) {
                        favoriteShops.remove(i);
                        if (shopAdapter != null) shopAdapter.notifyItemRemoved(i);
                    } else {
                        if (shopAdapter != null) shopAdapter.notifyItemChanged(i, payload);
                    }
                    break;
                }
            }
        });
    }

    @Override
    public void onProductSyncUpdate(String productId, Bundle payload) {
        if (productId == null || favoriteProducts == null || getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            for (int i = 0; i < favoriteProducts.size(); i++) {
                if (productId.equals(favoriteProducts.get(i).getProductId())) {
                    if (payload.containsKey("isFavorite") && !payload.getBoolean("isFavorite")) {
                        favoriteProducts.remove(i);
                        if (productAdapter != null) {
                            productAdapter.updateProducts(new ArrayList<>(favoriteProducts));
                        }
                    } else {
                        if (productAdapter != null) productAdapter.notifyItemChanged(i, payload);
                    }
                    break;
                }
            }
        });
    }

    private void setupRecyclerViews() {
        try {
            // Setup RecyclerView pour boutiques
            if (recyclerViewBoutiques != null && getContext() != null) {
                recyclerViewBoutiques.setLayoutManager(new LinearLayoutManager(getContext()));
                // Create the ShopAdapter with the favorite shops list and this fragment as listener
                shopAdapter = new ShopAdapter(getContext(), favoriteShops, this);
                recyclerViewBoutiques.setAdapter(shopAdapter);
            }

            // Setup RecyclerView pour produits
            if (recyclerViewProducts != null && getContext() != null) {
                recyclerViewProducts.setLayoutManager(new GridLayoutManager(getContext(), 2));
                productAdapter = new CleanProductsAdapter(getContext(), new CleanProductsAdapter.OnProductClickListener() {
                    @Override
                    public void onProductClick(ProductModel product) {
                        // Navigate to product detail
                        navigateToProductDetail(product);
                    }

                    @Override
                    public void onProductLongClick(ProductModel product) {
                        // Handle product long click
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Long pressed: " + product.getName(), Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFavoriteClick(ProductModel product, int position) {
                        showUnfavoriteConfirmation(product, position);
                    }
                }, productViewModel, true); // true = favorites context
                
                // Set lifecycle owner for proper observer management
                productAdapter.setLifecycleOwner(getViewLifecycleOwner());
                recyclerViewProducts.setAdapter(productAdapter);
            } else {
                Log.w(TAG, "recyclerViewProducts is null or context is null");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up RecyclerViews", e);
        }
    }

    private void selectBoutiques() {
        // Check buttons first
        if (btnBoutiques == null || btnProducts == null) {
            Log.e(TAG, "selectBoutiques(): one of the buttons is null, aborting selection");
            return;
        }

        showingBoutiques = true;

        // Boutiques = sélectionné, Produits = non sélectionné
        btnBoutiques.setSelected(true);
        btnProducts.setSelected(false);

        // Safe visibility changes
        if (recyclerViewBoutiques != null) {
            recyclerViewBoutiques.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "recyclerViewBoutiques is null in selectBoutiques()");
        }
        if (recyclerViewProducts != null) {
            recyclerViewProducts.setVisibility(View.GONE);
        } // else it's fine, products list not present yet

        loadFavoriteShops();
    }

    private void selectProducts() {
        if (btnBoutiques == null || btnProducts == null) {
            Log.e(TAG, "selectProducts(): one of the buttons is null, aborting selection");
            return;
        }

        showingBoutiques = false;

        // Produits = sélectionné, Boutiques = non sélectionné
        btnProducts.setSelected(true);
        btnBoutiques.setSelected(false);

        if (recyclerViewBoutiques != null) {
            recyclerViewBoutiques.setVisibility(View.GONE);
        }
        if (recyclerViewProducts != null) {
            recyclerViewProducts.setVisibility(View.VISIBLE);
        }

        loadFavoriteProducts();
    }

    public void onShareClick(ShopModel shop, int position) {
        // Ici tu peux ajouter le code pour partager la boutique, par exemple :
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = "Regarde cette boutique : " + shop.getName() + " - " + shop.getLocation();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Partager via"));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (showingBoutiques) {
            loadFavoriteShops();
        } else {
            loadFavoriteProducts();
        }
    }

    private void loadFavoriteShops() {
        try {
            if (favoritesTableRepository != null) {
                favoritesTableRepository.loadFavoriteShops();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorite shops", e);
        }
    }

    @Override
    public void onRatingChanged(ShopModel shop, float newRating, int position) {
        if (shop == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Mettre à jour la Map des évaluations
        Map<String, Float> userRatings = shop.getUserRatings();
        if (userRatings == null) {
            userRatings = new java.util.HashMap<>();
            shop.setUserRatings(userRatings);
        }
        
        userRatings.put(currentUserId, newRating);

        // Recalculer la moyenne et mettre à jour le nombre d'avis
        shop.calculateAverageRating();

        // Préparer les mises à jour pour Firebase
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("userRatings", userRatings);
        updates.put("rating", shop.getRating());
        updates.put("reviews", shop.getReviews());

        // Mettre à jour dans Firebase
        FirebaseFirestore.getInstance()
                .collection("shops")
                .document(shop.getShopId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && shopAdapter != null) {
                        shopAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), R.string.rating_saved, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), R.string.rating_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadFavoriteProducts() {
        try {
            if (favoritesTableRepository != null) {
                favoritesTableRepository.loadFavoriteProducts();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading favorite products", e);
        }
    }

    private void navigateToProductDetail(ProductModel product) {
        try {
            // Use Navigation Component for proper navigation
            if (getContext() instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = 
                    (androidx.fragment.app.FragmentActivity) getContext();
                
                androidx.navigation.NavController navController = 
                    androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
                
                // Create bundle with product data
                Bundle bundle = new Bundle();
                bundle.putParcelable("product", product);
                
                // Navigate using global navigation action
                navController.navigate(R.id.global_action_to_productDetail, bundle);
                
                Log.d(TAG, "Navigated to product detail for: " + product.getName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to product detail: " + e.getMessage(), e);
        }
    }

    private void observeViewModel() {
        if (favoritesTableRepository == null || getViewLifecycleOwner() == null) {
            Log.e(TAG, "Cannot observe ViewModel - repository or lifecycle owner is null");
            return;
        }

        try {
            // Observe favorite shops
            favoritesTableRepository.getFavoriteShops().observe(getViewLifecycleOwner(), shops -> {
                try {
                    Log.d(TAG, "=== FAVORITE SHOPS OBSERVER TRIGGERED ===");
                    Log.d(TAG, "Shops received: " + (shops != null ? shops.size() : 0));
                    Log.d(TAG, "isUserInteracting flag: " + isUserInteracting);
                    
                    if (shops != null) {
                        // Load like and favorite states for each shop with a small delay to avoid conflicts
                        new Handler().postDelayed(() -> {
                            Log.d(TAG, "Loading like/favorite states after delay...");
                            loadLikeAndFavoriteStatesForShops(shops);
                        }, 500); // 500ms delay to allow Firestore operations to complete
                        
                        favoriteShops.clear();
                        favoriteShops.addAll(shops);
                        if (shopAdapter != null) {
                            shopAdapter.notifyDataSetChanged();
                        }

                        if (shops.isEmpty() && getContext() != null) {
                            Toast.makeText(getContext(), R.string.no_favorite_shops, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in shops observer", e);
                }
            });

            // Observe favorite products
            favoritesTableRepository.getFavoriteProducts().observe(getViewLifecycleOwner(), products -> {
                try {
                    if (products != null) {
                        favoriteProducts.clear();
                        favoriteProducts.addAll(products);
                        if (productAdapter != null) {
                            productAdapter.updateProducts(favoriteProducts);
                        }

                        if (products.isEmpty() && getContext() != null) {
                            Toast.makeText(getContext(), R.string.no_favorite_products, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in products observer", e);
                }
            });

            // Observe loading state
            favoritesTableRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                try {
                    showLoading(isLoading != null && isLoading);
                } catch (Exception e) {
                    Log.e(TAG, "Error in loading observer", e);
                }
            });

            // Observe errors
            favoritesTableRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
                try {
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        showError(errorMessage);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error in error observer", e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up observers", e);
        }
    }

    @Override
    public void onFavoriteClick(ShopModel shopModel, int position) {
        showUnfavoriteConfirmation(shopModel, position);
    }

    private void showUnfavoriteConfirmation(ShopModel shop, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.unfavorite_title)
                .setMessage(getString(R.string.unfavorite_message_format, shop.getName()))
                .setPositiveButton(R.string.unfavorite_positive, (dialog, which) -> {
                    if (favoritesTableRepository != null) {
                        favoritesTableRepository.removeShopFromFavorites(shop.getShopId());
                        ShopSync.FavoriteSync.update(shop.getShopId(), false);
                        // Optimization: Local removal is handled by onShopSyncUpdate if it works correctly,
                        // but we can also do it here for even faster feel.
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showUnfavoriteConfirmation(ProductModel product, int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.unfavorite_title)
                .setMessage(getString(R.string.unfavorite_message_format, product.getName()))
                .setPositiveButton(R.string.unfavorite_positive, (dialog, which) -> {
                    if (productViewModel != null) {
                        productViewModel.toggleFavoriteProduct(product.getProductId());
                        ProductSync.FavoriteSync.update(product.getProductId(), false);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public void onShopClick(ShopModel shop) {
        try {
            if (shop == null) {
                Log.e(TAG, "Shop model is null");
                return;
            }

            Log.d(TAG, "Shop clicked: " + shop.getName());
            
            // Create bundle with shop data
            Bundle args = new Bundle();
            args.putString("shopId", shop.getShopId());
            args.putString("shopName", shop.getName());
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
            args.putBoolean("hideDialogs", true); // Flag to hide dialogs and FAB
            
            // Navigate to ShopHomeFragment using Navigation Component
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_navigation_favorites_to_shopHome, args);
            
            Log.d(TAG, "Navigated to shop: " + shop.getName());
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to shop detail", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.shop_open_error, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onLikeClick(ShopModel shopModel, int position) {
        Log.d(TAG, "=== ON LIKE CLICK DEBUG ===");
        Log.d(TAG, "ShopModel: " + (shopModel != null ? shopModel.getName() : "NULL"));
        Log.d(TAG, "Position: " + position);
        Log.d(TAG, "Current liked status: " + (shopModel != null ? shopModel.isLiked() : "NULL"));
        Log.d(TAG, "Current likes count: " + (shopModel != null ? shopModel.getLikesCount() : "NULL"));
        
        if (shopModel == null) {
            Log.e(TAG, "ShopModel is NULL - cannot process like");
            return;
        }

        // Set user interaction flag to prevent state loading conflicts
        isUserInteracting = true;
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "CurrentUserId: " + currentUserId);
        
        if (currentUserId == null) {
            Log.e(TAG, "User not authenticated - cannot like shop");
            isUserInteracting = false;
            if (getContext() != null) {
                Toast.makeText(getContext(), R.string.login_required_like, Toast.LENGTH_SHORT).show();
            }
            return;
        }

        boolean newLikedStatus = !shopModel.isLiked();
        int newLikesCount = newLikedStatus ? shopModel.getLikesCount() + 1 : Math.max(0, shopModel.getLikesCount() - 1);
        
        Log.d(TAG, "New liked status: " + newLikedStatus);
        Log.d(TAG, "New likes count: " + newLikesCount);

        // Update model
        shopModel.setLiked(newLikedStatus);
        shopModel.setLikesCount(newLikesCount);

        // ✅ Global synchronization for immediate UI update in other fragments
        ShopSync.LikeSync.update(shopModel.getShopId(), newLikedStatus, newLikesCount);

        // Update UI immediately
        if (shopAdapter != null) {
            Log.d(TAG, "Updating UI - notifying adapter");
            shopAdapter.notifyItemChanged(position);
        } else {
            Log.e(TAG, "ShopAdapter is NULL - cannot update UI");
        }

        // Prepare likedByUserIds list
        ArrayList<String> likedByUserIds = shopModel.getLikedByUserIds();
        if (likedByUserIds == null) {
            likedByUserIds = new ArrayList<>();
        }

        final ArrayList<String> newLikedUsers = new ArrayList<>(likedByUserIds);
        if (newLikedStatus && !newLikedUsers.contains(currentUserId)) {
            newLikedUsers.add(currentUserId);
        } else if (!newLikedStatus && newLikedUsers.contains(currentUserId)) {
            newLikedUsers.remove(currentUserId);
        }

        Log.d(TAG, "Updating Firestore with likedByUserIds: " + newLikedUsers.size() + " users");
        
        shopModel.setLikedByUserIds(newLikedUsers);

        // Update Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shops").document(shopModel.getShopId())
                .update(
                        "likedByUserIds", newLikedUsers,
                        "likesCount", newLikesCount
                )
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Firestore update successful for shop: " + shopModel.getName());
                    
                    // Update favoriteShops list for consistency
                    for (ShopModel s : favoriteShops) {
                        if (s.getShopId().equals(shopModel.getShopId())) {
                            s.setLiked(newLikedStatus);
                            s.setLikesCount(newLikesCount);
                            s.setLikedByUserIds(new ArrayList<>(newLikedUsers));
                            break;
                        }
                    }

                    String message = newLikedStatus ?
                            "Vous avez liké! (" + newLikesCount + " like" + (newLikesCount > 1 ? "s" : "") + ")" :
                            "Like retiré (" + newLikesCount + " like" + (newLikesCount > 1 ? "s" : "") + ")";
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    
                    // Reset user interaction flag after a delay to allow Firestore sync
                    new Handler().postDelayed(() -> {
                        isUserInteracting = false;
                        Log.d(TAG, "User interaction flag reset");
                    }, 1000);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Firestore update failed for shop: " + shopModel.getName(), e);
                    
                    // Revert changes on error
                    shopModel.setLiked(!newLikedStatus);
                    shopModel.setLikesCount(shopModel.getLikesCount() + (newLikedStatus ? -1 : 1));

                    // Restore likedByUserIds list
                    ArrayList<String> revertedList = new ArrayList<>(newLikedUsers);
                    if (newLikedStatus) {
                        revertedList.remove(currentUserId);
                    } else {
                        if (!revertedList.contains(currentUserId)) {
                            revertedList.add(currentUserId);
                        }
                    }
                    shopModel.setLikedByUserIds(revertedList);

                    // Update UI to revert
                    if (shopAdapter != null) {
                        shopAdapter.notifyItemChanged(position);
                    }

                    if (getContext() != null) {
                Toast.makeText(getContext(), R.string.like_update_error, Toast.LENGTH_SHORT).show();
            }
                    
                    // Reset user interaction flag on error
                    isUserInteracting = false;
                });
        
        Log.d(TAG, "=== END ON LIKE CLICK DEBUG ===");
    }

    // 🌟 Nouvelle méthode pour ouvrir le chat
    @Override
    public void onChatClick(ShopModel shop, int position) {
        if (getContext() != null && shop != null) {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("shopId", shop.getShopId());
            intent.putExtra("shopName", shop.getName());
            startActivity(intent);
        }
    }

    
    private void loadLikeAndFavoriteStatesForShops(List<ShopModel> shops) {
        if (shops == null || shops.isEmpty()) return;
        
        Log.d(TAG, "=== LOAD LIKE AND FAVORITE STATES STARTED ===");
        Log.d(TAG, "Number of shops to process: " + shops.size());
        Log.d(TAG, "isUserInteracting flag: " + isUserInteracting);
        
        // Skip loading if user is currently interacting to prevent conflicts
        if (isUserInteracting) {
            Log.d(TAG, "User is interacting, skipping state loading to prevent conflicts");
            return;
        }
        
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (currentUserId == null) return;
        
        Log.d(TAG, "Current user ID: " + currentUserId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Ajouter un listener en temps réel pour synchroniser les likes
        setupRealtimeLikeListener(currentUserId);
        
        for (ShopModel shop : shops) {
            Log.d(TAG, "Processing shop: " + shop.getName() + " (ID: " + shop.getShopId() + ")");
            
            // Set favorite state to true since this shop is in favorites
            shop.setFavorite(true);
            
            if (shop.getShopId() == null) continue;
            
            Log.d(TAG, "Current shop state before Firestore query - liked: " + shop.isLiked() + ", likesCount: " + shop.getLikesCount());
            
            db.collection("shops").document(shop.getShopId()).get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "Firestore document received for shop: " + shop.getName());
                    
                    if (document.exists()) {
                        // Store current state to avoid unnecessary updates
                        boolean currentLikedState = shop.isLiked();
                        int currentLikesCount = shop.getLikesCount();
                        
                        Log.d(TAG, "Current local state - liked: " + currentLikedState + ", likesCount: " + currentLikesCount);
                        
                        // Load like state
                        if (document.contains("likedByUserIds")) {
                            Object likedByUserIdsObj = document.get("likedByUserIds");
                            if (likedByUserIdsObj instanceof List) {
                                ArrayList<String> likedByUserIds = new ArrayList<>((List<String>) likedByUserIdsObj);
                                shop.setLikedByUserIds(likedByUserIds);
                                
                                // Check if current user liked this shop
                                boolean isLiked = likedByUserIds.contains(currentUserId);
                                
                                Log.d(TAG, "Firestore liked state: " + isLiked + " (user in likedByUserIds: " + likedByUserIds.contains(currentUserId) + ")");
                                
                                // Only update if the state is different to avoid conflicts
                                if (shop.isLiked() != isLiked) {
                                    Log.d(TAG, "STATE DIFFERENCE DETECTED - Local: " + shop.isLiked() + ", Firestore: " + isLiked);
                                    shop.setLiked(isLiked);
                                    Log.d(TAG, "Updated shop " + shop.getName() + " like state from Firestore: " + isLiked);
                                } else {
                                    Log.d(TAG, "Like states match, no update needed");
                                }
                            } else {
                                Log.d(TAG, "likedByUserIds is not a List, resetting to false");
                                shop.setLikedByUserIds(new ArrayList<>());
                                if (shop.isLiked() != false) {
                                    shop.setLiked(false);
                                }
                            }
                        } else {
                            Log.d(TAG, "likedByUserIds field not found, setting to false");
                            shop.setLikedByUserIds(new ArrayList<>());
                            if (shop.isLiked() != false) {
                                shop.setLiked(false);
                            }
                        }
                        
                        // Load likes count
                        if (document.contains("likesCount")) {
                            Object v = document.get("likesCount");
                            if (v instanceof Number) {
                                int firestoreLikesCount = ((Number) v).intValue();
                                Log.d(TAG, "Firestore likes count: " + firestoreLikesCount);
                                
                                // Only update if different to avoid conflicts
                                if (shop.getLikesCount() != firestoreLikesCount) {
                                    Log.d(TAG, "LIKES COUNT DIFFERENCE DETECTED - Local: " + shop.getLikesCount() + ", Firestore: " + firestoreLikesCount);
                                    shop.setLikesCount(firestoreLikesCount);
                                    Log.d(TAG, "Updated shop " + shop.getName() + " likes count from Firestore: " + firestoreLikesCount);
                                } else {
                                    Log.d(TAG, "Likes counts match, no update needed");
                                }
                            }
                        }
                        
                        // Load favorites count
                        if (document.contains("favoritesCount")) {
                            Object v = document.get("favoritesCount");
                            if (v instanceof Number) {
                                shop.setFavoritesCount(((Number) v).intValue());
                            }
                        }
                        
                        // Only update UI if state actually changed and user is not interacting
                        boolean stateChanged = (shop.isLiked() != currentLikedState) || 
                                             (shop.getLikesCount() != currentLikesCount);
                        
                        Log.d(TAG, "State changed: " + stateChanged + ", isUserInteracting: " + isUserInteracting);
                        
                        if (stateChanged && shopAdapter != null && favoriteShops.contains(shop) && !isUserInteracting) {
                            int position = favoriteShops.indexOf(shop);
                            Log.d(TAG, "UI state changed for shop " + shop.getName() + ", updating adapter at position " + position);
                            shopAdapter.notifyItemChanged(position);
                        } else {
                            Log.d(TAG, "No UI update needed - stateChanged: " + stateChanged + ", shopAdapter: " + (shopAdapter != null) + ", containsShop: " + favoriteShops.contains(shop) + ", isUserInteracting: " + isUserInteracting);
                        }
                    } else {
                        Log.w(TAG, "Firestore document does not exist for shop: " + shop.getName());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading like/favorite state for shop " + shop.getShopId(), e);
                });
        }
        
        Log.d(TAG, "=== LOAD LIKE AND FAVORITE STATES COMPLETED ===");
    }

    private void showError(String message) {
        try {
            if (getActivity() != null && getContext() != null) {
                getActivity().runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing error message", e);
        }
    }

    // Show/hide loading state and manage list visibility consistently
    private void showLoading(boolean loading) {
        try {
            if (!isAdded()) return;

            if (progressBar != null) {
                progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
            }

            // When loading, hide both lists. When done, restore the selected one
            if (recyclerViewBoutiques != null) {
                recyclerViewBoutiques.setVisibility(loading ? View.GONE : (showingBoutiques ? View.VISIBLE : View.GONE));
            }
            if (recyclerViewProducts != null) {
                recyclerViewProducts.setVisibility(loading ? View.GONE : (!showingBoutiques ? View.VISIBLE : View.GONE));
            }

            // Optionally disable the toggle buttons during loading
            if (btnBoutiques != null) btnBoutiques.setEnabled(!loading);
            if (btnProducts != null) btnProducts.setEnabled(!loading);
        } catch (Exception e) {
            Log.e(TAG, "Error updating loading state", e);
        }
    }

    // Setup a realtime listener per favorite shop to keep likes in sync
    private void setupRealtimeLikeListener(String currentUserId) {
        try {
            if (currentUserId == null) return;
            if (favoriteShops == null) return;

            // Remove listeners for shops no longer in favorites
            for (String id : new ArrayList<>(shopLikeListeners.keySet())) {
                boolean stillFavorite = false;
                for (ShopModel s : favoriteShops) {
                    if (id != null && id.equals(s.getShopId())) { stillFavorite = true; break; }
                }
                if (!stillFavorite) {
                    ListenerRegistration reg = shopLikeListeners.remove(id);
                    if (reg != null) reg.remove();
                }
            }

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            // Add listeners for current favorite shops if missing
            for (int i = 0; i < favoriteShops.size(); i++) {
                ShopModel shop = favoriteShops.get(i);
                if (shop == null) continue;
                String id = shop.getShopId();
                if (id == null || shopLikeListeners.containsKey(id)) continue;

                ListenerRegistration reg = db.collection("shops").document(id)
                        .addSnapshotListener((snapshot, e) -> {
                            if (e != null || snapshot == null || !snapshot.exists()) return;
                            try {
                                Object likedObj = snapshot.get("likedByUserIds");
                                ArrayList<String> likedBy = new ArrayList<>();
                                if (likedObj instanceof List) {
                                    //noinspection unchecked
                                    likedBy = new ArrayList<>((List<String>) likedObj);
                                }
                                Object countObj = snapshot.get("likesCount");
                                int serverCount = (countObj instanceof Number) ? ((Number) countObj).intValue() : likedBy.size();
                                boolean serverLiked = likedBy.contains(currentUserId);

                                // Update the local model if present
                                for (int idx = 0; idx < favoriteShops.size(); idx++) {
                                    ShopModel s = favoriteShops.get(idx);
                                    if (s != null && id.equals(s.getShopId())) {
                                        boolean changed = (s.isLiked() != serverLiked) || (s.getLikesCount() != serverCount);
                                        s.setLikedByUserIds(likedBy);
                                        s.setLiked(serverLiked);
                                        s.setLikesCount(serverCount);
                                        if (changed && shopAdapter != null && !isUserInteracting) {
                                            shopAdapter.notifyItemChanged(idx);
                                        }
                                        break;
                                    }
                                }
                            } catch (Exception ex) {
                                Log.e(TAG, "Error handling realtime like update", ex);
                            }
                        });
                shopLikeListeners.put(id, reg);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up realtime like listeners", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nettoyer les références pour éviter les fuites mémoire
        recyclerViewBoutiques = null;
        recyclerViewProducts = null;
        progressBar = null;
        btnBoutiques = null;
        btnProducts = null;
        shopAdapter = null;
        productAdapter = null;

        // Remove Firestore listeners to prevent leaks
        try {
            if (shopLikeListeners != null && !shopLikeListeners.isEmpty()) {
                for (ListenerRegistration reg : shopLikeListeners.values()) {
                    if (reg != null) reg.remove();
                }
                shopLikeListeners.clear();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error cleaning up listeners", e);
        }
    }
}
