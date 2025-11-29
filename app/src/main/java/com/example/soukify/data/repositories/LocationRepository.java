package com.example.soukify.data.repositories;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.app.Application;
import com.example.soukify.data.remote.FirebaseManager;
import com.example.soukify.data.models.RegionModel;
import com.example.soukify.data.models.CityModel;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Location Repository - Firebase implementation
 * Follows MVVM pattern by abstracting data operations from ViewModels
 */
public class LocationRepository {
    private final MutableLiveData<List<RegionModel>> regions = new MutableLiveData<>();
    private final MutableLiveData<List<CityModel>> cities = new MutableLiveData<>();
    private final MutableLiveData<RegionModel> currentRegion = new MutableLiveData<>();
    private final MutableLiveData<CityModel> currentCity = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final Application application;
    
    public LocationRepository(Application application) {
        this.application = application;
        // Load regions and initialize sample data if needed
        loadRegions();
        // Optionally populate Firebase with initial data
        populateFirebaseWithInitialData();
    }

    public LiveData<List<RegionModel>> getRegions() {
        return regions;
    }
    
    public LiveData<List<CityModel>> getCities() {
        return cities;
    }
    
    public LiveData<RegionModel> getCurrentRegion() {
        return currentRegion;
    }
    
    public LiveData<CityModel> getCurrentCity() {
        return currentCity;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public void loadRegions() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // For now, use static Morocco regions data
        // In production, this would load from Firestore
        List<RegionModel> regionList = getMoroccoRegions();
        regions.setValue(regionList);
        isLoading.setValue(false);
    }
    
