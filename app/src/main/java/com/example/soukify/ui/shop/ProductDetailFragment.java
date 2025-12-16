package com.example.soukify.ui.shop;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.content.pm.PackageManager;
import android.widget.FrameLayout;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import java.util.ArrayList;
import java.util.List;
import android.net.Uri;
import android.view.Gravity;
import android.widget.FrameLayout;

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
import com.example.soukify.ui.shop.ShopViewModel;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailFragment extends Fragment {
    
    private static final String ARG_PRODUCT = "product";
    
    private ProductModel product;
    private FirebaseProductImageService imageService;
    private ShopViewModel shopViewModel;
    private ShopModel currentShop;
    
    // ProductDialogHelper for edit functionality
    //private ProductDialogHelper productDialogHelper;
    
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
            // Fallback for Navigation Component
            if (product == null) {
                product = getArguments().getParcelable("product");
            }
        }
        
        imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
        
        // Initialize ShopViewModel to get current shop data
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        
        // Initialize ProductDialogHelper for edit functionality
        ProductManager productManager = new ProductManager(requireActivity().getApplication());
        productDialogHelper = new ProductDialogHelper(this, productManager, getImagePickerLauncher());
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
        setupProductDetails();
        loadProductImage();
        observeCurrentShop();
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
        
        // Basic info
        productName.setText(product.getName());
        productPrice.setText(String.format("%.2f", product.getPrice()));
        productCurrency.setText(product.getCurrency());
        
        // Description (full text, no truncation)
        if (product.getDescription() != null && !product.getDescription().isEmpty()) {
            productDescription.setText(product.getDescription());
        } else {
            productDescription.setText("No description available");
        }
        
        // Product type
        // Product type is now stored directly in the product
        if (product.getProductType() != null && !product.getProductType().isEmpty()) {
            productType.setText(product.getProductType());
            productType.setVisibility(View.VISIBLE);
        } else {
            productType.setVisibility(View.GONE);
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
            Log.d("ProductDetailFragment", "No valid image URLs to show");
            showImagePlaceholder();
            return;
        }
        
        // Safety check: ensure fragment is still attached to context
        if (!isAdded() || getContext() == null) {
            Log.d("ProductDetailFragment", "Fragment not attached to context, skipping carousel display");
            return;
        }
        
        // Check if we need to replace the ImageView with a carousel
        if (productImage != null && productImage.getParent() instanceof FrameLayout) {
            FrameLayout imageContainer = (FrameLayout) productImage.getParent();
            
            // Hide the single ImageView
            productImage.setVisibility(View.GONE);
            
            // Try to find existing carousel or create one
            ProductImageCarousel carousel = imageContainer.findViewById(R.id.productImageCarousel);
            if (carousel == null) {
                // Create carousel dynamically
                carousel = new ProductImageCarousel(requireContext());
                carousel.setId(R.id.productImageCarousel);
                
                // Add carousel to the container
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                );
                imageContainer.addView(carousel, params);
            }
            
            // Set up carousel with click listener
            carousel.setOnImageClickListener(new ProductImageCarousel.OnImageClickListener() {
                @Override
                public void onImageClick(int position, String imageUrl) {
                    Log.d("ProductDetailFragment", "Carousel image clicked: position=" + position + ", url=" + imageUrl);
                }
                
                @Override
                public void onImageLongClick(int position, String imageUrl) {
                    Log.d("ProductDetailFragment", "Carousel image long clicked: position=" + position + ", url=" + imageUrl);
                }
            });
            
            // Set images and show carousel
            carousel.setImageUrls(imageUrls);
            carousel.setVisibility(View.VISIBLE);
            
            // Hide placeholder
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisibility(View.GONE);
            }
            
            Log.d("ProductDetailFragment", "Showing " + imageUrls.size() + " images in carousel");
        } else {
            // Fallback to single image
            if (!imageUrls.isEmpty()) {
                showImage(Uri.parse(imageUrls.get(0)));
            } else {
                showImagePlaceholder();
            }
        }
    }
    
    private void showImage(Uri imageUri) {
        productImage.setVisibility(View.VISIBLE);
        imagePlaceholder.setVisibility(View.GONE);
        
        Glide.with(requireContext())
                .load(imageUri)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .into(productImage);
    }
    
    private void showImagePlaceholder() {
        productImage.setVisibility(View.GONE);
        imagePlaceholder.setVisibility(View.VISIBLE);
    }
    
    private void setupClickListeners() {
        // Toolbar back button navigation
        toolbar.setNavigationOnClickListener(v -> {
            // Simple navigation back
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        
        // Call button functionality
        callButton.setOnClickListener(v -> {
            // Get shop phone number
            String phoneNumber = getShopPhoneNumber();
            if (phoneNumber != null && !phoneNumber.isEmpty()) {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + phoneNumber));
                startActivity(callIntent);
            } else {
                Log.e("ProductDetailFragment", "Shop phone number not available");
            }
        });
        
        // Email button functionality
        emailButton.setOnClickListener(v -> {
            // Get shop email
            String sellerEmail = getShopEmail();
            if (sellerEmail != null && !sellerEmail.isEmpty()) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + sellerEmail));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about product: " + product.getName());
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi, I'm interested in your product. Please provide more details.");
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            } else {
                Log.e("ProductDetailFragment", "Shop email not available");
            }
        });
        
        editButton.setOnClickListener(v -> {
            if (productDialogHelper != null && product != null) {
                productDialogHelper.showEditProductDialog(product);
            }
        });
        
        deleteButton.setOnClickListener(v -> {
            showDeleteProductDialog();
        });
    }
    
    private void observeCurrentShop() {
        shopViewModel.getRepositoryShop().observe(getViewLifecycleOwner(), shop -> {
            currentShop = shop;
            Log.d("ProductDetailFragment", "Current shop updated: " + (shop != null ? shop.getName() : "null"));
        });
    }
    
    private String getShopPhoneNumber() {
        if (currentShop != null && currentShop.getPhone() != null && !currentShop.getPhone().isEmpty()) {
            return currentShop.getPhone();
        }
        Log.w("ProductDetailFragment", "Shop phone not available");
        return null;
    }
    
    private String getShopEmail() {
        if (currentShop != null && currentShop.getEmail() != null && !currentShop.getEmail().isEmpty()) {
            return currentShop.getEmail();
        }
        Log.w("ProductDetailFragment", "Shop email not available");
        return null;
    }
    
    private void showDeleteProductDialog() {
        if (product == null) {
            Log.e("ProductDetailFragment", "Product is null, cannot delete");
            return;
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete '" + product.getName() + "'? This action cannot be undone.")
                .setPositiveButton("Delete", (dialogInterface, which) -> {
                    String productId = product.getProductId();
                    String shopId = product.getShopId();
                    
                    if (productId != null && shopId != null) {
                        shopViewModel.deleteProduct(productId, shopId);
                        
                        // Navigate back after deletion
                        if (getActivity() != null) {
                            getActivity().getSupportFragmentManager().popBackStack();
                        }
                    } else {
                        Log.e("ProductDetailFragment", "Product ID or Shop ID is null");
                        shopViewModel.setErrorMessage("Cannot delete product: Missing IDs");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private boolean validateProductInput(String name, String description, String priceStr, String type) {
        if (TextUtils.isEmpty(name)) {
            shopViewModel.setErrorMessage("Product name is required");
            return false;
        }
        
        if (TextUtils.isEmpty(priceStr)) {
            shopViewModel.setErrorMessage("Price is required");
            return false;
        }
        
        try {
            double price = Double.parseDouble(priceStr);
            if (price < 0) {
                shopViewModel.setErrorMessage("Price must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            shopViewModel.setErrorMessage("Invalid price format");
            return false;
        }
        
        return true;
    }
}
