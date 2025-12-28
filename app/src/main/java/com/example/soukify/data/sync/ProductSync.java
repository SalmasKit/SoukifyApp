package com.example.soukify.data.sync;

import android.app.Application;
import android.os.Bundle;
import com.example.soukify.data.repositories.FavoritesTableRepository;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ProductSync {

    public interface SyncListener {
        void onProductSyncUpdate(String productId, Bundle payload);
    }

    public static class LikeSync {
        private static final ConcurrentHashMap<String, LikeState> STATES = new ConcurrentHashMap<>();
        private static final CopyOnWriteArrayList<WeakReference<SyncListener>> LISTENERS = new CopyOnWriteArrayList<>();

        public static LikeState getState(String id) {
            if (id == null) return null;
            return STATES.get(id);
        }

        public static void update(String id, boolean isLiked, int count) {
            if (id == null) return;
            STATES.put(id, new LikeState(isLiked, count));
            Bundle payload = new Bundle();
            payload.putBoolean("isLiked", isLiked);
            payload.putInt("likesCount", count);
            notifyListeners(id, payload);
        }

        public static void register(SyncListener listener) {
            if (listener == null) return;
            LISTENERS.add(new WeakReference<>(listener));
        }

        public static void unregister(SyncListener listener) {
            LISTENERS.removeIf(ref -> {
                SyncListener l = ref.get();
                return l == null || l == listener;
            });
        }

        private static void notifyListeners(String id, Bundle payload) {
            for (WeakReference<SyncListener> ref : LISTENERS) {
                SyncListener l = ref.get();
                if (l != null) {
                    l.onProductSyncUpdate(id, payload);
                }
            }
        }

        public static void clear() {
            STATES.clear();
        }

        public static class LikeState {
            public final boolean isLiked;
            public final int count;
            LikeState(boolean isLiked, int count) {
                this.isLiked = isLiked;
                this.count = count;
            }
        }
    }

    public static class FavoriteSync {
        private static final ConcurrentHashMap<String, FavoriteState> STATES = new ConcurrentHashMap<>();
        private static final CopyOnWriteArrayList<WeakReference<SyncListener>> LISTENERS = new CopyOnWriteArrayList<>();

        public static FavoriteState getState(String id, Application app) {
            if (id == null) return null;
            FavoriteState s = STATES.get(id);
            if (s == null) {
                // Fallback to FavoritesTableRepository cache
                boolean isFav = FavoritesTableRepository.getInstance(app).isProductFavoriteSync(id);
                s = new FavoriteState(isFav);
                STATES.put(id, s);
            }
            return s;
        }

        public static void update(String id, boolean favorite) {
            if (id == null) return;
            STATES.put(id, new FavoriteState(favorite));
            Bundle payload = new Bundle();
            payload.putBoolean("isFavorite", favorite);
            notifyListeners(id, payload);
        }

        public static void register(SyncListener listener) {
            if (listener == null) return;
            LISTENERS.add(new WeakReference<>(listener));
        }

        public static void unregister(SyncListener listener) {
            LISTENERS.removeIf(ref -> {
                SyncListener l = ref.get();
                return l == null || l == listener;
            });
        }

        private static void notifyListeners(String id, Bundle payload) {
            for (WeakReference<SyncListener> ref : LISTENERS) {
                SyncListener l = ref.get();
                if (l != null) {
                    l.onProductSyncUpdate(id, payload);
                }
            }
        }

        public static void clear() {
            STATES.clear();
        }

        public static class FavoriteState {
            public final boolean isFavorite;
            FavoriteState(boolean isFavorite) { this.isFavorite = isFavorite; }
        }
    }
}
