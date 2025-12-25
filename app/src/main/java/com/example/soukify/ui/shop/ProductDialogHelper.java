package com.example.soukify.ui.shop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;
import com.example.soukify.ui.shop.ShopViewModel;
import com.example.soukify.data.remote.firebase.FirebaseProductImageService;
import com.google.firebase.firestore.FirebaseFirestore;
import androidx.lifecycle.ViewModelProvider;
import com.example.soukify.data.models.ProductModel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.example.soukify.utils.CurrencyHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to handle product-related dialogs
 * Separates dialog UI logic from business logic
 */
public class ProductDialogHelper {

    private static final String TAG = "ProductDialogHelper";

    // Callback interface for product updates
    public interface OnProductUpdatedListener {
        void onProductUpdated();
    }

    private final Fragment fragment;
    private final ProductManager productManager;
    private final ActivityResultLauncher<Intent> imagePickerLauncher;
    private OnProductUpdatedListener productUpdatedListener;

    // Current dialog references for image picker callback
    private ImageView currentImageView;
    private LinearLayout currentImageContainer;
    private ProductImageCarousel currentProductImageCarousel;
    private LinearLayout currentAddMoreImagesContainer;
    private TextView currentImageCountTextView;
    private Button currentAddMoreImagesButton;

    private List<Uri> selectedProductImageUris = new ArrayList<>();

    public ProductDialogHelper(Fragment fragment, ProductManager productManager, ActivityResultLauncher<Intent> imagePickerLauncher) {
        this.fragment = fragment;
        this.productManager = productManager;
        this.imagePickerLauncher = imagePickerLauncher;
    }

    public void setOnProductUpdatedListener(OnProductUpdatedListener listener) {
        this.productUpdatedListener = listener;
    }

    public void showAddProductDialog() {
        Context context = fragment.requireContext();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

        // Create custom header
        LinearLayout mainContainer = createDialogContainer(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_product, null);

        mainContainer.addView(createDialogHeader(context, "Add Product", R.drawable.ic_baseline_add_24));
        mainContainer.addView(dialogView);

        builder.setView(mainContainer);

        // Setup image views
        setupProductImageViews(dialogView);

        // Clear previous selections
        selectedProductImageUris.clear();

        builder
            .setPositiveButton("Add Product", (dialogInterface, which) -> {
                
                handleAddProduct(dialogView);
            })
            .setNegativeButton("Cancel", (dialogInterface, which) -> {
                selectedProductImageUris.clear();
            });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int)(context.getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * Handle image picker result for product images
     */
    public void handleImagePickerResult(Intent data) {
        if (data == null) return;

        android.content.ClipData clipData = data.getClipData();

        if (clipData != null) {
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri mediaUri = clipData.getItemAt(i).getUri();
                if (mediaUri != null && !selectedProductImageUris.contains(mediaUri)) {
                    selectedProductImageUris.add(mediaUri);
                }
            }
        } else {
            Uri mediaUri = data.getData();
            if (mediaUri != null && !selectedProductImageUris.contains(mediaUri)) {
                selectedProductImageUris.add(mediaUri);
            }
        }
        updateProductImagePreview();
    }

    /**
     * Open gallery for product media selection
     */
    public void openGalleryForProductMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = {"image/*", "video/*"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Product Media"));
    }

    /**
     * Get selected product image URIs
     */
    public List<Uri> getSelectedProductImageUris() {
        return selectedProductImageUris;
    }

    /**
     * Clear selected images
     */
    public void clearSelectedImages() {
        selectedProductImageUris.clear();
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private LinearLayout createDialogContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundColor(context.getResources().getColor(R.color.background, null));
        return container;
    }

