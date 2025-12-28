package com.example.soukify.ui.shop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.utils.CurrencyHelper;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.soukify.data.sync.ProductSync;

import java.util.ArrayList;
import java.util.List;

public class CleanProductsAdapter extends RecyclerView.Adapter<CleanProductsAdapter.ProductViewHolder> implements ProductSync.SyncListener {

    private static final String TAG = "CleanProductsAdapter";

    private List<ProductModel> products = new ArrayList<>();
    private OnProductClickListener listener;
    private Context context;
    private FirebaseProductImageService imageService;
    private ProductViewModel productViewModel;
    private boolean isFavoritesContext = false;

    public interface OnProductClickListener {
        void onProductClick(ProductModel product);
        void onProductLongClick(ProductModel product);
        void onFavoriteClick(ProductModel product, int position);
    }

    public CleanProductsAdapter(Context context, OnProductClickListener listener, ProductViewModel productViewModel) {
        this(context, listener, productViewModel, false);
    }

    public CleanProductsAdapter(Context context, OnProductClickListener listener,
                                ProductViewModel productViewModel, boolean isFavoritesContext) {
        this.context = context;
        this.listener = listener;
        this.productViewModel = productViewModel;
        this.isFavoritesContext = isFavoritesContext;
        this.imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
        setHasStableIds(true);
    }

    public void setLifecycleOwner(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        this.setLifecycleOwner(lifecycleOwner, null);
    }

    public void setLifecycleOwner(androidx.lifecycle.LifecycleOwner lifecycleOwner, RecyclerView recyclerView) {
        // Observers are now managed externally by the Fragments/UI Managers 
        // to avoid conflicts between different data sources (ProductManager vs ProductViewModel).
        if (lifecycleOwner != null && productViewModel != null) {
            removeObservers();
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        ProductSync.LikeSync.register(this);
        ProductSync.FavoriteSync.register(this);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        ProductSync.LikeSync.unregister(this);
        ProductSync.FavoriteSync.unregister(this);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @Override
    public void onProductSyncUpdate(String productId, Bundle payload) {
        if (productId == null || products == null) return;
        for (int i = 0; i < products.size(); i++) {
            if (productId.equals(products.get(i).getProductId())) {
                notifyItemChanged(i, payload);
            }
        }
    }

    private void removeObservers() {
        if (productViewModel != null) {
            productViewModel.getProducts().removeObservers(null);
        }
    }

    /**
     * Met à jour la liste de produits en utilisant DiffUtil pour des animations fluides
     */
    private void updateProductsWithDiff(List<ProductModel> newProducts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new ProductDiffCallback(this.products, newProducts)
        );

        this.products.clear();
        this.products.addAll(newProducts);
        diffResult.dispatchUpdatesTo(this);

        Log.d(TAG, "Updated products list with " + newProducts.size() + " items using DiffUtil");
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_clean, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductModel product = products.get(position);
        holder.bind(product, null);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            ProductModel product = products.get(position);
            Bundle bundle = (Bundle) payloads.get(0);
            holder.bind(product, bundle);
        }
    }

