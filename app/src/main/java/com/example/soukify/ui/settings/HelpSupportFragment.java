package com.example.soukify.ui.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.soukify.R;

public class HelpSupportFragment extends Fragment {

    private LinearLayout faqHeader1, faqHeader2, faqHeader3, faqHeader4, faqHeader5, faqHeader6, faqHeader7, faqHeader8, faqHeader9, faqHeader10;
    private TextView faqAnswer1, faqAnswer2, faqAnswer3, faqAnswer4, faqAnswer5, faqAnswer6, faqAnswer7, faqAnswer8, faqAnswer9, faqAnswer10;
    private ImageView faqIcon1, faqIcon2, faqIcon3, faqIcon4, faqIcon5, faqIcon6, faqIcon7, faqIcon8, faqIcon9, faqIcon10;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_help_support, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize toolbar with back button
        initializeToolbar(view);

        initFAQViews(view);
        setupFAQClickListeners();
        
        View contactSupportButton = view.findViewById(R.id.contactSupportButton);
        contactSupportButton.setOnClickListener(v -> contactSupport());
    }

    private void initFAQViews(View view) {
        // FAQ Item 1
        faqHeader1 = view.findViewById(R.id.faqHeader1);
        faqAnswer1 = view.findViewById(R.id.faqAnswer1);
        faqIcon1 = view.findViewById(R.id.faqIcon1);

        // FAQ Item 2
        faqHeader2 = view.findViewById(R.id.faqHeader2);
        faqAnswer2 = view.findViewById(R.id.faqAnswer2);
        faqIcon2 = view.findViewById(R.id.faqIcon2);

        // FAQ Item 3
        faqHeader3 = view.findViewById(R.id.faqHeader3);
        faqAnswer3 = view.findViewById(R.id.faqAnswer3);
        faqIcon3 = view.findViewById(R.id.faqIcon3);

        // FAQ Item 4
        faqHeader4 = view.findViewById(R.id.faqHeader4);
        faqAnswer4 = view.findViewById(R.id.faqAnswer4);
        faqIcon4 = view.findViewById(R.id.faqIcon4);

        // FAQ Item 5
        faqHeader5 = view.findViewById(R.id.faqHeader5);
        faqAnswer5 = view.findViewById(R.id.faqAnswer5);
        faqIcon5 = view.findViewById(R.id.faqIcon5);

        // FAQ Item 6
        faqHeader6 = view.findViewById(R.id.faqHeader6);
        faqAnswer6 = view.findViewById(R.id.faqAnswer6);
        faqIcon6 = view.findViewById(R.id.faqIcon6);

        // FAQ Item 7
        faqHeader7 = view.findViewById(R.id.faqHeader7);
        faqAnswer7 = view.findViewById(R.id.faqAnswer7);
        faqIcon7 = view.findViewById(R.id.faqIcon7);

        // FAQ Item 8
        faqHeader8 = view.findViewById(R.id.faqHeader8);
        faqAnswer8 = view.findViewById(R.id.faqAnswer8);
        faqIcon8 = view.findViewById(R.id.faqIcon8);

        // FAQ Item 9
        faqHeader9 = view.findViewById(R.id.faqHeader9);
        faqAnswer9 = view.findViewById(R.id.faqAnswer9);
        faqIcon9 = view.findViewById(R.id.faqIcon9);

        // FAQ Item 10
        faqHeader10 = view.findViewById(R.id.faqHeader10);
        faqAnswer10 = view.findViewById(R.id.faqAnswer10);
        faqIcon10 = view.findViewById(R.id.faqIcon10);
    }

    private void setupFAQClickListeners() {
        faqHeader1.setOnClickListener(v -> toggleFAQ(1));
        faqHeader2.setOnClickListener(v -> toggleFAQ(2));
        faqHeader3.setOnClickListener(v -> toggleFAQ(3));
        faqHeader4.setOnClickListener(v -> toggleFAQ(4));
        faqHeader5.setOnClickListener(v -> toggleFAQ(5));
        faqHeader6.setOnClickListener(v -> toggleFAQ(6));
        faqHeader7.setOnClickListener(v -> toggleFAQ(7));
        faqHeader8.setOnClickListener(v -> toggleFAQ(8));
        faqHeader9.setOnClickListener(v -> toggleFAQ(9));
        faqHeader10.setOnClickListener(v -> toggleFAQ(10));
    }

    private void toggleFAQ(int faqNumber) {
        switch (faqNumber) {
            case 1:
                toggleAnswer(faqAnswer1, faqIcon1);
                break;
            case 2:
                toggleAnswer(faqAnswer2, faqIcon2);
                break;
            case 3:
                toggleAnswer(faqAnswer3, faqIcon3);
                break;
            case 4:
                toggleAnswer(faqAnswer4, faqIcon4);
                break;
            case 5:
                toggleAnswer(faqAnswer5, faqIcon5);
                break;
            case 6:
                toggleAnswer(faqAnswer6, faqIcon6);
                break;
            case 7:
                toggleAnswer(faqAnswer7, faqIcon7);
                break;
            case 8:
                toggleAnswer(faqAnswer8, faqIcon8);
                break;
            case 9:
                toggleAnswer(faqAnswer9, faqIcon9);
                break;
            case 10:
                toggleAnswer(faqAnswer10, faqIcon10);
                break;
        }
    }

    private void toggleAnswer(TextView answerView, ImageView iconView) {
        if (answerView.getVisibility() == View.GONE) {
            answerView.setVisibility(View.VISIBLE);
            iconView.setImageResource(R.drawable.ic_expand_less);
        } else {
            answerView.setVisibility(View.GONE);
            iconView.setImageResource(R.drawable.ic_expand_more);
        }
    }

    private void contactSupport() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bksalma26@gmail.com"});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Soukify Support Request");
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
    
    private void initializeToolbar(View view) {
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> {
                // Navigate back to previous screen
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                } else {
                    // Fallback to navigation controller
                    Navigation.findNavController(requireView()).navigateUp();
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
