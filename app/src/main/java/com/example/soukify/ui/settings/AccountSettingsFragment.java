package com.example.soukify.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.soukify.databinding.FragmentAccountSettingsBinding;
import com.example.soukify.ui.settings.SettingsViewModel;

public class AccountSettingsFragment extends Fragment {
    private FragmentAccountSettingsBinding binding;
    private SettingsViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAccountSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        
        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                binding.nameEditText.setText(user.getFullName());
                binding.emailEditText.setText(user.getEmail());
                binding.phoneEditText.setText(user.getPhoneNumber());
            }
        });
    }

    private void setupClickListeners() {
        binding.saveButton.setOnClickListener(v -> {
            String name = binding.nameEditText.getText().toString().trim();
            String email = binding.emailEditText.getText().toString().trim();
            String phone = binding.phoneEditText.getText().toString().trim();
            
            if (name.isEmpty()) {
                binding.nameLayout.setError("Name is required");
                return;
            }
            
            if (email.isEmpty()) {
                binding.emailLayout.setError("Email is required");
                return;
            }
            
            // Update user profile
            viewModel.updateUserProfile(name, email, phone);
            Toast.makeText(getContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show();
        });
        
        binding.changePasswordButton.setOnClickListener(v -> {
            // TODO: Implement password change functionality
            Toast.makeText(getContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.changePhotoButton.setOnClickListener(v -> {
            // TODO: Implement photo change functionality
            Toast.makeText(getContext(), "Change photo feature coming soon", Toast.LENGTH_SHORT).show();
        });
        
        binding.refreshButton.setOnClickListener(v -> {
            viewModel.refreshCurrentUser();
            Toast.makeText(getContext(), "Data refreshed", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
