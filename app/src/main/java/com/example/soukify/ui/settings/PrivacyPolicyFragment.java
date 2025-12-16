package com.example.soukify.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;

public class PrivacyPolicyFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_privacy_policy, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        View contactPrivacyButton = view.findViewById(R.id.contactPrivacyButton);
        contactPrivacyButton.setOnClickListener(v -> contactPrivacySupport());
    }

    private void contactPrivacySupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bksalma26@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Soukify Privacy Policy Inquiry");
        try {
            startActivity(Intent.createChooser(intent, "Contact Privacy Support"));
        } catch (ActivityNotFoundException e) {
            showToast("No email app found");
        }
    }

    private void showToast(String msg) {
        if (getContext() != null) {
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
