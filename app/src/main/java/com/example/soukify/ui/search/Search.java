package com.example.soukify.ui.search;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.soukify.R;

public class Search extends AppCompatActivity {

    // Déclaration des vues importantes
    private EditText searchInput;
    private Button btnSearch, btnPromotions, btnObjectType, btnSortBy, btnTopRated;
    private TextView textViewVille;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Lie le layout XML
        setContentView(R.layout.activity_main);

        // Récupération des vues par leurs IDs
        searchInput = findViewById(R.id.search_input);
        btnSearch = findViewById(R.id.btn_search);
        btnPromotions = findViewById(R.id.btn_promotions);
        btnObjectType = findViewById(R.id.btn_object_type);
        btnSortBy = findViewById(R.id.btn_sort_by);
        btnTopRated = findViewById(R.id.btn_top_rated);
        textViewVille = findViewById(R.id.textView_ville);

        // Exemple de test : afficher un toast quand on clique sur "Search"
        btnSearch.setOnClickListener(v -> {
            String query = searchInput.getText().toString();
            Toast.makeText(Search.this, "Recherche : " + query, Toast.LENGTH_SHORT).show();
        });

        // Exemple : cliquer sur les boutons de filtres
        btnPromotions.setOnClickListener(v -> Toast.makeText(this, "Pro clicked", Toast.LENGTH_SHORT).show());
        btnObjectType.setOnClickListener(v -> Toast.makeText(this, "Type clicked", Toast.LENGTH_SHORT).show());
        btnSortBy.setOnClickListener(v -> Toast.makeText(this, "Sort clicked", Toast.LENGTH_SHORT).show());
        btnTopRated.setOnClickListener(v -> Toast.makeText(this, "Top clicked", Toast.LENGTH_SHORT).show());

        // Exemple : cliquer sur la ville
        textViewVille.setOnClickListener(v -> Toast.makeText(this, "Ville clicked", Toast.LENGTH_SHORT).show());
    }
}
