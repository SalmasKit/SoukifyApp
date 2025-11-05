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
import com.example.soukify.databinding.FragmentLanguageCurrencyBinding;

public class LanguageCurrencyFragment extends Fragment {
    private FragmentLanguageCurrencyBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLanguageCurrencyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Populate dropdowns from arrays
        ArrayAdapter<CharSequence> langAdapter = ArrayAdapter.createFromResource(requireContext(),
                com.example.soukify.R.array.supported_languages, android.R.layout.simple_list_item_1);
        binding.languageDropdown.setAdapter(langAdapter);

        ArrayAdapter<CharSequence> currAdapter = ArrayAdapter.createFromResource(requireContext(),
                com.example.soukify.R.array.supported_currencies, android.R.layout.simple_list_item_1);
        binding.currencyDropdown.setAdapter(currAdapter);

        // TODO: Load saved selections (persisted values)
        // For now, default to English and MAD
        binding.languageDropdown.setText(langAdapter.getItem(1), false); // English
        binding.currencyDropdown.setText(currAdapter.getItem(0), false); // MAD

        // Device language toggle disables manual language selection
        binding.switchDeviceLanguage.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.languageLayout.setEnabled(!isChecked);
            binding.languageDropdown.setEnabled(!isChecked);
        });

        // No preview: nothing to update on currency change

        binding.saveLangCurrButton.setOnClickListener(v -> save());
    }

    private void save() {
        boolean useDeviceLang = binding.switchDeviceLanguage.isChecked();
        String lang = useDeviceLang ? "Device" : (binding.languageDropdown.getText() != null ? binding.languageDropdown.getText().toString() : "");
        String curr = binding.currencyDropdown.getText() != null ? binding.currencyDropdown.getText().toString() : "";
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
        binding = null;
    }
}
