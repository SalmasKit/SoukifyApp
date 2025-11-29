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
import com.example.soukify.data.repositories.FavoritesRepository;
import com.example.soukify.data.models.ShopModel;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment implements ShopAdapter.OnShopClickListener {

    private RecyclerView recyclerViewFavorites;
    private ProgressBar progressBar;
    private ShopAdapter shopAdapter;
    private List<ShopModel> favoriteShops = new ArrayList<>();
    private FavoritesRepository favoritesRepository;

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

            // Initialize FavoritesRepository
            if (getContext() != null) {
                favoritesRepository = new FavoritesRepository(requireActivity().getApplication());
                loadFavoriteShops();
                observeViewModel();
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
        if (favoritesRepository != null) {
            favoritesRepository.loadFavoriteShops();
        }
    }
    
    private void observeViewModel() {
        if (favoritesRepository == null) return;
        
        // Observe favorite shops
        favoritesRepository.getFavoriteShops().observe(getViewLifecycleOwner(), shops -> {
            if (shops != null) {
                favoriteShops.clear();
                favoriteShops.addAll(shops);
                shopAdapter.notifyDataSetChanged();
                
                if (shops.isEmpty()) {
                    Toast.makeText(getContext(), "No favorite shops found", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Observe loading state
        favoritesRepository.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            showLoading(isLoading != null && isLoading);
        });
        
        // Observe errors
        favoritesRepository.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                showError(errorMessage);
            }
        });
    }

    @Override
    public void onFavoriteClick(ShopModel shopModel, int position) {
        // Update via repository using new method
        if (favoritesRepository != null) {
            favoritesRepository.toggleFavorite(shopModel);
        }
    }

    @Override
    public void onLikeClick(ShopModel shopModel, int position) {
        // Like functionality not handled by FavoritesRepository anymore
        // This would be handled by a separate ShopRepository or similar
        if (getContext() != null) {
            Toast.makeText(getContext(), "Like functionality not implemented", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShopClick(ShopModel shopModel, int position) {
        if (getContext() != null) {
            Toast.makeText(getContext(), "Selected: " + shopModel.getName(), Toast.LENGTH_SHORT).show();
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
