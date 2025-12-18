package com.example.soukify.ui.login;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.MainActivity;
import com.example.soukify.R;
import com.example.soukify.ui.sign.SignActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;

public class LoginActivity extends AppCompatActivity {

    private Button loginBtn;
    private EditText emailEdit, passwordEdit;
    private TextView forgotPassword, signupText;
    private ImageView googleBtn;

    private LoginActivityViewModel loginViewModel;
    private GoogleSignInClient googleClient;

    // ---------------- GOOGLE RESULT ----------------
    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                if (result.getData() == null) {
                    Toast.makeText(this, "Google result NULL", Toast.LENGTH_LONG).show();
                    return;
                }

                try {
                    GoogleSignInAccount account = GoogleSignIn
                            .getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);

                    if (account != null && account.getIdToken() != null) {
                        Toast.makeText(this, "Google Token OK", Toast.LENGTH_SHORT).show();
                        loginViewModel.signInWithGoogle(account.getIdToken());
                    } else {
                        Toast.makeText(this, "Google ID Token manquant", Toast.LENGTH_LONG).show();
                    }

                } catch (ApiException e) {
                    Toast.makeText(this,
                            "Erreur Google: " + e.getStatusCode(),
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        initViews();
        initViewModel();
        initGoogleAuth();
        setupListeners();
        setupObservers();
    }

    // ---------------- INIT VIEWS ----------------
    private void initViews() {
        loginBtn = findViewById(R.id.logine);
        emailEdit = findViewById(R.id.user);
        passwordEdit = findViewById(R.id.passwd);
        forgotPassword = findViewById(R.id.forgetpassword);
        googleBtn = findViewById(R.id.google);
        signupText = findViewById(R.id.tvSignup);

        signupText.setPaintFlags(signupText.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    // ---------------- VIEWMODEL ----------------
    private void initViewModel() {
        loginViewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);
    }

    // ---------------- GOOGLE AUTH ----------------
    private void initGoogleAuth() {
        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleClient = GoogleSignIn.getClient(this, gso);
    }

    // ---------------- LISTENERS ----------------
    private void setupListeners() {

        // LOGIN EMAIL / PASSWORD
        loginBtn.setOnClickListener(v -> {
            String email = emailEdit.getText().toString().trim();
            String password = passwordEdit.getText().toString().trim();
            loginViewModel.login(email, password);
        });

        // SIGN UP
        signupText.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignActivity.class)));

        // RESET PASSWORD (version simple)
        forgotPassword.setOnClickListener(v -> {
            String email = emailEdit.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Entrez votre email d'abord", Toast.LENGTH_SHORT).show();
                return;
            }

            forgotPassword.setEnabled(false);
            loginViewModel.resetPassword(email);
        });

        // SHOW / HIDE PASSWORD
        passwordEdit.setOnTouchListener((v, event) -> {
            final int drawableRight = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (passwordEdit.getRight()
                        - passwordEdit.getCompoundDrawables()[drawableRight].getBounds().width())) {

                    if (passwordEdit.getInputType()
                            == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

                        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                    } else {
                        passwordEdit.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }

                    passwordEdit.setSelection(passwordEdit.getText().length());
                    return true;
                }
            }
            return false;
        });

        // GOOGLE LOGIN
        googleBtn.setOnClickListener(v ->
                googleClient.signOut()
                        .addOnCompleteListener(task ->
                                googleLauncher.launch(googleClient.getSignInIntent()))
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        "Erreur Google SignOut: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show())
        );
    }

    // ---------------- OBSERVERS ----------------
    private void setupObservers() {

        loginViewModel.getLoginSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) navigateToMain();
        });

        loginViewModel.getPhoneAuthSuccess().observe(this, success -> {
            if (Boolean.TRUE.equals(success)) navigateToMain();
        });

        loginViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
                forgotPassword.setEnabled(true);
            }
        });

        loginViewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, "📨 " + msg, Toast.LENGTH_LONG).show();
                forgotPassword.setEnabled(true);
            }
        });

        loginViewModel.getPhoneAuthError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
            }
        });

        loginViewModel.getIsLoading().observe(this,
                isLoading -> forgotPassword.setEnabled(!Boolean.TRUE.equals(isLoading)));
    }

    // ---------------- NAVIGATION ----------------
    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
