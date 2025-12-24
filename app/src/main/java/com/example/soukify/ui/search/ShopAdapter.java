package com.example.soukify.ui.search;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.repositories.ChatRepository;
import com.example.soukify.ui.chat.ChatActivity;
import com.example.soukify.ui.conversations.ConversationsListActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder> {

    private static final String TAG = "ShopAdapter";

    // Global like state cache and fan-out to all adapters using this view
    private static class LikeState {
        final boolean liked;
        final int count;
        LikeState(boolean liked, int count) {
            this.liked = liked;
            this.count = count;
        }
    }

    private static class LikeSync {
        private static final ConcurrentHashMap<String, LikeState> STATES = new ConcurrentHashMap<>();
        private static final CopyOnWriteArrayList<WeakReference<ShopAdapter>> ADAPTERS = new CopyOnWriteArrayList<>();

        static LikeState getState(String id) {
            return id == null ? null : STATES.get(id);
        }

        static void update(String id, boolean liked, int count) {
            if (id == null) return;
            STATES.put(id, new LikeState(liked, count));
            for (WeakReference<ShopAdapter> ref : ADAPTERS) {
                ShopAdapter a = ref.get();
                if (a != null) {
                    a.notifyItemChangedByShopId(id);
                }
            }
        }

        static void register(ShopAdapter a) {
            if (a == null) return;
            ADAPTERS.add(new WeakReference<>(a));
        }

        static void unregister(ShopAdapter a) {
            for (WeakReference<ShopAdapter> ref : ADAPTERS) {
                ShopAdapter existing = ref.get();
                if (existing == null || existing == a) {
                    ADAPTERS.remove(ref);
                }
            }
        }
    }
    private Context context;
    private List<ShopModel> shopList;
    private OnShopClickListener listener;
    private String currentUserId;

    private final int COLOR_LIKED;
    private final int COLOR_UNLIKED;
    
    // Cache pour les noms de régions et villes
    private Map<String, String> regionNamesCache = new HashMap<>();
    private Map<String, String> cityNamesCache = new HashMap<>();
    private FirebaseFirestore db;

    public ShopAdapter(Context context, List<ShopModel> shopList, OnShopClickListener listener) {
        this.context = context;
        this.shopList = shopList;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();

        COLOR_LIKED = Color.RED;
        COLOR_UNLIKED = Color.GRAY;

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
        LikeSync.register(this);
    }

    /**
     * Charge toutes les régions et villes depuis Firestore dans le cache
     */
    private void loadRegionsAndCities() {
        // Charger les régions
        db.collection("regions")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String regionId = doc.getId();
                    String regionName = doc.getString("name");
                    if (regionName != null) {
                        regionNamesCache.put(regionId, regionName);
                        Log.d(TAG, "✅ Région chargée: " + regionId + " -> " + regionName);
                    }
                }
                notifyDataSetChanged(); // Rafraîchir l'affichage
            })
            .addOnFailureListener(e -> Log.e(TAG, "❌ Erreur chargement régions", e));

        // Charger toutes les villes de toutes les régions
        db.collectionGroup("cities")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    String cityId = doc.getId();
                    String cityName = doc.getString("name");
                    if (cityName != null) {
                        cityNamesCache.put(cityId, cityName);
                        Log.d(TAG, "✅ Ville chargée: " + cityId + " -> " + cityName);
                    }
                }
                notifyDataSetChanged(); // Rafraîchir l'affichage
            })
            .addOnFailureListener(e -> Log.e(TAG, "❌ Erreur chargement villes", e));
    }

    /**
     * Récupère le nom d'une région depuis le cache ou depuis l'ID
     */
    private String getRegionName(String regionId) {
        if (regionId == null || regionId.isEmpty()) {
            return "Région non spécifiée";
        }
        
        // Retourner depuis le cache si disponible
        String cachedName = regionNamesCache.get(regionId);
        if (cachedName != null) {
            return cachedName;
        }
        
        // Sinon charger depuis Firestore de manière asynchrone
        db.collection("regions").document(regionId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("name");
                    if (name != null) {
                        regionNamesCache.put(regionId, name);
                        notifyDataSetChanged();
                    }
                }
            });
        
        return regionId; // Temporairement retourner l'ID en attendant le chargement
    }

    /**
     * Récupère le nom d'une ville depuis le cache ou depuis l'ID
     */
    private String getCityName(String cityId, String regionId) {
        if (cityId == null || cityId.isEmpty()) {
            return "Ville non spécifiée";
        }
        
        // Retourner depuis le cache si disponible
        String cachedName = cityNamesCache.get(cityId);
        if (cachedName != null) {
            return cachedName;
        }
        
        // Sinon charger depuis Firestore de manière asynchrone
        if (regionId != null && !regionId.isEmpty()) {
            db.collection("regions").document(regionId)
                .collection("cities").document(cityId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        if (name != null) {
                            cityNamesCache.put(cityId, name);
                            notifyDataSetChanged();
                        }
                    }
                });
        }
        
        return cityId; // Temporairement retourner l'ID en attendant le chargement
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

        // Texte
        holder.nameText.setText(shop.getName() != null ? shop.getName() : "Nom inconnu");
        holder.categoryText.setText(shop.getCategory() != null ? shop.getCategory() : "Catégorie");
        
        // ✅ AFFICHER LA RÉGION ET LA VILLE AVEC LEURS VRAIS NOMS
        String regionName = getRegionName(shop.getRegionId());
        String cityName = getCityName(shop.getCityId(), shop.getRegionId());
        String locationText = (shop.getAddress() != null ? shop.getAddress() : "") + ", " + cityName + ", " + regionName;
        holder.locationText.setText(locationText);
        
        LikeState likeState = LikeSync.getState(shop.getShopId());
        boolean likedBinding = likeState != null ? likeState.liked : shop.isLiked();
        int likesBinding = likeState != null ? likeState.count : shop.getLikesCount();
        // Keep the model in sync with the shared state
        shop.setLiked(likedBinding);
        shop.setLikesCount(likesBinding);
        holder.likesCount.setText(String.valueOf(likesBinding));
        holder.ratingText.setText(String.format(Locale.getDefault(), "%.1f", shop.getRating()));
        // reviewsText TextView not implemented in layout - commented out
        // holder.reviewsText.setText("(" + shop.getReviews() + " avis)");

        // Rating utilisateur
        float userRating = 0f;
        if (currentUserId != null && shop.getUserRatings() != null) {
            Float rating = shop.getUserRatings().get(currentUserId);
            if (rating != null) userRating = rating;
        }
        holder.ratingBar.setRating(userRating);

        // Contact
        holder.phoneText.setText("📞 " + (shop.getPhone() != null ? shop.getPhone() : "Non disponible"));
        holder.emailText.setText("✉️ " + (shop.getEmail() != null ? shop.getEmail() : "Non disponible"));

        // Promotion
        holder.promotionBadge.setVisibility(shop.isHasPromotion() ? View.VISIBLE : View.GONE);

        // Image
        String imageUrl = shop.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).centerCrop().into(holder.shopImage);
        } else {
            Glide.with(context).load(R.drawable.ic_launcher_background).centerCrop().into(holder.shopImage);
        }

        // Boutons
        holder.favoriteButton.setImageResource(shop.isFavorite() ? R.drawable.star_filled : R.drawable.star_outline);
        holder.likeButton.setImageResource(likedBinding ? R.drawable.like_filled : R.drawable.like_outline);
        holder.likeButton.setColorFilter(likedBinding ? COLOR_LIKED : COLOR_UNLIKED);

        // Click listeners
        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onShopClick(shopList.get(pos), pos);
        });

        holder.favoriteButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onFavoriteClick(shopList.get(pos), pos);
        });

        // ✅ Like button: update shared state so all adapters stay in sync
        holder.likeButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;

            ShopModel clickedShop = shopList.get(pos);
            LikeState st = LikeSync.getState(clickedShop.getShopId());
            boolean currentLiked = st != null ? st.liked : clickedShop.isLiked();
            int currentCount = st != null ? st.count : clickedShop.getLikesCount();

            boolean newLiked = !currentLiked;
            int newCount = newLiked ? currentCount + 1 : Math.max(0, currentCount - 1);

            // Keep the model updated for this list
            clickedShop.setLiked(newLiked);
            clickedShop.setLikesCount(newCount);

            // Immediate UI feedback
            holder.likeButton.setImageResource(newLiked ? R.drawable.like_filled : R.drawable.like_outline);
            holder.likeButton.setColorFilter(newLiked ? COLOR_LIKED : COLOR_UNLIKED);
            holder.likesCount.setText(String.valueOf(newCount));

            // Broadcast to all adapters using shared cache
            LikeSync.update(clickedShop.getShopId(), newLiked, newCount);

            // Notify the listener to persist
            if (listener != null) {
                listener.onLikeClick(clickedShop, pos);
            }
        });

        holder.shareButton.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onShareClick(shopList.get(pos), pos);
        });

        // Chat
        String sellerId = shop.getUserId();
        boolean isMyShop = currentUserId != null && sellerId != null && currentUserId.equals(sellerId);

        if (isMyShop) {
            if (holder.chatBadge != null) loadUnreadCount(holder.chatBadge);
            holder.chatButton.setOnClickListener(v -> {
                Intent intent = new Intent(context, ConversationsListActivity.class);
                intent.putExtra(ConversationsListActivity.EXTRA_IS_SELLER_VIEW, true);
                context.startActivity(intent);
            });
        } else {
            if (holder.chatBadge != null) holder.chatBadge.setVisibility(View.GONE);
            holder.chatButton.setOnClickListener(v -> {
                if (currentUserId == null) {
                    Toast.makeText(context, "Veuillez vous connecter", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (sellerId == null || sellerId.isEmpty()) {
                    Toast.makeText(context, "Cette boutique n'a pas de propriétaire", Toast.LENGTH_LONG).show();
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

        // Rating listener
        holder.ratingBar.setOnRatingBarChangeListener(null);
        holder.ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser && rating > 0) {
                int pos = holder.getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onRatingChanged(shopList.get(pos), rating, pos);
            }
        });
    }

    private void loadUnreadCount(TextView chatBadge) {
        if (currentUserId == null) {
            chatBadge.setVisibility(View.GONE);
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Conversation")
                .whereEqualTo("sellerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        chatBadge.setVisibility(View.GONE);
                        return;
                    }
                    if (value != null && !value.isEmpty()) {
                        int totalUnread = 0;
                        for (com.google.firebase.firestore.QueryDocumentSnapshot doc : value) {
                            Long unread = doc.getLong("unreadCountSeller");
                            if (unread != null) totalUnread += unread.intValue();
                        }
                        chatBadge.setVisibility(totalUnread > 0 ? View.VISIBLE : View.GONE);
                        if (totalUnread > 0) chatBadge.setText(String.valueOf(totalUnread));
                    } else chatBadge.setVisibility(View.GONE);
                });
    }

    @Override
    public int getItemCount() {
        return shopList != null ? shopList.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        ShopModel shop = shopList != null && position >= 0 && position < shopList.size() ? shopList.get(position) : null;
        String id = shop != null ? shop.getShopId() : null;
        return id != null ? id.hashCode() : RecyclerView.NO_ID;
    }

    private void notifyItemChangedByShopId(String shopId) {
        if (shopId == null || shopList == null) return;
        for (int i = 0; i < shopList.size(); i++) {
            ShopModel s = shopList.get(i);
            if (s != null && shopId.equals(s.getShopId())) {
                notifyItemChanged(i);
            }
        }
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        LikeSync.unregister(this);
    }

    public static class ShopViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, categoryText, locationText, likesCount;
        TextView ratingText, reviewsText, phoneText, emailText;
        TextView promotionBadge, chatBadge;
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
            //reviewsText = itemView.findViewById(R.id.shop_reviews);
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
        void onShopClick(ShopModel shop, int position);
        void onFavoriteClick(ShopModel shop, int position);
        void onLikeClick(ShopModel shop, int position);
        void onRatingChanged(ShopModel shop, float newRating, int position);
        void onShareClick(ShopModel shop, int position);
        void onChatClick(ShopModel shop, int position);
    }
}