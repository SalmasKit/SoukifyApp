package com.example.soukify.ui.favorites;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.ui.search.ShopAdapter;
import com.example.soukify.data.AppDatabase;
import com.example.soukify.data.dao.ShopDao;
import com.example.soukify.data.entities.Shop;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment implements ShopAdapter.OnShopClickListener {

    private RecyclerView recyclerViewFavorites;
    private ProgressBar progressBar;
    private ShopAdapter shopAdapter;
    private List<Shop> favoriteShops = new ArrayList<>();
    private ShopDao shopDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorites, container, false);

        try {
            // Initialize views
            recyclerViewFavorites = view.findViewById(R.id.recycler_favorites);
            progressBar = view.findViewById(R.id.progress_bar_favorites);

            // Setup RecyclerView
            recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
            shopAdapter = new ShopAdapter(getContext(), favoriteShops, this);
            recyclerViewFavorites.setAdapter(shopAdapter);

            // Initialize database
            if (getContext() != null) {
                shopDao = AppDatabase.getInstance(getContext()).shopDao();
                loadFavoriteShops();
            } else {
                Log.e("FavoritesFragment", "Context is null in onCreateView");
            }
        } catch (Exception e) {
            Log.e("FavoritesFragment", "Error in onCreateView", e);
            if (getActivity() != null) {
                Toast.makeText(getActivity(), "Error initializing favorites", Toast.LENGTH_SHORT).show();
            }
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteShops();
    }

    private void loadFavoriteShops() {
        if (getActivity() == null || getContext() == null) {
            Log.e("FavoritesFragment", "Activity or context is null in loadFavoriteShops");
            return;
        }

        showLoading(true);
        Log.d("FavoritesFragment", "Loading favorite shops...");

        new Thread(() -> {
            try {
                // Get all shops from database
                List<Shop> allShops;
                try {
                    allShops = shopDao.getAllShops();
                    Log.d("FavoritesFragment", "Total shops in database: " + allShops.size());
                } catch (Exception e) {
                    Log.e("FavoritesFragment", "Error reading from database", e);
                    showError("Error loading favorites");
                    return;
                }

                // Filter favorites
                List<Shop> filteredFavorites = new ArrayList<>();
                for (Shop shop : allShops) {
                    if (shop.isFavorite()) {
                        filteredFavorites.add(shop);
                    }
                }
                Log.d("FavoritesFragment", "Found " + filteredFavorites.size() + " favorite shops");

                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        favoriteShops.clear();
                        favoriteShops.addAll(filteredFavorites);
                        shopAdapter.notifyDataSetChanged();
                        showLoading(false);

                        if (filteredFavorites.isEmpty()) {
                            Toast.makeText(getContext(), "No favorite shops found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e("FavoritesFragment", "Error updating UI", e);
                        showError("Error displaying favorites");
                    }
                });

            } catch (Exception e) {
                Log.e("FavoritesFragment", "Unexpected error", e);
                showError("An error occurred");
            }
        }).start();
    }

    @Override
    public void onFavoriteClick(Shop shop, int position) {
        // Update UI immediately for better responsiveness
        boolean newFavoriteState = !shop.isFavorite();
        shop.setFavorite(newFavoriteState);
        shopAdapter.notifyItemChanged(position);

        // Update in database on background thread
        new Thread(() -> {
            try {
                shopDao.update(shop);
                
                // If removed from favorites, update the list
                if (!newFavoriteState && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        favoriteShops.remove(position);
                        shopAdapter.notifyItemRemoved(position);
                        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                Log.e("FavoritesFragment", "Error updating favorite status", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Revert UI changes if update fails
                        shop.setFavorite(!newFavoriteState);
                        shopAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Error updating favorite", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    @Override
    public void onLikeClick(Shop shop, int position) {
        // Update UI immediately
        boolean newLikedState = !shop.isLiked();
        int newLikesCount = newLikedState ? shop.getLikesCount() + 1 : shop.getLikesCount() - 1;
        
        shop.setLiked(newLikedState);
        shop.setLikesCount(newLikesCount);
        shopAdapter.notifyItemChanged(position);

        // Update in database on background thread
        new Thread(() -> {
            try {
                shopDao.update(shop);
            } catch (Exception e) {
                Log.e("FavoritesFragment", "Error updating like status", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Revert UI changes if update fails
                        shop.setLiked(!newLikedState);
                        shop.setLikesCount(newLikedState ? shop.getLikesCount() - 1 : shop.getLikesCount() + 1);
                        shopAdapter.notifyItemChanged(position);
                        Toast.makeText(getContext(), "Error updating like", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    @Override
    public void onShopClick(Shop shop, int position) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Selected: " + shop.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showLoading(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
                recyclerViewFavorites.setVisibility(show ? View.GONE : View.VISIBLE);
            });
        }
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                showLoading(false);
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}