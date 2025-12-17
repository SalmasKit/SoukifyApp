package com.example.soukify.ui.shop;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.ProductImageModel;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class CleanProductsAdapter extends RecyclerView.Adapter<CleanProductsAdapter.ProductViewHolder> {
    
    private List<ProductModel> products = new ArrayList<>();
    private OnProductClickListener listener;
    private Context context;
    private FirebaseProductImageService imageService;
    private UserProductPreferencesRepository userPreferences;
    
    public interface OnProductClickListener {
        void onProductClick(ProductModel product);
        void onProductLongClick(ProductModel product);
    }
    
    public CleanProductsAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
        this.imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
        this.userPreferences = new UserProductPreferencesRepository(context);
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
        holder.bind(product);
    }
    
    @Override
    public int getItemCount() {
        return products.size();
    }
    
    public void updateProducts(List<ProductModel> newProducts) {
        this.products.clear();
        if (newProducts != null) {
            this.products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }
    
    private void displayProductType(ProductModel product, TextView typeTextView) {
        // Product type is now stored directly in the product
        if (product.getProductType() != null && !product.getProductType().isEmpty()) {
            typeTextView.setText(product.getProductType());
            typeTextView.setVisibility(View.VISIBLE);
        } else {
            typeTextView.setVisibility(View.GONE);
        }
    }
    
    private void navigateToProductDetail(ProductModel product) {
        try {
            // Use Navigation Component for proper navigation
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                androidx.fragment.app.FragmentActivity activity = 
                    (androidx.fragment.app.FragmentActivity) context;
                
                androidx.navigation.NavController navController = 
                    androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
                
                // Create bundle with product data
                Bundle bundle = new Bundle();
                bundle.putParcelable("product", product);
                
                // Navigate using global navigation action
                navController.navigate(R.id.global_action_to_productDetail, bundle);
                
                Log.d("CleanProductsAdapter", "Navigated to product detail for: " + product.getName());
            }
        } catch (Exception e) {
            Log.e("CleanProductsAdapter", "Error navigating to product detail: " + e.getMessage(), e);
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
        }
        
        public void bind(@NonNull ProductModel product) {
            Log.d("CleanProductsAdapter", "Binding product: " + product.getName() + " (ID: " + product.getProductId() + ")");
            
            productName.setText(product.getName());
            
            // Display likes and favorites counts
            likesCount.setText(String.valueOf(product.getLikesCount()));
            favoritesCount.setText(String.valueOf(product.getFavoritesCount()));
            
            // Set price and currency separately
            productPrice.setText(String.format("%.2f", product.getPrice()));
            productCurrency.setText(product.getCurrency());
            
            // Set product type if available
            if (product.getProductType() != null && !product.getProductType().isEmpty()) {
                displayProductType(product, productType);
            } else {
                productType.setVisibility(View.GONE);
            }
            
            // Setup like and favorite buttons
            setupLikeAndFavoriteButtons(product);
            
            // Load product images using imageIds from product
            loadProductImages(product);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                    // Navigate to product detail
                    navigateToProductDetail(product);
                }
            });
            
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onProductLongClick(product);
                }
                return true;
            });
        }
        
        private void setupLikeAndFavoriteButtons(ProductModel product) {
            // Set like button state using user preferences
            boolean isLiked = userPreferences.isProductLiked(product.getProductId());
            likeButton.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            
            // Set favorite button state using user preferences
            boolean isFavorited = userPreferences.isProductFavorited(product.getProductId());
            favoriteButton.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            
            // Set like button click listener
            likeButton.setOnClickListener(v -> {
                toggleLike(product);
            });
            
            // Set favorite button click listener
            favoriteButton.setOnClickListener(v -> {
                toggleFavorite(product);
            });
        }
        
        private void toggleLike(ProductModel product) {
            // Use UserProductPreferencesRepository for persistence
            userPreferences.toggleLike(product.getProductId());
            
            // Update likes count in the product model
            boolean isLiked = userPreferences.isProductLiked(product.getProductId());
            int currentLikesCount = product.getLikesCount();
            int newLikesCount = isLiked ? currentLikesCount + 1 : Math.max(0, currentLikesCount - 1);
            product.setLikesCount(newLikesCount);
            
            // Update UI immediately based on new state
            likeButton.setImageResource(isLiked ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            
            // Update the likes count display
            likesCount.setText(String.valueOf(newLikesCount));
        }
        
        private void toggleFavorite(ProductModel product) {
            // Use UserProductPreferencesRepository for persistence
            userPreferences.toggleFavorite(product.getProductId());
            
            // Update favorites count in the product model
            boolean isFavorited = userPreferences.isProductFavorited(product.getProductId());
            int currentFavoritesCount = product.getFavoritesCount();
            int newFavoritesCount = isFavorited ? currentFavoritesCount + 1 : Math.max(0, currentFavoritesCount - 1);
            product.setFavoritesCount(newFavoritesCount);
            
            // Update UI immediately based on new state
            favoriteButton.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            
            // Update the favorites count display
            favoritesCount.setText(String.valueOf(newFavoritesCount));
        }
        
        private void loadProductImages(ProductModel product) {
            if (product.hasImages()) {
                // Use primary image ID for now
                String primaryImageId = product.getPrimaryImageId();
                if (primaryImageId != null && !primaryImageId.isEmpty()) {
                    android.util.Log.d("CleanProductsAdapter", "Loading primary image for product: " + product.getProductId());
                    
                    // Load primary image asynchronously
                    imageService.getProductImage(primaryImageId)
                        .addOnSuccessListener(imageModel -> {
                            if (imageModel != null && imageModel.getImageUrl() != null) {
                                List<String> imageUrls = new ArrayList<>();
                                imageUrls.add(imageModel.getImageUrl());
                                
                                // Set up carousel with click listener
                                productImageCarousel.setOnImageClickListener(new ProductImageCarousel.OnImageClickListener() {
                                    @Override
                                    public void onImageClick(int position, String imageUrl) {
                                        Log.d("CleanProductsAdapter", "Carousel image clicked: position=" + position + ", product=" + product.getName());
                                        // Navigate to product detail when image is clicked
                                        navigateToProductDetail(product);
                                    }
                                    
                                    @Override
                                    public void onImageLongClick(int position, String imageUrl) {
                                        Log.d("CleanProductsAdapter", "Carousel image long clicked: position=" + position + ", product=" + product.getName());
                                        if (listener != null) {
                                            listener.onProductLongClick(product);
                                        }
                                    }
                                });
                                
                                // Set image URLs in carousel
                                productImageCarousel.setImageUrls(imageUrls);
                                productImageCarousel.setVisibility(View.VISIBLE);
                            } else {
                                Log.d("CleanProductsAdapter", "Image model is null or has no URL for product: " + product.getProductId());
                                showPlaceholder(productImageCarousel);
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("CleanProductsAdapter", "Failed to load primary image for product: " + product.getProductId(), e);
                            showPlaceholder(productImageCarousel);
                        });
                } else {
                    Log.d("CleanProductsAdapter", "No primary image ID for product: " + product.getProductId());
                    showPlaceholder(productImageCarousel);
                }
            } else {
                // No images, show placeholder
                android.util.Log.d("CleanProductsAdapter", "No images for product: " + product.getProductId() + ", showing placeholder");
                showPlaceholder(productImageCarousel);
            }
        }
        
        private void showPlaceholder(ProductImageCarousel carousel) {
            android.util.Log.d("CleanProductsAdapter", "Showing placeholder in carousel");
            // Hide carousel when no images available
            carousel.setVisibility(View.GONE);
        }
    }
    
    private List<String> getProductImageUrls(ProductModel product) {
        List<String> imageUrls = new ArrayList<>();
        
        if (product.hasImages()) {
            // Use primary image ID for now to avoid async issues
            String primaryImageId = product.getPrimaryImageId();
            if (primaryImageId != null && !primaryImageId.isEmpty()) {
                // For now, return empty list and let the carousel handle the primary image
                // TODO: Implement proper async image loading in future iteration
                android.util.Log.d("CleanProductsAdapter", "Using primary image ID: " + primaryImageId);
            }
        }
        
        return imageUrls;
    }
}