    @Override
    public long getItemId(int position) {
        ProductModel p = (position >= 0 && position < products.size()) ? products.get(position) : null;
        String id = p != null ? p.getProductId() : null;
        return id != null ? id.hashCode() : RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateProducts(List<ProductModel> newProducts) {
        updateProductsWithDiff(newProducts);
    }

    private void displayProductType(ProductModel product, TextView typeTextView) {
        if (product.getProductType() != null && !product.getProductType().isEmpty()) {
            typeTextView.setText(product.getProductType());
            typeTextView.setVisibility(View.VISIBLE);
        } else {
            typeTextView.setVisibility(View.GONE);
        }
    }

    private void navigateToProductDetail(ProductModel product) {
        try {
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity =
                        (androidx.fragment.app.FragmentActivity) context;

                androidx.navigation.NavController navController =
                        androidx.navigation.Navigation.findNavController(
                                activity, R.id.nav_host_fragment_activity_main);

                Bundle bundle = new Bundle();
                bundle.putParcelable("product", product);

                if (isFavoritesContext) {
                    bundle.putString("source", "favorites");
                }

                navController.navigate(R.id.global_action_to_productDetail, bundle);

                Log.d(TAG, "Navigated to product detail for: " + product.getName() +
                        (isFavoritesContext ? " (from favorites)" : ""));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error navigating to product detail: " + e.getMessage(), e);
        }
    }

    /**
     * DiffUtil Callback pour comparer les anciennes et nouvelles listes de produits
     */
    private static class ProductDiffCallback extends DiffUtil.Callback {
        private final List<ProductModel> oldList;
        private final List<ProductModel> newList;

        public ProductDiffCallback(List<ProductModel> oldList, List<ProductModel> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getProductId()
                    .equals(newList.get(newItemPosition).getProductId());
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            ProductModel oldProduct = oldList.get(oldItemPosition);
            ProductModel newProduct = newList.get(newItemPosition);

            return oldProduct.getLikesCount() == newProduct.getLikesCount() &&
                    oldProduct.isLikedByUser() == newProduct.isLikedByUser() &&
                    oldProduct.isFavoriteByUser() == newProduct.isFavoriteByUser() &&
                    oldProduct.getName().equals(newProduct.getName()) &&
                    oldProduct.getPrice() == newProduct.getPrice();
        }

        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            ProductModel oldProduct = oldList.get(oldItemPosition);
            ProductModel newProduct = newList.get(newItemPosition);

            Bundle diff = new Bundle();

            if (oldProduct.getLikesCount() != newProduct.getLikesCount()) {
                diff.putInt("likesCount", newProduct.getLikesCount());
            }
            if (oldProduct.isLikedByUser() != newProduct.isLikedByUser()) {
                diff.putBoolean("isLiked", newProduct.isLikedByUser());
            }
            if (oldProduct.isFavoriteByUser() != newProduct.isFavoriteByUser()) {
                diff.putBoolean("isFavorite", newProduct.isFavoriteByUser());
            }

            return diff.size() > 0 ? diff : null;
        }
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private ProductImageCarousel productImageCarousel;
        private TextView productName;
        private TextView productPrice;
        private TextView productType;
        private TextView productCurrency;
        private TextView likesCount;
        private TextView favoritesCount;
        private ImageButton likeButton;
        private ImageButton favoriteButton;
        private ImageButton shopLinkButton;
        private ProductModel currentProduct;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);

