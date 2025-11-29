package com.example.soukify.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.soukify.R;
import com.example.soukify.MainActivity;
import com.example.soukify.ui.sign.SignActivity;

public class LoginActivity extends AppCompatActivity {

    private Button signupbtn1;
    private Button loginbtn;
    private EditText uselog;
    private EditText passlog;
    private LoginActivityViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        
        // Initialize ViewModel
        loginViewModel = new ViewModelProvider(this).get(LoginActivityViewModel.class);
        
        signupbtn1=findViewById(R.id.signupbtn);
        loginbtn=findViewById(R.id.logine);
        uselog=findViewById(R.id.user);
        passlog=findViewById(R.id.passwd);
        
        signupbtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentsign=new Intent(LoginActivity.this, SignActivity.class);
                startActivity(intentsign);
            }
        });
        
        loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = uselog.getText().toString().trim();
                String password = passlog.getText().toString().trim();
                
                Log.d("LoginActivity", "Login attempt with email: " + email);
                
                if (email.isEmpty() || password.isEmpty()) {
                    Log.d("LoginActivity", "Login failed: empty email or password");
                    Toast.makeText(LoginActivity.this, getString(R.string.please_enter_email_and_password), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Basic email validation
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Log.d("LoginActivity", "Login failed: invalid email format");
                    Toast.makeText(LoginActivity.this, getString(R.string.please_enter_valid_email), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Attempt login using ViewModel
                Log.d("LoginActivity", "Calling loginViewModel.login()");
                loginViewModel.login(email, password);
            }
        });
        
        // Observe login result
        loginViewModel.getLoginSuccess().observe(this, isLoggedIn -> {
            Log.d("LoginActivity", "Login success observed: " + isLoggedIn);
            if (isLoggedIn) {
                Log.d("LoginActivity", "Login successful, navigating to MainActivity");
                Toast.makeText(LoginActivity.this, getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                Intent intenhome = new Intent(LoginActivity.this, MainActivity.class);
                intenhome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intenhome);
                finish();
            }
        });
        
        // Observe error messages
        loginViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Log.e("LoginActivity", "Login error: " + errorMessage);
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}