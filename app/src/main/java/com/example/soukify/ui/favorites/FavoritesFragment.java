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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.ui.chat.ChatActivity;
import com.example.soukify.ui.search.ShopAdapter;
import com.example.soukify.ui.shop.CleanProductsAdapter;
import com.example.soukify.data.repositories.FavoritesRepository;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.models.ProductModel;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private FavoritesRepository favoritesRepository;
    private UserProductPreferencesRepository userPreferences;

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
                favoritesRepository = new FavoritesRepository(requireActivity().getApplication());
                userPreferences = new UserProductPreferencesRepository(requireContext());
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
                recyclerViewProducts.setLayoutManager(new LinearLayoutManager(getContext()));
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
                });
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
            if (favoritesRepository != null) {
                favoritesRepository.loadFavoriteShops();
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
            String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
            
            if (currentUserId == null) {
                Log.w(TAG, "User not logged in, cannot load favorite products");
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Please login to see favorite products", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            showLoading(true);
            
            // Get user's favorited product IDs from SharedPreferences
            java.util.Set<String> favoritedProductIds = userPreferences.getFavoritedProducts();
            
            if (favoritedProductIds.isEmpty()) {
                Log.d(TAG, "No favorited products found");
                favoriteProducts.clear();
                if (productAdapter != null) {
                    productAdapter.updateProducts(favoriteProducts);
                }
                if (getContext() != null) {
                    Toast.makeText(getContext(), "No favorite products found", Toast.LENGTH_SHORT).show();
                }
                showLoading(false);
                return;
            }
            
            // Query products that are in user's favorites list
            FirebaseFirestore.getInstance()
                .collection("products")
                .whereIn("productId", new ArrayList<>(favoritedProductIds))
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    try {
                        favoriteProducts.clear();
                        
                        for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                            ProductModel product = document.toObject(ProductModel.class);
                            if (product != null) {
                                product.setProductId(document.getId());
                                
                                // Set favorited status based on user preferences
                                product.setFavorited(userPreferences.isProductFavorited(product.getProductId()));
                                favoriteProducts.add(product);
                            }
                        }
                        
                        if (productAdapter != null) {
                            productAdapter.updateProducts(favoriteProducts);
                        }
                        
                        if (getContext() != null) {
                            if (favoriteProducts.isEmpty()) {
                                Toast.makeText(getContext(), "No favorite products found", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getContext(), favoriteProducts.size() + " favorite products loaded", Toast.LENGTH_SHORT).show();
                            }
                        }
                        
                        Log.d(TAG, "Loaded " + favoriteProducts.size() + " favorite products");
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing favorite products", e);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error loading favorite products", Toast.LENGTH_SHORT).show();
                        }
                    } finally {
                        showLoading(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading favorite products", e);
                    showLoading(false);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to load favorite products", Toast.LENGTH_SHORT).show();
                    }
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error in loadFavoriteProducts", e);
            showLoading(false);
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
        if (favoritesRepository == null || getViewLifecycleOwner() == null) {
            Log.e(TAG, "Cannot observe ViewModel - repository or lifecycle owner is null");
            return;
        }

        try {
            // Observe favorite shops
            favoritesRepository.getFavoriteShops().observe(getViewLifecycleOwner(), shops -> {
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

            // Observe loading state
            favoritesRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
                try {
                    showLoading(isLoading != null && isLoading);
                } catch (Exception e) {
                    Log.e(TAG, "Error in loading observer", e);
                }
            });

            // Observe errors
            favoritesRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
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
            if (favoritesRepository != null) {
                favoritesRepository.toggleFavorite(shopModel);
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
    public void onShopClick(ShopModel shopModel, int position) {
        if (getContext() != null && shopModel != null) {
            Toast.makeText(getContext(), "Sélectionné: " + shopModel.getName(), Toast.LENGTH_SHORT).show();
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
