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
import com.example.soukify.data.models.ShopModel;
import com.bumptech.glide.Glide;

import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private Context context;
    private List<ShopModel> shopList;
    private OnShopClickListener listener;

    public ShopAdapter(Context context, List<ShopModel> shopList, OnShopClickListener listener) {
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
        ShopModel shop = shopList.get(position);

        // Nom, catégorie, localisation
        holder.nameText.setText(shop.getName() != null ? shop.getName() : "Nom inconnu");
        holder.categoryText.setText(shop.getCategory() != null ? shop.getCategory() : "Catégorie");
        holder.locationText.setText(shop.getLocation() != null ? shop.getLocation() : "Localisation");
        holder.likesCount.setText(String.valueOf(shop.getLikesCount()));

        // Rating et Reviews
        holder.ratingText.setText("⭐ " + shop.getRating());
        holder.reviewsText.setText("(" + shop.getReviews() + " avis)");

        // Contact Info
        holder.phoneText.setText("📞 " + (shop.getPhone() != null && !shop.getPhone().isEmpty() ? shop.getPhone() : "Non disponible"));
        holder.emailText.setText("✉️ " + (shop.getEmail() != null && !shop.getEmail().isEmpty() ? shop.getEmail() : "Non disponible"));

        // Promotion Badge
        if (shop.isHasPromotion()) {
            holder.promotionBadge.setVisibility(View.VISIBLE);
        } else {
            holder.promotionBadge.setVisibility(View.GONE);
        }

        // Image
        String imageUrl = shop.getImageUrl();
        android.util.Log.d("ShopAdapter", "Loading image for shop: " + shop.getName() + ", imageUrl: " + imageUrl);
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.shopImage);
        } else {
            android.util.Log.d("ShopAdapter", "No image URL for shop: " + shop.getName() + ", using placeholder");
            Glide.with(context)
                    .load(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(holder.shopImage);
        }

        // Boutons
        holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.star_filled : R.drawable.star_outline);
        holder.likeButton.setImageResource(shop.isLiked() ? R.drawable.like_filled : R.drawable.like_outline);

        // Listeners
        holder.itemView.setOnClickListener(v -> listener.onShopClick(shop, position));

        holder.favoriteButton.setOnClickListener(v -> {
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
        TextView ratingText, reviewsText, phoneText, emailText;
        TextView promotionBadge;
        ImageButton favoriteButton, likeButton;
        ImageView shopImage;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.shop_name);
            categoryText = itemView.findViewById(R.id.shop_category);
            locationText = itemView.findViewById(R.id.shop_location);
            likesCount = itemView.findViewById(R.id.likes_count);
            ratingText = itemView.findViewById(R.id.shop_rating);
            reviewsText = itemView.findViewById(R.id.shop_reviews);
            phoneText = itemView.findViewById(R.id.shop_phone);
            emailText = itemView.findViewById(R.id.shop_email);
            promotionBadge = itemView.findViewById(R.id.promotion_badge);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
            likeButton = itemView.findViewById(R.id.btn_like);
            shopImage = itemView.findViewById(R.id.shop_image);
        }
    }

    public interface OnShopClickListener {
        void onShopClick(ShopModel shop, int position);
        void onFavoriteClick(ShopModel shop, int position); // appelé après modification favorite
        void onLikeClick(ShopModel shop, int position);
    }
}