    private LinearLayout createDialogHeader(Context context, String title, int iconRes) {
        LinearLayout headerContainer = new LinearLayout(context);
        headerContainer.setOrientation(LinearLayout.HORIZONTAL);
        headerContainer.setGravity(android.view.Gravity.CENTER_VERTICAL);

        float density = context.getResources().getDisplayMetrics().density;
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        headerParams.setMargins((int)(16 * density), (int)(24 * density), (int)(16 * density), (int)(24 * density));
        headerContainer.setLayoutParams(headerParams);

        // Header icon
        ImageView headerIcon = new ImageView(context);
        headerIcon.setImageResource(iconRes);
        headerIcon.setColorFilter(context.getResources().getColor(R.color.colorPrimary, null));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(
            (int)(24 * density), (int)(24 * density)
        );
        iconParams.setMargins(0, 0, (int)(12 * density), 0);
        headerIcon.setLayoutParams(iconParams);
        headerContainer.addView(headerIcon);

        // Header title
        TextView headerTitle = new TextView(context);
        headerTitle.setText(title);
        headerTitle.setTextSize(20);
        headerTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        headerTitle.setTextColor(context.getResources().getColor(R.color.text_primary, null));
        headerContainer.addView(headerTitle);

        return headerContainer;
    }

    private void setupProductImageViews(View dialogView) {
        ImageView ivProductPreview = dialogView.findViewById(R.id.ivProductPreview);
        LinearLayout productImageContainer = dialogView.findViewById(R.id.productImageContainer);
        ProductImageCarousel productImageCarousel = dialogView.findViewById(R.id.productImageCarousel);
        LinearLayout addMoreImagesContainer = dialogView.findViewById(R.id.addMoreImagesContainer);
        TextView tvImageCount = dialogView.findViewById(R.id.tvImageCount);
        Button btnAddMoreImages = dialogView.findViewById(R.id.btnAddMoreImages);

        currentImageView = ivProductPreview;
        currentImageContainer = productImageContainer;
        currentProductImageCarousel = productImageCarousel;
        currentAddMoreImagesContainer = addMoreImagesContainer;
        currentImageCountTextView = tvImageCount;
        currentAddMoreImagesButton = btnAddMoreImages;

        if (productImageContainer != null) {
            productImageContainer.setOnClickListener(v -> openGalleryForProductMedia());
        }

        if (btnAddMoreImages != null) {
            btnAddMoreImages.setOnClickListener(v -> openGalleryForProductMedia());
        }

        // Setup unit dropdowns
        setupUnitDropdowns(dialogView);
    }

