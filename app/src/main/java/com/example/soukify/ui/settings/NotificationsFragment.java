package com.example.soukify.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.provider.Settings;
import java.util.Calendar;
import android.widget.Button;
import com.google.android.material.switchmaterial.SwitchMaterial;
import android.widget.TextView;
import android.widget.Toast;
import com.example.soukify.R;
/**
 * Fragment to manage notification preferences.
 */
import androidx.lifecycle.ViewModelProvider;
import com.example.soukify.data.models.UserModel;

public class NotificationsFragment extends Fragment {
    private int quietStartHour = 22, quietStartMinute = 0;
    private int quietEndHour = 7, quietEndMinute = 0;
    private SettingsViewModel settingsViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        // Set up toolbar
        androidx.appcompat.widget.Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            // This will handle the back button press
            requireActivity().onBackPressed();
        });

        // Load local preferences first
        loadPreferences(view);
        
        // Observe backend data to sync if available
        settingsViewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
             if (user != null && user.getNotificationPreferences() != null) {
                 updateUIFromPreferences(view, user.getNotificationPreferences());
             }
        });

        // Quiet hours default labels
        updateQuietLabels(view);

        Button pickStartButton = view.findViewById(R.id.pickStartButton);
        Button pickEndButton = view.findViewById(R.id.pickEndButton);
        Button openSystemSettingsButton = view.findViewById(R.id.openSystemSettingsButton);
        Button saveNotifButton = view.findViewById(R.id.saveNotifButton);
        
        pickStartButton.setOnClickListener(v -> showTimePicker(view, true));
        pickEndButton.setOnClickListener(v -> showTimePicker(view, false));
        openSystemSettingsButton.setOnClickListener(v -> openSystemNotificationSettings());
        saveNotifButton.setOnClickListener(v -> savePreferences(view));
    }

    private void loadPreferences(View view) {
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("notifications", android.content.Context.MODE_PRIVATE);
        
        // Use SwitchMaterial to avoid ClassCastException
        ((SwitchMaterial) view.findViewById(R.id.switchPush)).setChecked(prefs.getBoolean("push", true));
        ((SwitchMaterial) view.findViewById(R.id.switchEmail)).setChecked(prefs.getBoolean("email", true));
        ((SwitchMaterial) view.findViewById(R.id.switchSound)).setChecked(prefs.getBoolean("sound", true));
        ((SwitchMaterial) view.findViewById(R.id.switchVibrate)).setChecked(prefs.getBoolean("vibrate", true));
        
        ((SwitchMaterial) view.findViewById(R.id.switchMessages)).setChecked(prefs.getBoolean("messages", true));
        ((SwitchMaterial) view.findViewById(R.id.switchNewProducts)).setChecked(prefs.getBoolean("new_products", true));
        ((SwitchMaterial) view.findViewById(R.id.switchShopPromotions)).setChecked(prefs.getBoolean("shop_promotions", true));
        
        ((SwitchMaterial) view.findViewById(R.id.switchPromotions)).setChecked(prefs.getBoolean("promotions", true));
        ((SwitchMaterial) view.findViewById(R.id.switchAppUpdates)).setChecked(prefs.getBoolean("app_updates", true));
        
        quietStartHour = prefs.getInt("quiet_start_hour", 22);
        quietStartMinute = prefs.getInt("quiet_start_minute", 0);
        quietEndHour = prefs.getInt("quiet_end_hour", 7);
        quietEndMinute = prefs.getInt("quiet_end_minute", 0);
    }

    private void updateUIFromPreferences(View view, UserModel.NotificationPreferences prefs) {
        ((SwitchMaterial) view.findViewById(R.id.switchPush)).setChecked(prefs.push);
        ((SwitchMaterial) view.findViewById(R.id.switchEmail)).setChecked(prefs.email);
        ((SwitchMaterial) view.findViewById(R.id.switchSound)).setChecked(prefs.sound);
        ((SwitchMaterial) view.findViewById(R.id.switchVibrate)).setChecked(prefs.vibrate);
        
        ((SwitchMaterial) view.findViewById(R.id.switchMessages)).setChecked(prefs.messages);
        ((SwitchMaterial) view.findViewById(R.id.switchNewProducts)).setChecked(prefs.newProducts);
        ((SwitchMaterial) view.findViewById(R.id.switchShopPromotions)).setChecked(prefs.shopPromotions);
        
        ((SwitchMaterial) view.findViewById(R.id.switchPromotions)).setChecked(prefs.promotions);
        ((SwitchMaterial) view.findViewById(R.id.switchAppUpdates)).setChecked(prefs.appUpdates);
        
        quietStartHour = prefs.quietStartHour;
        quietStartMinute = prefs.quietStartMinute;
        quietEndHour = prefs.quietEndHour;
        quietEndMinute = prefs.quietEndMinute;
        
        updateQuietLabels(view);
    }

    private void savePreferences(View view) {
        SwitchMaterial switchPush = view.findViewById(R.id.switchPush);
        SwitchMaterial switchEmail = view.findViewById(R.id.switchEmail);
        SwitchMaterial switchSound = view.findViewById(R.id.switchSound);
        SwitchMaterial switchVibrate = view.findViewById(R.id.switchVibrate);
        
        SwitchMaterial switchMessages = view.findViewById(R.id.switchMessages);
        SwitchMaterial switchNewProducts = view.findViewById(R.id.switchNewProducts);
        SwitchMaterial switchShopPromotions = view.findViewById(R.id.switchShopPromotions);
        
        SwitchMaterial switchPromotions = view.findViewById(R.id.switchPromotions);
        SwitchMaterial switchAppUpdates = view.findViewById(R.id.switchAppUpdates);
        
        android.content.SharedPreferences.Editor editor = requireContext().getSharedPreferences("notifications", android.content.Context.MODE_PRIVATE).edit();
        
        editor.putBoolean("push", switchPush.isChecked());
        editor.putBoolean("email", switchEmail.isChecked());
        editor.putBoolean("sound", switchSound.isChecked());
        editor.putBoolean("vibrate", switchVibrate.isChecked());
        
        editor.putBoolean("messages", switchMessages.isChecked());
        editor.putBoolean("new_products", switchNewProducts.isChecked());
        editor.putBoolean("shop_promotions", switchShopPromotions.isChecked());
        
        editor.putBoolean("promotions", switchPromotions.isChecked());
        editor.putBoolean("app_updates", switchAppUpdates.isChecked());
        
        editor.putInt("quiet_start_hour", quietStartHour);
        editor.putInt("quiet_start_minute", quietStartMinute);
        editor.putInt("quiet_end_hour", quietEndHour);
        editor.putInt("quiet_end_minute", quietEndMinute);
        
        editor.apply();
        
        // Sync to backend (users_settings collection)
        UserModel.NotificationPreferences prefs = new UserModel.NotificationPreferences();
        prefs.push = switchPush.isChecked();
        prefs.email = switchEmail.isChecked();
        prefs.sound = switchSound.isChecked();
        prefs.vibrate = switchVibrate.isChecked();
        
        prefs.messages = switchMessages.isChecked();
        prefs.newProducts = switchNewProducts.isChecked();
        prefs.shopPromotions = switchShopPromotions.isChecked();
        
        prefs.promotions = switchPromotions.isChecked();
        prefs.appUpdates = switchAppUpdates.isChecked();
        
        prefs.quietStartHour = quietStartHour;
        prefs.quietStartMinute = quietStartMinute;
        prefs.quietEndHour = quietEndHour;
        prefs.quietEndMinute = quietEndMinute;
        
        settingsViewModel.updateDetailedNotificationPreferences(prefs);
        // Note: The global "notifications" boolean in UserSettingModel could be updated here too if desired,
        // typically mirroring the main 'push' switch or a master toggle. 
        // For now, we sync detailed prefs.

        showToast(getString(R.string.notification_preferences_saved));
        requireActivity().onBackPressed();
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker(View view, boolean isStart) {
        Calendar cal = Calendar.getInstance();
        int hour = isStart ? quietStartHour : quietEndHour;
        int minute = isStart ? quietStartMinute : quietEndMinute;
        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (timePickerView, h, m) -> {
            if (isStart) {
                quietStartHour = h;
                quietStartMinute = m;
            } else {
                quietEndHour = h;
                quietEndMinute = m;
            }
            updateQuietLabels(view);
        }, hour, minute, true);
        dialog.show();
    }

    private void updateQuietLabels(View view) {
        TextView quietStartText = view.findViewById(R.id.quietStartText);
        TextView quietEndText = view.findViewById(R.id.quietEndText);
        quietStartText.setText(formatTime(quietStartHour, quietStartMinute));
        quietEndText.setText(formatTime(quietEndHour, quietEndMinute));
    }

    private String formatTime(int h, int m) {
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
    }

    private void openSystemNotificationSettings() {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(intent);
        } catch (Exception e) {
            showToast(getString(R.string.unable_to_open_system_settings));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    
}
