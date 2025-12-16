package com.example.soukify.ui.search;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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

        // Nom, catégorie, localisation - WITH NULL CHECKS
        if (holder.nameText != null) {
            holder.nameText.setText(shop.getName() != null ? shop.getName() : "Nom inconnu");
        }

        if (holder.categoryText != null) {
            holder.categoryText.setText(shop.getCategory() != null ? shop.getCategory() : "Catégorie");
        }

        if (holder.locationText != null) {
            holder.locationText.setText(shop.getLocation() != null ? shop.getLocation() : "Localisation");
        }

        if (holder.likesCount != null) {
            holder.likesCount.setText(String.valueOf(shop.getLikesCount()));
        }

        // Rating et Reviews - WITH NULL CHECKS
        if (holder.ratingText != null) {
            holder.ratingText.setText("⭐ " + shop.getRating());
        }

        if (holder.reviewsText != null) {
            holder.reviewsText.setText("(" + shop.getReviews() + " avis)");
        }

        // Contact Info - WITH NULL CHECKS
        if (holder.phoneText != null) {
            holder.phoneText.setText("📞 " + (shop.getPhone() != null && !shop.getPhone().isEmpty() ? shop.getPhone() : "Non disponible"));
        }

        if (holder.emailText != null) {
            holder.emailText.setText("✉️ " + (shop.getEmail() != null && !shop.getEmail().isEmpty() ? shop.getEmail() : "Non disponible"));
        }

        // Working Hours & Social Media - WITH NULL CHECKS
        if (holder.workingHoursText != null) {
            String workingHours = shop.getWorkingHours();
            String workingDays = shop.getWorkingDays();
            if ((workingHours != null && !workingHours.isEmpty()) || (workingDays != null && !workingDays.isEmpty())) {
                String hoursText = "";
                if (workingDays != null && !workingDays.isEmpty()) {
                    hoursText += workingDays;
                }
                if (workingHours != null && !workingHours.isEmpty()) {
                    if (!hoursText.isEmpty()) hoursText += " ";
                    hoursText += workingHours;
                }
                holder.workingHoursText.setText("⏰ " + hoursText);
            } else {
                holder.workingHoursText.setText("⏰ Non disponible");
            }
        }

        // Social Media Icons - only show if links exist - WITH NULL CHECKS
        boolean hasSocialMedia = false;

        // Facebook
        if (holder.facebookIcon != null) {
            String facebook = shop.getFacebook();
            if (facebook != null && !facebook.isEmpty()) {
                holder.facebookIcon.setVisibility(View.VISIBLE);
                holder.facebookIcon.setOnClickListener(v -> openSocialMediaLink(facebook));
                hasSocialMedia = true;
            } else {
                holder.facebookIcon.setVisibility(View.GONE);
            }
        }

        // Instagram
        if (holder.instagramIcon != null) {
            String instagram = shop.getInstagram();
            if (instagram != null && !instagram.isEmpty()) {
                holder.instagramIcon.setVisibility(View.VISIBLE);
                holder.instagramIcon.setOnClickListener(v -> openSocialMediaLink(instagram));
                hasSocialMedia = true;
            } else {
                holder.instagramIcon.setVisibility(View.GONE);
            }
        }

        // Website
        if (holder.websiteIcon != null) {
            String website = shop.getWebsite();
            if (website != null && !website.isEmpty()) {
                holder.websiteIcon.setVisibility(View.VISIBLE);
                holder.websiteIcon.setOnClickListener(v -> openSocialMediaLink(website));
                hasSocialMedia = true;
            } else {
                holder.websiteIcon.setVisibility(View.GONE);
            }
        }

        // Promotion Badge - WITH NULL CHECK
        if (holder.promotionBadge != null) {
            if (shop.isHasPromotion()) {
                holder.promotionBadge.setVisibility(View.VISIBLE);
            } else {
                holder.promotionBadge.setVisibility(View.GONE);
            }
        }

        // Image - WITH NULL CHECK
        if (holder.shopImage != null) {
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
        }

        // Boutons - WITH NULL CHECKS
        if (holder.favoriteButton != null) {
            holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.star_filled : R.drawable.star_outline);
            holder.favoriteButton.setOnClickListener(v -> {
                // Notifier le listener pour mettre à jour la DB ou l'interface favoris
                if (listener != null) {
                    listener.onFavoriteClick(shop, position);
                }
            });
        }

        if (holder.likeButton != null) {
            holder.likeButton.setImageResource(shop.isLiked() ? R.drawable.like_filled : R.drawable.like_outline);
            holder.likeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClick(shop, position);
                }
            });
        }

        // Listeners
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onShopClick(shop, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return shopList.size();
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, locationText, likesCount;
        TextView ratingText, reviewsText, phoneText, emailText, workingHoursText;
        TextView promotionBadge;
        ImageButton favoriteButton, likeButton;
        ImageView shopImage;
        ImageView facebookIcon, instagramIcon, websiteIcon;

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
            workingHoursText = itemView.findViewById(R.id.shop_working_hours);
            facebookIcon = itemView.findViewById(R.id.facebookLink);
            instagramIcon = itemView.findViewById(R.id.instagramLink);
            websiteIcon = itemView.findViewById(R.id.websiteLink);
            promotionBadge = itemView.findViewById(R.id.promotion_badge);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
            likeButton = itemView.findViewById(R.id.btn_like);
            shopImage = itemView.findViewById(R.id.shop_image);
        }
    }

    private void openSocialMediaLink(String link) {
        if (link == null || link.isEmpty()) return;

        Uri uri;
        if (link.startsWith("http://") || link.startsWith("https://")) {
            uri = Uri.parse(link);
        } else if (link.contains("facebook.com")) {
            // Handle Facebook links - extract username from full URL
            if (link.contains("facebook.com/")) {
                String username = link.substring(link.indexOf("facebook.com/") + 13);
                if (username.startsWith("/")) {
                    username = username.substring(1);
                }
                uri = Uri.parse("https://facebook.com/" + username);
            } else {
                uri = Uri.parse("https://facebook.com/" + link);
            }
        } else if (link.startsWith("@")) {
            // Handle Facebook username with @
            uri = Uri.parse("https://facebook.com/" + link.substring(1));
        } else if (link.contains("instagram.com")) {
            // Handle Instagram links or usernames
            if (link.contains("instagram.com/")) {
                String username = link.substring(link.indexOf("instagram.com/") + 13);
                if (username.startsWith("/")) {
                    username = username.substring(1);
                }
                uri = Uri.parse("https://instagram.com/" + username);
            } else {
                uri = Uri.parse("https://instagram.com/" + link);
            }
        } else {
            // Default to treating as Facebook username (not website)
            uri = Uri.parse("https://facebook.com/" + link);
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        android.util.Log.d("ShopAdapter", "Opening Facebook link: " + uri.toString());
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            android.util.Log.e("ShopAdapter", "Failed to open link: " + uri.toString(), e);
            Toast.makeText(context, "Unable to open link", Toast.LENGTH_SHORT).show();
        }
    }

    public interface OnShopClickListener {
        void onShopClick(ShopModel shop, int position);
        void onFavoriteClick(ShopModel shop, int position); // appelé après modification favorite
        void onLikeClick(ShopModel shop, int position);
    }
}