package com.example.soukify.ui.search;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.exemple.soukify.data.AppDatabase;
import com.exemple.soukify.data.dao.ShopDao;
import com.exemple.soukify.data.entities.Shop;

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
    private List<Shop> allShops = new ArrayList<>();
    private List<Shop> filteredShops = new ArrayList<>();
    private String selectedCategory = "ALL";

    private ShopDao shopDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Initialiser Room
        shopDao = AppDatabase.getInstance(getContext()).shopDao();

        initViews(view);
        setupRecyclerView();
        setupCategoryListeners();
        setupSearchListener();
        setupFilterListeners();

        loadShopsFromRoomWithTestData();

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

    private void loadShopsFromRoomWithTestData() {
        showLoading(true);
        new Thread(() -> {
            if (shopDao.getAllShops().isEmpty()) {
                shopDao.insert(new Shop("ext1", "La Potterie de Safae", "POTTERIE", 5, 120, "Oujda", "", false, System.currentTimeMillis(), 0, 0, 0));
                shopDao.insert(new Shop("ext2", "Épices Traditionnelles", "FOOD", 4, 95, "Marrakech", "", true, System.currentTimeMillis(), 0, 0, 0));
                shopDao.insert(new Shop("ext3", "Tapis du Maroc", "TAPIS", 5, 80, "Fès", "", false, System.currentTimeMillis(), 0, 0, 0));
            }

            List<Shop> shopsFromDb = shopDao.getAllShops();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allShops.clear();
                    allShops.addAll(shopsFromDb);

                    filteredShops.clear();
                    filteredShops.addAll(allShops);

                    shopAdapter.notifyDataSetChanged();
                    showLoading(false);

                    Toast.makeText(getContext(), allShops.size() + " boutique(s) chargée(s)", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewShops.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onFavoriteClick(Shop shop, int position) {
        // Ne PAS inverser shop.isFavorite ici
        new Thread(() -> shopDao.update(shop)).start();

        // Juste mettre à jour l’UI si nécessaire
        shopAdapter.notifyItemChanged(position);
    }
    @Override
    public void onLikeClick(Shop shop, int position) {
        new Thread(() -> {
            shop.setLiked(!shop.isLiked());
            if (shop.isLiked()) shop.setLikesCount(shop.getLikesCount() + 1);
            else shop.setLikesCount(shop.getLikesCount() - 1);
            shopDao.update(shop); // mettre à jour Room
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> shopAdapter.notifyItemChanged(position));
            }
        }).start();
    }

    @Override
    public void onShopClick(Shop shop, int position) {
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
        for (Shop shop : allShops) {
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
                for (Shop shop : allShops) {
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
        for (Shop shop : allShops) if (shop.isHasPromotion()) filteredShops.add(shop);
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
        Collections.sort(filteredShops, (s1, s2) -> Long.compare(s2.getCreatedAt(), s1.getCreatedAt()));
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
