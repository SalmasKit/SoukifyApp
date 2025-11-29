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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.example.soukify.R;
/**
 * Fragment to manage notification preferences.
 */
public class NotificationsFragment extends Fragment {
    private int quietStartHour = 22, quietStartMinute = 0;
    private int quietEndHour = 7, quietEndMinute = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // TODO: Load existing notification preferences

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

    private void savePreferences(View view) {
        Switch switchPush = view.findViewById(R.id.switchPush);
        Switch switchEmail = view.findViewById(R.id.switchEmail);
        Switch switchSms = view.findViewById(R.id.switchSms);
        Switch switchSound = view.findViewById(R.id.switchSound);
        Switch switchVibrate = view.findViewById(R.id.switchVibrate);
        Switch switchOrderUpdates = view.findViewById(R.id.switchOrderUpdates);
        Switch switchPromotions = view.findViewById(R.id.switchPromotions);
        Switch switchAppUpdates = view.findViewById(R.id.switchAppUpdates);
        
        boolean push = switchPush.isChecked();
        boolean email = switchEmail.isChecked();
        boolean sms = switchSms.isChecked();
        boolean sound = switchSound.isChecked();
        boolean vibrate = switchVibrate.isChecked();
        boolean order = switchOrderUpdates.isChecked();
        boolean promos = switchPromotions.isChecked();
        boolean app = switchAppUpdates.isChecked();

        // TODO: Persist preferences to storage/server
        showToast("Notification preferences saved");
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
            showToast("Unable to open system settings");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
