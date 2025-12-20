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
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavoritesFragment extends Fragment implements ShopAdapter.OnShopClickListener {

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
                favoritesTableRepository = new FavoritesTableRepository(requireActivity().getApplication());
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
                Toast.makeText(getContext(), "Erreur lors de l'initialisation", Toast.LENGTH_SHORT).show();
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
        // 1. Récupérer l'ID de l'utilisateur connecté
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 2. Mettre à jour la Map des évaluations
        Map<String, Float> userRatings = shop.getUserRatings();
        userRatings.put(currentUserId, newRating);

        // 3. Recalculer la moyenne
        shop.calculateAverageRating();

        // 4. Mettre à jour dans Firebase
        FirebaseFirestore.getInstance()
                .collection("shops")
                .document(shop.getShopId())
                .update("userRatings", userRatings,
                        "rating", shop.getRating(),
                        "reviews", shop.getReviews())
                .addOnSuccessListener(aVoid -> {
                    // Mise à jour réussie
                    shopAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Évaluation enregistrée !", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Erreur lors de l'évaluation", Toast.LENGTH_SHORT).show();
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
                    if (shops != null) {
                        favoriteShops.clear();
                        favoriteShops.addAll(shops);
                        if (shopAdapter != null) {
                            shopAdapter.notifyDataSetChanged();
                        }

                        if (shops.isEmpty() && getContext() != null) {
                            Toast.makeText(getContext(), "Aucune boutique favorite", Toast.LENGTH_SHORT).show();
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
                            Toast.makeText(getContext(), "No favorite products found", Toast.LENGTH_SHORT).show();
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
        try {
            if (favoritesTableRepository != null) {
                // Toggle favorite status
                favoritesTableRepository.isShopFavorite(shopModel.getShopId()).observe(getViewLifecycleOwner(), isFavorite -> {
                    if (isFavorite != null && isFavorite) {
                        favoritesTableRepository.removeShopFromFavorites(shopModel.getShopId());
                    } else {
                        favoritesTableRepository.addShopToFavorites(shopModel);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling favorite", e);
        }
    }

    @Override
    public void onLikeClick(ShopModel shopModel, int position) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Fonctionnalité Like à implémenter", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShopClick(ShopModel shop, int position) {
        if (isAdded() && shop != null) {
            // Pass shop data as individual strings (ShopModel is not Parcelable)
            Bundle args = new Bundle();
            args.putString("shopId", shop.getShopId() != null ? shop.getShopId() : "");
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
            args.putString("shopCreatedAt", shop.getCreatedAt() != null ? shop.getCreatedAt() : "");
            args.putLong("shopCreatedAtTimestamp", shop.getCreatedAtTimestamp() > 0 ? shop.getCreatedAtTimestamp() : System.currentTimeMillis());
            args.putBoolean("hideDialogs", true); // Flag to hide dialogs and FAB
            
            // Navigate to ShopHomeFragment using Navigation Component
            NavController navController = Navigation.findNavController(requireView());
            navController.navigate(R.id.action_navigation_favorites_to_shopHome, args);
            
            Log.d(TAG, "Navigated to shop: " + shop.getName());
        }
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

    private void showLoading(boolean show) {
        try {
            if (getActivity() != null && progressBar != null) {
                getActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                    
                    if (showingBoutiques) {
                        if (recyclerViewBoutiques != null) {
                            recyclerViewBoutiques.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    } else {
                        if (recyclerViewProducts != null) {
                            recyclerViewProducts.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing loading", e);
        }
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
    }
}