    public void loadCitiesByRegion(String regionName) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // For now, use static Morocco cities data
        // In production, this would load from Firestore
        List<CityModel> cityList = getCitiesForRegion(regionName);
        cities.setValue(cityList);
        isLoading.setValue(false);
    }
    
    public void getRegionByName(String regionName) {
        List<RegionModel> allRegions = regions.getValue();
        if (allRegions != null) {
            for (RegionModel region : allRegions) {
                if (region.getName().equals(regionName)) {
                    currentRegion.setValue(region);
                    return;
                }
            }
        }
        currentRegion.setValue(null);
    }
    
    public void getCityByName(String cityName) {
        List<CityModel> allCities = cities.getValue();
        if (allCities != null) {
            for (CityModel city : allCities) {
                if (city.getName().equals(cityName)) {
                    currentCity.setValue(city);
                    return;
                }
            }
        }
        currentCity.setValue(null);
    }
    
    public LiveData<RegionModel> getRegionById(int regionId) {
        MutableLiveData<RegionModel> result = new MutableLiveData<>();
        List<RegionModel> allRegions = regions.getValue();
        if (allRegions != null) {
            for (RegionModel region : allRegions) {
                if (region.getRegionId() != null && region.getRegionId().endsWith("_" + regionId)) {
                    result.setValue(region);
                    return result;
                }
            }
        }
        result.setValue(null);
        return result;
    }
    
    public LiveData<CityModel> getCityById(int cityId) {
        MutableLiveData<CityModel> result = new MutableLiveData<>();
        List<CityModel> allCities = cities.getValue();
        if (allCities != null) {
            for (CityModel city : allCities) {
                if (city.getCityId() != null && city.getCityId().endsWith("_" + cityId)) {
                    result.setValue(city);
                    return result;
                }
            }
        }
        result.setValue(null);
        return result;
    }
    
    // Synchronous methods for backward compatibility
    public RegionModel getRegionModelByName(String regionName) {
        List<RegionModel> allRegions = regions.getValue();
        if (allRegions != null) {
            for (RegionModel region : allRegions) {
                if (region.getName().equals(regionName)) {
                    return region;
                }
            }
        }
        return null;
    }
    
    public CityModel getCityModelByName(String cityName) {
        List<CityModel> allCities = cities.getValue();
        if (allCities != null) {
            for (CityModel city : allCities) {
                if (city.getName().equals(cityName)) {
                    return city;
                }
            }
        }
        return null;
    }

    private List<RegionModel> getMoroccoRegions() {
        List<String> moroccoRegions = Arrays.asList(
            "Tanger-Tétouan-Al Hoceima",
            "L'Oriental",
            "Fès-Meknès",
            "Rabat-Salé-Kénitra",
            "Béni Mellal-Khénifra",
            "Casablanca-Settat",
            "Marrakech-Safi",
            "Drâa-Tafilalet",
            "Souss-Massa",
            "Guelmim-Oued Noun",
            "Laâyoune-Sakia El Hamra",
            "Dakhla-Oued Ed-Dahab"
        );

        List<RegionModel> regionModels = new ArrayList<>();
        for (int i = 0; i < moroccoRegions.size(); i++) {
            RegionModel region = new RegionModel(moroccoRegions.get(i));
            region.setRegionId("region_" + (i + 1));
            regionModels.add(region);
        }
        return regionModels;
    }

    private List<CityModel> getCitiesForRegion(String regionName) {
        List<String> cityNames = getSampleCitiesForRegion(regionName);
        List<CityModel> cityModels = new ArrayList<>();
        
        for (int i = 0; i < cityNames.size(); i++) {
            CityModel city = new CityModel("region_" + regionName.hashCode(), cityNames.get(i));
            city.setCityId("city_" + regionName.hashCode() + "_" + (i + 1));
            cityModels.add(city);
        }
        return cityModels;
    }

    public void clearAndRepopulateCities() {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        
        // Clear current cities and reload them
        cities.setValue(new ArrayList<>());
        loadCitiesByRegion(currentRegion.getValue() != null ? currentRegion.getValue().getName() : "Casablanca-Settat");
        isLoading.setValue(false);
    }
    
    private List<String> getSampleCitiesForRegion(String regionName) {
        // Morocco cities data by region
        switch (regionName) {
            case "Tanger-Tétouan-Al Hoceima":
                return Arrays.asList("Tangier", "Tétouan", "Al Hoceima", "Chefchaouen", "Larache", "Martil", "M'diq", "Fnideq", "Ksar El Kebir", "Assilah", "Ouezzane", "Imzouren");
            case "L'Oriental":
                return Arrays.asList("Oujda", "Nador", "Berkane", "Jerada", "Taourirt", "Ahfir", "Beni Ansar", "El Aaroui", "Zaio", "Driouch", "Figuig", "Saidia");
            case "Fès-Meknès":
                return Arrays.asList("Fès", "Meknès", "Ifrane", "El Hajeb", "Sefrou", "Boulemane", "Azrou", "Missour", "Kariat Ba Mohamed", "Imouzzer Kandar", "Taza", "Moulay Yacoub");
            case "Rabat-Salé-Kénitra":
                return Arrays.asList("Rabat", "Salé", "Kénitra", "Témara", "Skhirate", "Khemisset", "Sidi Slimane", "Sidi Kacem", "Tiflet", "Rommani", "Bouknadel", "Ain El Aouda");
            case "Béni Mellal-Khénifra":
                return Arrays.asList("Beni Mellal", "Khénifra", "Azilal", "Fquih Ben Salah", "Khouribga", "Oued Zem", "Zaouiat Cheikh", "Demnate", "El Ksiba", "Afourer", "Aghbala");
            case "Casablanca-Settat":
                return Arrays.asList("Casablanca", "Settat", "Mohammedia", "Berrechid", "El Jadida", "Benslimane", "Bouznika", "Nouaceur", "Médiouna", "Deroua", "Bouskoura", "Azemmour");
            case "Marrakech-Safi":
                return Arrays.asList("Marrakech", "Safi", "Essaouira", "El Kelaa des Sraghna", "Benguerir", "Chichaoua", "Youssoufia", "Imintanoute", "Ait Ourir", "Amizmiz", "Sidi Bou Othmane", "Jemaa Shaim");
            case "Drâa-Tafilalet":
                return Arrays.asList("Errachidia", "Ouarzazate", "Midelt", "Zagora", "Tinghir", "Kelaat Mgouna", "Boumalne Dades", "Goulmima", "Rich", "Erfoud", "Rissani", "Tinejdad", "Agdz", "Nkob");
            case "Souss-Massa":
                return Arrays.asList("Agadir", "Inezgane", "Ait Melloul", "Taroudant", "Tiznit", "Biougra", "Oulad Teima", "Dcheira El Jihadia", "Drargua", "Massa", "Lqliâa", "Temsia", "Tafraout", "Sidi Ifni", "Tata");
            case "Guelmim-Oued Noun":
                return Arrays.asList("Guelmim", "Tan-Tan", "Assa", "Bouizakarne", "Ifrane Atlas Saghir", "Ouatia", "Taghjijt", "Tighmert", "Zag");
            case "Laâyoune-Sakia El Hamra":
                return Arrays.asList("Laâyoune", "Boujdour", "Tarfaya", "El Marsa", "Es-Semara");
            case "Dakhla-Oued Ed-Dahab":
                return Arrays.asList("Dakhla", "Aousserd", "Bir Gandouz", "Guerguerat");
            default:
                return Arrays.asList("City 1", "City 2", "City 3");
        }
    }
    
    /**
     * Populates Firebase with initial Morocco regions and cities data
     * This should be called once to initialize the database
     */
    private void populateFirebaseWithInitialData() {
        // Check if regions collection is empty before populating
        FirebaseManager.getInstance(application).getFirestore()
                .collection("regions")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Regions collection is empty, populate it
                        populateRegionsInFirebase();
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't check, assume we need to populate
                    populateRegionsInFirebase();
                });
        
        // Check if cities collection is empty before populating
        FirebaseManager.getInstance(application).getFirestore()
                .collection("cities")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Cities collection is empty, populate it
                        populateCitiesInFirebase();
                    }
                })
                .addOnFailureListener(e -> {
                    // If we can't check, assume we need to populate
                    populateCitiesInFirebase();
                });
    }
    
    private void populateRegionsInFirebase() {
        List<RegionModel> regionList = getMoroccoRegions();
        
        for (RegionModel region : regionList) {
            FirebaseManager.getInstance(application).getFirestore()
                    .collection("regions")
                    .document(region.getRegionId())
                    .set(region)
                    .addOnSuccessListener(aVoid -> {
                        android.util.Log.d("LocationRepository", "Region added to Firebase: " + region.getName());
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("LocationRepository", "Failed to add region to Firebase: " + region.getName(), e);
                    });
        }
    }
    
    private void populateCitiesInFirebase() {
        List<RegionModel> allRegions = getMoroccoRegions();
        
        for (RegionModel region : allRegions) {
            List<CityModel> cityList = getCitiesForRegion(region.getName());
            
            for (CityModel city : cityList) {
                FirebaseManager.getInstance(application).getFirestore()
                        .collection("cities")
                        .document(city.getCityId())
                        .set(city)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("LocationRepository", "City added to Firebase: " + city.getName() + " (region: " + region.getName() + ")");
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("LocationRepository", "Failed to add city to Firebase: " + city.getName(), e);
                        });
            }
        }
    }
    
    /**
     * Public method to manually trigger Firebase population
     * Call this if you want to force repopulation of the collections
     */
    public void repopulateFirebaseData() {
        populateRegionsInFirebase();
        populateCitiesInFirebase();
    }
}
