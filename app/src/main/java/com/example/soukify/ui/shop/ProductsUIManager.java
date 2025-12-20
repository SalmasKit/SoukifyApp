package com.example.soukify.ui.shop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.data.models.ProductModel;

/**
 * Manages UI components and display logic for products in a shop
 * Handles RecyclerView setup, empty states, and product count updates
 */
public class ProductsUIManager {
    
    private static final String TAG = "ProductsUIManager";
    
    private final Fragment fragment;
    private final ProductManager productManager;
    private final ProductViewModel productViewModel;
    
    // UI Components
    private RecyclerView productsRecyclerView;
    private CleanProductsAdapter productsAdapter;
    private LinearLayout emptyProductsLayout;
    private TextView productsHeaderCount;
    private TextView productsCount;
    
    // Click listener interface
    public interface OnProductClickListener {
        void onProductClick(ProductModel product);
        void onProductLongClick(ProductModel product);
    }
    
    private OnProductClickListener productClickListener;
    
    public ProductsUIManager(Fragment fragment, ProductManager productManager, ProductViewModel productViewModel) {
        this.fragment = fragment;
        this.productManager = productManager;
        this.productViewModel = productViewModel;
    }
    
    /**
     * Initialize UI components and setup RecyclerView
     */
    public void initializeUI(View rootView) {
        Log.d(TAG, "Initializing products UI components");
        
        // Find UI components
        productsRecyclerView = rootView.findViewById(R.id.productsRecyclerView);
        emptyProductsLayout = rootView.findViewById(R.id.emptyProductsLayout);
        productsHeaderCount = rootView.findViewById(R.id.productsHeaderCount);
        productsCount = rootView.findViewById(R.id.productsCount);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Observe product data
        observeProductData();
        
        Log.d(TAG, "Products UI initialization completed");
    }
    
    /**
     * Setup RecyclerView with adapter and layout manager
     */
    private void setupRecyclerView() {
        if (productsRecyclerView == null) {
            Log.e(TAG, "Products RecyclerView not found in layout");
            return;
        }
        
        // Create adapter with click listener
        productsAdapter = new CleanProductsAdapter(fragment.requireContext(), new CleanProductsAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(ProductModel product) {
                if (productClickListener != null) {
                    productClickListener.onProductClick(product);
                }
            }
            
            @Override
            public void onProductLongClick(ProductModel product) {
                if (productClickListener != null) {
                    productClickListener.onProductLongClick(product);
                }
            }
        }, productViewModel);
        
        // Set lifecycle owner for proper observer management
        productsAdapter.setLifecycleOwner(fragment.getViewLifecycleOwner());
        
        // Setup RecyclerView
        productsRecyclerView.setAdapter(productsAdapter);
        
        // Use GridLayoutManager with 2 columns for better product display
        GridLayoutManager layoutManager = new GridLayoutManager(fragment.getContext(), 2);
        productsRecyclerView.setLayoutManager(layoutManager);
        
        // Optimize performance
        productsRecyclerView.setHasFixedSize(true);
        productsRecyclerView.setNestedScrollingEnabled(false);
        
        Log.d(TAG, "RecyclerView setup completed with 2-column grid");
    }
    
    /**
     * Observe product data and update UI accordingly
     */
    private void observeProductData() {
        // Observe products list
        productManager.getProducts().observe(fragment.getViewLifecycleOwner(), new Observer<List<ProductModel>>() {
            @Override
            public void onChanged(List<ProductModel> products) {
                updateProductsDisplay(products);
            }
        });
        
        // Observe loading state (optional UI updates)
        productManager.getIsLoading().observe(fragment.getViewLifecycleOwner(), new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean isLoading) {
                // Could show/hide loading indicators here if needed
                Log.d(TAG, "Loading state: " + isLoading);
            }
        });
        
        // Observe error messages
        productManager.getErrorMessage().observe(fragment.getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String errorMessage) {
                if (errorMessage != null && !errorMessage.isEmpty()) {
                    Log.e(TAG, "Product error: " + errorMessage);
                    // Could show error message to user (Toast, Snackbar, etc.)
                }
            }
        });
    }
    
    /**
     * Update products display based on data
     */
    private void updateProductsDisplay(List<ProductModel> products) {
        Log.d(TAG, "Updating products display: " + (products != null ? products.size() : 0) + " items");
        
        if (products == null || products.isEmpty()) {
            // Show empty state
            showEmptyState();
            updateProductCount(0);
        } else {
            // Show products
            showProductsList();
            updateProductCount(products.size());
            
            // Update adapter with new products
            if (productsAdapter != null) {
                productsAdapter.updateProducts(products);
            }
        }
    }
    
    /**
     * Show empty state when no products exist
     */
    private void showEmptyState() {
        if (emptyProductsLayout != null) {
            emptyProductsLayout.setVisibility(View.VISIBLE);
        }
        if (productsRecyclerView != null) {
            productsRecyclerView.setVisibility(View.GONE);
        }
        Log.d(TAG, "Showing empty state");
    }
    
    /**
     * Show products list when products exist
     */
    private void showProductsList() {
        if (emptyProductsLayout != null) {
            emptyProductsLayout.setVisibility(View.GONE);
        }
        if (productsRecyclerView != null) {
            productsRecyclerView.setVisibility(View.VISIBLE);
        }
        Log.d(TAG, "Showing products list");
    }
    
    /**
     * Update product count displays
     */
    private void updateProductCount(int count) {
        // Update header count
        if (productsHeaderCount != null) {
            String countText = count == 1 ? "1 item" : count + " items";
            productsHeaderCount.setText(countText);
        }
        
        // Update shop stats count
        if (productsCount != null) {
            productsCount.setText(String.valueOf(count));
        }
        
        Log.d(TAG, "Updated product count: " + count);
    }
    
    /**
     * Set product click listener
     */
    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }
    
    /**
     * Refresh products display
     */
    public void refreshProducts() {
        productManager.refreshProducts();
    }
    
    /**
     * Get current products count
     */
    public int getCurrentProductsCount() {
        List<ProductModel> currentProducts = productManager.getProducts().getValue();
        return currentProducts != null ? currentProducts.size() : 0;
    }
    
    /**
     * Check if there are any products
     */
    public boolean hasProducts() {
        return getCurrentProductsCount() > 0;
    }
    
    /**
     * Cleanup resources
     */
    public void cleanup() {
        // Clear adapter data
        if (productsAdapter != null) {
            productsAdapter.updateProducts(null);
        }
        
        // Clear click listener
        productClickListener = null;
        
        Log.d(TAG, "ProductsUIManager cleaned up");
    }
}
