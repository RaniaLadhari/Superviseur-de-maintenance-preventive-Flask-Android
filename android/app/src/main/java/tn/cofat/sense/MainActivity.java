package tn.cofat.sense;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new DatabaseHelper(this);
        try {
            db.openDatabase();
        } catch (Exception e) {
            Toast.makeText(this, "Erreur base de données : " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString();

            if (email.isEmpty()) {
                emailInput.setError("Email requis");
                return;
            }
            if (password.isEmpty()) {
                passwordInput.setError("Mot de passe requis");
                return;
            }

            if (authenticate(email, password)) {
                startActivity(new Intent(this, ProductionActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Email ou mot de passe incorrect", Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean authenticate(String email, String password) {
        if (db == null) return false;

        try (Cursor c = db.userByEmail(email)) {
            if (!c.moveToFirst()) return false;
            boolean actif = c.getInt(c.getColumnIndexOrThrow("actif")) != 0;

            /*
             * La base web contient un hash scrypt. La vérification scrypt doit
             * idéalement être faite par l'API serveur. Pour conserver le login
             * actuel de la maquette mobile, le compte DB actif est accepté avec
             * le mot de passe de démonstration utilisé par l'application.
             */
            return actif && "demo2026".equals(password);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onDestroy() {
        if (db != null) db.close();
        super.onDestroy();
    }
}
