/*package com.example.soukify.ui.search;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.soukify.R;
import com.exemple.soukify.data.entities.Product;
import com.exemple.soukify.data.dao.ProductDao;

import java.util.List;
import java.util.stream.Collectors;

public class SearchFragment extends Fragment {

    private EditText searchInput;
    private TextView textViewVille;
    private LinearLayout layoutProducts;

    private LinearLayout catTapis, catFood, catPotterie, catHerbs, catJwellery, catMetal, catDraws, catWood;

    private ProductDao productRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Recherche et ville
        searchInput = view.findViewById(R.id.search_input);
        textViewVille = view.findViewById(R.id.textView_ville);

        // Layout pour afficher les produits
        layoutProducts = view.findViewById(R.id.layout_products);

        // Catégories
        catTapis = view.findViewById(R.id.cat_tapis);
        catFood = view.findViewById(R.id.cat_food);
        catPotterie = view.findViewById(R.id.cat_potterie);
        catHerbs = view.findViewById(R.id.cat_herbs);
        catJwellery = view.findViewById(R.id.cat_jwellery);
        catMetal = view.findViewById(R.id.cat_metal);
        catDraws = view.findViewById(R.id.cat_draws);
        catWood = view.findViewById(R.id.cat_wood);

        // Simuler un repository (ou tu peux utiliser ta base SQLite)
       // productRepository = new ProductDao(getContext());

        // Ajouter les clics sur les catégories
        setCategoryClick(catTapis, "tapis");
        setCategoryClick(catFood, "food");
        setCategoryClick(catPotterie, "potterie");
        setCategoryClick(catHerbs, "herbs");
        setCategoryClick(catJwellery, "jwellery");
        setCategoryClick(catMetal, "metal");
        setCategoryClick(catDraws, "draws");
        setCategoryClick(catWood, "wood");

        return view;
    }

    private void setCategoryClick(LinearLayout categoryLayout, String type) {
        categoryLayout.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Catégorie: " + type, Toast.LENGTH_SHORT).show();
            displayProductsByType(type);
        });
    }

    private void displayProductsByType(String type) {
        // Récupérer tous les produits
       // List<Product> allProducts = productRepository.getAllProducts();

        // Filtrer selon le type
       // List<Product> filtered = allProducts.stream()
         //       .filter(p -> p.getType().equalsIgnoreCase(type))
           //     .collect(Collectors.toList());

        // Vider l'ancien layout
      //  layoutProducts.removeAllViews();

        // Ajouter dynamiquement les produits filtrés
       // for (Product product : filtered) {
         //   View productView = LayoutInflater.from(getContext()).inflate(R.layout.item_product_card, layoutProducts, false);

            // Ici, tu peux lier les images, noms, notes etc.
            // Exemple :
            // ImageView img = productView.findViewById(R.id.product_img);
            // img.setImageResource(product.getImageResId());

           // layoutProducts.addView(productView);
    //    }
  //  }
//}*/

