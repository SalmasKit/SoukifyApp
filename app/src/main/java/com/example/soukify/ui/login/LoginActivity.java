package com.example.soukify.ui.login;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.soukify.R;
import com.example.soukify.ui.sign.SignActivity;

public class LoginActivity extends AppCompatActivity {



    private Button signupbtn1;
    private Button loginbtn;
    private EditText uselog;
    private EditText passlog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);
        signupbtn1=findViewById(R.id.signupbtn);
        loginbtn=findViewById(R.id.logine);
        uselog=findViewById(R.id.user);
        passlog=findViewById(R.id.passwd);
       // Intent intent = getIntent();
       // String username= intent.getStringExtra("username");
       // String password=intent.getStringExtra("password");
        signupbtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentsign=new Intent(LoginActivity.this, SignActivity.class);
                startActivity(intentsign);
            }
        });
        /*loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(LoginActivity.this,"le boutton est clicke",Toast.LENGTH_LONG).show();
            }
        });*/

        /*loginbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(uselog.getText().toString().equals(username) && passlog.getText().toString().equals(password)){
                    Intent intenhome=new Intent(LoginActivity.this,.class);
                    startActivity(intenhome);
                    Toast.makeText(LoginActivity.this,"hello",Toast.LENGTH_LONG).show();
                }
                else{
                    Toast.makeText(LoginActivity.this,"il ya un problem",Toast.LENGTH_LONG).show();
                }
            }
        });


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });*/
    }
}