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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;
import com.example.soukify.R;
public class LanguageCurrencyFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_language_currency, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Populate dropdowns from arrays
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.supported_languages, android.R.layout.simple_list_item_1);
        AutoCompleteTextView languageDropdown = view.findViewById(R.id.languageDropdown);
        languageDropdown.setAdapter(langAdapter);

        ArrayAdapter<CharSequence> currAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.supported_currencies, android.R.layout.simple_list_item_1);
        AutoCompleteTextView currencyDropdown = view.findViewById(R.id.currencyDropdown);
        currencyDropdown.setAdapter(currAdapter);

        // TODO: Load saved selections (persisted values)
        // For now, default to English and MAD
        if (langAdapter.getCount() > 1) {
            languageDropdown.setText(langAdapter.getItem(1), false); // English
        }
        if (currAdapter.getCount() > 0) {
            currencyDropdown.setText(currAdapter.getItem(0), false); // MAD
        }

        // Device language toggle disables manual language selection
        com.google.android.material.switchmaterial.SwitchMaterial switchDeviceLanguage = view.findViewById(R.id.switchDeviceLanguage);
        android.view.ViewGroup languageLayout = view.findViewById(R.id.languageLayout);
        if (switchDeviceLanguage != null && languageLayout != null && languageDropdown != null) {
            switchDeviceLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
                languageLayout.setEnabled(!isChecked);
                languageDropdown.setEnabled(!isChecked);
            });
        }

        // No preview: nothing to update on currency change

        Button saveLangCurrButton = view.findViewById(R.id.saveLangCurrButton);
        if (saveLangCurrButton != null) {
            saveLangCurrButton.setOnClickListener(v -> save(view));
        }
    }

    private void save(View view) {
        com.google.android.material.switchmaterial.SwitchMaterial switchDeviceLanguage = view.findViewById(R.id.switchDeviceLanguage);
        AutoCompleteTextView languageDropdown = view.findViewById(R.id.languageDropdown);
        AutoCompleteTextView currencyDropdown = view.findViewById(R.id.currencyDropdown);
        
        if (switchDeviceLanguage == null || languageDropdown == null || currencyDropdown == null) {
            Toast.makeText(requireContext(), "Error: Unable to save settings", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean useDeviceLang = switchDeviceLanguage.isChecked();
        String lang = useDeviceLang ? "Device" : (languageDropdown.getText() != null ? languageDropdown.getText().toString() : "");
        String curr = currencyDropdown.getText() != null ? currencyDropdown.getText().toString() : "";
        if ((!useDeviceLang && lang.isEmpty()) || curr.isEmpty()) {
            Toast.makeText(requireContext(), "Please select both language and currency", Toast.LENGTH_SHORT).show();
            return;
        }
        // TODO: Persist selections and apply locale/format if needed
        Toast.makeText(requireContext(), "Saved: " + (useDeviceLang ? "Device language" : lang) + ", " + curr, Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    // Preview removed; no helper methods needed

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
