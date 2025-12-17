package com.example.soukify.ui.login;

import android.app.Activity;
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

    private Button loginbtn;
    private EditText uselog, passlog;
    private TextView logpasswrod, tvSignup;
    private ImageView googleimg;


    private LoginActivityViewModel loginViewModel;
    private GoogleSignInClient googleClient;

    // ---------------- GOOGLE SIGN IN ---------------- //
    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {

                Toast.makeText(this, "GoogleLauncher → déclenché", Toast.LENGTH_SHORT).show();

                if (result.getData() == null) {
                    Toast.makeText(this, "GoogleLauncher → result.getData() = NULL ❌", Toast.LENGTH_LONG).show();
                    return;
                }

                Toast.makeText(this, "GoogleLauncher → Intent reçu ✔️", Toast.LENGTH_SHORT).show();

                try {
                    GoogleSignInAccount account = GoogleSignIn
                            .getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);

                    if (account != null) {
                        Toast.makeText(this, "Google Account récupéré ✔️", Toast.LENGTH_SHORT).show();

                        if (account.getIdToken() != null) {
                            Toast.makeText(this, "ID TOKEN OK ✔️", Toast.LENGTH_SHORT).show();
                            loginViewModel.signInWithGoogle(account.getIdToken());
                        } else {
                            Toast.makeText(this, "ID TOKEN NULL (problème clientID)", Toast.LENGTH_LONG).show();
                        }

                    } else {
                        Toast.makeText(this, "Account NULL", Toast.LENGTH_LONG).show();
                    }

                } catch (ApiException e) {
                    Toast.makeText(this,
                            "ApiException Google: " + e.getStatusCode(),
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        Toast.makeText(this, "LoginActivity → onCreate()", Toast.LENGTH_SHORT).show();

        initViews();
        initViewModel();
        initGoogleAuth();
        setupListeners();
        setupObservers();

        logpasswrod.setOnClickListener(v -> {
            String email = uselog.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Entrez votre email d'abord", Toast.LENGTH_SHORT).show();
                return;
            }

            logpasswrod.setEnabled(false); // Désactive le bouton
            loginViewModel.resetPassword(email);
        });
        loginViewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                logpasswrod.setEnabled(true);
            }
        });
        loginViewModel.getErrorMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                logpasswrod.setEnabled(true);
            }
        });

    }

    // ---------------- INITIALISATION DES VUES ---------------- //
    private void initViews() {
        loginbtn = findViewById(R.id.logine);
        uselog = findViewById(R.id.user);
        passlog = findViewById(R.id.passwd);
        logpasswrod = findViewById(R.id.forgetpassword);
        googleimg = findViewById(R.id.google);
        tvSignup = findViewById(R.id.tvSignup);

        tvSignup.setPaintFlags(tvSignup.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
    }

    // ---------------- VIEWMODEL ---------------- //
    private void initViewModel() {


        loginViewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);
    }

    // ---------------- GOOGLE CONFIG ---------------- //
    private void initGoogleAuth() {
        Toast.makeText(this, "Init Google Auth...", Toast.LENGTH_SHORT).show();

        GoogleSignInOptions gso =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();

        googleClient = GoogleSignIn.getClient(this, gso);

        Toast.makeText(this, "Google Auth Configurée ✔️", Toast.LENGTH_SHORT).show();
    }

    // ---------------- CLICK LISTENERS ---------------- //
    private void setupListeners() {

        // Login normal
        loginbtn.setOnClickListener(v -> {

            String email = uselog.getText().toString().trim();
            String password = passlog.getText().toString().trim();
            loginViewModel.login(email, password);
        });

        // Aller vers signup
        tvSignup.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignActivity.class));
        });

        // Reset password
        logpasswrod.setOnClickListener(v -> {

            String email = uselog.getText().toString().trim();

            if (email.isEmpty()) {
                Toast.makeText(this, "Entrez votre email d'abord", Toast.LENGTH_SHORT).show();
                return;
            }
            loginViewModel.resetPassword(email);
        });

        // Show/hide password
        passlog.setOnTouchListener((v, event) -> {
            final int drawableRight = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getRawX() >= (passlog.getRight()
                        - passlog.getCompoundDrawables()[drawableRight].getBounds().width())) {

                    Toast.makeText(this, "Changement affichage mot de passe", Toast.LENGTH_SHORT).show();

                    if (passlog.getInputType()
                            == (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)) {

                        passlog.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);

                    } else {
                        passlog.setInputType(InputType.TYPE_CLASS_TEXT |
                                InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    }

                    passlog.setSelection(passlog.getText().length());
                    return true;
                }
            }
            return false;
        });

        // Google login
        googleimg.setOnClickListener(v -> {
            Toast.makeText(this, "Bouton Google cliqué ✔️", Toast.LENGTH_SHORT).show();

            googleClient.signOut()
                    .addOnCompleteListener(task -> {
                        Toast.makeText(this, "Google SignOut OK, lancement Intent...", Toast.LENGTH_SHORT).show();
                        googleLauncher.launch(googleClient.getSignInIntent());

                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Erreur SignOut : " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );

        });
    }

    // ---------------- OBSERVERS ---------------- //
    private void setupObservers() {
        loginViewModel.getLoginSuccess().observe(this, success -> {
            Toast.makeText(this, "Observer loginSuccess = " + success, Toast.LENGTH_SHORT).show();
            if (Boolean.TRUE.equals(success)) navigateToMain();
        });

        loginViewModel.getPhoneAuthSuccess().observe(this, success -> {
            Toast.makeText(this, "Observer phoneAuthSuccess = " + success, Toast.LENGTH_SHORT).show();
            if (Boolean.TRUE.equals(success)) navigateToMain();
        });

        loginViewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty())
                Toast.makeText(this, "Observer ErrorMessage: " + message, Toast.LENGTH_LONG).show();
        });

        loginViewModel.getPhoneAuthError().observe(this, error -> {
            if (error != null && !error.isEmpty())
                Toast.makeText(this, "Phone Auth Error: " + error, Toast.LENGTH_LONG).show();
        });

        loginViewModel.getSuccessMessage().observe(this, msg -> {
            if (msg != null && !msg.isEmpty())
                Toast.makeText(this, "Message: " + msg, Toast.LENGTH_LONG).show();
        });
    }

    // ---------------- NAVIGATION ---------------- //
    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}