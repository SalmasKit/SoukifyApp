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
import android.widget.Toast;
import android.widget.FrameLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.ProductModel;
import com.example.soukify.data.models.ShopModel;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ProductDetailFragment extends Fragment implements ProductDialogHelper.OnProductUpdatedListener {

    private static final String TAG = "ProductDetailFragment";
    private static final String ARG_PRODUCT = "product";
    private static final String ARG_SOURCE = "source";
    private static final String SOURCE_FAVORITES = "favorites";

    private ProductModel product;
    private String productId;
    private String source;
    private FirebaseProductImageService imageService;
    private ShopViewModel shopViewModel;
    private ProductViewModel productViewModel;
    private ShopModel currentShop;
    private ProductManager productManager;
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

    // Realtime listener for product stats
    private ListenerRegistration productStatsListener;

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

        if (getArguments() != null) {
            product = getArguments().getParcelable(ARG_PRODUCT);
            source = getArguments().getString(ARG_SOURCE);
            if (product == null) {
                product = getArguments().getParcelable("product");
            }
        }

        imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
        shopViewModel = new ViewModelProvider(requireActivity()).get(ShopViewModel.class);
        productViewModel = new ViewModelProvider(requireActivity(),
                new ProductViewModel.Factory(requireActivity().getApplication())).get(ProductViewModel.class);

        if (product != null) {
            productViewModel.loadProduct(product.getProductId());
            productId = product.getProductId();
        }

        productManager = new ProductManager(requireActivity().getApplication());
        productDialogHelper = new ProductDialogHelper(this, productManager, getImagePickerLauncher());
        productDialogHelper.setOnProductUpdatedListener(this);
    }

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
        observeProductViewModel();
        setupClickListeners();
        setupRealtimeProductStats();

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
        likeButton = view.findViewById(R.id.likeButton);
        favoriteButton = view.findViewById(R.id.favoriteButton);
        likesCount = view.findViewById(R.id.likesCount);
        favoritesCount = view.findViewById(R.id.favoritesCount);

        if (favoritesCount != null) {
            favoritesCount.setVisibility(View.GONE);
        }

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
            Log.e(TAG, "Product is null");
            return;
        }

        Log.d(TAG, "Setting up product info for: " + product.getName());

        productName.setText(product.getName());
        productPrice.setText(String.format("%.2f", product.getPrice()));
        productCurrency.setText(product.getCurrency());
        productDescription.setText(product.getDescription());

        if (product.getProductType() != null && !product.getProductType().isEmpty()) {
            productType.setText(product.getProductType());
            productType.setVisibility(View.VISIBLE);
        } else {
            productType.setVisibility(View.GONE);
        }
    }

    private void setupProductDetails() {
        if (product == null) {
            Log.e(TAG, "Product is null, cannot setup details");
            return;
        }

        if (!product.hasDetails()) {
            productDetailsCard.setVisibility(View.GONE);
            return;
        }

        productDetailsCard.setVisibility(View.VISIBLE);

        // Weight
        if (product.getWeight() != null && product.getWeight() > 0) {
            productWeight.setText(String.format("%.1f kg", product.getWeight()));
            weightRow.setVisibility(View.VISIBLE);
        } else {
            weightRow.setVisibility(View.GONE);
        }

        // Dimensions
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
    }

    private void loadProductImage() {
        if (product == null) {
            showImagePlaceholder();
            return;
        }

        if (product.hasImages() && product.getImageIds() != null && !product.getImageIds().isEmpty()) {
            loadMultipleProductImages();
        } else if (product.getPrimaryImageId() != null && !product.getPrimaryImageId().isEmpty()) {
            loadSingleProductImage(product.getPrimaryImageId());
        } else {
            showImagePlaceholder();
        }
    }

    private void loadMultipleProductImages() {
        List<String> imageIds = product.getImageIds();
        List<String> imageUrls = new ArrayList<>();
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
                                    showImagesInCarousel(imageUrls);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            synchronized (loadedCount) {
                                loadedCount[0]++;
                                if (loadedCount[0] == totalImages) {
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
                        if (imageUrl.startsWith("file://")) {
                            try {
                                URI uri = new URI(imageUrl);
                                File imageFile = new File(uri);
                                if (imageFile.exists()) {
                                    showImage(Uri.fromFile(imageFile));
                                } else {
                                    showImagePlaceholder();
                                }
                            } catch (Exception e) {
                                showImagePlaceholder();
                            }
                        } else {
                            showImage(Uri.parse(imageUrl));
                        }
                    } else {
                        showImagePlaceholder();
                    }
                })
                .addOnFailureListener(e -> {
                    showImagePlaceholder();
                });
    }

    private void showImagesInCarousel(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            showImagePlaceholder();
            return;
        }

        if (!isAdded() || getContext() == null) {
            return;
        }

        if (productImage != null && productImage.getParent() instanceof FrameLayout) {
            FrameLayout imageContainer = (FrameLayout) productImage.getParent();
            productImage.setVisibility(View.GONE);
            if (imagePlaceholder != null) {
                imagePlaceholder.setVisibility(View.GONE);
            }

            ProductImageCarousel carousel = imageContainer.findViewById(R.id.productImageCarousel);
            if (carousel == null) {
                carousel = new ProductImageCarousel(requireContext());
                carousel.setId(R.id.productImageCarousel);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                );
                imageContainer.addView(carousel, params);
            }

            carousel.setOnImageClickListener(new ProductImageCarousel.OnImageClickListener() {
                @Override
                public void onImageClick(int position, String imageUrl) {
                    Log.d(TAG, "Image clicked at position: " + position);
                }

                @Override
                public void onImageLongClick(int position, String imageUrl) {
                    Log.d(TAG, "Image long clicked at position: " + position);
                }
            });

            carousel.setImageUrls(imageUrls);
            carousel.setVisibility(View.VISIBLE);
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
        shopViewModel.getShop().observe(getViewLifecycleOwner(), shop -> {
            currentShop = shop;
            updateShopDependentUI();
        });
    }

    /**
     * Observer le ProductViewModel pour les mises à jour de like/favorite
     */
    private void observeProductViewModel() {
        if (productViewModel == null) return;

        // Observer les changements du produit
        productViewModel.getCurrentProduct().observe(getViewLifecycleOwner(), updatedProduct -> {
            if (updatedProduct != null && product != null &&
                    updatedProduct.getProductId().equals(product.getProductId())) {

                Log.d(TAG, "✅ Product updated: " + updatedProduct.getName() +
                        " (liked=" + updatedProduct.isLikedByUser() +
                        ", favorite=" + updatedProduct.isFavoriteByUser() +
                        ", likes=" + updatedProduct.getLikesCount() + ")");

                // Mettre à jour la référence locale
                product = updatedProduct;

                // Mettre à jour l'UI
                updateLikeButton(product.isLikedByUser(), product.getLikesCount());
                updateFavoriteButton(product.isFavoriteByUser());
            }
        });

        // Observer les erreurs
        productViewModel.getErrorMessage().observe(getViewLifecycleOwner(), errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty() && getContext() != null) {
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                productViewModel.clearError();
            }
        });
    }

    /**
     * Met à jour le bouton like avec l'état et le compteur
     */
    private void updateLikeButton(boolean liked, int count) {
        if (likeButton != null) {
            likeButton.setImageResource(liked ?
                    R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            likeButton.setColorFilter(liked ? 0xFFE85F4C : 0xFF757575);
        }
        if (likesCount != null) {
            likesCount.setText(String.valueOf(count));
        }
        Log.d(TAG, "✅ Like button updated: liked=" + liked + ", count=" + count);
    }

    /**
     * Met à jour le bouton favorite avec l'état
     */
    private void updateFavoriteButton(boolean favorite) {
        if (favoriteButton != null) {
            favoriteButton.setImageResource(favorite ?
                    R.drawable.ic_star_filled : R.drawable.ic_star_outline);
            favoriteButton.setColorFilter(favorite ? 0xFFFFC107 : 0xFF757575);
        }
        Log.d(TAG, "✅ Favorite button updated: favorite=" + favorite);
    }

    private void setupClickListeners() {
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().onBackPressed();
                }
            });
        }

        // Like button - déléguer au ViewModel
        if (likeButton != null) {
            likeButton.setOnClickListener(v -> {
                Log.d(TAG, "❤️ Like button clicked");
                if (product != null && productViewModel != null) {
                    productViewModel.toggleLikeProduct(product.getProductId());
                    // L'UI sera mise à jour via l'observer
                } else {
                    Log.e(TAG, "❌ Cannot toggle like: product or viewModel is null");
                }
            });
        }

        // Favorite button - déléguer au ViewModel
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> {
                Log.d(TAG, "⭐ Favorite button clicked");
                if (product != null && productViewModel != null) {
                    productViewModel.toggleFavoriteProduct(product.getProductId());
                    // L'UI sera mise à jour via l'observer
                } else {
                    Log.e(TAG, "❌ Cannot toggle favorite: product or viewModel is null");
                }
            });
        }

        if (editButton != null) {
            editButton.setOnClickListener(v -> {
                if (currentShop != null && product != null) {
                    showEditProductDialog();
                }
            });
        }

        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> {
                if (currentShop != null && product != null) {
                    showDeleteConfirmationDialog();
                }
            });
        }

        if (callButton != null) {
            callButton.setOnClickListener(v -> {
                if (currentShop != null && currentShop.getPhone() != null) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + currentShop.getPhone()));
                    startActivity(callIntent);
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
                }
            });
        }
    }

    private void updateShopDependentUI() {
        String currentUserId = null;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        boolean isOwner = false;
        if (currentShop != null && currentUserId != null) {
            isOwner = currentUserId.equals(currentShop.getUserId());
        }

        if (editButton != null) {
            editButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }

        if (deleteButton != null) {
            deleteButton.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }
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
                .setPositiveButton("Delete", (dialog, which) -> deleteProduct())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteProduct() {
        if (productManager != null && product != null) {
            if (getContext() != null) {
                Toast.makeText(getContext(), getString(R.string.deleting_product), Toast.LENGTH_SHORT).show();
            }

            final boolean[] messageHandled = {false};

            productManager.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
                if (!messageHandled[0] && message != null && !message.isEmpty()) {
                    messageHandled[0] = true;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                    if (getActivity() != null) {
                        getActivity().onBackPressed();
                    }
                }
            });

            productManager.getErrorMessage().observe(getViewLifecycleOwner(), message -> {
                if (!messageHandled[0] && message != null && !message.isEmpty()) {
                    messageHandled[0] = true;
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                    }
                }
            });

            productManager.deleteProduct(product);
        }
    }

    @Override
    public void onProductUpdated() {
        if (product != null) {
            setupProductInfo();
            setupProductDetails();
            loadProductImage();
        }
    }

    /**
     * Écouter les changements en temps réel depuis Firestore
     * Cela permet de synchroniser les données même si un autre utilisateur modifie le produit
     */
    private void setupRealtimeProductStats() {
        if (product == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        productStatsListener = db.collection("products")
                .document(product.getProductId())
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) {
                        Log.e(TAG, "Error listening to product stats", e);
                        return;
                    }
                    applyProductSnapshot(snapshot);
                });
    }

    /**
     * Applique les données du snapshot Firestore au produit local
     */
    private void applyProductSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists() || product == null) return;

        // Mettre à jour le compteur de likes depuis Firestore
        Long likesLong = snapshot.getLong("likesCount");
        if (likesLong != null) {
            int newLikesCount = likesLong.intValue();
            if (newLikesCount != product.getLikesCount()) {
                product.setLikesCount(newLikesCount);
                if (likesCount != null) {
                    likesCount.setText(String.valueOf(newLikesCount));
                }
                Log.d(TAG, "📊 Likes count updated from Firestore: " + newLikesCount);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (productStatsListener != null) {
            productStatsListener.remove();
            productStatsListener = null;
        }

        // Cleanup
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
        likesCount = null;
        favoritesCount = null;
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