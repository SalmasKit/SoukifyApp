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
import androidx.lifecycle.ViewModelProvider;
import com.example.soukify.MainActivity;

import com.example.soukify.R;
import com.example.soukify.data.repositories.SessionRepository;
import com.example.soukify.utils.ValidationUtils;

public class SignActivity extends AppCompatActivity {
    private static final String TAG = "SignActivity";
    private SignActivityViewModel viewModel;
    private EditText fullNameField, phoneField, emailField, passwordField, confirmField;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(SignActivityViewModel.class);

        // Initialize views
        fullNameField = findViewById(R.id.username);
        passwordField = findViewById(R.id.password);
        confirmField = findViewById(R.id.confirm);
        phoneField = findViewById(R.id.phone);
        emailField = findViewById(R.id.email);
        signupButton = findViewById(R.id.signbtn);
        
        // Debug: Log field assignments
        Log.d(TAG, "Fields initialized - username: " + (fullNameField != null) + 
                   ", password: " + (passwordField != null) + 
                   ", confirm: " + (confirmField != null) + 
                   ", phone: " + (phoneField != null) + 
                   ", email: " + (emailField != null));

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
        emailField.addTextChangedListener(validationWatcher);
        passwordField.addTextChangedListener(validationWatcher);
        confirmField.addTextChangedListener(validationWatcher);

        // Signup button click listener
        signupButton.setOnClickListener(v -> attemptSignup());

        // Observe ViewModel for signup results
        viewModel.getSignupSuccess().observe(this, success -> {
            if (success != null && success) {
                Toast.makeText(this, getString(R.string.signup_successful), Toast.LENGTH_SHORT).show();
                Log.i(TAG, "Signup successful");
                // Redirect to MainActivity
                Intent intent = new Intent(SignActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish(); // Close the signup activity
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (error != null) {
                Toast.makeText(this, getString(R.string.error_prefix, error), Toast.LENGTH_LONG).show();
                Log.e(TAG, "Signup error: " + error);
            }
        });
    }

    private void validateForm() {
        String fullName = fullNameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirm = confirmField.getText().toString().trim();

        // Debug: Log field values
        Log.d(TAG, "validateForm - fullName: '" + fullName + "', phone: '" + phone + 
                   "', email: '" + email + "', password: '" + password + "', confirm: '" + confirm + "'");

        boolean phoneValid = ValidationUtils.isValidPhone(phone) || phone.isEmpty();
        boolean emailValid = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() || email.isEmpty();
        boolean passwordValid = (password.equals(confirm) || confirm.isEmpty()) && 
                              (password.length() >= 6 || password.isEmpty());
        boolean allFilled = !fullName.isEmpty() && !phone.isEmpty() && !email.isEmpty() &&
                           !password.isEmpty() && !confirm.isEmpty();

        // Real-time error messages
        if (!phone.isEmpty() && !ValidationUtils.isValidPhone(phone)) {
            phoneField.setError("Numéro invalide (06 ou 07 + 8 chiffres)");
        } else {
            phoneField.setError(null);
        }

        if (!email.isEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError(getString(R.string.invalid_email));
        } else {
            emailField.setError(null);
        }

        if (!password.isEmpty() && password.length() < 6) {
            passwordField.setError("Le mot de passe doit contenir au moins 6 caractères");
        } else if (!password.equals(confirm) && !confirm.isEmpty()) {
            confirmField.setError(getString(R.string.passwords_do_not_match_fr));
        } else {
            passwordField.setError(null);
            confirmField.setError(null);
        }

        // Enable button only if all fields are valid
        signupButton.setEnabled(allFilled && 
                              ValidationUtils.isValidPhone(phone) && 
                              android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                              password.length() >= 6 && 
                              password.equals(confirm));
    }

    private void attemptSignup() {
        Log.d(TAG, "Attempting to sign up user");
        
        String fullName = fullNameField.getText().toString().trim();
        String phone = phoneField.getText().toString().trim();
        String email = emailField.getText().toString().trim();
        String password = passwordField.getText().toString().trim();
        String confirm = confirmField.getText().toString().trim();

        Log.d(TAG, String.format("Signup attempt - Name: %s, Phone: %s, Email: %s", fullName, phone, email));

        // Final validation
        if (!validateInputs(fullName, phone, email, password, confirm)) {
            return;
        }

        // Use Firebase ViewModel for signup
        viewModel.signup(fullName, email, password, phone);
    }

    private boolean validateInputs(String fullName, String phone, String email, String password, String confirm) {
        if (fullName.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            Toast.makeText(this, getString(R.string.all_fields_obligatory), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!ValidationUtils.isValidPhone(phone)) {
            phoneField.setError("Numéro de téléphone invalide");
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailField.setError("Email invalide");
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
