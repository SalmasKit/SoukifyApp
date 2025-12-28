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
import com.example.soukify.ui.settings.SettingsFragment;
import com.example.soukify.utils.LocaleHelper;
import com.example.soukify.utils.CurrencyHelper;
import com.google.android.gms.tasks.Task;
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
import com.example.soukify.data.sync.ShopSync;
import com.example.soukify.data.sync.ProductSync;
import java.util.HashMap;
import java.util.Map;

public class ProductDetailFragment extends Fragment implements ProductDialogHelper.OnProductUpdatedListener, ShopSync.SyncListener, ProductSync.SyncListener {

    private static final String TAG = "ProductDetailFragment";
    private static final String ARG_PRODUCT = "product";
    private static final String ARG_PRODUCT_ID = "productId";
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
            productId = getArguments().getString(ARG_PRODUCT_ID);
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
            productId = product.getProductId();
        }

        if (productId != null) {
            productViewModel.loadProduct(productId);
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
            // Localized price
            String localizedPrice = CurrencyHelper.formatLocalizedPrice(requireContext(), product.getPrice(), product.getCurrency());
            productPrice.setText(localizedPrice);
            productCurrency.setVisibility(View.GONE);
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
            productWeight.setText(getString(R.string.weight_format, product.getWeight(), getString(R.string.unit_kg)));
            weightRow.setVisibility(View.VISIBLE);
        } else {
            weightRow.setVisibility(View.GONE);
        }

        // Dimensions
        if (product.getLength() != null && product.getLength() > 0) {
            productLength.setText(getString(R.string.dimension_format, product.getLength(), getString(R.string.unit_cm)));
            lengthRow.setVisibility(View.VISIBLE);
        } else {
            lengthRow.setVisibility(View.GONE);
        }

        if (product.getWidth() != null && product.getWidth() > 0) {
            productWidth.setText(getString(R.string.dimension_format, product.getWidth(), getString(R.string.unit_cm)));
            widthRow.setVisibility(View.VISIBLE);
        } else {
            widthRow.setVisibility(View.GONE);
        }

        if (product.getHeight() != null && product.getHeight() > 0) {
            productHeight.setText(getString(R.string.dimension_format, product.getHeight(), getString(R.string.unit_cm)));
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
     * Observer le ProductViewModel pour les mises à jour
     */
    private void observeProductViewModel() {
        if (productViewModel == null) return;

        // Connecter le ViewModel au Repository via le LifecycleOwner de la vue
        productViewModel.setupObservers(getViewLifecycleOwner());

        // Observer les changements du produit
        // Observer les changements du produit
        productViewModel.getCurrentProduct().observe(getViewLifecycleOwner(), updatedProduct -> {
            // Fix: Allow update if product is null (e.g., opened from notification with only ID)
            if (updatedProduct != null && (product == null || 
                    updatedProduct.getProductId().equals(product.getProductId()))) {

                Log.d(TAG, "✅ Product updated: " + updatedProduct.getName());

                // Mettre à jour la référence locale
                product = updatedProduct;

                // Mettre à jour l'UI complète
                setupProductInfo();
                setupProductDetails();
                
                // Mettre à jour l'image si elle a changé
                // Note: loadProductImage vérifie si l'image est déjà chargée ou différente
                // Mais pour être sûr lors d'un edit, on peut recharger
                if (updatedProduct.hasImages() || updatedProduct.getPrimaryImageId() != null) {
                     loadProductImage();
                }

                // Use Sync States for initial display
                if (getActivity() != null && getActivity().getApplication() != null) {
                    android.app.Application app = (android.app.Application) getActivity().getApplication();
                    ProductSync.LikeSync.LikeState likeState = ProductSync.LikeSync.getState(product.getProductId());
                    boolean liked = likeState != null ? likeState.isLiked : product.isLikedByUser();
                    int count = likeState != null ? likeState.count : product.getLikesCount();
                    
                    ProductSync.FavoriteSync.FavoriteState favState = ProductSync.FavoriteSync.getState(product.getProductId(), app);
                    boolean fav = favState != null ? favState.isFavorite : product.isFavoriteByUser();
    
                    updateLikeButton(liked, count);
                    updateFavoriteButton(fav);
                }
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



    @Override
    public void onStart() {
        super.onStart();
        ShopSync.LikeSync.register(this);
        ShopSync.FavoriteSync.register(this);
        ProductSync.LikeSync.register(this);
        ProductSync.FavoriteSync.register(this);
    }

    @Override
    public void onStop() {
        ShopSync.LikeSync.unregister(this);
        ShopSync.FavoriteSync.unregister(this);
        ProductSync.LikeSync.unregister(this);
        ProductSync.FavoriteSync.unregister(this);
        super.onStop();
    }

    @Override
    public void onShopSyncUpdate(String shopId, Bundle payload) {
        // Implement if shop likes/favorites are shown in product detail
    }

    @Override
    public void onProductSyncUpdate(String productId, Bundle payload) {
        if (product != null && productId.equals(product.getProductId())) {
            if (payload.containsKey("isLiked")) {
                boolean liked = payload.getBoolean("isLiked");
                int count = payload.getInt("likesCount", product.getLikesCount());
                updateLikeButton(liked, count);
            }
            if (payload.containsKey("isFavorite")) {
                boolean fav = payload.getBoolean("isFavorite");
                updateFavoriteButton(fav);
            }
        }
    }

    /**
     * Met à jour le bouton like avec l'état et le compteur
     */
    private void updateLikeButton(boolean liked, int count) {
        if (likeButton != null) {
            likeButton.setImageResource(liked ?
                    R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
            likeButton.setColorFilter(liked ? 0xFFE8574D : 0xFF757575);
        }
        if (likesCount != null) {
            likesCount.setText(String.valueOf(count));
        }
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
    }

    private void setupClickListeners() {
        // Like button
        if (likeButton != null) {
            likeButton.setOnClickListener(v -> {
                if (product != null && productViewModel != null) {
                    boolean newLiked = !product.isLikedByUser();
                    int newCount = product.getLikesCount() + (newLiked ? 1 : -1);
                    product.setLikedByUser(newLiked);
                    product.setLikesCount(newCount);
                    ProductSync.LikeSync.update(product.getProductId(), newLiked, newCount);
                    productViewModel.toggleLikeProduct(product.getProductId());
                }
            });
        }

        // Favorite button
        if (favoriteButton != null) {
            favoriteButton.setOnClickListener(v -> {
                if (product != null && productViewModel != null) {
                    boolean newFavorite = !product.isFavoriteByUser();
                    product.setFavoriteByUser(newFavorite);
                    ProductSync.FavoriteSync.update(product.getProductId(), newFavorite);
                    productViewModel.toggleFavoriteProduct(product.getProductId());
                }
            });
        }
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
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.inquiry_subject,
                            (product != null ? product.getName() : getString(R.string.unknown_product))));
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
                .setTitle(R.string.delete_product_title)
                .setMessage(getString(R.string.delete_product_message, product.getName()))
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteProduct())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void deleteProduct() {
        if (productManager != null && product != null) {
            final android.content.Context context = getContext();
            
            if (context != null) {
                Toast.makeText(context, getString(R.string.deleting_product), Toast.LENGTH_SHORT).show();
            }

            // Fire and forget (let Repository handle background work)
            productManager.deleteProduct(product, null);
            
            // Close immediately as requested
            if (getActivity() != null) {
                // Perform navigation back
                getActivity().onBackPressed();
                
                // Show toast using captured context if available, otherwise skip
                if (context != null) {
                    Toast.makeText(context, context.getString(R.string.product_deleted_success), Toast.LENGTH_SHORT).show();
                }
            }
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