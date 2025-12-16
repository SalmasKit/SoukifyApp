package com.example.soukify.ui.shop;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import java.io.IOException;

/**
 * TextureView-based video player that properly clips within parent bounds
 * Unlike VideoView (SurfaceView), TextureView respects clipping
 */
public class ScalableTextureVideoView extends TextureView implements TextureView.SurfaceTextureListener {
    
    private MediaPlayer mediaPlayer;
    private String videoPath;
    private boolean isPrepared = false;
    
    public ScalableTextureVideoView(Context context) {
        super(context);
        init();
    }
    
    public ScalableTextureVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public ScalableTextureVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        setSurfaceTextureListener(this);
    }
    
    public void setVideoURI(Uri uri) {
        this.videoPath = uri.toString();
        if (isAvailable()) {
            setupMediaPlayer();
        }
    }
    
    private void setupMediaPlayer() {
        try {
            releaseMediaPlayer();
            
            // Validate video path before proceeding
            if (videoPath == null || videoPath.trim().isEmpty()) {
                android.util.Log.e("ScalableTextureVideoView", "Video path is null or empty");
                return;
            }
            
            // Check if this is a valid URI
            Uri videoUri = Uri.parse(videoPath);
            if (videoUri == null) {
                android.util.Log.e("ScalableTextureVideoView", "Failed to parse video URI: " + videoPath);
                return;
            }
            
            mediaPlayer = new MediaPlayer();
            try {
                // Check URI scheme and handle accordingly
                if (videoUri.getScheme() != null && videoUri.getScheme().equals("file")) {
                    // For file URIs, check if file exists
                    String filePath = videoUri.getPath();
                    if (filePath != null && new java.io.File(filePath).exists()) {
                        mediaPlayer.setDataSource(getContext(), videoUri);
                    } else {
                        android.util.Log.e("ScalableTextureVideoView", "Video file not found: " + filePath);
                        return;
                    }
                } else {
                    // For content URIs and other schemes
                    mediaPlayer.setDataSource(getContext(), videoUri);
                }
            } catch (IOException e) {
                android.util.Log.e("ScalableTextureVideoView", "Error setting data source: " + e.getMessage());
                releaseMediaPlayer();
                return;
            } catch (Exception e) {
                android.util.Log.e("ScalableTextureVideoView", "Unexpected error setting data source: " + e.getMessage());
                releaseMediaPlayer();
                return;
            }
            mediaPlayer.setSurface(new Surface(getSurfaceTexture()));
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0f, 0f); // Muted by default
            
            mediaPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                
                // Get video dimensions
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                
                // Get view dimensions
                int viewWidth = getWidth();
                int viewHeight = getHeight();
                
                if (viewWidth > 0 && viewHeight > 0 && videoWidth > 0 && videoHeight > 0) {
                    // Calculate scaling for centerCrop effect
                    float videoAspect = (float) videoWidth / videoHeight;
                    float viewAspect = (float) viewWidth / viewHeight;
                    
                    Matrix matrix = new Matrix();
                    float scaleX, scaleY;
                    
                    if (videoAspect > viewAspect) {
                        // Video is wider - fit height and crop width
                        scaleY = 1f;
                        scaleX = videoAspect / viewAspect;
                    } else {
                        // Video is taller - fit width and crop height
                        scaleX = 1f;
                        scaleY = viewAspect / videoAspect;
                    }
                    
                    // Center the video
                    float pivotX = viewWidth / 2f;
                    float pivotY = viewHeight / 2f;
                    
                    matrix.setScale(scaleX, scaleY, pivotX, pivotY);
                    setTransform(matrix);
                }
                
                mp.start();
                android.util.Log.d("ScalableTextureVideoView", "Video started playing");
            });
            
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                android.util.Log.e("ScalableTextureVideoView", "MediaPlayer error: " + what + ", " + extra + " for URI: " + videoPath);
                releaseMediaPlayer();
                return true; // Error handled
            });
            
            mediaPlayer.prepareAsync();
            android.util.Log.d("ScalableTextureVideoView", "MediaPlayer preparing: " + videoPath);
            
        } catch (Exception e) {
            android.util.Log.e("ScalableTextureVideoView", "Error setting up MediaPlayer", e);
        }
    }
    
    public void stopPlayback() {
        releaseMediaPlayer();
    }
    
    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.reset();
                mediaPlayer.release();
            } catch (Exception e) {
                android.util.Log.e("ScalableTextureVideoView", "Error releasing MediaPlayer", e);
            }
            mediaPlayer = null;
            isPrepared = false;
        }
    }
    
    public boolean isPlaying() {
        return mediaPlayer != null && isPrepared && mediaPlayer.isPlaying();
    }
    
    // TextureView.SurfaceTextureListener methods
    
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        android.util.Log.d("ScalableTextureVideoView", "Surface available: " + width + "x" + height);
        if (videoPath != null) {
            setupMediaPlayer();
        }
    }
    
    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        android.util.Log.d("ScalableTextureVideoView", "Surface size changed: " + width + "x" + height);
    }
    
    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        android.util.Log.d("ScalableTextureVideoView", "Surface destroyed");
        releaseMediaPlayer();
        return true;
    }
    
    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Called when surface has new frame - not needed for our case
    }
    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseMediaPlayer();
    }
}
