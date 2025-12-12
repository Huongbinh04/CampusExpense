package com.example.campusexpense.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.campusexpense.R;
import com.example.campusexpense.data.database.AppDatabase;
import com.example.campusexpense.data.database.UserDao;
import com.example.campusexpense.data.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.security.MessageDigest;

public class RegisterActivity extends AppCompatActivity {
    private TextView loginText;
    private TextInputLayout usernameLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText usernameInput;
    private TextInputEditText passwordInput;
    private TextInputEditText confirmPasswordInput;
    private Button registerButton;
    private UserDao userDao;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameLayout = findViewById(R.id.usernameLayout);
        passwordLayout = findViewById(R.id.passwordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        usernameInput = findViewById(R.id.usernameInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.irmPasswordInput);
        registerButton = findViewById(R.id.registerButton);

        loginText = findViewById(R.id.loginText);
        loginText.setOnClickListener(v -> finish());

        AppDatabase database = AppDatabase.getInstance(this);
        userDao = database.userDao();
        registerButton.setOnClickListener(v -> register());

    }
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }

    private void register() {
        String username = usernameInput.getText().toString();
        String password = passwordInput.getText().toString();
        String confirmPassword = confirmPasswordInput.getText().toString();

        usernameLayout.setError(null);
        passwordLayout.setError(null);
        confirmPasswordLayout.setError(null);

        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.error_empty_username));
            return;
        }
        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_empty_password));
            return;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_password_mismatch));
            return;
        }
        registerButton.setEnabled(false);
        registerButton.setText("Registering...");
        new Thread(() -> {
            int count = userDao.checkUsernameExists(username);
            if (count > 0) {
                runOnUiThread(() -> {
                    registerButton.setEnabled(true);
                    registerButton.setText(R.string.register);
                    usernameLayout.setError(getString(R.string.error_username_exists));
                });
                return;
            }
            String hashedPassword = hashPassword(password);
            User user = new User(username, hashedPassword);
            long result = userDao.insert(user);
            runOnUiThread(() -> {
                registerButton.setEnabled(true);
                registerButton.setText(R.string.register);
                if (result > 0) {
                    Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.register_failed, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();

    }
}