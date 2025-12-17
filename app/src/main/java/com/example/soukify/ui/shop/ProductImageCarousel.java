package com.example.soukify.ui.shop;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

public class ProductImageCarousel extends FrameLayout {
    
    private ViewPager2 viewPager;
    private ProductCarouselAdapter adapter;
    
    public interface OnImageClickListener {
        void onImageClick(int position, String imageUrl);
        void onImageLongClick(int position, String imageUrl);
    }
    
    private OnImageClickListener onImageClickListener;
    
    public ProductImageCarousel(@NonNull Context context) {
        super(context);
        init();
    }
    
    public ProductImageCarousel(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    private void init() {
        // Create ViewPager2
        viewPager = new ViewPager2(getContext());
        addView(viewPager, new LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ));
        
        // Enable clipping to prevent overflow
        setClipChildren(true);
        setClipToPadding(true);
        viewPager.setClipChildren(true);
        viewPager.setClipToPadding(true);
        viewPager.setOverScrollMode(View.OVER_SCROLL_NEVER);
        
        // Disable nested scrolling to prevent conflicts with NestedScrollView
        viewPager.setNestedScrollingEnabled(false);
        
        // Request disallow intercept to handle touch events properly
        viewPager.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    // When user touches carousel, tell parent not to intercept
                    v.getParent().requestDisallowInterceptTouchEvent(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    // Release parent intercept when done
                    v.getParent().requestDisallowInterceptTouchEvent(false);
                    break;
            }
            return false;
        });
    }
    
    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
        // Update adapter if it exists
        if (adapter != null) {
            // Need to recreate adapter with new listener
            java.util.List<String> currentUrls = getCurrentUrls();
            adapter = new ProductCarouselAdapter(currentUrls, position -> {
                if (listener != null && position < currentUrls.size()) {
                    listener.onImageClick(position, currentUrls.get(position));
                }
            });
            viewPager.setAdapter(adapter);
        }
    }
    
    private java.util.List<String> getCurrentUrls() {
        // This would need to be tracked as a member variable
        // For now, return empty list - this should be improved
        return new java.util.ArrayList<>();
    }
    
    public void setImageUrls(java.util.List<String> urls) {
        android.util.Log.d("ProductImageCarousel", "setImageUrls called with " + (urls != null ? urls.size() : "null") + " URLs");
        if (urls != null) {
            for (int i = 0; i < urls.size(); i++) {
                android.util.Log.d("ProductImageCarousel", "URL " + i + ": " + urls.get(i));
            }
        }
        
        if (adapter == null) {
            // Create adapter with click listener
            ProductCarouselAdapter.OnImageClickListener adapterClickListener = position -> {
                if (onImageClickListener != null && position < urls.size()) {
                    onImageClickListener.onImageClick(position, urls.get(position));
                }
            };
            adapter = new ProductCarouselAdapter(urls, adapterClickListener);
            viewPager.setAdapter(adapter);
            android.util.Log.d("ProductImageCarousel", "Created new adapter and set it to ViewPager2");
        } else {
            adapter.updateMediaUrls(urls);
            android.util.Log.d("ProductImageCarousel", "Updated existing adapter with new URLs");
        }
        
        // Set up other configurations
        if (adapter != null) {
            adapter.setShowButtons(true);
            adapter.setShowDeleteButton(false); // Default to false for cards
            android.util.Log.d("ProductImageCarousel", "Set adapter configurations: showButtons=true, showDeleteButton=false");
        }
    }
    
    public void clearMediaUrls() {
        if (adapter != null) {
            java.util.List<String> emptyList = new java.util.ArrayList<>();
            adapter.updateMediaUrls(emptyList);
        }
    }
    
    public void setShowButtons(boolean show) {
        if (adapter != null) {
            adapter.setShowButtons(show);
        }
    }
    
    public void setShowDeleteButton(boolean show) {
        if (adapter != null) {
            adapter.setShowDeleteButton(show);
        }
    }
    
    public void setOnMediaDeleteListener(ProductCarouselAdapter.OnMediaDeleteListener listener) {
        if (adapter != null) {
            adapter.setOnMediaDeleteListener(listener);
        }
    }
    
    public int getCurrentItem() {
        if (viewPager != null) {
            return viewPager.getCurrentItem();
        }
        return 0;
    }
    
    public void setCurrentItem(int position, boolean smoothScroll) {
        if (viewPager != null && adapter != null) {
            // Check if position is valid
            if (position >= 0 && position < adapter.getItemCount()) {
                viewPager.setCurrentItem(position, smoothScroll);
            }
        }
    }
    
    public int getItemCount() {
        if (adapter != null) {
            return adapter.getItemCount();
        }
        return 0;
    }
}