            productImageCarousel = itemView.findViewById(R.id.productImageCarousel);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productType = itemView.findViewById(R.id.productType);
            productCurrency = itemView.findViewById(R.id.productCurrency);
            likesCount = itemView.findViewById(R.id.likesCount);
            favoritesCount = itemView.findViewById(R.id.favoritesCount);
            likeButton = itemView.findViewById(R.id.likeButton);
            favoriteButton = itemView.findViewById(R.id.favoriteButton);
            shopLinkButton = itemView.findViewById(R.id.shopLinkButton);
        }

        /**
         * Bind avec support de mise à jour partielle via payload
         */
        public void bind(@NonNull ProductModel product, @Nullable Bundle payload) {
            currentProduct = product;

            // Mise à jour partielle si payload existe
            if (payload != null && !payload.isEmpty()) {
                if (payload.containsKey("likesCount")) {
                    int likes = payload.getInt("likesCount");
                    likesCount.setText(String.valueOf(likes));
                    Log.d(TAG, "Partial update: likesCount=" + likes);
                }
                if (payload.containsKey("isLiked")) {
                    boolean liked = payload.getBoolean("isLiked");
                    updateLikeButton(liked);
                    Log.d(TAG, "Partial update: isLiked=" + liked);
                }
                if (payload.containsKey("isFavorite")) {
                    boolean favorite = payload.getBoolean("isFavorite");
                    updateFavoriteButton(favorite);
                    Log.d(TAG, "Partial update: isFavorite=" + favorite);
                }
                return;
            }

            productName.setText(product.getName());

            android.app.Application app = (android.app.Application) context.getApplicationContext();
            ProductSync.LikeSync.LikeState likeState = ProductSync.LikeSync.getState(product.getProductId());
            boolean likedBinding = likeState != null ? likeState.isLiked : product.isLikedByUser();
            int likesBinding = likeState != null ? likeState.count : product.getLikesCount();

            ProductSync.FavoriteSync.FavoriteState favState = ProductSync.FavoriteSync.getState(product.getProductId(), app);
            boolean isFavoriteBinding = favState != null ? favState.isFavorite : product.isFavoriteByUser();

            // Keep the model in sync
            product.setLikedByUser(likedBinding);
            product.setLikesCount(likesBinding);
            product.setFavoriteByUser(isFavoriteBinding);

            likesCount.setText(String.valueOf(likesBinding));
            updateLikeButton(likedBinding);
            updateFavoriteButton(isFavoriteBinding);

            if (favoritesCount != null) {
                favoritesCount.setVisibility(View.GONE);
            }

            // Format localized price using CurrencyHelper
            String formattedPrice = CurrencyHelper.formatLocalizedPrice(context, product.getPrice(), product.getCurrency());
            productPrice.setText(formattedPrice);
            
            // Hide separate currency field as it's included in formatted price
            productCurrency.setVisibility(View.GONE);

            if (product.getProductType() != null && !product.getProductType().isEmpty()) {
                displayProductType(product, productType);
            } else {
                productType.setVisibility(View.GONE);
            }

            setupButtons(product);
            loadProductImages(product);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                }
                navigateToProductDetail(product);
            });
        }



        /**
         * Met à jour le bouton like
         */
        private void updateLikeButton(boolean liked) {
            if (likeButton != null) {
                likeButton.setImageResource(liked ?
                        R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                likeButton.setColorFilter(liked ?
                        Color.parseColor("#E8574D") : Color.parseColor("#757575"));
            }
        }

        /**
         * Met à jour le bouton favorite
         */
        private void updateFavoriteButton(boolean favorite) {
            if (favoriteButton != null) {
                favoriteButton.setImageResource(favorite ?
                        R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                favoriteButton.setColorFilter(favorite ?
                        Color.parseColor("#FFC107") : Color.parseColor("#757575"));
            }
        }

        /**
         * Configure les boutons
         */
        private void setupButtons(ProductModel product) {
            // Like button
            if (likeButton != null) {
                likeButton.setOnClickListener(v -> {
                    Log.d(TAG, "❤️ Like button clicked for: " + product.getName());
                    boolean newLiked = !product.isLikedByUser();
                    int newCount = product.getLikesCount() + (newLiked ? 1 : -1);
                    product.setLikedByUser(newLiked);
                    product.setLikesCount(newCount);
                    ProductSync.LikeSync.update(product.getProductId(), newLiked, newCount);
                    
                    if (productViewModel != null) {
                        productViewModel.toggleLikeProduct(product.getProductId());
                    }
                });
            }

            // Favorite button
            if (favoriteButton != null) {
                favoriteButton.setOnClickListener(v -> {
                    Log.d(TAG, "⭐ Favorite button clicked for: " + product.getName());
                    if (listener != null) {
                        listener.onFavoriteClick(product, getAdapterPosition());
                    } else {
                        boolean newFavorite = !product.isFavoriteByUser();
                        product.setFavoriteByUser(newFavorite);
                        ProductSync.FavoriteSync.update(product.getProductId(), newFavorite);
                        if (productViewModel != null) {
                            productViewModel.toggleFavoriteProduct(product.getProductId());
                        }
                    }
                });
            }
            // Shop link button (uniquement en contexte favoris)
            if (isFavoritesContext) {
                shopLinkButton.setOnClickListener(v -> {
                    Log.d(TAG, "🏪 Shop link button clicked for: " + product.getName());
                    navigateToShop(product.getShopId());
                });

                shopLinkButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return false;
                });
                shopLinkButton.setVisibility(View.VISIBLE);
            } else {
                shopLinkButton.setVisibility(View.GONE);
            }
        }

        private void navigateToShop(String shopId) {
            if (shopId == null || shopId.isEmpty()) {
                Log.w(TAG, "Shop ID is null or empty, cannot navigate");
                return;
            }

            try {
                Activity activity = (Activity) context;
                if (activity != null) {
                    androidx.navigation.NavController navController =
                            androidx.navigation.Navigation.findNavController(
                                    activity, R.id.nav_host_fragment_activity_main);

                    Bundle bundle = new Bundle();
                    bundle.putString("shopId", shopId);

                    if (isFavoritesContext) {
                        bundle.putString("source", "favorites");
                    }

                    navController.navigate(R.id.navigation_shop, bundle);

                    Log.d(TAG, "Navigated to shop: " + shopId +
                            (isFavoritesContext ? " (from favorites)" : ""));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error navigating to shop: " + e.getMessage(), e);
            }
        }

        private void loadProductImages(ProductModel product) {
            if (product.hasImages()) {
                String primaryImageId = product.getPrimaryImageId();
                if (primaryImageId != null && !primaryImageId.isEmpty()) {
                    Log.d(TAG, "Loading primary image for product: " + product.getProductId());

                    imageService.getProductImage(primaryImageId)
                            .addOnSuccessListener(imageModel -> {
                                if (imageModel != null && imageModel.getImageUrl() != null) {
                                    List<String> imageUrls = new ArrayList<>();
                                    imageUrls.add(imageModel.getImageUrl());

                                    productImageCarousel.setOnImageClickListener(
                                            new ProductImageCarousel.OnImageClickListener() {
                                                @Override
                                                public void onImageClick(int position, String imageUrl) {
                                                    Log.d(TAG, "Image clicked: " + product.getName());
                                                    navigateToProductDetail(product);
                                                }

                                                @Override
                                                public void onImageLongClick(int position, String imageUrl) {
                                                    Log.d(TAG, "Image long clicked: " + product.getName());
                                                    if (listener != null) {
                                                        listener.onProductLongClick(product);
                                                    }
                                                }
                                            });

                                    productImageCarousel.setImageUrls(imageUrls);
                                    productImageCarousel.setVisibility(View.VISIBLE);

                                    FrameLayout parentLayout = (FrameLayout) productImageCarousel.getParent();
                                    if (parentLayout != null) {
                                        productImageCarousel.setClickable(false);
                                        productImageCarousel.setFocusable(false);

                                        parentLayout.setOnClickListener(v -> {
                                            Log.d(TAG, "Image area clicked: " + product.getName());
                                            navigateToProductDetail(product);
                                        });
                                        parentLayout.setClickable(true);
                                        parentLayout.setFocusable(true);
                                    }
                                } else {
                                    Log.d(TAG, "Image model null for: " + product.getProductId());
                                    showPlaceholder(productImageCarousel);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to load image: " + product.getProductId(), e);
                                showPlaceholder(productImageCarousel);
                            });
                } else {
                    Log.d(TAG, "No primary image ID for: " + product.getProductId());
                    showPlaceholder(productImageCarousel);
                }
            } else {
                Log.d(TAG, "No images for: " + product.getProductId());
                showPlaceholder(productImageCarousel);
            }
        }

        private void showPlaceholder(ProductImageCarousel carousel) {
            Log.d(TAG, "Showing placeholder in carousel");
            carousel.setVisibility(View.GONE);

            FrameLayout parentLayout = (FrameLayout) carousel.getParent();
            if (parentLayout != null) {
                parentLayout.setOnClickListener(v -> {
                    if (currentProduct != null) {
                        Log.d(TAG, "Placeholder clicked: " + currentProduct.getName());
                        navigateToProductDetail(currentProduct);
                    }
                });
                parentLayout.setClickable(true);
                parentLayout.setFocusable(true);
                parentLayout.setBackgroundColor(Color.parseColor("#FAFAFA"));
            }
        }
    }
}