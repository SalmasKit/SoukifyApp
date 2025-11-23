package com.example.soukify.ui.sign;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.soukify.R;
import com.exemple.soukify.data.AppDatabase;
import com.exemple.soukify.data.entities.User;
import com.example.soukify.utils.PasswordHash;
import com.example.soukify.utils.ValidationUtils;

public class SignActivity extends AppCompatActivity {

    private AppDatabase db;
    private EditText fullNameField, phoneField, passwordField, confirmField;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        // Initialisation des vues
        fullNameField = findViewById(R.id.username);
        phoneField = findViewById(R.id.phone);
        passwordField = findViewById(R.id.password);
        confirmField = findViewById(R.id.confirm);
        signupButton = findViewById(R.id.signbtn);

        // Base de données
        db=AppDatabase.getInstance(this);
        if (db == null) {
            Toast.makeText(this, "DB non initialisée !", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "DB initialisée !", Toast.LENGTH_LONG).show();

        }


        // Désactiver le bouton au départ
        //signupButton.setEnabled(false);

        // Validation temps réel du téléphone et mot de passe
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String fullName = fullNameField.getText().toString().trim();
                String phone = phoneField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();
                String confirm = confirmField.getText().toString().trim();

                boolean phoneValid = ValidationUtils.isValidPhone(phone);
                boolean passwordValid = ValidationUtils.isValidPassword(password) && password.equals(confirm);
                boolean allFilled = !fullName.isEmpty() && !phone.isEmpty() && !password.isEmpty() && !confirm.isEmpty();

                // Messages d'erreur temps réel
                if (!phone.isEmpty() && !phoneValid) {
                    phoneField.setError("Numéro invalide (06 ou 07 + 8 chiffres)");
                } else {
                    phoneField.setError(null);
                }

                if (!password.equals(confirm) && !confirm.isEmpty()) {
                    confirmField.setError("Les mots de passe ne correspondent pas");
                } else {
                    confirmField.setError(null);
                }

                // Activer bouton seulement si tout est valide
                signupButton.setEnabled(allFilled && phoneValid && passwordValid);
            }
        };

        fullNameField.addTextChangedListener(validationWatcher);
        phoneField.addTextChangedListener(validationWatcher);
        passwordField.addTextChangedListener(validationWatcher);
        confirmField.addTextChangedListener(validationWatcher);

        // Bouton inscription
        signupButton.setOnClickListener(v -> {
            String fullName = fullNameField.getText().toString().trim();
            String phone = phoneField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();
            String confirm = confirmField.getText().toString().trim();

            // Vérification finale avant insertion
            if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(SignActivity.this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirm)) {
                Toast.makeText(SignActivity.this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!ValidationUtils.isValidPhone(phone)) {
                Toast.makeText(SignActivity.this, "Numéro de téléphone invalide", Toast.LENGTH_SHORT).show();
                return;
            }

            // Hachage du mot de passe
            String hashed = PasswordHash.hashPassword(password);

                try {
                    User newUser = new User();
                    newUser.full_name = fullName;
                    newUser.password_hash = hashed;
                    newUser.phone_number = phone;
                    db.userDao().insert(newUser);

                } catch (Exception e) {
                    e.printStackTrace();
                }

        });
    }
}