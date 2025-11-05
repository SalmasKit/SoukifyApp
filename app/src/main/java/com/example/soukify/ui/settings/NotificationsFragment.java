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
import com.example.soukify.databinding.FragmentNotificationsBinding;

/**
 * Fragment to manage notification preferences.
 */
public class NotificationsFragment extends Fragment {
    private FragmentNotificationsBinding binding;
    private int quietStartHour = 22, quietStartMinute = 0;
    private int quietEndHour = 7, quietEndMinute = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Load existing notification preferences

        // Quiet hours default labels
        updateQuietLabels();

        binding.pickStartButton.setOnClickListener(v -> showTimePicker(true));
        binding.pickEndButton.setOnClickListener(v -> showTimePicker(false));

        binding.openSystemSettingsButton.setOnClickListener(v -> openSystemNotificationSettings());

        binding.saveNotifButton.setOnClickListener(v -> savePreferences());
    }

    private void savePreferences() {
        boolean push = binding.switchPush.isChecked();
        boolean email = binding.switchEmail.isChecked();
        boolean sms = binding.switchSms.isChecked();
        boolean sound = binding.switchSound.isChecked();
        boolean vibrate = binding.switchVibrate.isChecked();
        boolean order = binding.switchOrderUpdates.isChecked();
        boolean promos = binding.switchPromotions.isChecked();
        boolean app = binding.switchAppUpdates.isChecked();

        // TODO: Persist preferences to storage/server
        showToast("Notification preferences saved");
        requireActivity().onBackPressed();
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        int hour = isStart ? quietStartHour : quietEndHour;
        int minute = isStart ? quietStartMinute : quietEndMinute;
        TimePickerDialog dialog = new TimePickerDialog(requireContext(), (view, h, m) -> {
            if (isStart) {
                quietStartHour = h;
                quietStartMinute = m;
            } else {
                quietEndHour = h;
                quietEndMinute = m;
            }
            updateQuietLabels();
        }, hour, minute, true);
        dialog.show();
    }

    private void updateQuietLabels() {
        binding.quietStartText.setText(formatTime(quietStartHour, quietStartMinute));
        binding.quietEndText.setText(formatTime(quietEndHour, quietEndMinute));
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
            showToast("Unable to open system settings");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
