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
import android.widget.Button;
import android.widget.Toast;

import com.example.soukify.R;


public class HelpSupportFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button faqButton = view.findViewById(R.id.faqButton);
        Button contactSupportButton = view.findViewById(R.id.contactSupportButton);
        
        faqButton.setOnClickListener(v -> openFaq());
        contactSupportButton.setOnClickListener(v -> contactSupport());
    }

    private void openFaq() {
        // TODO: Replace with your real FAQ URL
        String url = "https://example.com/faq";
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            showToast("Unable to open FAQ");
        }
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@example.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Soukify Support");
        try {
            startActivity(Intent.createChooser(intent, "Contact Support"));
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
