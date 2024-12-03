package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;

public class NavMainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_main);

        setupToolbarAndNavigationDrawer();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ShoppingListFragment())
                    .commit();
            NavigationView navigationView = findViewById(R.id.nav_view);
            navigationView.setCheckedItem(R.id.nav_shopping_list);
        }

        // Load user email into the navigation header
        loadUserEmail();
    }

    private void setupToolbarAndNavigationDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open_nav,
                R.string.close_nav
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void loadUserEmail() {
        NavigationView navigationView = findViewById(R.id.nav_view);
        android.view.View headerView = navigationView.getHeaderView(0);

        TextView emailTextView = headerView.findViewById(R.id.nav_email);

        // Fetch user email from Firebase Authentication
        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            Log.d("NavMainActivity", "User ID: " + uid);

            // Fetch user email from Firebase Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference("users").child(uid);

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        String email = dataSnapshot.child("email").getValue(String.class);
                        Log.d("NavMainActivity", "User email: " + email);

                        String displayText = "Logged in User: " + (email != null ? email : getString(R.string.contact_email));
                        emailTextView.setText(displayText);
                    } else {
                        Log.d("NavMainActivity", "No such user in the database");
                    }
                }

                @Override
                public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                    Log.e("NavMainActivity", "Error fetching user email: " + error.getMessage());
                }
            });
        } else {
            Log.d("NavMainActivity", "User not authenticated");
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_shopping_list) {
            loadFragment(new ShoppingListFragment());
        } else if (id == R.id.nav_shopping_basket) {
            loadFragment(new ShoppingBasketFragment());
        } else if (id == R.id.nav_purchases) {
            loadFragment(new PurchasesFragment());
        } else if (id == R.id.nav_expenses) {
            loadFragment(new ExpensesFragment());
        } else if (id == R.id.nav_roommate_expenses) {
            loadFragment(new RoommateExpensesFragment());
        } else if (id == R.id.nav_logout) {
            performLogout();
        }
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void loadFragment(androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    private void performLogout() {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

        // Redirect to the splash
        Intent intent = new Intent(NavMainActivity.this, SplashActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
