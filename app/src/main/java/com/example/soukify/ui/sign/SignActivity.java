package com.example.soukify.ui.sign;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import com.example.soukify.MainActivity;

import com.example.soukify.R;
import com.example.soukify.data.AppDatabase;
import com.example.soukify.data.entities.User;
import com.example.soukify.utils.PasswordHash;
import com.example.soukify.utils.ValidationUtils;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SignActivity extends AppCompatActivity {
    private static final String TAG = "SignActivity";
    private AppDatabase db;
    private EditText fullNameField, phoneField, passwordField, confirmField;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        // Initialize views
        fullNameField = findViewById(R.id.username);
        phoneField = findViewById(R.id.phone);
        passwordField = findViewById(R.id.password);
        confirmField = findViewById(R.id.confirm);
        signupButton = findViewById(R.id.signbtn);

        // Initialize database
        db = AppDatabase.getInstance(this);
        if (db == null) {
            Log.e(TAG, "Failed to initialize database");
            Toast.makeText(this, "Database initialization failed", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        Log.d(TAG, "Database initialized successfully");

        // Real-time validation
        TextWatcher validationWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            
            @Override
            public void afterTextChanged(Editable s) {
                validateForm();
            }
        };

        fullNameField.addTextChangedListener(validationWatcher);
        phoneField.addTextChangedListener(validationWatcher);
        passwordField.addTextChangedListener(validationWatcher);
        confirmField.addTextChangedListener(validationWatcher);

        // Signup button click listener
        signupButton.setOnClickListener(v -> attemptSignup());
    }

    private void validateForm() {
        String fullName = fullNameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirm = confirmField.getText().toString().trim();

        boolean phoneValid = ValidationUtils.isValidPhone(phone) || phone.isEmpty();
        boolean passwordValid = (password.equals(confirm) || confirm.isEmpty()) && 
                              (password.length() >= 6 || password.isEmpty());
        boolean allFilled = !fullName.isEmpty() && !phone.isEmpty() && 
                           !password.isEmpty() && !confirm.isEmpty();

        // Real-time error messages
        if (!phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
            phoneField.setError("Numéro invalide (06 ou 07 + 8 chiffres)");
        } else {
            phoneField.setError(null);
        }

        if (!password.isEmpty() && password.length() < 6) {
            passwordField.setError("Le mot de passe doit contenir au moins 6 caractères");
        } else if (!password.equals(confirm) && !confirm.isEmpty()) {
            confirmField.setError("Les mots de passe ne correspondent pas");
        } else {
            passwordField.setError(null);
            confirmField.setError(null);
        }

        // Enable button only if all fields are valid
        signupButton.setEnabled(allFilled && 
                              ValidationUtils.isValidPhone(phone) && 
                              password.length() >= 6 && 
                              password.equals(confirm));
    }

    private void attemptSignup() {
    Log.d(TAG, "Attempting to sign up user");
    
    String fullName = fullNameField.getText().toString().trim();
    String phone = phoneField.getText().toString().trim();
    String password = passwordField.getText().toString().trim();
    String confirm = confirmField.getText().toString().trim();

    Log.d(TAG, String.format("Signup attempt - Name: %s, Phone: %s", fullName, phone));

    // Final validation
    if (!validateInputs(fullName, phone, password, confirm)) {
        return;
    }

    // Hash the password
    String hashed = PasswordHash.hashPassword(password);
    Log.d(TAG, "Password hashed successfully");

    // Run database operation on background thread
    new Thread(() -> {
        try {
            // Create new user with formatted timestamp
            User newUser = new User();
            newUser.full_name = fullName;
            newUser.password_hash = hashed;
            newUser.phone_number = phone;
            
            // Format timestamp to readable format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            String currentDateTime = sdf.format(new Date());
            newUser.created_at = currentDateTime;

            Log.d(TAG, "Creating user with created_at: " + currentDateTime);
            
            long userId = db.userDao().insert(newUser);
            Log.d(TAG, "User inserted with ID: " + userId);

            // Verify the user was actually inserted
            User verifyUser = db.userDao().getUserById((int)userId);
            if (verifyUser != null) {
                Log.d(TAG, "User verification successful: " + verifyUser.full_name);
            } else {
                Log.e(TAG, "Failed to verify user after insertion");
            }

            runOnUiThread(() -> {
                if (userId > 0) {
                    Toast.makeText(this, "Inscription réussie !", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Signup successful for user: " + fullName);
                     // Redirect to MainActivity
        Intent intent = new Intent(SignActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish(); // Close the signup activity
                } else {
                    Log.e(TAG, "Signup failed - User ID not returned");
                    Toast.makeText(this, "Erreur lors de l'inscription", Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error during signup", e);
            runOnUiThread(() -> 
                Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        }
    }).start();
}

    private boolean validateInputs(String fullName, String phone, String password, String confirm) {
        if (fullName.isEmpty() || phone.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont obligatoires", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            phoneField.setError("Numéro de téléphone invalide");
            return false;
        }

        if (password.length() < 6) {
            passwordField.setError("Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }

        if (!password.equals(confirm)) {
            confirmField.setError("Les mots de passe ne correspondent pas");
            return false;
        }

        return true;
    }
}