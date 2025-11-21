package com.example.soukify.ui.favorites;

import android.os.Bundle;
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
import com.exemple.soukify.data.AppDatabase;
import com.exemple.soukify.data.dao.ShopDao;
import com.exemple.soukify.data.entities.Shop;

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

        recyclerViewFavorites = view.findViewById(R.id.recycler_favorites);
        progressBar = view.findViewById(R.id.progress_bar_favorites);

        recyclerViewFavorites.setLayoutManager(new LinearLayoutManager(getContext()));
        shopAdapter = new ShopAdapter(getContext(), favoriteShops, this);
        recyclerViewFavorites.setAdapter(shopAdapter);

        shopDao = AppDatabase.getInstance(getContext()).shopDao();

        loadFavoriteShops();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavoriteShops(); // Recharge la liste à jour à chaque affichage du fragment
    }

    private void loadFavoriteShops() {
        showLoading(true);
        new Thread(() -> {
            List<Shop> allShops = shopDao.getAllShops();
            List<Shop> filteredFavorites = new ArrayList<>();
            for (Shop shop : allShops) {
                if (shop.isFavorite()) filteredFavorites.add(shop);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    favoriteShops.clear();
                    favoriteShops.addAll(filteredFavorites);
                    shopAdapter.notifyDataSetChanged();
                    showLoading(false);
                });
            }
        }).start();
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerViewFavorites.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onShopClick(Shop shop, int position) {
        Toast.makeText(getContext(), "Boutique sélectionnée : " + shop.getName(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFavoriteClick(Shop shop, int position) {
        // Mettre à jour la base Room (dans un thread séparé)
        new Thread(() -> shopDao.update(shop)).start();

        // Si retiré des favoris, supprimer de la liste locale
        if (!shop.isFavorite()) {
            favoriteShops.remove(position);
            shopAdapter.notifyItemRemoved(position);
        }
    }


    @Override
    public void onLikeClick(Shop shop, int position) {
        shop.setLiked(!shop.isLiked());
        if (shop.isLiked()) shop.setLikesCount(shop.getLikesCount() + 1);
        else shop.setLikesCount(shop.getLikesCount() - 1);

        // Mettre à jour dans Room
        new Thread(() -> shopDao.update(shop)).start();

        shopAdapter.notifyItemChanged(position);
    }
}

