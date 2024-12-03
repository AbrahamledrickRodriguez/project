package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ShoppingBasketFragment extends Fragment {

    private RecyclerView recyclerView;
    private ShoppingBasketAdapter adapter;
    private List<HelperClass.ShoppingItem> shoppingBasket;
    private DatabaseReference shoppingBasketReference;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_shopping_basket, container, false);

        recyclerView = rootView.findViewById(R.id.recycler_view_shopping_basket);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Button checkoutButton = rootView.findViewById(R.id.checkout_button);

        shoppingBasket = new ArrayList<>();
        adapter = new ShoppingBasketAdapter(shoppingBasket);
        recyclerView.setAdapter(adapter);

        shoppingBasketReference = FirebaseDatabase.getInstance().getReference("shopping_basket");

        fetchShoppingBasket();

        checkoutButton.setOnClickListener(v -> {
            if (!shoppingBasket.isEmpty()) {
                // Move all items to Purchases
                moveAllItemsToPurchases();
            } else {
                Toast.makeText(getContext(), "No items in the basket to checkout.", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    private void fetchShoppingBasket() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        shoppingBasketReference = FirebaseDatabase.getInstance().getReference("shopping_basket").child(userID);

        shoppingBasketReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                shoppingBasket.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                    if (item != null) {
                        item.setItemId(itemSnapshot.getKey());
                        shoppingBasket.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load shopping basket: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveAllItemsToPurchases() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference purchasesReference = FirebaseDatabase.getInstance().getReference("purchases").child(userID);

        for (HelperClass.ShoppingItem item : shoppingBasket) {
            purchasesReference.child(item.getItemId()).setValue(item);
        }

        // Clear basket after moving items to purchases
        shoppingBasketReference.setValue(null).addOnSuccessListener(aVoid -> {
            shoppingBasket.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(getContext(), "All items moved to purchases.", Toast.LENGTH_SHORT).show();

            navigateToCheckout();
        });
    }

    private void navigateToCheckout() {
        PurchasesFragment checkoutFragment = new PurchasesFragment();
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, checkoutFragment)
                .addToBackStack(null)
                .commit();
    }
}
