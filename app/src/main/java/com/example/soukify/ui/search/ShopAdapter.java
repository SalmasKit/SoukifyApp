package com.example.soukify.ui.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.entities.Shop;
import com.bumptech.glide.Glide;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private Context context;
    private List<Shop> shopList;
    private OnShopClickListener listener;

    public ShopAdapter(Context context, List<Shop> shopList, OnShopClickListener listener) {
        this.context = context;
        this.shopList = shopList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        Shop shop = shopList.get(position);

        // Nom, catégorie, localisation
        holder.nameText.setText(shop.getName() != null ? shop.getName() : "Nom inconnu");
        holder.categoryText.setText(shop.getCategory() != null ? shop.getCategory() : "Catégorie");
        holder.locationText.setText(shop.getLocation() != null ? shop.getLocation() : "Localisation");
        holder.likesCount.setText(String.valueOf(shop.getLikesCount()));

        // Image
        Glide.with(context)
                .load(shop.getImageUrl() != null && !shop.getImageUrl().isEmpty() ? shop.getImageUrl() : R.drawable.ic_launcher_background)
                .centerCrop()
                .into(holder.shopImage);

        // Boutons
        holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.star_filled : R.drawable.star_outline);
        holder.likeButton.setImageResource(shop.isLiked() ? R.drawable.like_filled : R.drawable.like_outline);

        // Listeners
        holder.itemView.setOnClickListener(v -> listener.onShopClick(shop, position));

        holder.favoriteButton.setOnClickListener(v -> {
            // Inverser la valeur favorite
            shop.setFavorite(!shop.isFavorite());
            // Mettre à jour l'affichage
            holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.star_filled : R.drawable.star_outline);
            // Notifier le listener pour mettre à jour la DB ou l'interface favoris
            listener.onFavoriteClick(shop, position);
        });

        holder.likeButton.setOnClickListener(v -> listener.onLikeClick(shop, position));
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, locationText, likesCount;
        ImageButton favoriteButton, likeButton;
        ImageView shopImage;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.shop_name);
            categoryText = itemView.findViewById(R.id.shop_category);
            locationText = itemView.findViewById(R.id.shop_location);
            likesCount = itemView.findViewById(R.id.likes_count);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
            likeButton = itemView.findViewById(R.id.btn_like);
            shopImage = itemView.findViewById(R.id.shop_image);
        }
    }

    public interface OnShopClickListener {
        void onShopClick(Shop shop, int position);
        void onFavoriteClick(Shop shop, int position); // appelé après modification favorite
        void onLikeClick(Shop shop, int position);
    }
}
