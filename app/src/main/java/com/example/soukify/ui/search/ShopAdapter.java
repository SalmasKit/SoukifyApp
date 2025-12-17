package com.example.soukify.ui.search;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private Context context;
    private List<ShopModel> shopList;
    private OnShopClickListener listener;
    private String currentUserId;

    public ShopAdapter(Context context, List<ShopModel> shopList, OnShopClickListener listener) {
        this.context = context;
        this.shopList = shopList;
        this.listener = listener;
        try {
            this.currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
        } catch (Exception e) {
            this.currentUserId = null;
        }
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
        // Likes count
        holder.likesCount.setText(String.valueOf(shop.getLikesCount()));
        
        // Favorites count
        holder.favoritesCount.setText(String.valueOf(shop.getFavoritesCount()));

        // Rating et Reviews
        holder.ratingText.setText(String.format(Locale.getDefault(), "%.1f", shop.getRating()));
        holder.reviewsText.setText("(" + shop.getReviews() + " avis)");

        // Rating de l'utilisateur actuel
        float userRating = 0f;
        if (currentUserId != null && shop.getUserRatings() != null) {
            Float rating = shop.getUserRatings().get(currentUserId);
            if (rating != null) {
                userRating = rating;
            }
        }
        holder.ratingBar.setRating(userRating);

        // Contact Info
        holder.phoneText.setText("📞 " + (shop.getPhone() != null && !shop.getPhone().isEmpty() ? shop.getPhone() : "Non disponible"));
        holder.emailText.setText("✉️ " + (shop.getEmail() != null && !shop.getEmail().isEmpty() ? shop.getEmail() : "Non disponible"));

        // Promotion Badge
        holder.promotionBadge.setVisibility(shop.isHasPromotion() ? View.VISIBLE : View.GONE);

        // Image
        String imageUrl = shop.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).centerCrop().into(holder.shopImage);
        } else {
            Glide.with(context).load(R.drawable.ic_launcher_background).centerCrop().into(holder.shopImage);
        }

        // Boutons
        Log.d("ShopAdapter", "Binding shop: " + shop.getName() + ", isLiked: " + shop.isLiked());
        holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        holder.likeButton.setImageResource(shop.isLiked() ? R.drawable.like_filled : R.drawable.like_outline);

        // Listeners
        holder.itemView.setOnClickListener(v -> listener.onShopClick(shop, position));
        holder.favoriteButton.setOnClickListener(v -> listener.onFavoriteClick(shop, position));
        holder.likeButton.setOnClickListener(v -> listener.onLikeClick(shop, position));

        // 🌟 Bouton Chat
        holder.chatButton.setOnClickListener(v -> listener.onChatClick(shop, position));

        // RatingBar listener
        holder.ratingBar.setOnRatingBarChangeListener(null); // reset
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser && rating > 0) {
                listener.onRatingChanged(shop, rating, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, locationText, likesCount, favoritesCount;
        TextView ratingText, reviewsText, phoneText, emailText;
        TextView promotionBadge;
        ImageButton favoriteButton, likeButton, chatButton;
        ImageView shopImage;
        RatingBar ratingBar;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.shop_name);
            categoryText = itemView.findViewById(R.id.shop_category);
            locationText = itemView.findViewById(R.id.shop_location);
            likesCount = itemView.findViewById(R.id.likes_count);
            favoritesCount = itemView.findViewById(R.id.favorites_count);
            ratingText = itemView.findViewById(R.id.shop_rating);
            reviewsText = itemView.findViewById(R.id.shop_reviews);
            phoneText = itemView.findViewById(R.id.shop_phone);
            emailText = itemView.findViewById(R.id.shop_email);
            promotionBadge = itemView.findViewById(R.id.promotion_badge);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
            likeButton = itemView.findViewById(R.id.btn_like);
            //shareButton = itemView.findViewById(R.id.btn_share);
            chatButton = itemView.findViewById(R.id.btnChat); // 🌟 nouveau bouton Chat
            shopImage = itemView.findViewById(R.id.shop_image);
            ratingBar = itemView.findViewById(R.id.shop_rating_bar);
        }
    }

    public interface OnShopClickListener {
        void onShopClick(ShopModel shop, int position);
        void onFavoriteClick(ShopModel shop, int position);
        void onLikeClick(ShopModel shop, int position);
        void onRatingChanged(ShopModel shop, float newRating, int position);
        void onChatClick(ShopModel shop, int position); // 🌟 nouveau callback Chat
    }
}
