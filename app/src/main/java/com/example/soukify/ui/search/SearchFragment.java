package com.example.soukify.ui.search;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseShopService;
import com.example.soukify.data.repositories.FavoritesRepository;
import com.example.soukify.data.models.ShopModel;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment implements ShopAdapter.OnShopClickListener {

    private EditText searchInput;
    private RecyclerView recyclerViewShops;
    private ProgressBar progressBar;

    private LinearLayout catTapis, catFood, catPotterie, catHerbs,
            catJwellery, catMetal, catDraws, catWood;

    private Button btnPromotions, btnObjectType, btnSortBy, btnTopRated;

    private ShopAdapter shopAdapter;
    private List<ShopModel> allShops = new ArrayList<>();
    private List<ShopModel> filteredShops = new ArrayList<>();
    private String selectedCategory = "ALL";

    private FirebaseShopService shopService;
    private FavoritesRepository favoritesRepository;
    private List<String> favoriteShopIds = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialiser Firebase
        FirebaseManager firebaseManager = FirebaseManager.getInstance(requireActivity().getApplication());
        shopService = new FirebaseShopService(firebaseManager.getFirestore());
        favoritesRepository = new FavoritesRepository(requireActivity().getApplication());

        initViews(view);
        setupRecyclerView();
        setupCategoryListeners();
        setupSearchListener();
        setupFilterListeners();

        loadShopsFromFirebase();
        loadFavorites();

        return view;
    }

    private void initViews(View view) {
        searchInput = view.findViewById(R.id.search_input);
        recyclerViewShops = view.findViewById(R.id.recycler_products);
        progressBar = view.findViewById(R.id.progress_bar);

        catTapis = view.findViewById(R.id.cat_tapis);
        catFood = view.findViewById(R.id.cat_food);
        catPotterie = view.findViewById(R.id.cat_potterie);
        catHerbs = view.findViewById(R.id.cat_herbs);
        catJwellery = view.findViewById(R.id.cat_jwellery);
        catMetal = view.findViewById(R.id.cat_metal);
        catDraws = view.findViewById(R.id.cat_draws);
        catWood = view.findViewById(R.id.cat_wood);

        btnPromotions = view.findViewById(R.id.btn_promotions);
        btnObjectType = view.findViewById(R.id.btn_object_type);
        btnSortBy = view.findViewById(R.id.btn_sort_by);
        btnTopRated = view.findViewById(R.id.btn_top_rated);
    }

    private void setupRecyclerView() {
        recyclerViewShops.setLayoutManager(new LinearLayoutManager(getContext()));
        shopAdapter = new ShopAdapter(getContext(), filteredShops, this);
        recyclerViewShops.setAdapter(shopAdapter);
    }

    private void loadShopsFromFirebase() {
        showLoading(true);
        
        shopService.getAllShops().get()
                .addOnSuccessListener(querySnapshot -> {
                    allShops.clear();
                    filteredShops.clear();
                    
                    for (QueryDocumentSnapshot document : querySnapshot) {
                        // Handle both old (Long) and new (String) createdAt formats
                        ShopModel shop = new ShopModel();
                        shop.setShopId(document.getId());
                        
                        // Set basic fields
                        if (document.contains("name")) {
                            shop.setName(document.getString("name"));
                        }
                        if (document.contains("category")) {
                            shop.setCategory(document.getString("category"));
                        }
                        if (document.contains("location")) {
                            shop.setLocation(document.getString("location"));
                        }
                        if (document.contains("imageUrl")) {
                            String imageUrl = document.getString("imageUrl");
                            shop.setImageUrl(imageUrl);
                            android.util.Log.d("SearchFragment", "Shop: " + shop.getName() + ", imageUrl: " + imageUrl);
                        } else {
                            android.util.Log.d("SearchFragment", "Shop: " + shop.getName() + " has no imageUrl field");
                        }
                        if (document.contains("userId")) {
                            shop.setUserId(document.getString("userId"));
                        }
                        if (document.contains("phone")) {
                            shop.setPhone(document.getString("phone"));
                        }
                        if (document.contains("email")) {
                            shop.setEmail(document.getString("email"));
                        }
                        if (document.contains("address")) {
                            shop.setAddress(document.getString("address"));
                        }
                        if (document.contains("regionId")) {
                            shop.setRegionId(document.getString("regionId"));
                        }
                        if (document.contains("cityId")) {
                            shop.setCityId(document.getString("cityId"));
                        }
                        
                        // Handle createdAt - convert Long to String if needed
                        if (document.contains("createdAt")) {
                            Object createdAtValue = document.get("createdAt");
                            if (createdAtValue instanceof Long) {
                                // Convert old timestamp to formatted string
                                Long timestamp = (Long) createdAtValue;
                                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                                shop.setCreatedAt(sdf.format(new java.util.Date(timestamp)));
                            } else if (createdAtValue instanceof String) {
                                // Use new string format directly
                                shop.setCreatedAt((String) createdAtValue);
                            }
                        }
                        
                        allShops.add(shop);
                    }
                    
                    // Debug logging
                    android.util.Log.d("SearchFragment", "Loaded " + allShops.size() + " shops from Firebase");
                    
                    // Force all shops to start as non-favorite since favorites aren't loading
                    for (ShopModel shop : allShops) {
                        shop.setFavorite(false);
                        android.util.Log.d("SearchFragment", "Set " + shop.getName() + " favorite to false");
                    }
                    
                    filteredShops.addAll(allShops);
                    shopAdapter.notifyDataSetChanged();
                    showLoading(false);
                    Toast.makeText(getContext(), allShops.size() + " boutique(s) chargée(s)", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    showError("Erreur lors du chargement des boutiques: " + e.getMessage());
                });
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadFavorites() {
        android.util.Log.d("SearchFragment", "Starting to load favorites...");
        
        // Check if user is authenticated
        if (!FirebaseManager.getInstance(requireActivity().getApplication()).isUserLoggedIn()) {
            android.util.Log.d("SearchFragment", "User not authenticated - cannot load favorites");
            return;
        }
        
        favoritesRepository.loadFavoriteShops();
        favoritesRepository.getFavoriteShops().observe(getViewLifecycleOwner(), favoriteShops -> {
            android.util.Log.d("SearchFragment", "Favorites observer triggered with " + favoriteShops.size() + " shops");
            favoriteShopIds.clear();
            for (ShopModel shop : favoriteShops) {
                favoriteShopIds.add(shop.getShopId());
            }
            // Debug logging
            android.util.Log.d("SearchFragment", "Loaded " + favoriteShopIds.size() + " favorite shops: " + favoriteShopIds.toString());
            // Update favorite status for all shops
            updateFavoriteStatusForShops();
        });
        
        // Also observe errors
        favoritesRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null) {
                android.util.Log.e("SearchFragment", "Favorites error: " + errorMessage);
            }
        });
    }

    private void updateFavoriteStatusForShops() {
        android.util.Log.d("SearchFragment", "Updating favorite status for " + allShops.size() + " shops...");
        for (ShopModel shop : allShops) {
            boolean isFavorite = favoriteShopIds.contains(shop.getShopId());
            shop.setFavorite(isFavorite);
            android.util.Log.d("SearchFragment", "Shop: " + shop.getName() + " -> Favorite: " + isFavorite + " (ID: " + shop.getShopId() + ")");
        }
        
        // Also update filtered shops
        for (ShopModel shop : filteredShops) {
            boolean isFavorite = favoriteShopIds.contains(shop.getShopId());
            shop.setFavorite(isFavorite);
            android.util.Log.d("SearchFragment", "Filtered shop: " + shop.getName() + " -> Favorite: " + isFavorite);
        }
        
        shopAdapter.notifyDataSetChanged();
        android.util.Log.d("SearchFragment", "Favorite status updated and adapter notified");
        
        // Verify the status after update
        for (int i = 0; i < filteredShops.size(); i++) {
            ShopModel shop = filteredShops.get(i);
            android.util.Log.d("SearchFragment", "After notify - Shop " + i + ": " + shop.getName() + " -> Favorite: " + shop.isFavorite());
        }
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewShops.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onFavoriteClick(ShopModel shop, int position) {
        // Debug logging - check actual shop object
        android.util.Log.d("SearchFragment", "Favorite clicked - Shop: " + shop.getName() + 
                          ", Current favorite status: " + shop.isFavorite() + 
                          ", Shop object hash: " + shop.hashCode());
        
        // Also check the adapter's version
        ShopModel adapterShop = filteredShops.get(position);
        android.util.Log.d("SearchFragment", "Adapter shop - Favorite: " + adapterShop.isFavorite() + 
                          ", Hash: " + adapterShop.hashCode());
        
        // Toggle favorite status
        boolean wasFavorite = shop.isFavorite();
        boolean newFavoriteState = !wasFavorite;
        shop.setFavorite(newFavoriteState);
        
        // Also update the adapter's list
        adapterShop.setFavorite(newFavoriteState);
        
        // Update UI immediately
        shopAdapter.notifyItemChanged(position);
        
        // Update favorite status in Firebase using new repository
        favoritesRepository.toggleFavorite(shop);
        
        // Show success message
        if (newFavoriteState) {
            Toast.makeText(getContext(), shop.getName() + " ajouté aux favoris", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), shop.getName() + " retiré des favoris", Toast.LENGTH_SHORT).show();
        }
        
        android.util.Log.d("SearchFragment", "Favorite toggled - New status: " + newFavoriteState);
    }

    @Override
    public void onLikeClick(ShopModel shop, int position) {
        // Update UI immediately for better responsiveness
        boolean wasLiked = shop.isLiked();
        boolean newLikedState = !wasLiked;
        shop.setLiked(newLikedState);
        shop.setLikesCount(wasLiked ? shop.getLikesCount() - 1 : shop.getLikesCount() + 1);
        shopAdapter.notifyItemChanged(position);
        
        // Update like count in Firebase (not favorite)
        shopService.updateShop(shop.getShopId(), shop)
                .addOnSuccessListener(aVoid -> {
                    // Already updated UI
                    if (newLikedState) {
                        Toast.makeText(getContext(), shop.getName() + " ajouté aux favoris", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), shop.getName() + " retiré des favoris", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    // Revert UI changes if update fails
                    shop.setLiked(wasLiked);
                    shop.setLikesCount(wasLiked ? shop.getLikesCount() + 1 : shop.getLikesCount() - 1);
                    shopAdapter.notifyItemChanged(position);
                    Toast.makeText(getContext(), "Erreur lors de la mise à jour du favori", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onShopClick(ShopModel shop, int position) {
        Toast.makeText(getContext(), "Boutique sélectionnée : " + shop.getName(), Toast.LENGTH_SHORT).show();
    }

    // --- catégories et filtres (inchangés) ---
    private void setupCategoryListeners() {
        if (catTapis != null) catTapis.setOnClickListener(v -> filterByCategory("TAPIS", catTapis));
        if (catFood != null) catFood.setOnClickListener(v -> filterByCategory("FOOD", catFood));
        if (catPotterie != null) catPotterie.setOnClickListener(v -> filterByCategory("POTTERIE", catPotterie));
        if (catHerbs != null) catHerbs.setOnClickListener(v -> filterByCategory("HERBS", catHerbs));
        if (catJwellery != null) catJwellery.setOnClickListener(v -> filterByCategory("JWELLERY", catJwellery));
        if (catMetal != null) catMetal.setOnClickListener(v -> filterByCategory("METAL", catMetal));
        if (catDraws != null) catDraws.setOnClickListener(v -> filterByCategory("DRAWS", catDraws));
        if (catWood != null) catWood.setOnClickListener(v -> filterByCategory("WOOD", catWood));
    }

    private void filterByCategory(String category, LinearLayout categoryView) {
        if (categoryView == null) return;
        selectedCategory = category;
        filteredShops.clear();
        for (ShopModel shop : allShops) {
            if (shop.getCategory().equalsIgnoreCase(category)) filteredShops.add(shop);
        }
        shopAdapter.notifyDataSetChanged();
        highlightCategory(categoryView);
        if (filteredShops.isEmpty()) {
            Toast.makeText(getContext(), "Aucune boutique dans cette catégorie", Toast.LENGTH_SHORT).show();
        }
    }

    private void highlightCategory(LinearLayout selectedCategoryView) {
        if (selectedCategoryView == null) return;
        resetCategoryHighlights();
        selectedCategoryView.setAlpha(1.0f);
        selectedCategoryView.setScaleX(1.15f);
        selectedCategoryView.setScaleY(1.15f);
    }

    private void resetCategoryHighlights() {
        LinearLayout[] categories = {catTapis, catFood, catPotterie, catHerbs,
                catJwellery, catMetal, catDraws, catWood};
        for (LinearLayout category : categories) {
            if (category != null) {
                category.setAlpha(0.7f);
                category.setScaleX(1.0f);
                category.setScaleY(1.0f);
            }
        }
    }

    private void setupSearchListener() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { safeSearch(s.toString()); }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void safeSearch(String query) {
        try {
            filteredShops.clear();
            if (query.isEmpty()) filteredShops.addAll(allShops);
            else {
                String lowerQuery = query.toLowerCase();
                for (ShopModel shop : allShops) {
                    if (shop.getName().toLowerCase().contains(lowerQuery) ||
                            shop.getCategory().toLowerCase().contains(lowerQuery) ||
                            shop.getLocation().toLowerCase().contains(lowerQuery)) {
                        filteredShops.add(shop);
                    }
                }
            }
            shopAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            Toast.makeText(getContext(), "Erreur lors de la recherche: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupFilterListeners() {
        if (btnPromotions != null) btnPromotions.setOnClickListener(v -> filterByPromotions());
        if (btnTopRated != null) btnTopRated.setOnClickListener(v -> filterByTopSearched());
        if (btnObjectType != null) btnObjectType.setOnClickListener(v -> resetAllFilters());
        if (btnSortBy != null) btnSortBy.setOnClickListener(v -> sortByDateDescending());
    }

    private void filterByPromotions() {
        filteredShops.clear();
        for (ShopModel shop : allShops) if (shop.isHasPromotion()) filteredShops.add(shop);
        shopAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), filteredShops.size() + " boutique(s) en promotion", Toast.LENGTH_SHORT).show();
    }

    private void filterByTopSearched() {
        filteredShops.clear();
        filteredShops.addAll(allShops);
        Collections.sort(filteredShops, (s1, s2) -> Integer.compare(s2.getSearchCount(), s1.getSearchCount()));
        if (filteredShops.size() > 10) filteredShops = filteredShops.subList(0, 10);
        shopAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Top " + filteredShops.size() + " boutiques les plus cherchées", Toast.LENGTH_SHORT).show();
    }

    private void sortByDateDescending() {
        Collections.sort(filteredShops, (s1, s2) -> s2.getCreatedAt().compareTo(s1.getCreatedAt()));
        shopAdapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Trié par : Récent", Toast.LENGTH_SHORT).show();
    }

    private void resetAllFilters() {
        resetCategoryHighlights();
        selectedCategory = "ALL";
        filteredShops.clear();
        filteredShops.addAll(allShops);
        shopAdapter.notifyDataSetChanged();
        if (searchInput != null) searchInput.setText("");
        Toast.makeText(getContext(), "Filtres réinitialisés", Toast.LENGTH_SHORT).show();
    }
}
