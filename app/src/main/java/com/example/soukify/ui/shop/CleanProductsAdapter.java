package com.example.soukify.ui.shop;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.ProductImageModel;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.data.repositories.FavoritesTableRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CleanProductsAdapter extends RecyclerView.Adapter<CleanProductsAdapter.ProductViewHolder> {
    
    private List<ProductModel> products = new ArrayList<>();
    private OnProductClickListener listener;
    private Context context;
    private FirebaseProductImageService imageService;
    private ProductViewModel productViewModel;
    private FavoritesTableRepository favoritesRepository;
    private RecyclerView recyclerView;
    private boolean isFavoritesContext = false;
    
    public interface OnProductClickListener {
        void onProductClick(ProductModel product);
        void onProductLongClick(ProductModel product);
    }
    
    public CleanProductsAdapter(Context context, OnProductClickListener listener, ProductViewModel productViewModel) {
        this(context, listener, productViewModel, false);
    }
    
    public CleanProductsAdapter(Context context, OnProductClickListener listener, ProductViewModel productViewModel, boolean isFavoritesContext) {
        this.context = context;
        this.listener = listener;
        this.productViewModel = productViewModel;
        this.isFavoritesContext = isFavoritesContext;
        this.imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
        // FavoritesTableRepository will be accessed through ProductViewModel
        
        // Note: We'll set up observers when we have a lifecycle owner
    }
    
    public void setLifecycleOwner(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        this.setLifecycleOwner(lifecycleOwner, null);
    }
    
    public void setLifecycleOwner(androidx.lifecycle.LifecycleOwner lifecycleOwner, RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        if (lifecycleOwner != null && productViewModel != null) {
            // Remove any existing observers
            removeObservers();
            
            // Observe current product changes with lifecycle awareness
            productViewModel.getCurrentProduct().observe(lifecycleOwner, new Observer<ProductModel>() {
                @Override
                public void onChanged(ProductModel updatedProduct) {
                    if (updatedProduct != null) {
                        updateProductInList(updatedProduct);
                    }
                }
            });
            
            // Observe products list changes with lifecycle awareness
            productViewModel.getProducts().observe(lifecycleOwner, new Observer<List<ProductModel>>() {
                @Override
                public void onChanged(List<ProductModel> updatedProducts) {
                    if (updatedProducts != null) {
                        products = updatedProducts;
                        notifyDataSetChanged();
                    }
                }
            });
            
            // Observe product favorite states changes with lifecycle awareness
            productViewModel.getProductFavoriteStates().observe(lifecycleOwner, new Observer<Map<String, Boolean>>() {
                @Override
                public void onChanged(Map<String, Boolean> favoriteStates) {
                    if (favoriteStates != null) {
                        // Update all visible items with new favorite states
                        for (int i = 0; i < products.size(); i++) {
                            ProductModel product = products.get(i);
                            Boolean isFavorited = favoriteStates.get(product.getProductId());
                            if (isFavorited != null) {
                                // Find the ViewHolder for this position and update button state
                                if (recyclerView != null) {
                                    CleanProductsAdapter.ProductViewHolder viewHolder = 
                                        (CleanProductsAdapter.ProductViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                                    if (viewHolder != null) {
                                        ImageButton favoriteButton = viewHolder.itemView.findViewById(R.id.favoriteButton);
                                        if (favoriteButton != null) {
                                            favoriteButton.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
    
    private void removeObservers() {
        if (productViewModel != null) {
            // Remove observers when lifecycle owner is no longer valid
            productViewModel.getCurrentProduct().removeObservers(null);
            productViewModel.getProducts().removeObservers(null);
            productViewModel.getProductFavoriteStates().removeObservers(null);
        }
    }
    
    private void updateProductInList(ProductModel updatedProduct) {
        if (updatedProduct == null) return;
        
        // Find the product in our list and update it
        for (int i = 0; i < products.size(); i++) {
            ProductModel product = products.get(i);
            if (product.getProductId().equals(updatedProduct.getProductId())) {
                products.set(i, updatedProduct);
                notifyItemChanged(i);
                Log.d("CleanProductsAdapter", "Updated product in list: " + updatedProduct.getName() + 
                       " (Likes: " + updatedProduct.getLikesCount() + ")");
                break;
            }
        }
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
                
                // Add source parameter if in favorites context
                if (isFavoritesContext) {
                    bundle.putString("source", "favorites");
                }
                
                // Navigate using global navigation action
                navController.navigate(R.id.global_action_to_productDetail, bundle);
                
                Log.d("CleanProductsAdapter", "Navigated to product detail for: " + product.getName() + 
                       (isFavoritesContext ? " (from favorites)" : ""));
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
        private ImageButton shopLinkButton;
        
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
        
        public void bind(@NonNull ProductModel product) {
            Log.d("CleanProductsAdapter", "Binding product: " + product.getName() + " (ID: " + product.getProductId() + ")");
            
            productName.setText(product.getName());
            
            // Display likes and favorites counts
            likesCount.setText(String.valueOf(product.getLikesCount()));
            // Favorites count is handled by FavoritesTableRepository
            favoritesCount.setText("0"); // Will be updated by repository
            
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
            
            // Set click listener for card navigation (buttons have their own listeners)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onProductClick(product);
                    navigateToProductDetail(product);
                }
            });
        }
        
        private void setupLikeAndFavoriteButtons(ProductModel product) {
            // Set like button state - will be updated by observers
            likeButton.setImageResource(R.drawable.ic_heart_outline);
            
            // Set favorite button state - will be updated by observers
            favoriteButton.setImageResource(R.drawable.ic_star_outline);
            
            // Set proper click listeners for like and favorite buttons
            likeButton.setOnClickListener(v -> {
                Log.d("CleanProductsAdapter", "Like button clicked for product: " + product.getName());
                toggleLike(product);
            });
            
            likeButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    // Consume the touch event to prevent it from reaching the parent
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false; // Let the click listener handle the event
            });
            
            favoriteButton.setOnClickListener(v -> {
                Log.d("CleanProductsAdapter", "Favorite button clicked for product: " + product.getName());
                toggleFavorite(product);
            });
            
            favoriteButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                    // Consume the touch event to prevent it from reaching the parent
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                }
                return false; // Let the click listener handle the event
            });
            
            // Shop link button click handler - only show in favorites context
            if (isFavoritesContext) {
                shopLinkButton.setOnClickListener(v -> {
                    Log.d("CleanProductsAdapter", "Shop link button clicked for product: " + product.getName());
                    navigateToShop(product.getShopId());
                });
                
                shopLinkButton.setOnTouchListener((v, event) -> {
                    if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                        // Consume the touch event to prevent it from reaching the parent
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return false; // Let the click listener handle the event
                });
                shopLinkButton.setVisibility(View.VISIBLE);
            } else {
                shopLinkButton.setVisibility(View.GONE);
            }
            
            // Store the current product for click handling
            currentProduct = product;
        }
        
        private ProductModel currentProduct;
        
        private void toggleLike(ProductModel product) {
            if (productViewModel != null) {
                // Use ProductViewModel for Firebase synchronization
                productViewModel.toggleLikeProduct(product.getProductId());
                
                // Like state is handled by repository
                
                // Like count updates are handled by repository
                
                Log.d("CleanProductsAdapter", "Like toggled for product: " + product.getName());
            }
        }
        
        private void toggleFavorite(ProductModel product) {
            if (productViewModel != null) {
                // Use ProductViewModel for Firebase synchronization
                productViewModel.toggleFavoriteProduct(product.getProductId());
                
                // Update UI immediately for better user experience
                // Favorite state is handled by FavoritesTableRepository
                
                // Update favorite count - handled by FavoritesTableRepository
                
                // Update button icon immediately - handled by repository
                favoriteButton.setImageResource(R.drawable.ic_star_outline);
                
                // Update count text immediately
                // Favorites count is handled by FavoritesTableRepository
                favoritesCount.setText("0"); // Will be updated by repository
                
                // If in favorites context and product was unfavorited, remove it from the list
                // This logic is handled by FavoritesTableRepository
                
                Log.d("CleanProductsAdapter", "Favorite toggled for product: " + product.getName());
            }
        }
        
        private void navigateToShop(String shopId) {
            if (shopId == null || shopId.isEmpty()) {
                Log.w("CleanProductsAdapter", "Shop ID is null or empty, cannot navigate");
                return;
            }
            
            try {
                Activity activity = (Activity) context;
                if (activity != null) {
                    // Use navigation to go to shop home fragment directly
                    androidx.navigation.NavController navController = 
                        androidx.navigation.Navigation.findNavController(activity, R.id.nav_host_fragment_activity_main);
                    
                    // Create bundle with shop ID and source
                    Bundle bundle = new Bundle();
                    bundle.putString("shopId", shopId);
                    
                    // Add source parameter if in favorites context
                    if (isFavoritesContext) {
                        bundle.putString("source", "favorites");
                    }
                    
                    // Navigate to shop home fragment directly
                    navController.navigate(R.id.navigation_shop, bundle);
                    
                    Log.d("CleanProductsAdapter", "Navigated to shop for shop ID: " + shopId + 
                           (isFavoritesContext ? " (from favorites)" : ""));
                }
            } catch (Exception e) {
                Log.e("CleanProductsAdapter", "Error navigating to shop: " + e.getMessage(), e);
            }
        }
        
        private void removeProductFromList(ProductModel product) {
            int position = -1;
            for (int i = 0; i < products.size(); i++) {
                if (products.get(i).getProductId().equals(product.getProductId())) {
                    position = i;
                    break;
                }
            }
            
            if (position != -1) {
                products.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, products.size());
                Log.d("CleanProductsAdapter", "Removed product from favorites list: " + product.getName());
            }
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
                                
                                // Make the parent FrameLayout clickable when carousel is visible
                                FrameLayout parentLayout = (FrameLayout) productImageCarousel.getParent();
                                if (parentLayout != null) {
                                    // Disable carousel's own click handling to let parent handle it
                                    productImageCarousel.setClickable(false);
                                    productImageCarousel.setFocusable(false);
                                    
                                    parentLayout.setOnClickListener(v -> {
                                        Log.d("CleanProductsAdapter", "Image area clicked for product: " + product.getName());
                                        navigateToProductDetail(product);
                                    });
                                    parentLayout.setClickable(true);
                                    parentLayout.setFocusable(true);
                                }
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
            
            // Make the parent FrameLayout clickable to navigate to product detail
            FrameLayout parentLayout = (FrameLayout) carousel.getParent();
            if (parentLayout != null) {
                parentLayout.setOnClickListener(v -> {
                    Log.d("CleanProductsAdapter", "Placeholder image area clicked for product: " + currentProduct.getName());
                    navigateToProductDetail(currentProduct);
                });
                parentLayout.setClickable(true);
                parentLayout.setFocusable(true);
                parentLayout.setBackgroundColor(android.graphics.Color.parseColor("#FAFAFA"));
            }
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
