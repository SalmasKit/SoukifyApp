package com.example.soukify.ui.search;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.remote.firebase.FirebaseShopService;
import com.example.soukify.data.repositories.FavoritesRepository;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.ui.chat.ChatActivity;
import com.example.soukify.ui.shop.ShopHomeFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SearchFragment extends Fragment implements ShopAdapter.OnShopClickListener {

    private EditText searchInput;
    private RecyclerView recyclerViewShops;
    private ProgressBar progressBar;
    private TextView textViewVille;

    private LinearLayout catTapis, catFood, catPotterie, catTraditionalWear, catLeatherCrafts, catHerbs,
            catJwellery, catMetal, catDraws, catWood;

    private Button btnPromotions, btnObjectType, btnSortBy, btnTopRated;

    private ShopAdapter shopAdapter;
    private final List<ShopModel> allShops = new ArrayList<>();
    private final List<ShopModel> filteredShops = new ArrayList<>();
    private String selectedCategory = "ALL";
    private String selectedCity = null;

    private FirebaseShopService shopService;
    private FavoritesRepository favoritesRepository;
    private final List<String> favoriteShopIds = new ArrayList<>();

    private ListView suggestionsList;
    private ArrayAdapter<String> suggestionsAdapter;
    private List<String> suggestions = new ArrayList<>();
    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private boolean shopsLoaded = false;
    private boolean favoritesLoaded = false;
    private com.google.firebase.firestore.ListenerRegistration shopsListener;
    private TextView textViewNotFound;

    // Variable de classe pour garder l'état du tri actuel
    private boolean isSortedByRecent = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        FirebaseManager firebaseManager = FirebaseManager.getInstance(requireActivity().getApplication());
        shopService = new FirebaseShopService(firebaseManager.getFirestore());
        favoritesRepository = new FavoritesRepository(requireActivity().getApplication());

        initViews(view);

        if (getArguments() != null) {
            selectedCity = getArguments().getString("selectedCity");
            if (selectedCity != null && textViewVille != null) {
                textViewVille.setText(selectedCity);
            }
        }

        setupRecyclerView();
        setupCategoryListeners();
        setupSearchListener();
        setupFilterListeners();
        setupSuggestions();

        loadFavorites();
        loadShopsFromFirebase();

        return view;
    }

    private void initViews(View view) {
        searchInput = view.findViewById(R.id.search_input);
        recyclerViewShops = view.findViewById(R.id.recycler_products);
        progressBar = view.findViewById(R.id.progress_bar);
        textViewVille = view.findViewById(R.id.textView_ville);

        suggestionsList = view.findViewById(R.id.suggestions_list);
        textViewNotFound = view.findViewById(R.id.textView_not_found);

        catTapis = view.findViewById(R.id.cat_tapis);
        catFood = view.findViewById(R.id.cat_food);
        catPotterie = view.findViewById(R.id.cat_potterie);
        catTraditionalWear = view.findViewById(R.id.cat_traditional_wear);
        catLeatherCrafts = view.findViewById(R.id.cat_leather_crafts);
        catHerbs = view.findViewById(R.id.cat_herbs);
        catJwellery = view.findViewById(R.id.cat_jwellery);
        catMetal = view.findViewById(R.id.cat_metal);
        catDraws = view.findViewById(R.id.cat_draws);
        catWood = view.findViewById(R.id.cat_wood);

        btnPromotions = view.findViewById(R.id.btn_promotions);
        btnObjectType = view.findViewById(R.id.btnLiv);
        btnSortBy = view.findViewById(R.id.btn_sort_by);
        btnTopRated = view.findViewById(R.id.btn_top_rated);

        if (btnObjectType != null) {
            btnObjectType.setOnClickListener(v -> filterByLivraison());
        }
    }

    private void setupRecyclerView() {
        recyclerViewShops.setLayoutManager(new LinearLayoutManager(getContext()));
        shopAdapter = new ShopAdapter(getContext(), filteredShops, this);
        recyclerViewShops.setAdapter(shopAdapter);
    }

    private void resetAllLikes() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("shops")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = 0;
                    for (com.google.firebase.firestore.DocumentSnapshot document : queryDocumentSnapshots) {
                        db.collection("shops")
                                .document(document.getId())
                                .update(
                                        "likesCount", 0,
                                        "likedByUserIds", new ArrayList<>()
                                );
                        count++;
                    }
                    safeToast("Tous les likes ont été réinitialisés (" + count + " boutiques)");
                    loadShopsFromFirebase();
                })
                .addOnFailureListener(e -> {
                    safeToast("Erreur lors de la réinitialisation: " + e.getMessage());
                    android.util.Log.e("ResetLikes", "Erreur: ", e);
                });
    }

    private void loadShopsFromFirebase() {
        showLoading(true);
        shopsLoaded = false;
        shopService.getAllShops().addSnapshotListener((querySnapshot, error) -> {
            // Gérer les erreurs
            if (error != null) {
                showLoading(false);
                safeToast("Erreur lors du chargement des boutiques: " + error.getMessage());
                return;
            }

            // Vérifier que querySnapshot n'est pas null
            if (querySnapshot == null) {
                showLoading(false);
                return;
            }

            // Le reste du code reste IDENTIQUE
            allShops.clear();
            filteredShops.clear();

            for (QueryDocumentSnapshot document : querySnapshot) {
                ShopModel shop = new ShopModel();
                shop.setShopId(document.getId());

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

                // Handle hasLivraison
                if (document.contains("hasLivraison")) {
                    Object v = document.get("hasLivraison");
                    if (v instanceof Boolean) {
                        shop.setHasLivraison((Boolean) v);
                    }
                }

                if (document.contains("createdAt")) {
                    Object createdAtValue = document.get("createdAt");
                    if (createdAtValue instanceof Long) {
                        Long timestamp = (Long) createdAtValue;
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                        shop.setCreatedAt(sdf.format(new java.util.Date(timestamp)));
                    }
                }

                if (document.contains("likesCount")) {
                    Object v = document.get("likesCount");
                    if (v instanceof Number) shop.setLikesCount(((Number) v).intValue());
                }

                if (document.contains("favoritesCount")) {
                    Object v = document.get("favoritesCount");
                    if (v instanceof Number) shop.setFavoritesCount(((Number) v).intValue());
                }

                if (document.contains("hasPromotion")) {
                    Object v = document.get("hasPromotion");
                    if (v instanceof Boolean) shop.setHasPromotion((Boolean) v);
                }

                // 🌟 NOUVEAU : Charger les données de rating
                if (document.contains("rating")) {
                    Object ratingValue = document.get("rating");
                    if (ratingValue instanceof Double) {
                        shop.setRating((Double) ratingValue);
                    } else if (ratingValue instanceof Long) {
                        shop.setRating(((Long) ratingValue).doubleValue());
                    }
                }

                if (document.contains("reviews")) {
                    Object reviewsValue = document.get("reviews");
                    if (reviewsValue instanceof Long) {
                        shop.setReviews(((Long) reviewsValue).intValue());
                    } else if (reviewsValue instanceof Integer) {
                        shop.setReviews((Integer) reviewsValue);
                    }
                }

// 🌟 CRITIQUE : Charger la Map des évaluations par utilisateur
                if (document.contains("userRatings")) {
                    Map<String, Object> userRatingsObj = (Map<String, Object>) document.get("userRatings");
                    if (userRatingsObj != null) {
                        Map<String, Float> userRatings = new java.util.HashMap<>();
                        for (Map.Entry<String, Object> entry : userRatingsObj.entrySet()) {
                            Object value = entry.getValue();
                            if (value instanceof Double) {
                                userRatings.put(entry.getKey(), ((Double) value).floatValue());
                            } else if (value instanceof Long) {
                                userRatings.put(entry.getKey(), ((Long) value).floatValue());
                            } else if (value instanceof Float) {
                                userRatings.put(entry.getKey(), (Float) value);
                            }
                        }
                        shop.setUserRatings(userRatings);
                    }
                }
                allShops.add(shop);
            }

            if (selectedCity != null && !selectedCity.isEmpty()) {
                for (ShopModel shop : allShops) {
                    String shopLocation = safeString(shop.getLocation()).trim();
                    if (shopLocation.equalsIgnoreCase(selectedCity.trim())) {
                        filteredShops.add(shop);
                    }
                }
                if (filteredShops.isEmpty()) {
                    safeToast("Aucune boutique trouvée à " + selectedCity);
                } else {
                    safeToast(filteredShops.size() + " boutique(s) à " + selectedCity);
                }
            } else {
                filteredShops.addAll(allShops);
                if (!shopsLoaded) {
                    safeToast(allShops.size() + " boutique(s) chargée(s)");
                }
            }

            shopAdapter.notifyDataSetChanged();
            showLoading(false);
            shopsLoaded = true;

            if (favoritesLoaded) updateFavoriteStatusForShops();
        });
    }
    private void showError(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadFavorites() {
        if (!FirebaseManager.getInstance(requireActivity().getApplication()).isUserLoggedIn()) {
            return;
        }

        favoritesLoaded = false;
        favoritesRepository.loadFavoriteShops();
        favoritesRepository.getFavoriteShops().observe(getViewLifecycleOwner(), favoriteShops -> {
            favoriteShopIds.clear();
            if (favoriteShops != null) {
                for (ShopModel shop : favoriteShops) {
                    if (shop.getShopId() != null) favoriteShopIds.add(shop.getShopId());
                }
            }
            favoritesLoaded = true;
            if (shopsLoaded) updateFavoriteStatusForShops();
        });

        favoritesRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                android.util.Log.e("SearchFragment", "Favorites error: " + errorMessage);
            }
        });
    }

    private void updateFavoriteStatusForShops() {
        for (ShopModel shop : allShops) {
            boolean isFavorite = shop.getShopId() != null && favoriteShopIds.contains(shop.getShopId());
            shop.setFavorite(isFavorite);
        }

        for (ShopModel shop : filteredShops) {
            boolean isFavorite = shop.getShopId() != null && favoriteShopIds.contains(shop.getShopId());
            shop.setFavorite(isFavorite);
        }

        if (shopAdapter != null) {
            shopAdapter.notifyDataSetChanged();
        }
    }

    private void showLoading(boolean show) {
        if (isAdded()) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerViewShops.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onFavoriteClick(ShopModel shop, int position) {
        if (shop == null) return;

        if (position < 0 || position >= filteredShops.size()) {
            android.util.Log.w("SearchFragment", "Position invalide dans onFavoriteClick: " + position);
            return;
        }

        boolean wasFavorite = shop.isFavorite();
        boolean newFavoriteState = !wasFavorite;
        shop.setFavorite(newFavoriteState);

        ShopModel adapterShop = filteredShops.get(position);
        adapterShop.setFavorite(newFavoriteState);

        if (shopAdapter != null) shopAdapter.notifyItemChanged(position);

        favoritesRepository.toggleFavorite(shop);

        if (newFavoriteState) {
            if (shop.getShopId() != null && !favoriteShopIds.contains(shop.getShopId())) {
                favoriteShopIds.add(shop.getShopId());
            }
        } else {
            if (shop.getShopId() != null) favoriteShopIds.remove(shop.getShopId());
        }

        safeToast(shop.getName() + (newFavoriteState ? " ajouté aux favoris" : " retiré des favoris"));

        android.util.Log.d("SearchFragment", "Favorite toggled - New status: " + newFavoriteState);
    }

    // 🌟 Méthode obligatoire pour le bouton chat
    @Override
    public void onChatClick(ShopModel shop, int position) {
        if (getContext() != null && shop != null) {
            Intent intent = new Intent(getContext(), ChatActivity.class);
            intent.putExtra("shopId", shop.getShopId());
            intent.putExtra("shopName", shop.getName());
            startActivity(intent);
        }
    }


    @Override
    public void onLikeClick(ShopModel shop, int position) {
        if (shop == null) return;

        final String currentUserId = FirebaseManager.getInstance(requireActivity().getApplication()).getCurrentUserId();
        if (currentUserId == null) {
            safeToast("Vous devez être connecté pour liker");
            return;
        }

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shops").document(shop.getShopId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        safeToast("Boutique introuvable");
                        return;
                    }

                    List<String> likedUsersList = (List<String>) documentSnapshot.get("likedByUserIds");
                    if (likedUsersList == null) likedUsersList = new ArrayList<>();
                    final List<String> likedUsers = new ArrayList<>(likedUsersList);

                    Object likesCountObj = documentSnapshot.get("likesCount");
                    final int currentLikesCount = likesCountObj instanceof Number ? ((Number) likesCountObj).intValue() : 0;

                    final boolean alreadyLiked = likedUsers.contains(currentUserId);

                    final List<String> newLikedUsers = new ArrayList<>(likedUsers);
                    final int newLikesCount;
                    final boolean newLikedStatus;

                    if (alreadyLiked) {
                        newLikedUsers.remove(currentUserId);
                        newLikesCount = Math.max(0, currentLikesCount - 1);
                        newLikedStatus = false;
                    } else {
                        newLikedUsers.add(currentUserId);
                        newLikesCount = currentLikesCount + 1;
                        newLikedStatus = true;
                    }

                    shop.setLiked(newLikedStatus);
                    shop.setLikesCount(newLikesCount);
                    shop.setLikedByUserIds(new ArrayList<>(newLikedUsers));
                    
                    // Debug logging
                    Log.d("SearchFragment", "Like status updated - newLikedStatus: " + newLikedStatus + ", newLikesCount: " + newLikesCount);
                    
                    if (shopAdapter != null) {
                        shopAdapter.notifyItemChanged(position);
                        Log.d("SearchFragment", "Notified adapter for position: " + position);
                    }

                    db.collection("shops").document(shop.getShopId())
                            .update(
                                    "likedByUserIds", newLikedUsers,
                                    "likesCount", newLikesCount
                            )
                            .addOnSuccessListener(aVoid -> {
                                for (ShopModel s : allShops) {
                                    if (s.getShopId().equals(shop.getShopId())) {
                                        s.setLiked(newLikedStatus);
                                        s.setLikesCount(newLikesCount);
                                        s.setLikedByUserIds(new ArrayList<>(newLikedUsers));
                                        break;
                                    }
                                }

                                String message = alreadyLiked ?
                                        "Like retiré (" + newLikesCount + " like" + (newLikesCount > 1 ? "s" : "") + ")" :
                                        "Vous avez liké! (" + newLikesCount + " like" + (newLikesCount > 1 ? "s" : "") + ")";
                                safeToast(message);
                            })
                            .addOnFailureListener(e -> {
                                safeToast("Erreur lors de la mise à jour du like");
                            });
                })
                .addOnFailureListener(e -> {
                    safeToast("Erreur lors de la récupération des données");
                });
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
            navController.navigate(R.id.action_navigation_search_to_shopHome, args);
        }
    }

    private void setupCategoryListeners() {
        if (catTapis != null) catTapis.setOnClickListener(v -> filterByCategory(getString(R.string.tapestry), catTapis));
        if (catFood != null) catFood.setOnClickListener(v -> filterByCategory(getString(R.string.food), catFood));
        if (catPotterie != null) catPotterie.setOnClickListener(v -> filterByCategory(getString(R.string.pottery), catPotterie));
        if (catTraditionalWear != null) catTraditionalWear.setOnClickListener(v -> filterByCategory(getString(R.string.traditional_wear), catTraditionalWear));
        if (catLeatherCrafts != null) catLeatherCrafts.setOnClickListener(v -> filterByCategory("Leather Crafts", catLeatherCrafts));
        if (catHerbs != null) catHerbs.setOnClickListener(v -> filterByCategory(getString(R.string.naturalProducts), catHerbs));
        if (catJwellery != null) catJwellery.setOnClickListener(v -> filterByCategory(getString(R.string.jewellery), catJwellery));
        if (catMetal != null) catMetal.setOnClickListener(v -> filterByCategory(getString(R.string.metal), catMetal));
        if (catDraws != null) catDraws.setOnClickListener(v -> filterByCategory(getString(R.string.paintings), catDraws));
        if (catWood != null) catWood.setOnClickListener(v -> filterByCategory(getString(R.string.woodwork), catWood));
    }

    private void filterByCategory(String category, LinearLayout categoryView) {
        if (categoryView == null) return;

        selectedCategory = category;
        filteredShops.clear();

        for (ShopModel shop : allShops) {
            String shopCategory = safeString(shop.getCategory()).trim();
            String targetCategory = category.trim();

            if (shopCategory.equalsIgnoreCase(targetCategory)) {
                filteredShops.add(shop);
            }
        }

        shopAdapter.notifyDataSetChanged();
        highlightCategory(categoryView);

        if (filteredShops.isEmpty()) {
            safeToast("Aucune boutique trouvée pour: " + category);
            android.util.Log.d("SearchFragment", "Catégorie recherchée: '" + category + "'");
            java.util.Set<String> availableCategories = new java.util.HashSet<>();
            for (ShopModel shop : allShops) {
                String cat = safeString(shop.getCategory());
                if (!cat.isEmpty()) availableCategories.add(cat);
            }
            android.util.Log.d("SearchFragment", "Catégories disponibles dans la BD: " + availableCategories);
        } else {
            safeToast(filteredShops.size() + " boutique(s) - " + category);
        }
    }

    private void highlightCategory(LinearLayout selectedCategoryView) {
        if (selectedCategoryView == null) return;
        resetCategoryHighlights();
        selectedCategoryView.setAlpha(1.0f);
        selectedCategoryView.setScaleX(1.03f);
        selectedCategoryView.setScaleY(1.03f);
    }

    private void resetCategoryHighlights() {
        LinearLayout[] categories = {catTapis, catFood, catPotterie, catTraditionalWear, catLeatherCrafts, catHerbs,
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
        if (searchInput == null) return;

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();

                // Afficher les suggestions
                if (query.length() >= 2) { // Au moins 2 caractères
                    showSuggestions(query);
                } else {
                    suggestionsList.setVisibility(View.GONE);
                }

                // Rechercher avec délai
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> safeSearch(query);
                searchHandler.postDelayed(searchRunnable, 300);
            }

            private void showSuggestions(String query) {
                suggestions.clear();
                String lowerQuery = query.toLowerCase().trim();

                // Rechercher dans toutes les boutiques
                for (ShopModel shop : allShops) {
                    String name = safeString(shop.getName()).toLowerCase();
                    String category = safeString(shop.getCategory()).toLowerCase();
                    String location = safeString(shop.getLocation()).toLowerCase();

                    // Ajouter le nom si correspond
                    if (name.contains(lowerQuery) && !suggestions.contains(shop.getName())) {
                        suggestions.add(shop.getName());
                    }

                    // Ajouter la catégorie si correspond
                    if (category.contains(lowerQuery) && !suggestions.contains(shop.getCategory())) {
                        suggestions.add(shop.getCategory());
                    }

                    // Ajouter la localisation si correspond
                    if (location.contains(lowerQuery) && !suggestions.contains(shop.getLocation())) {
                        suggestions.add(shop.getLocation());
                    }

                    // Limiter à 5-6 suggestions
                    if (suggestions.size() >= 6) {
                        break;
                    }
                }

                // Afficher ou cacher la liste
                if (suggestions.isEmpty()) {
                    suggestionsList.setVisibility(View.GONE);
                } else {
                    suggestionsAdapter.notifyDataSetChanged();
                    suggestionsList.setVisibility(View.VISIBLE);
                }
            }


            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Cacher les suggestions quand on clique ailleurs
        searchInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                suggestionsList.setVisibility(View.GONE);
            }
        });
    }

    private void safeSearch(String query) {
        try {
            filteredShops.clear();

            if (query == null || query.trim().isEmpty()) {
                filteredShops.addAll(allShops);
                hideNotFoundMessage(); // Cacher le message
            } else {
                String lowerQuery = query.toLowerCase().trim();
                List<ShopWithScore> scoredShops = new ArrayList<>();

                for (ShopModel shop : allShops) {
                    if (matchShop(shop, lowerQuery)) {
                        int score = calculateRelevanceScore(shop, lowerQuery);
                        scoredShops.add(new ShopWithScore(shop, score));
                    }
                }

                Collections.sort(scoredShops, (s1, s2) ->
                        Integer.compare(s2.score, s1.score)
                );

                for (ShopWithScore shopWithScore : scoredShops) {
                    filteredShops.add(shopWithScore.shop);
                }

                if (filteredShops.isEmpty()) {
                    showNotFoundMessage();
                } else {
                    hideNotFoundMessage();
                }
            }

            if (shopAdapter != null) shopAdapter.notifyDataSetChanged();

        } catch (Exception e) {
            safeToast("Erreur lors de la recherche: " + e.getMessage());
        }
    }
    private void showNotFoundMessage() {
        if (textViewNotFound != null) {
            textViewNotFound.setVisibility(View.VISIBLE);
            recyclerViewShops.setVisibility(View.GONE);
        }
    }

    private void hideNotFoundMessage() {
        if (textViewNotFound != null) {
            textViewNotFound.setVisibility(View.GONE);
            recyclerViewShops.setVisibility(View.VISIBLE);
        }
    }


    public void onShareClick(ShopModel shop, int position) {
        // Exemple : partager le nom et la localisation de la boutique
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String shareText = "Regarde cette boutique : " + shop.getName() + " - " + shop.getLocation();
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Partager via"));
    }

    @Override
    public void onRatingChanged(ShopModel shop, float newRating, int position) {
        // Vérifier que l'utilisateur est connecté
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(getContext(), "Vous devez être connecté pour évaluer", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Log pour déboguer
        android.util.Log.d("RatingDebug", "Shop ID: " + shop.getShopId());
        android.util.Log.d("RatingDebug", "User ID: " + currentUserId);
        android.util.Log.d("RatingDebug", "New Rating: " + newRating);

        // 🌟 IMPORTANT : Récupérer ou créer la Map
        Map<String, Float> userRatings = shop.getUserRatings();
        if (userRatings == null) {
            userRatings = new java.util.HashMap<>();
            shop.setUserRatings(userRatings);
        }

        // Mettre à jour la note de l'utilisateur
        userRatings.put(currentUserId, newRating);

        // Recalculer la moyenne
        shop.calculateAverageRating();

        android.util.Log.d("RatingDebug", "Average Rating: " + shop.getRating());
        android.util.Log.d("RatingDebug", "Total Reviews: " + shop.getReviews());
        android.util.Log.d("RatingDebug", "UserRatings Map: " + userRatings.toString());

        // 🌟 SOLUTION : Créer une copie finale pour utiliser dans le lambda
        final Map<String, Float> finalUserRatings = new java.util.HashMap<>(userRatings);

        // 🌟 CRITIQUE : Préparer les données pour Firebase
        Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("userRatings", finalUserRatings);  // Utiliser la copie finale
        updates.put("rating", shop.getRating());
        updates.put("reviews", shop.getReviews());

        // Mettre à jour Firebase
        FirebaseFirestore.getInstance()
                .collection("shops")
                .document(shop.getShopId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("RatingDebug", "✅ Firebase mise à jour avec succès!");
                    Toast.makeText(getContext(), "Évaluation enregistrée : " + newRating + "⭐", Toast.LENGTH_SHORT).show();

                    // Mettre à jour aussi dans la liste allShops
                    for (ShopModel s : allShops) {
                        if (s.getShopId().equals(shop.getShopId())) {
                            s.setUserRatings(new java.util.HashMap<>(finalUserRatings));
                            s.setRating(shop.getRating());
                            s.setReviews(shop.getReviews());
                            break;
                        }
                    }

                    // Rafraîchir l'adapter
                    if (shopAdapter != null) {
                        shopAdapter.notifyItemChanged(position);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("RatingDebug", "❌ Erreur Firebase: " + e.getMessage());
                    Toast.makeText(getContext(), "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setupFilterListeners() {
        if (btnPromotions != null) btnPromotions.setOnClickListener(v -> filterByPromotions());
        if (btnTopRated != null) btnTopRated.setOnClickListener(v -> filterByTopSearched());
        if (btnSortBy != null) btnSortBy.setOnClickListener(v -> showSortDialog());
    }

    private void filterByPromotions() {
        filteredShops.clear();

        for (ShopModel shop : allShops) {
            if (shop.isHasPromotion()) {
                filteredShops.add(shop);
            }
        }

        if (shopAdapter != null) shopAdapter.notifyDataSetChanged();

        if (filteredShops.isEmpty()) {
            safeToast("Aucune boutique en promotion actuellement");
            android.util.Log.d("SearchFragment", "Aucune boutique avec hasPromotion=true trouvée");
        } else {
            safeToast(filteredShops.size() + " boutique(s) en promotion");
            android.util.Log.d("SearchFragment", filteredShops.size() + " boutiques en promotion trouvées");
        }
    }

    private void filterByTopSearched() {
        filteredShops.clear();
        filteredShops.addAll(allShops);
        Collections.sort(filteredShops, (s1, s2) -> Integer.compare(s2.getSearchCount(), s1.getSearchCount()));
        if (filteredShops.size() > 10) {
            List<ShopModel> top = new ArrayList<>(filteredShops.subList(0, 10));
            filteredShops.clear();
            filteredShops.addAll(top);
        }
        if (shopAdapter != null) shopAdapter.notifyDataSetChanged();
        safeToast("Top " + filteredShops.size() + " boutiques les plus cherchées");
    }

    private void filterByLivraison() {
        filteredShops.clear();

        for (ShopModel shop : allShops) {
            if (shop.hasLivraison()) {
                filteredShops.add(shop);
            }
        }

        if (shopAdapter != null) shopAdapter.notifyDataSetChanged();

        if (filteredShops.isEmpty()) {
            safeToast("Aucune boutique avec livraison disponible");
        } else {
            safeToast(filteredShops.size() + " boutique(s) avec livraison");
        }
    }

    // Méthode appelée lors du clic sur le bouton "Sort"
    private void showSortDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Trier par date");

        String[] options = {"recent", "ancien"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Tri récent → ancien
                sortByDateDescending();
            } else {
                // Tri ancien → récent
                sortByDateAscending();
            }
        });

        builder.setNegativeButton("Annuler", null);
        builder.show();
    }

    // Tri du plus récent au plus ancien
    private void sortByDateDescending() {
        Collections.sort(filteredShops, (s1, s2) -> {
            try {
                // Récupérer les dates
                String d1 = safeString(s1.getCreatedAt());
                String d2 = safeString(s2.getCreatedAt());

                // Si les dates sont vides, les mettre à la fin
                if (d1.isEmpty()) return 1;
                if (d2.isEmpty()) return -1;

                // Parser les dates au format "dd/MM/yyyy HH:mm"
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                java.util.Date date1 = sdf.parse(d1);
                java.util.Date date2 = sdf.parse(d2);

                // Comparer les dates (récent → ancien = date2 avant date1)
                return date2.compareTo(date1);

            } catch (Exception e) {
                // En cas d'erreur de parsing, comparer comme des strings
                android.util.Log.e("SortError", "Erreur tri date: " + e.getMessage());
                String d1 = safeString(s1.getCreatedAt());
                String d2 = safeString(s2.getCreatedAt());
                return d2.compareTo(d1);
            }
        });

        isSortedByRecent = true;

        if (shopAdapter != null) {
            shopAdapter.notifyDataSetChanged();
        }
        safeToast("Trié par : Récent → Ancien");
    }


  // Tri du plus ancien au plus récent (AMÉLIORÉ)
    private void sortByDateAscending() {
        Collections.sort(filteredShops, (s1, s2) -> {
            try {
                // Récupérer les dates
                String d1 = safeString(s1.getCreatedAt());
                String d2 = safeString(s2.getCreatedAt());

                // Si les dates sont vides, les mettre à la fin
                if (d1.isEmpty()) return 1;
                if (d2.isEmpty()) return -1;

                // Parser les dates au format "dd/MM/yyyy HH:mm"
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault());
                java.util.Date date1 = sdf.parse(d1);
                java.util.Date date2 = sdf.parse(d2);

                // Comparer les dates (ancien → récent = date1 avant date2)
                return date1.compareTo(date2);

            } catch (Exception e) {
                // En cas d'erreur de parsing, comparer comme des strings
                android.util.Log.e("SortError", "Erreur tri date: " + e.getMessage());
                String d1 = safeString(s1.getCreatedAt());
                String d2 = safeString(s2.getCreatedAt());
                return d1.compareTo(d2);
            }
        });

        isSortedByRecent = false;

        if (shopAdapter != null) {
            shopAdapter.notifyDataSetChanged();
        }
        safeToast("Trié par : Ancien → Récent");
    }



    private void resetAllFilters() {
        resetCategoryHighlights();
        selectedCategory = "ALL";
        filteredShops.clear();
        filteredShops.addAll(allShops);
        if (shopAdapter != null) shopAdapter.notifyDataSetChanged();
        if (searchInput != null) searchInput.setText("");
        safeToast("Filtres réinitialisés");
    }

    // Méthode helper pour éviter les null
    private String safeString(String str) {
        return str != null ? str : "";
    }

    private void safeToast(String message) {
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show());
        } else {
            android.util.Log.d("SearchFragment", "Toast skipped (fragment not added): " + message);
        }
    }


    private void setupSuggestions() {
        if (suggestionsList == null) {
            android.util.Log.e("SearchFragment", "suggestionsList est null ! Vérifie ton XML.");
            return;
        }

        suggestionsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                suggestions
        );
        suggestionsList.setAdapter(suggestionsAdapter);

        suggestionsList.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSuggestion = suggestions.get(position);
            searchInput.setText(selectedSuggestion);
            searchInput.setSelection(selectedSuggestion.length());
            suggestionsList.setVisibility(View.GONE);
            safeSearch(selectedSuggestion);
        });
    }


    private boolean matchShop(ShopModel shop, String query) {
        String name = safeString(shop.getName()).toLowerCase();
        String category = safeString(shop.getCategory()).toLowerCase();
        String location = safeString(shop.getLocation()).toLowerCase();
        String address = safeString(shop.getAddress()).toLowerCase();

        // Recherche exacte
        if (name.contains(query) || category.contains(query) ||
                location.contains(query) || address.contains(query)) {
            return true;
        }

        // Recherche floue (tolérance aux fautes)
        return fuzzyMatch(name, query);
    }
    private int calculateRelevanceScore(ShopModel shop, String query) {
        int score = 0;
        String name = safeString(shop.getName()).toLowerCase();
        String category = safeString(shop.getCategory()).toLowerCase();
        String location = safeString(shop.getLocation()).toLowerCase();

        // Score plus élevé si le nom commence par la recherche
        if (name.startsWith(query)) {
            score += 100;
        }
        // Score si le nom contient la recherche
        else if (name.contains(query)) {
            score += 50;
        }

        // Score si la catégorie correspond
        if (category.contains(query)) {
            score += 30;
        }

        // Score si la localisation correspond
        if (location.contains(query)) {
            score += 20;
        }

        // Bonus pour les boutiques populaires
        score += shop.getSearchCount() / 10;

        // Bonus pour les boutiques avec promotion
        if (shop.isHasPromotion()) {
            score += 10;
        }

        return score;
    }
    private boolean fuzzyMatch(String text, String query) {
        if (text.length() < query.length()) {
            return false;
        }

        // Tolérance : 1 faute pour query < 5 caractères, 2 fautes sinon
        int maxErrors = query.length() < 5 ? 1 : 2;

        // Parcourir le texte avec une fenêtre glissante
        for (int i = 0; i <= text.length() - query.length(); i++) {
            int errors = 0;

            for (int j = 0; j < query.length(); j++) {
                if (text.charAt(i + j) != query.charAt(j)) {
                    errors++;
                    if (errors > maxErrors) {
                        break;
                    }
                }
            }

            if (errors <= maxErrors) {
                return true;
            }
        }

        return false;
    }
    // Classe helper pour trier par pertinence
    private static class ShopWithScore {
        ShopModel shop;
        int score;

        ShopWithScore(ShopModel shop, int score) {
            this.shop = shop;
            this.score = score;
        }
    }
}