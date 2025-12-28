package com.example.soukify.ui.search;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.sync.ShopSync;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import android.content.Intent;
import android.widget.Toast;
import com.example.soukify.data.repositories.ChatRepository;
import com.example.soukify.ui.chat.ChatActivity;
import com.example.soukify.ui.conversations.ConversationsListActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> implements ShopSync.SyncListener {

    private static final String TAG = "ShopAdapter";

    private Context context;
    private List<ShopModel> shopList;
    private OnShopClickListener listener;
    private String currentUserId;

    private final int COLOR_LIKED;
    private final int COLOR_UNLIKED;
    private final int COLOR_FAVORITE;
    private final int COLOR_UNFAVORITE;
    
    // Cache pour les noms de régions et villes
    private Map<String, String> regionNamesCache = new HashMap<>();
    private Map<String, String> cityNamesCache = new HashMap<>();
    private FirebaseFirestore db;

    public ShopAdapter(Context context, List<ShopModel> shopList, OnShopClickListener listener) {
        this.context = context;
        this.shopList = shopList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();

        COLOR_LIKED = Color.parseColor("#E8574D");
        COLOR_UNLIKED = Color.GRAY;
        COLOR_FAVORITE = Color.parseColor("#FFC107"); // Yellow
        COLOR_UNFAVORITE = Color.GRAY;

        try {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Log.d(TAG, "✅ CurrentUserId: " + currentUserId);
            } else {
                this.currentUserId = null;
                Log.d(TAG, "ℹ️ Aucun user connecté");
            }
        } catch (Exception e) {
            this.currentUserId = null;
            Log.e(TAG, "❌ Erreur récupération userId", e);
        }
        
        // Précharger les régions et villes
        loadRegionsAndCities();
        setHasStableIds(true);
    }

    private void loadRegionsAndCities() {
        db.collection("regions")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String regionId = doc.getId();
                    String regionName = getLocalizedNameFromDoc(doc);
                    if (regionName != null) {
                        regionNamesCache.put(regionId, regionName);
                        loadCitiesForRegion(regionId);
                    }
                }
                notifyDataSetChanged();
            })
            .addOnFailureListener(e -> Log.e(TAG, "❌ Erreur chargement régions", e));
    }

    private void loadCitiesForRegion(String regionId) {
        db.collection("regions").document(regionId).collection("cities")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean added = false;
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String docId = doc.getId();
                    String name = getLocalizedNameFromDoc(doc);
                    if (name != null) {
                        cityNamesCache.put(docId, name);
                        added = true;
                    }
                }
                if (added) notifyDataSetChanged();
            });
    }

    private java.util.Set<String> fetchingCityIds = new java.util.HashSet<>();

    private String getRegionName(String regionId) {
        if (regionId == null || regionId.isEmpty()) return context.getString(R.string.region_not_specified);
        String cachedName = regionNamesCache.get(regionId);
        if (cachedName != null) return cachedName;
        db.collection("regions").document(regionId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = getLocalizedNameFromDoc(doc);
                if (name != null) {
                    regionNamesCache.put(regionId, name);
                    notifyDataSetChanged();
                }
            }
        });
        return regionId;
    }

    private String getCityName(String cityId, String regionId) {
        if (cityId == null || cityId.isEmpty()) return context.getString(R.string.city_not_specified);
        String cachedName = cityNamesCache.get(cityId);
        if (cachedName != null) return cachedName;
        if (fetchingCityIds.contains(cityId)) return cityId;
        fetchingCityIds.add(cityId);
        if (regionId != null && !regionId.isEmpty()) {
            db.collection("regions").document(regionId).collection("cities").document(cityId).get()
                .addOnSuccessListener(doc -> {
                    String name = getLocalizedNameFromDoc(doc);
                    if (doc.exists() && name != null) {
                        cityNamesCache.put(cityId, name);
                        fetchingCityIds.remove(cityId);
                        notifyDataSetChanged();
                    } else fetchCityFromRoot(cityId);
                })
                .addOnFailureListener(e -> fetchCityFromRoot(cityId));
        } else fetchCityFromRoot(cityId);
        return cityId;
    }

    private void fetchCityFromRoot(String cityId) {
        db.collection("cities").document(cityId).get()
                .addOnSuccessListener(doc -> {
                    String name = getLocalizedNameFromDoc(doc);
                    if (doc.exists() && name != null) {
                        cityNamesCache.put(cityId, name);
                        fetchingCityIds.remove(cityId);
                        notifyDataSetChanged();
                    }
                });
    }

    private String getLocalizedNameFromDoc(DocumentSnapshot doc) {
        String lang = java.util.Locale.getDefault().getLanguage();
        String name_localized = doc.getString("name_" + lang);
        if (name_localized != null && !name_localized.isEmpty()) return name_localized;
        return doc.getString("name");
    }

    @NonNull
    @Override
    public ShopViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false);
        return new ShopViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position) {
        onBindViewHolder(holder, position, java.util.Collections.emptyList());
    }

    @Override
    public void onBindViewHolder(@NonNull ShopViewHolder holder, int position, @NonNull List<Object> payloads) {
        ShopModel shop = shopList.get(position);

        if (!payloads.isEmpty()) {
            for (Object payload : payloads) {
                if (payload instanceof Bundle) {
                    Bundle bundle = (Bundle) payload;
                    if (bundle.containsKey("isFavorite")) {
                        boolean fav = bundle.getBoolean("isFavorite");
                        shop.setFavorite(fav); // ✅ Synchronize model
                        holder.favoriteButton.setImageResource(fav ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                        holder.favoriteButton.setColorFilter(fav ? COLOR_FAVORITE : COLOR_UNFAVORITE);
                    }
                    if (bundle.containsKey("isLiked")) {
                        boolean liked = bundle.getBoolean("isLiked");
                        int count = bundle.getInt("likesCount", shop.getLikesCount());
                        shop.setLiked(liked); // ✅ Synchronize model
                        shop.setLikesCount(count); // ✅ Synchronize model
                        holder.likeButton.setImageResource(liked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                        holder.likeButton.setColorFilter(liked ? COLOR_LIKED : COLOR_UNLIKED);
                        holder.likesCount.setText(String.valueOf(count));
                    }
                }
            }
            return;
        }

        holder.nameText.setText(shop.getName() != null ? shop.getName() : context.getString(R.string.unknown_name));
        holder.categoryText.setText(com.example.soukify.utils.CategoryUtils.getLocalizedCategory(context, shop.getCategory()));
        
        String regionName = getRegionName(shop.getRegionId());
        String cityName = getCityName(shop.getCityId(), shop.getRegionId());
        String locationTextStr = (shop.getAddress() != null ? shop.getAddress() : "") + ", " + cityName + ", " + regionName;
        holder.locationText.setText(locationTextStr);
        
        android.app.Application app = (android.app.Application) context.getApplicationContext();
        ShopSync.LikeSync.LikeState likeState = ShopSync.LikeSync.getState(shop.getShopId(), app);
        boolean likedBinding = likeState != null ? likeState.isLiked : shop.isLiked();
        int likesBinding = likeState != null ? likeState.count : shop.getLikesCount();
        
        ShopSync.FavoriteSync.FavoriteState favState = ShopSync.FavoriteSync.getState(shop.getShopId(), app);
        boolean isFavoriteBinding = favState != null ? favState.isFavorite : shop.isFavorite();

        shop.setLiked(likedBinding);
        shop.setLikesCount(likesBinding);
        shop.setFavorite(isFavoriteBinding);
        holder.likesCount.setText(String.valueOf(likesBinding));
        holder.ratingText.setText(String.format(Locale.getDefault(), "%.1f", shop.getRating()));

        float userRating = 0f;
        if (currentUserId != null && shop.getUserRatings() != null) {
            Float rating = shop.getUserRatings().get(currentUserId);
            if (rating != null) userRating = rating;
        }
        holder.ratingBar.setOnRatingBarChangeListener(null);
        holder.ratingBar.setRating(userRating);
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    ShopModel clickedShop = shopList.get(pos);
                    if (listener != null) {
                        listener.onRatingChanged(clickedShop, rating, pos);
                    }
                }
            }
        });

        holder.phoneText.setText("📞 " + (shop.getPhone() != null ? shop.getPhone() : context.getString(R.string.not_available)));
        holder.emailText.setText("✉️ " + (shop.getEmail() != null ? shop.getEmail() : context.getString(R.string.not_available)));
        holder.promotionBadge.setVisibility(shop.isHasPromotion() ? View.VISIBLE : View.GONE);

        String imageUrl = shop.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).centerCrop().into(holder.shopImage);
        } else {
            Glide.with(context).load(R.drawable.ic_profile_placeholder).centerCrop().into(holder.shopImage);
        }

        holder.favoriteButton.setImageResource(isFavoriteBinding ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        holder.favoriteButton.setColorFilter(isFavoriteBinding ? COLOR_FAVORITE : COLOR_UNFAVORITE);
        holder.likeButton.setImageResource(likedBinding ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        holder.likeButton.setColorFilter(likedBinding ? COLOR_LIKED : COLOR_UNLIKED);

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onShopClick(shopList.get(pos));
        });

        holder.favoriteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ShopModel clickedShop = shopList.get(pos);
            if (listener != null) listener.onFavoriteClick(clickedShop, pos);
        });

        holder.likeButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            ShopModel clickedShop = shopList.get(pos);
            if (listener != null) listener.onLikeClick(clickedShop, pos);
        });

        holder.shareButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onShareClick(shopList.get(pos), pos);
        });

        String sellerId = shop.getUserId();
        boolean isMyShop = currentUserId != null && sellerId != null && currentUserId.equals(sellerId);

        if (isMyShop) {
            // If it's my shop, show unreachable count if available
            loadUnreadCount(holder.chatBadge);
            holder.chatButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, ConversationsListActivity.class);
                intent.putExtra(ConversationsListActivity.EXTRA_IS_SELLER_VIEW, true);
                context.startActivity(intent);
            });
        } else {
            // If it's not my shop, hide badge and start chat with seller
            if (holder.chatBadge != null) holder.chatBadge.setVisibility(View.GONE);
            holder.chatButton.setOnClickListener(v -> {
                if (currentUserId == null) {
                    Toast.makeText(context, R.string.please_login_toast, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (sellerId == null || sellerId.isEmpty()) {
                    Toast.makeText(context, R.string.no_owner_error, Toast.LENGTH_LONG).show();
                    return;
                }

                ChatRepository chatRepo = new ChatRepository();
                chatRepo.getOrCreateConversation(
                        currentUserId, sellerId, shop.getShopId(), shop.getName(), shop.getImageUrl(),
                        new ChatRepository.OnConversationLoadedListener() {
                            @Override
                            public void onSuccess(com.example.soukify.data.models.Conversation conversation) {
                                Intent intent = new Intent(context, ChatActivity.class);
                                intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
                                intent.putExtra(ChatActivity.EXTRA_SHOP_NAME, shop.getName());
                                intent.putExtra(ChatActivity.EXTRA_SHOP_ID, shop.getShopId());
                                intent.putExtra(ChatActivity.EXTRA_SELLER_ID, sellerId);
                                intent.putExtra(ChatActivity.EXTRA_SHOP_IMAGE, shop.getImageUrl());
                                context.startActivity(intent);
                            }

                            @Override
                            public void onFailure(String error) {
                                Toast.makeText(context, "Erreur: " + error, Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            });
        }
    }

    private void loadUnreadCount(TextView chatBadge) {
        if (currentUserId == null || chatBadge == null) return;
        FirebaseFirestore.getInstance().collection("Conversation")
                .whereEqualTo("sellerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        chatBadge.setVisibility(View.GONE);
                        return;
                    }
                    int totalUnread = 0;
                    for (DocumentSnapshot doc : value) {
                        Long unread = doc.getLong("unreadCountSeller");
                        if (unread != null) totalUnread += unread.intValue();
                    }
                    chatBadge.setVisibility(totalUnread > 0 ? View.VISIBLE : View.GONE);
                    if (totalUnread > 0) chatBadge.setText(String.valueOf(totalUnread));
                });
    }

    @Override
    public int getItemCount() { return shopList != null ? shopList.size() : 0; }

    @Override
    public long getItemId(int position) {
        ShopModel shop = shopList != null && position >= 0 && position < shopList.size() ? shopList.get(position) : null;
        String id = shop != null ? shop.getShopId() : null;
        return id != null ? id.hashCode() : RecyclerView.NO_ID;
    }

    private void notifyItemChangedByShopId(String shopId, Bundle payload) {
        if (shopId == null || shopList == null) return;
        for (int i = 0; i < shopList.size(); i++) {
            if (shopId.equals(shopList.get(i).getShopId())) {
                notifyItemChanged(i, payload);
            }
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        ShopSync.LikeSync.register(this);
        ShopSync.FavoriteSync.register(this);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        ShopSync.LikeSync.unregister(this);
        ShopSync.FavoriteSync.unregister(this);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public void onShopSyncUpdate(String shopId, Bundle payload) {
        notifyItemChangedByShopId(shopId, payload);
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, locationText, likesCount, ratingText, phoneText, emailText, promotionBadge, chatBadge;
        ImageButton favoriteButton, likeButton, shareButton, chatButton;
        ImageView shopImage;
        RatingBar ratingBar;

        public ShopViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.shop_name);
            categoryText = itemView.findViewById(R.id.shop_category);
            locationText = itemView.findViewById(R.id.shop_location);
            likesCount = itemView.findViewById(R.id.likes_count);
            ratingText = itemView.findViewById(R.id.shop_rating);
            phoneText = itemView.findViewById(R.id.shop_phone);
            emailText = itemView.findViewById(R.id.shop_email);
            promotionBadge = itemView.findViewById(R.id.promotion_badge);
            chatBadge = itemView.findViewById(R.id.chat_badge);
            favoriteButton = itemView.findViewById(R.id.btn_favorite);
            likeButton = itemView.findViewById(R.id.btn_like);
            shareButton = itemView.findViewById(R.id.btn_share);
            chatButton = itemView.findViewById(R.id.btnChat);
            shopImage = itemView.findViewById(R.id.shop_image);
            ratingBar = itemView.findViewById(R.id.shop_rating_bar);
        }
    }

    public interface OnShopClickListener {
        void onShopClick(ShopModel shop);
        void onFavoriteClick(ShopModel shop, int position);
        void onLikeClick(ShopModel shop, int position);
        void onRatingChanged(ShopModel shop, float newRating, int position);
        void onShareClick(ShopModel shop, int position);
        void onChatClick(ShopModel shop, int position);
    }
}