    private void setupUnitDropdowns(View dialogView) {
        // Weight unit dropdown (kg, g, mg)
        AutoCompleteTextView weightUnitSpinner = dialogView.findViewById(R.id.etWeightUnit);
        if (weightUnitSpinner != null) {
            String[] weightUnits = {"kg", "g", "mg"};
            ArrayAdapter<String> weightAdapter = new ArrayAdapter<>(fragment.requireContext(),
                    R.layout.dropdown_item, R.id.dropdown_text, weightUnits);
            weightUnitSpinner.setAdapter(weightAdapter);
            weightUnitSpinner.setText("kg", false);
            weightUnitSpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedUnit = (String) parent.getItemAtPosition(position);
                weightUnitSpinner.setText(selectedUnit, false);
            });
        }

        // Length unit dropdown (km, m, cm, mm)
        AutoCompleteTextView lengthUnitSpinner = dialogView.findViewById(R.id.etLengthUnit);
        if (lengthUnitSpinner != null) {
            String[] lengthUnits = {"km", "m", "cm", "mm"};
            ArrayAdapter<String> lengthAdapter = new ArrayAdapter<>(fragment.requireContext(),
                    R.layout.dropdown_item, R.id.dropdown_text, lengthUnits);
            lengthUnitSpinner.setAdapter(lengthAdapter);
            lengthUnitSpinner.setText("cm", false);
            lengthUnitSpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedUnit = (String) parent.getItemAtPosition(position);
                lengthUnitSpinner.setText(selectedUnit, false);
            });
        }

        // Width unit dropdown (km, m, cm, mm)
        AutoCompleteTextView widthUnitSpinner = dialogView.findViewById(R.id.etWidthUnit);
        if (widthUnitSpinner != null) {
            String[] widthUnits = {"km", "m", "cm", "mm"};
            ArrayAdapter<String> widthAdapter = new ArrayAdapter<>(fragment.requireContext(),
                    R.layout.dropdown_item, R.id.dropdown_text, widthUnits);
            widthUnitSpinner.setAdapter(widthAdapter);
            widthUnitSpinner.setText("cm", false);
            widthUnitSpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedUnit = (String) parent.getItemAtPosition(position);
                widthUnitSpinner.setText(selectedUnit, false);
            });
        }

        // Height unit dropdown (km, m, cm, mm)
        AutoCompleteTextView heightUnitSpinner = dialogView.findViewById(R.id.etHeightUnit);
        if (heightUnitSpinner != null) {
            String[] heightUnits = {"km", "m", "cm", "mm"};
            ArrayAdapter<String> heightAdapter = new ArrayAdapter<>(fragment.requireContext(),
                    R.layout.dropdown_item, R.id.dropdown_text, heightUnits);
            heightUnitSpinner.setAdapter(heightAdapter);
            heightUnitSpinner.setText("cm", false);
            heightUnitSpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selectedUnit = (String) parent.getItemAtPosition(position);
                heightUnitSpinner.setText(selectedUnit, false);
            });
        }

        // Currency dropdown
        AutoCompleteTextView currencySpinner = dialogView.findViewById(R.id.etProductCurrency);
        if (currencySpinner != null) {
            String[] currencies = fragment.requireContext().getResources().getStringArray(R.array.supported_currencies);
            ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(fragment.requireContext(),
                    R.layout.dropdown_item, R.id.dropdown_text, currencies);
            currencySpinner.setAdapter(currencyAdapter);
            
            // Default to MAD if it exists in the list
            String defaultCurr = "MAD";
            for (String curr : currencies) {
                if (curr.startsWith("MAD")) {
                    defaultCurr = curr;
                    break;
                }
            }
            currencySpinner.setText(defaultCurr, false);
            
            currencySpinner.setOnItemClickListener((parent, view, position, id) -> {
                String selected = (String) parent.getItemAtPosition(position);
                currencySpinner.setText(selected, false);
            });
        }
    }

    /**
     * Show dialog to edit an existing product
     */
    public void showEditProductDialog(ProductModel product) {
        Context context = fragment.requireContext();
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);

        // Create custom header
        LinearLayout mainContainer = createDialogContainer(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_product, null);

        mainContainer.addView(createDialogHeader(context, "Edit Product", R.drawable.ic_edit));
        mainContainer.addView(dialogView);

        builder.setView(mainContainer);

        // Setup image views and unit dropdowns
        setupProductImageViews(dialogView);

        // Pre-fill product data
        prefillProductData(dialogView, product);

        builder
            .setPositiveButton("Update Product", (dialogInterface, which) -> {
                handleEditProduct(dialogView, product);
            })
            .setNegativeButton("Cancel", (dialogInterface, which) -> {
                selectedProductImageUris.clear();
            });

        AlertDialog dialog = builder.create();
        dialog.show();

        // Set dialog width
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                (int)(context.getResources().getDisplayMetrics().widthPixels * 0.9),
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    /**
     * Pre-fill product data in edit dialog
     */
    private void prefillProductData(View dialogView, ProductModel product) {
        // Basic product info
        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etProductDescription = dialogView.findViewById(R.id.etProductDescription);
        TextInputEditText etProductPrice = dialogView.findViewById(R.id.etProductPrice);
        TextInputEditText etProductType = dialogView.findViewById(R.id.etProductType);

        if (etProductName != null) etProductName.setText(product.getName());
        if (etProductDescription != null) etProductDescription.setText(product.getDescription());
        if (etProductPrice != null) etProductPrice.setText(String.valueOf(product.getPrice()));
        
        // Product Currency
        AutoCompleteTextView etProductCurrency = dialogView.findViewById(R.id.etProductCurrency);
        if (etProductCurrency != null && product.getCurrency() != null) {
            String productCurr = product.getCurrency();
            String[] currencies = fragment.requireContext().getResources().getStringArray(R.array.supported_currencies);
            String displayCurr = productCurr;
            for (String curr : currencies) {
                if (curr.startsWith(productCurr)) {
                    displayCurr = curr;
                    break;
                }
            }
            etProductCurrency.setText(displayCurr, false);
        }

        if (etProductType != null) etProductType.setText(product.getProductType());

        // Product details
        TextInputEditText etProductWeight = dialogView.findViewById(R.id.etProductWeight);
        TextInputEditText etProductLength = dialogView.findViewById(R.id.etProductLength);
        TextInputEditText etProductWidth = dialogView.findViewById(R.id.etProductWidth);
        TextInputEditText etProductHeight = dialogView.findViewById(R.id.etProductHeight);
        TextInputEditText etProductColor = dialogView.findViewById(R.id.etProductColor);
        TextInputEditText etProductMaterial = dialogView.findViewById(R.id.etProductMaterial);

        // Unit fields
        AutoCompleteTextView etWeightUnit = dialogView.findViewById(R.id.etWeightUnit);
        AutoCompleteTextView etLengthUnit = dialogView.findViewById(R.id.etLengthUnit);
        AutoCompleteTextView etWidthUnit = dialogView.findViewById(R.id.etWidthUnit);
        AutoCompleteTextView etHeightUnit = dialogView.findViewById(R.id.etHeightUnit);

        // Set values and units for weight
        if (etProductWeight != null && product.getWeight() != null) {
            double weightInKg = product.getWeight();
            etProductWeight.setText(String.valueOf(weightInKg));
            if (etWeightUnit != null) etWeightUnit.setText("kg", false);
        }

        // Set values and units for dimensions (default to cm)
        if (etProductLength != null && product.getLength() != null) {
            double lengthInCm = product.getLength();
            etProductLength.setText(String.valueOf(lengthInCm));
            if (etLengthUnit != null) etLengthUnit.setText("cm", false);
        }

        if (etProductWidth != null && product.getWidth() != null) {
            double widthInCm = product.getWidth();
            etProductWidth.setText(String.valueOf(widthInCm));
            if (etWidthUnit != null) etWidthUnit.setText("cm", false);
        }

        if (etProductHeight != null && product.getHeight() != null) {
            double heightInCm = product.getHeight();
            etProductHeight.setText(String.valueOf(heightInCm));
            if (etHeightUnit != null) etHeightUnit.setText("cm", false);
        }

        // Set color and material
        if (etProductColor != null && product.getColor() != null) {
            etProductColor.setText(product.getColor());
        }
        if (etProductMaterial != null && product.getMaterial() != null) {
            etProductMaterial.setText(product.getMaterial());
        }

        // Load product images if they exist
        if (product.hasImages()) {
            loadProductImages(product.getImageIds());
        }
    }

    private void loadProductImages(List<String> imageIds) {
        // Clear existing selection
        selectedProductImageUris.clear();

        // Load real image URLs from Firebase
        List<String> imageUrls = new ArrayList<>();
        int[] loadedCount = {0};
        int totalImages = imageIds.size();

        for (String imageId : imageIds) {
            if (imageId != null && !imageId.isEmpty()) {
                // Use the same imageService approach as ProductDetailFragment
                if (fragment.getActivity() != null) {
                    FirebaseProductImageService imageService = new FirebaseProductImageService(FirebaseFirestore.getInstance());
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
                                    // All images loaded, convert to URIs and update preview
                                    for (String imageUrl : imageUrls) {
                                        selectedProductImageUris.add(Uri.parse(imageUrl));
                                    }
                                    updateProductImagePreview();
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ProductDialogHelper", "Failed to load image: " + imageId, e);
                            synchronized (loadedCount) {
                                loadedCount[0]++;
                                if (loadedCount[0] == totalImages) {
                                    // Even if some images failed, show what we have
                                    for (String imageUrl : imageUrls) {
                                        selectedProductImageUris.add(Uri.parse(imageUrl));
                                    }
                                    updateProductImagePreview();
                                }
                            }
                        });
                }
            } else {
                synchronized (loadedCount) {
                    loadedCount[0]++;
                    if (loadedCount[0] == totalImages) {
                        for (String imageUrl : imageUrls) {
                            selectedProductImageUris.add(Uri.parse(imageUrl));
                        }
                        updateProductImagePreview();
                    }
                }
            }
        }
    }

    private void handleEditProduct(View dialogView, ProductModel product) {
        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etProductDescription = dialogView.findViewById(R.id.etProductDescription);
        TextInputEditText etProductPrice = dialogView.findViewById(R.id.etProductPrice);
        TextInputEditText etProductType = dialogView.findViewById(R.id.etProductType);

        // Optional fields
        TextInputEditText etProductWeight = dialogView.findViewById(R.id.etProductWeight);
        TextInputEditText etProductLength = dialogView.findViewById(R.id.etProductLength);
        TextInputEditText etProductWidth = dialogView.findViewById(R.id.etProductWidth);
        TextInputEditText etProductHeight = dialogView.findViewById(R.id.etProductHeight);
        TextInputEditText etProductColor = dialogView.findViewById(R.id.etProductColor);
        TextInputEditText etProductMaterial = dialogView.findViewById(R.id.etProductMaterial);

        // Unit fields
        AutoCompleteTextView etWeightUnit = dialogView.findViewById(R.id.etWeightUnit);
        AutoCompleteTextView etLengthUnit = dialogView.findViewById(R.id.etLengthUnit);
        AutoCompleteTextView etWidthUnit = dialogView.findViewById(R.id.etWidthUnit);
        AutoCompleteTextView etHeightUnit = dialogView.findViewById(R.id.etHeightUnit);
        AutoCompleteTextView etProductCurrency = dialogView.findViewById(R.id.etProductCurrency);

        String name = etProductName.getText().toString().trim();
        String description = etProductDescription.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String type = etProductType.getText().toString().trim();

        // Parse price
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(fragment.requireContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get currency
        String currency = "MAD";
        if (etProductCurrency != null) {
            String selectedCurrency = etProductCurrency.getText().toString();
            currency = CurrencyHelper.extractCurrencyCode(selectedCurrency);
        }

        // Parse and convert weight based on unit
        Double weight = null;
        if (etProductWeight != null) {
            String weightText = etProductWeight.getText().toString().trim();
            if (!weightText.isEmpty()) {
                try {
                    double weightValue = Double.parseDouble(weightText);
                    String weightUnit = etWeightUnit != null ? etWeightUnit.getText().toString().trim() : "kg";

                    // Convert to base unit (kg) for storage
                    switch (weightUnit.toLowerCase()) {
                        case "g":
                            weight = weightValue / 1000.0; // g to kg
                            break;
                        case "mg":
                            weight = weightValue / 1000000.0; // mg to kg
                            break;
                        case "kg":
                        default:
                            weight = weightValue;
                            break;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(fragment.requireContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // Parse and convert dimensions based on units
        Double length = parseDimensionWithUnit(etProductLength, etLengthUnit);
        Double width = parseDimensionWithUnit(etProductWidth, etWidthUnit);
        Double height = parseDimensionWithUnit(etProductHeight, etHeightUnit);

        String color = getTextOrNull(etProductColor);
        String material = getTextOrNull(etProductMaterial);

        // Update product model
        product.setName(name);
        product.setDescription(description);
        product.setPrice(price);
        product.setCurrency(currency);
        product.setProductType(type);
        product.setWeight(weight);
        product.setLength(length);
        product.setWidth(width);
        product.setHeight(height);
        product.setColor(color);
        product.setMaterial(material);

        // Update product using ShopViewModel to ensure UI refresh
        if (fragment.getActivity() != null) {
            ShopViewModel shopViewModel = new ViewModelProvider(fragment.getActivity()).get(ShopViewModel.class);

            // Handle images if they were changed
            if (!selectedProductImageUris.isEmpty()) {
                // Convert URIs to strings for storage
                List<String> newImageUrls = new ArrayList<>();
                for (Uri uri : selectedProductImageUris) {
                    newImageUrls.add(uri.toString());
                }

                // Update product with new images
                product.setImageIds(newImageUrls);
                // Primary image is automatically the first image in the list via getPrimaryImageId()
            }

            // Update the product
            shopViewModel.updateProduct(product);

            // Show success message
            Toast.makeText(fragment.requireContext(), "Product updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            // Fallback to ProductManager
            productManager.updateProduct(product);
            Toast.makeText(fragment.requireContext(), "Product updated successfully", Toast.LENGTH_SHORT).show();
        }

        selectedProductImageUris.clear();
    }

    /**
     * Parse dimension value with unit conversion to base unit (cm)
     */
    private Double parseDimensionWithUnit(TextInputEditText editText, AutoCompleteTextView unitView) {
        if (editText == null) return null;
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return null;

        try {
            double value = Double.parseDouble(text);
            String unit = unitView != null ? unitView.getText().toString().trim() : "cm";

            // Convert to base unit (cm) for storage
            switch (unit.toLowerCase()) {
                case "km":
                    return value * 100000.0; // km to cm
                case "m":
                    return value * 100.0; // m to cm
                case "mm":
                    return value / 10.0; // mm to cm
                case "cm":
                default:
                    return value;
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void handleAddProduct(View dialogView) {
        TextInputEditText etProductName = dialogView.findViewById(R.id.etProductName);
        TextInputEditText etProductDescription = dialogView.findViewById(R.id.etProductDescription);
        TextInputEditText etProductPrice = dialogView.findViewById(R.id.etProductPrice);
        TextInputEditText etProductType = dialogView.findViewById(R.id.etProductType);

        // Optional fields
        TextInputEditText etProductWeight = dialogView.findViewById(R.id.etProductWeight);
        TextInputEditText etProductLength = dialogView.findViewById(R.id.etProductLength);
        TextInputEditText etProductWidth = dialogView.findViewById(R.id.etProductWidth);
        TextInputEditText etProductHeight = dialogView.findViewById(R.id.etProductHeight);
        TextInputEditText etProductColor = dialogView.findViewById(R.id.etProductColor);
        TextInputEditText etProductMaterial = dialogView.findViewById(R.id.etProductMaterial);

        // Unit fields
        AutoCompleteTextView etWeightUnit = dialogView.findViewById(R.id.etWeightUnit);
        AutoCompleteTextView etLengthUnit = dialogView.findViewById(R.id.etLengthUnit);
        AutoCompleteTextView etWidthUnit = dialogView.findViewById(R.id.etWidthUnit);
        AutoCompleteTextView etHeightUnit = dialogView.findViewById(R.id.etHeightUnit);
        AutoCompleteTextView etProductCurrency = dialogView.findViewById(R.id.etProductCurrency);

        String name = etProductName.getText().toString().trim();
        String description = etProductDescription.getText().toString().trim();
        String priceStr = etProductPrice.getText().toString().trim();
        String type = etProductType.getText().toString().trim();

        // Parse price
        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            Toast.makeText(fragment.requireContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get currency
        String currency = "MAD";
        if (etProductCurrency != null) {
            String selectedCurrency = etProductCurrency.getText().toString();
            currency = CurrencyHelper.extractCurrencyCode(selectedCurrency);
        }

        // Parse and convert weight based on unit
        Double weight = null;
        if (etProductWeight != null) {
            String weightText = etProductWeight.getText().toString().trim();
            if (!weightText.isEmpty()) {
                try {
                    double weightValue = Double.parseDouble(weightText);
                    String weightUnit = etWeightUnit != null ? etWeightUnit.getText().toString().trim() : "kg";

                    // Convert to base unit (kg) for storage
                    switch (weightUnit.toLowerCase()) {
                        case "g":
                            weight = weightValue / 1000.0; // g to kg
                            break;
                        case "mg":
                            weight = weightValue / 1000000.0; // mg to kg
                            break;
                        case "kg":
                        default:
                            weight = weightValue;
                            break;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(fragment.requireContext(), "Invalid weight format", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }

        // Parse and convert dimensions based on units
        Double length = parseDimensionWithUnit(etProductLength, etLengthUnit);
        Double width = parseDimensionWithUnit(etProductWidth, etWidthUnit);
        Double height = parseDimensionWithUnit(etProductHeight, etHeightUnit);

        String color = getTextOrNull(etProductColor);
        String material = getTextOrNull(etProductMaterial);

        // Add product through ProductManager
        if (selectedProductImageUris.isEmpty()) {
            productManager.addProduct(name, description, price, currency, type, null, weight, length, width, height, color, material);
        } else if (selectedProductImageUris.size() == 1) {
            String imageUrl = selectedProductImageUris.get(0).toString();
            productManager.addProduct(name, description, price, currency, type, imageUrl, weight, length, width, height, color, material);
        } else {
            List<String> imageUrls = new ArrayList<>();
            for (Uri uri : selectedProductImageUris) {
                imageUrls.add(uri.toString());
            }
            productManager.addProductWithMultipleImages(name, description, price, currency, type, imageUrls, weight, length, width, height, color, material);
        }

        selectedProductImageUris.clear();

        // Show success message
        Toast.makeText(fragment.requireContext(), "Product added successfully", Toast.LENGTH_SHORT).show();
    }

    private void updateProductImagePreview() {
        if (currentImageView == null || currentImageContainer == null) return;

        if (selectedProductImageUris.isEmpty()) {
            currentImageView.setVisibility(View.GONE);
            currentImageContainer.setVisibility(View.VISIBLE);

            if (currentProductImageCarousel != null) {
                currentProductImageCarousel.setVisibility(View.GONE);
            }

            if (currentAddMoreImagesContainer != null) {
                currentAddMoreImagesContainer.setVisibility(View.GONE);
            }
        } else {
            List<String> mediaUrls = new ArrayList<>();
            for (Uri uri : selectedProductImageUris) {
                mediaUrls.add(uri.toString());
            }

            currentImageView.setVisibility(View.GONE);
            currentImageContainer.setVisibility(View.GONE);

            if (currentProductImageCarousel != null) {
                currentProductImageCarousel.setVisibility(View.VISIBLE);
                currentProductImageCarousel.clearMediaUrls();

                currentProductImageCarousel.post(() -> {
                    currentProductImageCarousel.setImageUrls(mediaUrls);
                    currentProductImageCarousel.setShowButtons(true);
                    currentProductImageCarousel.setShowDeleteButton(true);

                    currentProductImageCarousel.setOnMediaDeleteListener((position, mediaUrl) -> {
                        if (position >= 0 && position < selectedProductImageUris.size()) {
                            selectedProductImageUris.remove(position);
                            updateProductImagePreview();
                            Toast.makeText(fragment.requireContext(), "Media deleted", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            }

            if (currentAddMoreImagesContainer != null && currentImageCountTextView != null) {
                currentAddMoreImagesContainer.setVisibility(View.VISIBLE);

                int videoCount = 0;
                int imageCount = 0;
                for (Uri uri : selectedProductImageUris) {
                    if (isVideo(uri)) {
                        videoCount++;
                    } else {
                        imageCount++;
                    }
                }

                String countText;
                if (videoCount > 0 && imageCount > 0) {
                    countText = imageCount + " image" + (imageCount == 1 ? "" : "s") +
                               ", " + videoCount + " video" + (videoCount == 1 ? "" : "s");
                } else if (videoCount > 0) {
                    countText = videoCount + " video" + (videoCount == 1 ? "" : "s");
                } else {
                    countText = imageCount + " image" + (imageCount == 1 ? "" : "s");
                }
                currentImageCountTextView.setText(countText);
            }
        }
    }

    private boolean isVideo(Uri uri) {
        if (uri == null) return false;

        try {
            String mimeType = fragment.requireContext().getContentResolver().getType(uri);
            if (mimeType != null && mimeType.startsWith("video/")) {
                return true;
            }
        } catch (Exception e) {
            // Ignore
        }

        String uriString = uri.toString().toLowerCase();
        return uriString.endsWith(".mp4") ||
               uriString.endsWith(".3gp") ||
               uriString.endsWith(".avi") ||
               uriString.endsWith(".mov") ||
               uriString.endsWith(".wmv");
    }

    private Double parseDoubleOrNull(TextInputEditText editText) {
        if (editText == null) return null;
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return null;

        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getTextOrNull(TextInputEditText editText) {
        if (editText == null) return null;
        String text = editText.getText().toString().trim();
        return text.isEmpty() ? null : text;
    }
}