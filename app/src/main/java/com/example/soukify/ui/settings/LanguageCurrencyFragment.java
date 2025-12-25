package com.example.soukify.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import com.example.soukify.R;
import com.example.soukify.data.repositories.UserSettingRepository;
import com.example.soukify.utils.LocaleHelper;
import com.example.soukify.utils.CurrencyHelper;
import com.example.soukify.data.models.UserSettingModel;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Fragment for managing user language and currency preferences
 * Integrates with Firebase (UserSettingRepository) for cloud storage
 * Provides multilingual support with English, French, and Arabic translations
 */
public class LanguageCurrencyFragment extends Fragment {
        private void initializeToolbar(View view) {
            androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.setNavigationOnClickListener(v -> {
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    } else {
                        requireActivity().onBackPressed();
                    }
                });
            }
        }
    
    private UserSettingRepository userSettingRepository;
    private String currentUserId;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_currency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeToolbar(view);
        // Initialize repository
        userSettingRepository = new UserSettingRepository(requireActivity().getApplication());
        
        // Get current user ID (you may need to adjust this based on your auth implementation)
        currentUserId = getCurrentUserId();
        
        // Setup language dropdown
        // Use localized language names for dropdown
        String[] localizedLanguages = new String[] {
            getString(R.string.lang_arabic),
            getString(R.string.lang_english),
            getString(R.string.lang_french)
        };
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, localizedLanguages);
        AutoCompleteTextView languageDropdown = view.findViewById(R.id.languageDropdown);
        languageDropdown.setAdapter(langAdapter);

        // Setup currency dropdown
        ArrayAdapter<CharSequence> currAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.supported_currencies, android.R.layout.simple_list_item_1);
        AutoCompleteTextView currencyDropdown = view.findViewById(R.id.currencyDropdown);
        currencyDropdown.setAdapter(currAdapter);

        // Load saved user settings
        loadSavedSettings(view, languageDropdown, currencyDropdown);

        // Device language toggle
        SwitchMaterial switchDeviceLanguage = view.findViewById(R.id.switchDeviceLanguage);
        ViewGroup languageLayout = view.findViewById(R.id.languageLayout);
        if (switchDeviceLanguage != null && languageLayout != null && languageDropdown != null) {
            switchDeviceLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                languageLayout.setEnabled(!isChecked);
                languageDropdown.setEnabled(!isChecked);
            });
        }

        // Save button
        Button saveLangCurrButton = view.findViewById(R.id.saveLangCurrButton);
        if (saveLangCurrButton != null) {
            saveLangCurrButton.setOnClickListener(v -> save(view));
        }
    }

    /**
     * Load saved user settings from Firebase/Database
     */
    private void loadSavedSettings(View view, AutoCompleteTextView languageDropdown, AutoCompleteTextView currencyDropdown) {
        if (currentUserId != null) {
            userSettingRepository.getCurrentUserSettings().observe(getViewLifecycleOwner(), userSettings -> {
                if (userSettings != null) {
                    // Set language
                    String language = userSettings.getLanguage();
                    if (language != null && !language.isEmpty()) {
                        // Just update the UI text, don't force apply the language here
                        // Applying language should happen in attachBaseContext or when explicitly saved
                        if ("device".equalsIgnoreCase(language)) {
                            languageDropdown.setText(getString(R.string.device_language), false);
                        } else {
                            languageDropdown.setText(convertLanguageCodeToName(language), false);
                        }
                    } else {
                        languageDropdown.setText(getString(R.string.language), false);
                    }
                    
                    // Set currency
                    String currency = userSettings.getCurrency();
                    if (currency != null && !currency.isEmpty()) {
                        // Apply saved currency locally
                        CurrencyHelper.setCurrency(getContext(), currency);
                        currencyDropdown.setText(currency, false);
                    } else {
                        currencyDropdown.setText(CurrencyHelper.getCurrency(getContext()), false);
                    }
                    
                    // Set device language toggle
                    SwitchMaterial switchDeviceLanguage = view.findViewById(R.id.switchDeviceLanguage);
                    if (switchDeviceLanguage != null && "device".equalsIgnoreCase(userSettings.getLanguage())) {
                        switchDeviceLanguage.setChecked(true);
                    }
                }
            });
            
            // Load from repository
            userSettingRepository.loadUserSettings(currentUserId);
        }
    }

    /**
     * Convert language code to display name
     */
    private String convertLanguageCodeToName(String code) {
        if (code == null) return getString(R.string.lang_english);
        switch (code.toLowerCase()) {
            case "fr":
                return getString(R.string.lang_french);
            case "ar":
                return getString(R.string.lang_arabic);
            case "en":
            default:
                return getString(R.string.lang_english);
        }
    }

    /**
     * Save selected language and currency preferences
     */
    private void save(View view) {
        SwitchMaterial switchDeviceLanguage = view.findViewById(R.id.switchDeviceLanguage);
        AutoCompleteTextView languageDropdown = view.findViewById(R.id.languageDropdown);
        AutoCompleteTextView currencyDropdown = view.findViewById(R.id.currencyDropdown);
        
        if (switchDeviceLanguage == null || languageDropdown == null || currencyDropdown == null) {
            showErrorToast(getString(R.string.error_unable_to_save_settings));
            return;
        }
        
        boolean useDeviceLang = switchDeviceLanguage.isChecked();
        String lang = useDeviceLang ? "device" : (languageDropdown.getText() != null ? languageDropdown.getText().toString() : "");
        String curr = currencyDropdown.getText() != null ? currencyDropdown.getText().toString() : "";
        
        // Validate input
        if ((!useDeviceLang && lang.isEmpty()) || curr.isEmpty()) {
            showErrorToast(getString(R.string.please_select_both_language_and_currency));
            return;
        }
        
        // Convert language name to code
        String languageCode = convertLanguageNameToCode(lang);
        
        // Create user settings model
        UserSettingModel userSettings = new UserSettingModel(currentUserId);
        userSettings.setLanguage(languageCode);
        userSettings.setCurrency(curr);
        
        // Save to Firebase
        userSettingRepository.updateUserSettings(userSettings);
        
        // Apply language change immediately
        applyLanguageChange(languageCode);
        
        // Apply currency change locally
        CurrencyHelper.setCurrency(getContext(), curr);
        
        // Show success message
        String displayLang = useDeviceLang ? getString(R.string.device_language) : lang;
        String successMessage = String.format(getString(R.string.saved_format), displayLang, curr);
        showSuccessToast(successMessage);
        
        requireActivity().onBackPressed();
    }

    /**
     * Convert language display name to language code
     */
    private String convertLanguageNameToCode(String languageName) {
        if (languageName == null) return "en";
        String english = getString(R.string.lang_english);
        String french = getString(R.string.lang_french);
        String arabic = getString(R.string.lang_arabic);
        if (languageName.equalsIgnoreCase(english)) {
            return "en";
        } else if (languageName.equalsIgnoreCase(french)) {
            return "fr";
        } else if (languageName.equalsIgnoreCase(arabic)) {
            return "ar";
        }
        return "en";
    }

    /**
     * Apply language change to the app
     */
    private void applyLanguageChange(String languageCode) {
        if (getContext() == null) return;
        
        // Update app locale and save to local preferences
        LocaleHelper.applyLanguage(getContext(), languageCode);
        
        // Recreate activity to apply new strings
        // We use a small delay to ensure the UI has updated its internal state if needed
        if (getActivity() != null) {
            getActivity().recreate();
        }
    }

    /**
     * Get current user ID from Firebase Auth
     */
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            return user.getUid();
        }
        // Fallback for non-logged in users (e.g., using a fixed ID or device ID)
        return "anonymous_user"; 
    }

    /**
     * Show error toast message using string resource
     */
    private void showErrorToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Show success toast message
     */
    private void showSuccessToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
