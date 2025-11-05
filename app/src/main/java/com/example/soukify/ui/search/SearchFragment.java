package com.example.soukify.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private Button btnSearch, btnPromotions, btnObjectType, btnSortBy, btnTopRated;
    private TextView textViewVille;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Lie le fragment au layout XML
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Récupération des vues depuis le fragment
        searchInput = view.findViewById(R.id.search_input);
        btnSearch = view.findViewById(R.id.btn_search);
        btnPromotions = view.findViewById(R.id.btn_promotions);
        btnObjectType = view.findViewById(R.id.btn_object_type);
        btnSortBy = view.findViewById(R.id.btn_sort_by);
        btnTopRated = view.findViewById(R.id.btn_top_rated);
        textViewVille = view.findViewById(R.id.textView_ville);

        // Exemple : clic sur "Search"
        btnSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            Toast.makeText(getActivity(), "Recherche : " + query, Toast.LENGTH_SHORT).show();
        });

        // Exemple : clic sur les boutons de filtres
        btnPromotions.setOnClickListener(v -> Toast.makeText(getActivity(), "Pro clicked", Toast.LENGTH_SHORT).show());
        btnObjectType.setOnClickListener(v -> Toast.makeText(getActivity(), "Type clicked", Toast.LENGTH_SHORT).show());
        btnSortBy.setOnClickListener(v -> Toast.makeText(getActivity(), "Sort clicked", Toast.LENGTH_SHORT).show());
        btnTopRated.setOnClickListener(v -> Toast.makeText(getActivity(), "Top clicked", Toast.LENGTH_SHORT).show());

        // Exemple : clic sur la ville
        textViewVille.setOnClickListener(v -> Toast.makeText(getActivity(), "Ville clicked", Toast.LENGTH_SHORT).show());

        return view;
    }
}
