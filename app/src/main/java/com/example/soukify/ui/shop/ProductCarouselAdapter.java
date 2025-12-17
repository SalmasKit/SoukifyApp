package com.example.soukify.ui.shop;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.ui.shop.ScalableTextureVideoView;

import java.util.List;

public class ProductCarouselAdapter extends RecyclerView.Adapter<ProductCarouselAdapter.MediaViewHolder> {
    
    private List<String> mediaUrls;
    private OnImageClickListener onImageClickListener;
    private OnMediaDeleteListener onMediaDeleteListener;
    private boolean showButtons = true;
    private boolean showDeleteButton = false;
    
    public interface OnImageClickListener {
        void onImageClick(int position);
    }
    
    public interface OnMediaDeleteListener {
        void onMediaDelete(int position, String mediaUrl);
    }
    
    public ProductCarouselAdapter(List<String> mediaUrls, OnImageClickListener onImageClickListener) {
        this.mediaUrls = mediaUrls;
        this.onImageClickListener = onImageClickListener;
    }
    
    public void setOnMediaDeleteListener(OnMediaDeleteListener onMediaDeleteListener) {
        this.onMediaDeleteListener = onMediaDeleteListener;
    }
    
    public void setShowDeleteButton(boolean showDeleteButton) {
        this.showDeleteButton = showDeleteButton;
        notifyDataSetChanged();
    }
    
    public void setShowButtons(boolean showButtons) {
        this.showButtons = showButtons;
        notifyDataSetChanged();
    }
    
    public void updateMediaUrls(List<String> newUrls) {
        this.mediaUrls = newUrls;
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.carousel_media_item, parent, false);
        return new MediaViewHolder(view);
    }
    
    @Override
    public void onViewRecycled(@NonNull MediaViewHolder holder) {
        super.onViewRecycled(holder);
        // Stop video playback when view is recycled
        if (holder.videoView != null) {
            holder.videoView.stopPlayback();
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        String mediaUrl = mediaUrls.get(position);
        android.util.Log.d("ProductCarouselAdapter", "Binding position " + position + " with URL: " + mediaUrl);
        holder.bind(mediaUrl, position);
    }
    
    @Override
    public int getItemCount() {
        return mediaUrls.size();
    }
    
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private ScalableTextureVideoView videoView;
        private ImageView videoIndicator;
        private ImageButton deleteButton;
        private FrameLayout mediaContainer;
        
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carouselImage);
            videoView = itemView.findViewById(R.id.carouselVideo);
            videoIndicator = itemView.findViewById(R.id.videoIndicator);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            mediaContainer = itemView.findViewById(R.id.mediaContainer);
        }
        
        public void bind(String mediaUrl, int position) {
            // Reset views
            imageView.setVisibility(View.GONE);
            videoView.setVisibility(View.GONE);
            videoIndicator.setVisibility(View.GONE);
            
            // Stop any existing playback
            if (videoView != null) {
                videoView.stopPlayback();
            }
            
            if (isVideo(mediaUrl)) {
                android.util.Log.d("ProductCarouselAdapter", "Loading video: " + mediaUrl);
                
                videoView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.GONE);
                videoIndicator.setVisibility(View.GONE);
                
                try {
                    Uri videoUri = Uri.parse(mediaUrl);
                    videoView.setVideoURI(videoUri);
                    android.util.Log.d("ProductCarouselAdapter", "Video URI set: " + videoUri);
                    
                } catch (Exception e) {
                    android.util.Log.e("ProductCarouselAdapter", "Error loading video", e);
                    videoView.setVisibility(View.GONE);
                }
                
                // Add click listener to view media (no editing)
                videoView.setOnClickListener(v -> {
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(position);
                    }
                });
                
            } else {
                // Show ImageView
                android.util.Log.d("ProductCarouselAdapter", "Loading image: " + mediaUrl);
                
                videoView.setVisibility(View.GONE);
                videoIndicator.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);
                
                try {
                    // Use Glide for better image loading
                    Glide.with(itemView.getContext())
                            .load(Uri.parse(mediaUrl))
                            .placeholder(R.drawable.ic_image_placeholder)
                            .error(R.drawable.ic_image_placeholder)
                            .into(imageView);
                } catch (Exception e) {
                    android.util.Log.e("ProductCarouselAdapter", "Error loading image", e);
                    imageView.setImageResource(R.drawable.ic_image_placeholder);
                }
                
                // Add click listener to view media (no editing)
                imageView.setOnClickListener(v -> {
                    if (onImageClickListener != null) {
                        onImageClickListener.onImageClick(position);
                    }
                });
            }
            
            // Show delete button only in dialog context
            if (deleteButton != null) {
                if (showDeleteButton) {
                    deleteButton.setVisibility(View.VISIBLE);
                    deleteButton.setOnClickListener(v -> {
                        if (onMediaDeleteListener != null) {
                            onMediaDeleteListener.onMediaDelete(position, mediaUrl);
                        }
                    });
                } else {
                    deleteButton.setVisibility(View.GONE);
                }
            }
            
            itemView.setOnClickListener(v -> {
                if (onImageClickListener != null) {
                    onImageClickListener.onImageClick(position);
                }
            });
        }
        
        private boolean isVideo(String mediaUrl) {
            if (mediaUrl == null) return false;
            
            // First try to get MIME type from content resolver
            try {
                Uri uri = Uri.parse(mediaUrl);
                String mimeType = itemView.getContext().getContentResolver().getType(uri);
                if (mimeType != null && mimeType.startsWith("video/")) {
                    android.util.Log.d("ProductCarouselAdapter", "Detected VIDEO by MIME: " + mimeType);
                    return true;
                }
            } catch (Exception e) {
                android.util.Log.e("ProductCarouselAdapter", "Error getting MIME type", e);
            }
            
            // Fallback to extension check
            String urlLower = mediaUrl.toLowerCase();
            boolean isVideoExt = urlLower.endsWith(".mp4") || 
                   urlLower.endsWith(".3gp") || 
                   urlLower.endsWith(".avi") || 
                   urlLower.endsWith(".mov") ||
                   urlLower.endsWith(".wmv") ||
                   urlLower.endsWith(".mkv") ||
                   urlLower.endsWith(".webm") ||
                   urlLower.endsWith(".flv") ||
                   urlLower.endsWith(".m4v");
            
            if (isVideoExt) {
                android.util.Log.d("ProductCarouselAdapter", "Detected VIDEO by extension");
            }
            
            return isVideoExt;
        }
    }
}
