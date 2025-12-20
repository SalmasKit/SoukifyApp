package com.example.soukify.ui.shop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.widget.FrameLayout;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.ProductImageModel;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.example.soukify.data.repositories.UserProductPreferencesRepository;
import com.example.soukify.ui.shop.ShopViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailFragment extends Fragment implements ProductDialogHelper.OnProductUpdatedListener {
    
    private static final String ARG_PRODUCT = "product";
    private static final String ARG_SOURCE = "source";
    private static final String SOURCE_FAVORITES = "favorites";

    private ProductModel product;
    private String source;
    private FirebaseProductImageService imageService;
    private ShopViewModel shopViewModel;
    private ProductViewModel productViewModel;
    private ShopModel currentShop;
    private ProductManager productManager;

    // ProductDialogHelper for edit functionality
    private ProductDialogHelper productDialogHelper;

    // Views
    private ImageView productImage;
    private LinearLayout imagePlaceholder;
    private ProductImageCarousel productImageCarousel;
    private TextView productName;
    private TextView productType;
    private TextView productPrice;
    private TextView productCurrency;
    private TextView productDescription;
    private Toolbar toolbar;
    private ImageButton editButton;
    private ImageButton deleteButton;
    private ImageButton callButton;
    private ImageButton emailButton;
    private ImageButton likeButton;
    private ImageButton favoriteButton;
    private TextView likesCount;
    private TextView favoritesCount;
    private UserProductPreferencesRepository userPreferences;

    // Product details views
    private com.google.android.material.card.MaterialCardView productDetailsCard;
    private LinearLayout weightRow;
    private LinearLayout lengthRow;
    private LinearLayout widthRow;
    private LinearLayout heightRow;
    private LinearLayout colorRow;
    private LinearLayout materialRow;
    private TextView productWeight;
    private TextView productLength;
    private TextView productWidth;
    private TextView productHeight;
    private TextView productColor;
    private TextView productMaterial;

    public ProductDetailFragment() {
        // Required empty public constructor
    }

    public static ProductDetailFragment newInstance(ProductModel product) {
        ProductDetailFragment fragment = new ProductDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PRODUCT, product);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Handle Navigation Component arguments
        if (getArguments() != null) {
            product = getArguments().getParcelable(ARG_PRODUCT);
            source = getArguments().getString(ARG_SOURCE);
            // Fallback for Navigation Component
            if (product == null) {
                product = getArguments().getParcelable("product");
            }
        }

        imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());

        // Initialize ShopViewModel to get current shop data
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        
        // Initialize ProductViewModel for like/favorite functionality
        productViewModel = new ViewModelProvider(requireActivity(), new ProductViewModel.Factory(requireActivity().getApplication())).get(ProductViewModel.class);
        
        // Load current product into ProductViewModel for like/favorite synchronization
        if (product != null) {
            productViewModel.loadProduct(product.getProductId());
        }

        // Initialize ProductDialogHelper for edit functionality
        productManager = new ProductManager(requireActivity().getApplication());
        productDialogHelper = new ProductDialogHelper(this, productManager, getImagePickerLauncher());
        productDialogHelper.setOnProductUpdatedListener(this);
        
        // Initialize UserProductPreferencesRepository for like/favorite functionality
        userPreferences = new UserProductPreferencesRepository(requireContext());
    }

    /**
     * Register image picker launcher
     */
    private ActivityResultLauncher<Intent> getImagePickerLauncher() {
        return registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    if (productDialogHelper != null) {
                        productDialogHelper.handleImagePickerResult(result.getData());
                    }
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_detail, container, false);

        initViews(view);
        setupProductInfo();
        setupLikeAndFavoriteButtons();
        setupProductDetails();
        loadProductImage();
        observeCurrentShop();
        observeProductUpdates();
        observeProductViewModel();
        setupClickListeners();

        return view;
    }

    private void initViews(View view) {
        toolbar = view.findViewById(R.id.toolbar);
        productImage = view.findViewById(R.id.productImage);
        imagePlaceholder = view.findViewById(R.id.imagePlaceholder);
        productImageCarousel = view.findViewById(R.id.productImageCarousel);
        productName = view.findViewById(R.id.productName);
        productType = view.findViewById(R.id.productType);
        productPrice = view.findViewById(R.id.productPrice);
        productCurrency = view.findViewById(R.id.productCurrency);
        productDescription = view.findViewById(R.id.productDescription);
        callButton = view.findViewById(R.id.callButton);
        emailButton = view.findViewById(R.id.emailButton);
        editButton = view.findViewById(R.id.editButton);
        deleteButton = view.findViewById(R.id.deleteButton);
        
        // Hide edit and delete buttons if accessed from favorites
        if (SOURCE_FAVORITES.equals(source)) {
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }
        likeButton = view.findViewById(R.id.likeButton);
        favoriteButton = view.findViewById(R.id.favoriteButton);
        likesCount = view.findViewById(R.id.likesCount);
        favoritesCount = view.findViewById(R.id.favoritesCount);

        // Product details views
        productDetailsCard = view.findViewById(R.id.productDetailsCard);
        weightRow = view.findViewById(R.id.weightRow);
        lengthRow = view.findViewById(R.id.lengthRow);
        widthRow = view.findViewById(R.id.widthRow);
        heightRow = view.findViewById(R.id.heightRow);
        colorRow = view.findViewById(R.id.colorRow);
        materialRow = view.findViewById(R.id.materialRow);
        productWeight = view.findViewById(R.id.productWeight);
        productLength = view.findViewById(R.id.productLength);
        productWidth = view.findViewById(R.id.productWidth);
        productHeight = view.findViewById(R.id.productHeight);
        productColor = view.findViewById(R.id.productColor);
        productMaterial = view.findViewById(R.id.productMaterial);
    }

    private void setupProductInfo() {
        if (product == null) {
            Log.e("ProductDetailFragment", "Product is null");
            return;
        }

        Log.d("ProductDetailFragment", "Setting up product info for: " + product.getName());

        productName.setText(product.getName());
        productPrice.setText(String.format("%.2f", product.getPrice()));
        productCurrency.setText(product.getCurrency());
        productDescription.setText(product.getDescription());
        
        // Set product type if available
        if (product.getProductType() != null && !product.getProductType().isEmpty()) {
            productType.setText(product.getProductType());
            productType.setVisibility(View.VISIBLE);
        } else {
            productType.setVisibility(View.GONE);
        }
    }
    
    private void setupLikeAndFavoriteButtons() {
        if (product != null && productViewModel != null) {
            // Set like button state - handled by repository
            likeButton.setImageResource(R.drawable.ic_heart_outline);
            
            // Set favorite button state - handled by repository
            favoriteButton.setImageResource(R.drawable.ic_star_outline);
            
            // Set like and favorite counts
            if (likesCount != null) {
                likesCount.setText(String.valueOf(product.getLikesCount()));
            }
            if (favoritesCount != null) {
                // Favorites count is handled by FavoritesTableRepository
                favoritesCount.setText("0"); // Will be updated by repository
            }
            
            // Set like button click listener
            likeButton.setOnClickListener(v -> {
                toggleLike();
            });
            
            // Set favorite button click listener
            favoriteButton.setOnClickListener(v -> {
                toggleFavorite();
            });
        }
    }
    
    private void toggleLike() {
        if (product != null && productViewModel != null) {
            // Use ProductViewModel for Firebase synchronization
            productViewModel.toggleLikeProduct(product.getProductId());
        }
    }
    
    private void toggleFavorite() {
        if (product != null && productViewModel != null) {
            // Use ProductViewModel for Firebase synchronization
            productViewModel.toggleFavoriteProduct(product.getProductId());
        }
    }

    private void setupProductDetails() {
        if (product == null) {
            Log.e("ProductDetailFragment", "Product is null, cannot setup details");
            return;
        }

        Log.d("ProductDetailFragment", "Setting up product details");

        // Check if product has any details to show
        if (!product.hasDetails()) {
            // Hide the entire details card if no details available
            productDetailsCard.setVisibility(View.GONE);
            return;
        }

        // Show the details card
        productDetailsCard.setVisibility(View.VISIBLE);

        // Weight
        if (product.getWeight() != null && product.getWeight() > 0) {
            productWeight.setText(String.format("%.1f kg", product.getWeight()));
            weightRow.setVisibility(View.VISIBLE);
        } else {
            weightRow.setVisibility(View.GONE);
        }

        // Dimensions - show each dimension individually
        String length = product.getFormattedLength();
        if (length != null) {
            productLength.setText(length);
            lengthRow.setVisibility(View.VISIBLE);
        } else {
            lengthRow.setVisibility(View.GONE);
        }

        String width = product.getFormattedWidth();
        if (width != null) {
            productWidth.setText(width);
            widthRow.setVisibility(View.VISIBLE);
        } else {
            widthRow.setVisibility(View.GONE);
        }

        String height = product.getFormattedHeight();
        if (height != null) {
            productHeight.setText(height);
            heightRow.setVisibility(View.VISIBLE);
        } else {
            heightRow.setVisibility(View.GONE);
        }

        // Color
        if (product.getColor() != null && !product.getColor().trim().isEmpty()) {
            productColor.setText(product.getColor());
            colorRow.setVisibility(View.VISIBLE);
        } else {
            colorRow.setVisibility(View.GONE);
        }

        // Material
        if (product.getMaterial() != null && !product.getMaterial().trim().isEmpty()) {
            productMaterial.setText(product.getMaterial());
            materialRow.setVisibility(View.VISIBLE);
        } else {
            materialRow.setVisibility(View.GONE);
        }

        Log.d("ProductDetailFragment", "Product details setup completed");
    }

    private void loadProductImage() {
        if (product == null) {
            Log.d("ProductDetailFragment", "Product is null, showing placeholder");
            showImagePlaceholder();
            return;
        }

        Log.d("ProductDetailFragment", "Checking image data for product: " + product.getName());
        Log.d("ProductDetailFragment", "hasImages: " + product.hasImages());
        Log.d("ProductDetailFragment", "imageIds: " + (product.getImageIds() != null ? product.getImageIds().size() : "null"));
        Log.d("ProductDetailFragment", "primaryImageId: " + product.getPrimaryImageId());

        if (product.hasImages() && product.getImageIds() != null && !product.getImageIds().isEmpty()) {
            Log.d("ProductDetailFragment", "Loading multiple images for product: " + product.getProductId());
            loadMultipleProductImages();
        } else if (product.getPrimaryImageId() != null && !product.getPrimaryImageId().isEmpty()) {
            Log.d("ProductDetailFragment", "Loading primary image with imageId: " + product.getPrimaryImageId());
            loadSingleProductImage(product.getPrimaryImageId());
        } else {
            Log.d("ProductDetailFragment", "No images available, showing placeholder");
            showImagePlaceholder();
        }
    }

    private void loadMultipleProductImages() {
        List<String> imageIds = product.getImageIds();
        List<String> imageUrls = new ArrayList<>();

        // Load all images asynchronously
        int[] loadedCount = {0};
        int totalImages = imageIds.size();

        for (String imageId : imageIds) {
            if (imageId != null && !imageId.isEmpty()) {
                imageService.getProductImage(imageId)
                    .addOnSuccessListener(productImageModel -> {
                        if (productImageModel != null && productImageModel.getImageUrl() != null) {
                            synchronized (imageUrls) {
                                imageUrls.add(productImageModel.getImageUrl());
                            }
                        }

                        synchronized (loadedCount) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalImages) {
                                // All images loaded, show them in carousel
                                showImagesInCarousel(imageUrls);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("ProductDetailFragment", "Failed to load image: " + imageId, e);

                        synchronized (loadedCount) {
                            loadedCount[0]++;
                            if (loadedCount[0] == totalImages) {
                                // All images processed (some may have failed), show what we have
                                showImagesInCarousel(imageUrls);
                            }
                        }
                    });
            } else {
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalImages) {
                        showImagesInCarousel(imageUrls);
                    }
                }
            }
        }
    }

    private void loadSingleProductImage(String imageId) {
        imageService.getProductImage(imageId)
                .addOnSuccessListener(productImageModel -> {
                    if (productImageModel != null && productImageModel.getImageUrl() != null) {
                        String imageUrl = productImageModel.getImageUrl();
                        Log.d("ProductDetailFragment", "Loading image from URL: " + imageUrl);

                        if (imageUrl.startsWith("file://")) {
                            // Local file
                            try {
                                URI uri = new URI(imageUrl);
                                File imageFile = new File(uri);
                                if (imageFile.exists()) {
                                    showImage(Uri.fromFile(imageFile));
                                } else {
                                    Log.e("ProductDetailFragment", "Local image file not found: " + imageUrl);
                                    showImagePlaceholder();
                                }
                            } catch (Exception e) {
                                Log.e("ProductDetailFragment", "Error loading local image: " + e.getMessage(), e);
                                showImagePlaceholder();
                            }
                        } else {
                            // Remote URL
                            showImage(Uri.parse(imageUrl));
                        }
                    } else {
                        Log.d("ProductDetailFragment", "ProductImageModel or imageUrl is null");
                        showImagePlaceholder();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ProductDetailFragment", "Failed to load product image", e);
                    showImagePlaceholder();
                });
    }

    private void showImagesInCarousel(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            Log.d("ProductDetailFragment", "No valid image URLs to show, adding test image");
            showImagePlaceholder();
            return;
        }

        Log.d("ProductDetailFragment", "Showing " + imageUrls.size() + " images in carousel");
        for (int i = 0; i < imageUrls.size(); i++) {
            Log.d("ProductDetailFragment", "Image URL " + i + ": " + imageUrls.get(i));
        }
        
        // Safety check: ensure fragment is still attached to context
        if (!isAdded() || getContext() == null) {
            Log.d("ProductDetailFragment", "Fragment not attached to context, skipping carousel display");
            return;
        }

        // Check if we need to replace ImageView with a carousel
        if (productImage != null && productImage.getParent() instanceof FrameLayout) {
            FrameLayout imageContainer = (FrameLayout) productImage.getParent();

            // Hide single ImageView and placeholder
            productImage.setVisibility(View.GONE);
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisibility(View.GONE);
            }

            // Try to find existing carousel or create one
            ProductImageCarousel carousel = imageContainer.findViewById(R.id.productImageCarousel);
            if (carousel == null) {
                Log.d("ProductDetailFragment", "Creating new carousel dynamically");
                // Create carousel dynamically
                carousel = new ProductImageCarousel(requireContext());
                carousel.setId(R.id.productImageCarousel);

                // Add carousel to the container
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                );
                imageContainer.addView(carousel, params);
            } else {
                Log.d("ProductDetailFragment", "Using existing carousel");
            }

            // Set up carousel with click listener
            carousel.setOnImageClickListener(new ProductImageCarousel.OnImageClickListener() {
                @Override
                public void onImageClick(int position, String imageUrl) {
                    Log.d("ProductDetailFragment", "Carousel image clicked: position=" + position + ", product=" + product.getName());
                }
                
                @Override
                public void onImageLongClick(int position, String imageUrl) {
                    Log.d("ProductDetailFragment", "Carousel image long clicked: position=" + position + ", product=" + product.getName());
                }
            });
            
            // Set image URLs in carousel
            carousel.setImageUrls(imageUrls);
            carousel.setVisibility(View.VISIBLE);
            
            Log.d("ProductDetailFragment", "Carousel visibility set to VISIBLE");
        } else {
            Log.e("ProductDetailFragment", "Could not find image container or productImage is null");
        }
    }

    private void showImage(Uri imageUri) {
        if (productImage != null && getContext() != null) {
            productImage.setVisibility(View.VISIBLE);
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisibility(View.GONE);
            }
            if (productImageCarousel != null) {
                productImageCarousel.setVisibility(View.GONE);
            }
            
            Glide.with(requireContext())
                    .load(imageUri)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(productImage);
        }
    }

    private void showImagePlaceholder() {
        if (getContext() != null) {
            if (productImage != null) {
                productImage.setVisibility(View.GONE);
            }
            if (productImageCarousel != null) {
                productImageCarousel.setVisibility(View.GONE);
            }
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisibility(View.VISIBLE);
            }
        }
    }

    private void observeCurrentShop() {
        // Observe current shop from ShopViewModel
        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            currentShop = shop;
            updateShopDependentUI();
        });
    }

    private void observeProductUpdates() {
        // TODO: Implement product updates observation when ShopViewModel methods are available
        // For now, this will be handled by onProductUpdated callback
    }
    
    private void observeProductViewModel() {
        if (productViewModel == null) return;
        
        // Observe current product for like/favorite updates
        productViewModel.getCurrentProduct().observe(getViewLifecycleOwner(), updatedProduct -> {
            if (updatedProduct != null && product != null && 
                updatedProduct.getProductId().equals(product.getProductId())) {
                // Update the local product reference
                product = updatedProduct;
                
                // Update like/favorite UI
                updateLikeFavoriteUI();
            }
        });
        
        // Observe loading states for like/favorite operations
        productViewModel.getLikeOperationInProgress().observe(getViewLifecycleOwner(), isInProgress -> {
            if (isInProgress != null && likeButton != null) {
                likeButton.setEnabled(!isInProgress);
            }
        });
        
        // Observe product favorite states changes
        productViewModel.getProductFavoriteStates().observe(getViewLifecycleOwner(), favoriteStates -> {
            if (favoriteStates != null && product != null) {
                Boolean isFavorited = favoriteStates.get(product.getProductId());
                if (isFavorited != null && favoriteButton != null) {
                    favoriteButton.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                }
            }
        });
        
        productViewModel.getFavoriteOperationInProgress().observe(getViewLifecycleOwner(), isInProgress -> {
            if (isInProgress != null && favoriteButton != null) {
                favoriteButton.setEnabled(!isInProgress);
            }
        });
        
        // Observe error messages
        productViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                productViewModel.clearError();
            }
        });
    }
    
    private void updateLikeFavoriteUI() {
        if (product == null) return;
        
        // Update like button state - handled by repository
        if (likeButton != null) {
            likeButton.setImageResource(R.drawable.ic_heart_outline);
        }
        
        // Update favorite button state - handled by repository
        if (favoriteButton != null) {
            favoriteButton.setImageResource(R.drawable.ic_star_outline);
        }
        
        // Update counts
        if (likesCount != null) {
            likesCount.setText(String.valueOf(product.getLikesCount()));
        }
        if (favoritesCount != null) {
            // Favorites count is handled by FavoritesTableRepository
            favoritesCount.setText("0"); // Will be updated by repository
        }
    }

    private void setupClickListeners() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                if (currentShop != null && product != null) {
                    showEditProductDialog();
                } else {
                    Log.w("ProductDetailFragment", "Cannot edit product: no current shop or product");
                }
            });
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                if (currentShop != null && product != null) {
                    showDeleteConfirmationDialog();
                } else {
                    Log.w("ProductDetailFragment", "Cannot delete product: no current shop or product");
                }
            });
        }

        if (callButton != null) {
            callButton.setOnClickListener(v -> {
                if (currentShop != null && currentShop.getPhone() != null) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + currentShop.getPhone()));
                    startActivity(callIntent);
                } else {
                    Log.w("ProductDetailFragment", "No phone number available");
                }
            });
        }

        if (emailButton != null) {
            emailButton.setOnClickListener(v -> {
                if (currentShop != null && currentShop.getEmail() != null) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                    emailIntent.setData(Uri.parse("mailto:" + currentShop.getEmail()));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about product: " + 
                            (product != null ? product.getName() : "Unknown"));
                    startActivity(emailIntent);
                } else {
                    Log.w("ProductDetailFragment", "No email address available");
                }
            });
        }
    }

    private void updateShopDependentUI() {
        // Check if user owns this shop
        boolean isOwner = false;
        if (currentShop != null && product != null) {
            // Check if the product belongs to the current shop
            isOwner = currentShop.getShopId() != null && 
                     currentShop.getShopId().equals(product.getShopId());
        }
        
        // Update edit/delete buttons based on shop ownership and source
        boolean shouldShowButtons = isOwner && !SOURCE_FAVORITES.equals(source);
        if (editButton != null) {
            editButton.setVisibility(shouldShowButtons ? View.VISIBLE : View.GONE);
        }
        if (deleteButton != null) {
            deleteButton.setVisibility(shouldShowButtons ? View.VISIBLE : View.GONE);
        }
        
        Log.d("ProductDetailFragment", "Edit/Delete buttons visibility: " + (isOwner ? "VISIBLE" : "GONE") + 
              " (currentShopId: " + (currentShop != null ? currentShop.getShopId() : "null") + 
              ", productShopId: " + (product != null ? product.getShopId() : "null") + ")");
    }

    private void showEditProductDialog() {
        if (productDialogHelper != null && product != null) {
            productDialogHelper.showEditProductDialog(product);
        }
    }

    private void showDeleteConfirmationDialog() {
        if (getContext() == null || product == null) return;

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete " + product.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteProduct();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct() {
        if (productManager != null && product != null) {
            Log.d("ProductDetailFragment", "Delete requested for product: " + product.getProductId());
            
            // Show loading indicator
            if (getContext() != null) {
                Toast.makeText(getContext(), "Deleting product...", Toast.LENGTH_SHORT).show();
            }
            
            // Use a flag to prevent multiple message handling
            final boolean[] messageHandled = {false};
            
            // Observe the success and error messages from ProductManager
            productManager.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
                if (!messageHandled[0] && message != null && !message.isEmpty()) {
                    messageHandled[0] = true; // Mark as handled
                    // Product deleted successfully
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    // Navigate back to previous screen
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });
            
            productManager.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                if (!messageHandled[0] && message != null && !message.isEmpty()) {
                    messageHandled[0] = true; // Mark as handled
                    // Error occurred during deletion
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });
            
            // Call ProductManager's delete method
            productManager.deleteProduct(product);
        }
    }

    @Override
    public void onProductUpdated() {
        // Refresh product display - the updated product should be available through other means
        if (product != null) {
            setupProductInfo();
            setupProductDetails();
            loadProductImage();
            setupLikeAndFavoriteButtons();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up references to avoid memory leaks
        productImage = null;
        imagePlaceholder = null;
        productImageCarousel = null;
        productName = null;
        productType = null;
        productPrice = null;
        productCurrency = null;
        productDescription = null;
        toolbar = null;
        editButton = null;
        deleteButton = null;
        callButton = null;
        emailButton = null;
        likeButton = null;
        favoriteButton = null;
        productDetailsCard = null;
        weightRow = null;
        lengthRow = null;
        widthRow = null;
        heightRow = null;
        colorRow = null;
        materialRow = null;
        productWeight = null;
        productLength = null;
        productWidth = null;
        productHeight = null;
        productColor = null;
        productMaterial = null;
    }
}
