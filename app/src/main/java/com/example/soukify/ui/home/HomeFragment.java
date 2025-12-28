package com.example.soukify.ui.home;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.soukify.MainActivity;
import com.example.soukify.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.os.Bundle;
import android.util.Log;
public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

    private LinearLayout mapContainer;
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private FloatingActionButton btnGpsLocation;

    // Carousel components
    private ViewPager2 imageCarousel;
    private LinearLayout carouselIndicators;
    private Handler carouselHandler;
    private Runnable carouselRunnable;
    private int currentCarouselPage = 0;

    // Location selection
    private LinearLayout selectedLocationOverlay;
    private TextView selectedCityText;
    private Button btnConfirmLocation;
    private String detectedCity = null;
    private GeoPoint detectedLocation = null;

    // Morocco boundaries (approximate)
    private static final double MOROCCO_NORTH = 36.0;
    private static final double MOROCCO_SOUTH = 21.0;
    private static final double MOROCCO_WEST = -17.5;
    private static final double MOROCCO_EAST = -1.0;

    // Major Moroccan cities
    private static final GeoPoint CASABLANCA = new GeoPoint(33.5731, -7.5898);
    private static final GeoPoint RABAT = new GeoPoint(34.0209, -6.8416);
    private static final GeoPoint MARRAKECH = new GeoPoint(31.6295, -7.9811);
    private static final GeoPoint FES = new GeoPoint(34.0181, -5.0078);
    private static final GeoPoint TANGIER = new GeoPoint(35.7595, -5.8340);
    private static final GeoPoint AGADIR = new GeoPoint(30.4278, -9.5981);
    private static final GeoPoint OUJDA = new GeoPoint(34.6867, -1.9114);
    private static final GeoPoint MEKNES = new GeoPoint(33.8935, -5.5473);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Needed for OSMDroid to work correctly
        Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());

        // Initialize views
        mapContainer = view.findViewById(R.id.map_container);
        imageCarousel = view.findViewById(R.id.image_carousel);
        carouselIndicators = view.findViewById(R.id.carousel_indicators);
        selectedLocationOverlay = view.findViewById(R.id.selected_location_overlay);
        selectedCityText = view.findViewById(R.id.selected_city_text);
        btnConfirmLocation = view.findViewById(R.id.btn_confirm_location);
        btnGpsLocation = view.findViewById(R.id.btn_gps_location);

        // Initialize carousel with auto-scroll
        setupCarousel();

        // Initialize map
        initializeMap();

        // Setup location confirmation
        setupLocationSelection();

        // Setup GPS button
        setupGpsButton();

        return view;
    }

    private void setupCarousel() {
        // Create carousel adapter with logo as first image + Moroccan images
        CarouselAdapter adapter = new CarouselAdapter(getCarouselImages());
        imageCarousel.setAdapter(adapter);

        // Reduce over-scroll effect for smoother transitions
        imageCarousel.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Create indicators dynamically
        createCarouselIndicators(adapter.getItemCount());

        // Auto-scroll setup
        carouselHandler = new Handler(Looper.getMainLooper());
        carouselRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentCarouselPage >= adapter.getItemCount()) {
                    currentCarouselPage = 0;
                }
                imageCarousel.setCurrentItem(currentCarouselPage++, true);
                carouselHandler.postDelayed(this, 2500); // Auto-scroll every 2.5 seconds
            }
        };

        // Update indicators on page change
        imageCarousel.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCarouselIndicators(position);
                currentCarouselPage = position;
            }
        });

        // Set page transformer for smooth transitions - REMOVED COMPLEX TRANSFORMER FOR SMOOTHNESS
        // imageCarousel.setPageTransformer(new DepthPageTransformer());
    }

    private List<Integer> getCarouselImages() {
        List<Integer> images = new ArrayList<>();
       


        // Then add your Moroccan souk PNG images
        images.add(R.drawable.image2);
        images.add(R.drawable.image3);
        images.add(R.drawable.image4);
        images.add(R.drawable.image5);
        images.add(R.drawable.image6);
        images.add(R.drawable.image7);
        images.add(R.drawable.image8);
        images.add(R.drawable.image9);
        images.add(R.drawable.image10);
        images.add(R.drawable.image12);    
        return images;
    }

    private void createCarouselIndicators(int count) {
        carouselIndicators.removeAllViews();

        for (int i = 0; i < count; i++) {
            View indicator = new View(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(8), dpToPx(8)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            indicator.setLayoutParams(params);
            indicator.setBackgroundResource(i == 0 ? R.drawable.indicator_active : R.drawable.indicator_inactive);
            carouselIndicators.addView(indicator);
        }
    }

    private void updateCarouselIndicators(int position) {
        int childCount = carouselIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View indicator = carouselIndicators.getChildAt(i);
            indicator.setBackgroundResource(
                    i == position ? R.drawable.indicator_active : R.drawable.indicator_inactive
            );
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void initializeMap() {
        // Create the MapView programmatically
        mapView = new MapView(requireContext());
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        // Put the map inside your placeholder layout
        mapContainer.removeAllViews();
        mapContainer.addView(mapView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // Center on Morocco with appropriate zoom
        mapView.getController().setZoom(6.0);
        mapView.getController().setCenter(new GeoPoint(31.7917, -7.0926)); // Center of Morocco

        // Set minimum and maximum zoom levels
        mapView.setMinZoomLevel(5.5);
        mapView.setMaxZoomLevel(18.0);

        // Limit map scrolling to Morocco boundaries
        BoundingBox moroccoBounds = new BoundingBox(
                MOROCCO_NORTH,  // North
                MOROCCO_EAST,   // East
                MOROCCO_SOUTH,  // South
                MOROCCO_WEST    // West
        );
        mapView.setScrollableAreaLimitDouble(moroccoBounds);

        // Add markers for major Moroccan cities
        addCityMarkers();

        // ADD MAP CLICK LISTENER - Click anywhere on map to get coordinates
        MapEventsReceiver mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                // User tapped on map - get city from coordinates
                onMapClick(p);
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        };

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mapView.getOverlays().add(0, mapEventsOverlay); // Add as first overlay

        // Compass Overlay
        CompassOverlay compass = new CompassOverlay(
                requireContext(),
                new InternalCompassOrientationProvider(requireContext()),
                mapView
        );
        compass.enableCompass();
        mapView.getOverlays().add(compass);

        // Scale bar
        ScaleBarOverlay scaleBar = new ScaleBarOverlay(mapView);
        scaleBar.setCentred(true);
        scaleBar.setScaleBarOffset(
                getResources().getDisplayMetrics().widthPixels / 2,
                100
        );
        mapView.getOverlays().add(scaleBar);

        // Rotation overlay
        RotationGestureOverlay rotation = new RotationGestureOverlay(mapView);
        rotation.setEnabled(true);
        mapView.getOverlays().add(rotation);

        // GPS Location overlay
        myLocationOverlay = new MyLocationNewOverlay(
                new GpsMyLocationProvider(requireContext()),
                mapView
        );
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    // NEW METHOD: Handle map click
    private void onMapClick(GeoPoint clickedPoint) {
        Toast.makeText(requireContext(), getString(R.string.getting_location_info), Toast.LENGTH_SHORT).show();

        // Get city name from clicked coordinates
        getCityNameFromLocation(clickedPoint);

        // Optionally zoom to clicked location
        mapView.getController().animateTo(clickedPoint, 10.0, 1000L);
    }

    private void addCityMarkers() {
        // Add clickable markers for major Moroccan cities
        addCityMarker(getString(R.string.city_casablanca), CASABLANCA);
        addCityMarker(getString(R.string.city_rabat), RABAT);
        addCityMarker(getString(R.string.city_marrakech), MARRAKECH);
        addCityMarker(getString(R.string.city_fes), FES);
        addCityMarker(getString(R.string.city_tangier), TANGIER);
        addCityMarker(getString(R.string.city_agadir), AGADIR);
        addCityMarker(getString(R.string.city_oujda), OUJDA);
        addCityMarker(getString(R.string.city_meknes), MEKNES);
    }

    private void addCityMarker(String cityName, GeoPoint location) {
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(cityName);

        mapView.getOverlays().add(marker);
    }

    private void setupLocationSelection() {
        btnConfirmLocation.setOnClickListener(v -> {
            if (detectedCity != null) {
                Toast.makeText(requireContext(), getString(R.string.navigating_to_city_shops, detectedCity), Toast.LENGTH_SHORT).show();

                selectedLocationOverlay.animate()
                        .alpha(0f)
                        .translationY(50f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            selectedLocationOverlay.setVisibility(View.GONE);
                            navigateToSearchWithCity(detectedCity);
                        })
                        .start();
            } else {
                Toast.makeText(requireContext(), getString(R.string.please_select_city_first), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ADD this new method:
    private void navigateToSearchWithCity(String cityName) {
        try {
            Log.d("HomeFragment", "Navigating to search with city: " + cityName);

            if (getActivity() != null) {
                getActivity().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                        .edit()
                        .putString("pending_city_filter", cityName)
                        .apply();

                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSearch();
                }
            }
        } catch (Exception e) {
            Log.e("HomeFragment", "Navigation error: " + e.getMessage());
            Toast.makeText(requireContext(), getString(R.string.error_navigating_to_search), Toast.LENGTH_SHORT).show();
        }
    }
    private void setupGpsButton() {
        btnGpsLocation.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                findMyLocation();
            } else {
                requestLocationPermission();
            }
        });
    }
    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                LOCATION_PERMISSION_REQUEST_CODE
        );
    }

    private void findMyLocation() {
        // Show loading message
        Toast.makeText(requireContext(), getString(R.string.finding_your_location), Toast.LENGTH_SHORT).show();

        // Animate GPS button to show it's working
        btnGpsLocation.animate()
                .rotationBy(360f)
                .setDuration(1000)
                .start();

        // Use handler to give GPS time to get location
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                GeoPoint myLocation = myLocationOverlay.getMyLocation();
                detectedLocation = myLocation;

                // Animate to user's location
                mapView.getController().animateTo(myLocation, 12.0, 1500L);

                // Get city name from coordinates
                getCityNameFromLocation(myLocation);

            } else {
                // Try again after a bit more time
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (myLocationOverlay != null && myLocationOverlay.getMyLocation() != null) {
                        GeoPoint myLocation = myLocationOverlay.getMyLocation();
                        detectedLocation = myLocation;
                        mapView.getController().animateTo(myLocation, 12.0, 1500L);
                        getCityNameFromLocation(myLocation);
                    } else {
                        // GPS failed, try to find nearest major city based on approximate location
                        Toast.makeText(requireContext(),
                                getString(R.string.unable_to_get_precise_location),
                                Toast.LENGTH_LONG).show();
                    }
                }, 2000);
            }
        }, 1500);
    }

    private void getCityNameFromLocation(GeoPoint location) {
        // Use Geocoder to get city name from coordinates
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(
                        location.getLatitude(),
                        location.getLongitude(),
                        1
                );

                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    String cityName = address.getLocality(); // City name

                    if (cityName == null || cityName.isEmpty()) {
                        cityName = address.getSubAdminArea(); // Try sub-admin area
                    }

                    if (cityName == null || cityName.isEmpty()) {
                        cityName = address.getAdminArea(); // Try admin area (region)
                    }

                    // If we got a city name, show it
                    if (cityName != null && !cityName.isEmpty()) {
                        String finalCityName = cityName;
                        requireActivity().runOnUiThread(() -> {
                            showLocationConfirmation(finalCityName, location);
                        });
                    } else {
                        // Fallback to nearest major city
                        requireActivity().runOnUiThread(() -> {
                            String nearestCity = findNearestCity(location);
                            showLocationConfirmation(nearestCity, location);
                        });
                    }
                } else {
                    // No address found, use nearest major city
                    requireActivity().runOnUiThread(() -> {
                        String nearestCity = findNearestCity(location);
                        showLocationConfirmation(nearestCity, location);
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
                // Error with geocoding, fallback to nearest major city
                requireActivity().runOnUiThread(() -> {
                    String nearestCity = findNearestCity(location);
                    showLocationConfirmation(nearestCity, location);
                });
            }
        }).start();
    }

    private String findNearestCity(GeoPoint userLocation) {
        int[] cityResIds = {
                R.string.city_casablanca, R.string.city_rabat, R.string.city_marrakech,
                R.string.city_fes, R.string.city_tangier, R.string.city_agadir,
                R.string.city_oujda, R.string.city_meknes
        };
        GeoPoint[] cityLocations = {CASABLANCA, RABAT, MARRAKECH, FES, TANGIER, AGADIR, OUJDA, MEKNES};

        int nearestIndex = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < cityResIds.length; i++) {
            double distance = userLocation.distanceToAsDouble(cityLocations[i]);
            if (distance < minDistance) {
                minDistance = distance;
                nearestIndex = i;
            }
        }

        return getString(cityResIds[nearestIndex]);
    }

    private void showLocationConfirmation(String cityName, GeoPoint location) {
        detectedCity = cityName;
        detectedLocation = location;
        selectedCityText.setText(cityName);

        // Show the confirmation overlay with animation
        selectedLocationOverlay.setVisibility(View.VISIBLE);
        selectedLocationOverlay.setAlpha(0f);
        selectedLocationOverlay.setTranslationY(100f);
        selectedLocationOverlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(400)
                .start();

        // Show success message
        Toast.makeText(requireContext(),
                getString(R.string.location_prefix) + cityName,
                Toast.LENGTH_SHORT).show();

        // Add a marker at detected location
        addDetectedLocationMarker(cityName, location);
    }

    private void addDetectedLocationMarker(String cityName, GeoPoint location) {
        // Remove any existing "Your Location" marker
        for (int i = mapView.getOverlays().size() - 1; i >= 0; i--) {
            if (mapView.getOverlays().get(i) instanceof Marker) {
                Marker marker = (Marker) mapView.getOverlays().get(i);
                if (getString(R.string.your_location_marker).equals(marker.getTitle())) {
                    mapView.getOverlays().remove(i);
                }
            }
        }

        // Add new marker at detected location
        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setTitle(getString(R.string.your_location_marker));
        marker.setSnippet(cityName);

        mapView.getOverlays().add(marker);
        mapView.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), getString(R.string.permission_granted_tap_gps), Toast.LENGTH_SHORT).show();
                // Automatically try to find location
                findMyLocation();
            } else {
                Toast.makeText(requireContext(),
                        getString(R.string.location_permission_required),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();

        // START CAROUSEL AUTO-SCROLL WHEN FRAGMENT IS VISIBLE
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.postDelayed(carouselRunnable, 2500);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();

        // STOP CAROUSEL AUTO-SCROLL WHEN FRAGMENT IS HIDDEN
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (carouselHandler != null && carouselRunnable != null) {
            carouselHandler.removeCallbacks(carouselRunnable);
        }
    }
}


// Carousel Adapter Class
class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CarouselViewHolder> {

    private List<Integer> images;

    public CarouselAdapter(List<Integer> images) {
        this.images = images;
    }

    @NonNull
    @Override
    public CarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.carousel_item, parent, false);
        return new CarouselViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarouselViewHolder holder, int position) {
        holder.bind(images.get(position), false); // No logo in list anymore
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class CarouselViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;

        public CarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.carousel_image);
        }

        public void bind(int imageResource, boolean isLogo) {
            imageView.setImageResource(imageResource);
            // Use FIT_CENTER for logo to maintain aspect ratio and show full logo
            // Use CENTER_CROP for souk images for full coverage
            if (isLogo) {
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                // Add padding for logo so it doesn't touch edges
                imageView.setPadding(40, 40, 40, 40);
            } else {
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(0, 0, 0, 0);
            }
        }
    }
}