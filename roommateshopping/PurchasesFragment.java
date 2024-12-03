package edu.uga.cs.roommateshopping;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PurchasesFragment extends Fragment {

    private RecyclerView recyclerView;
    private PurchasesAdapter adapter;
    private List<HelperClass.ShoppingItem> purchasedItems;
    private DatabaseReference purchasesReference;

    private TextView subtotalAmountTextView;
    private TextView taxAmountTextView;
    private TextView totalAmountTextView;
    private Button purchaseButton;

    private static final double TAX_RATE = 0.08;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_purchase, container, false);

        recyclerView = rootView.findViewById(R.id.recycler_view_checkout);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        purchasedItems = new ArrayList<>();
        adapter = new PurchasesAdapter(purchasedItems);
        recyclerView.setAdapter(adapter);

        // Firebase reference
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases");

        subtotalAmountTextView = rootView.findViewById(R.id.subtotal_amount);
        taxAmountTextView = rootView.findViewById(R.id.tax_amount);
        totalAmountTextView = rootView.findViewById(R.id.total_amount);
        purchaseButton = rootView.findViewById(R.id.purchase_button);

        // Fetch purchased items
        fetchPurchasedItems();

        purchaseButton.setOnClickListener(v -> {
            if (!purchasedItems.isEmpty()) {
                moveItemsToExpenses();

                // Replace the current fragment with ExpensesFragment
                FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, new ExpensesFragment());
                transaction.addToBackStack(null); // Optionally add to back stack
                transaction.commit();
            } else {
                Toast.makeText(getContext(), "No items to purchase.", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchPurchasedItems();
    }

    private void fetchPurchasedItems() {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases").child(userID);

        purchasesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                purchasedItems.clear();
                double subtotal = 0.0;

                // Iterate through purchased items and calculate subtotal
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                    if (item != null) {
                        item.setItemId(itemSnapshot.getKey());
                        purchasedItems.add(item);

                        // Add item's price to subtotal
                        subtotal += item.getPrice();
                    }
                }

                // Calculate tax and total
                double tax = subtotal * TAX_RATE;
                double total = subtotal + tax;

                updateTotalUI(subtotal, tax, total);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load purchased items: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveItemsToExpenses() {
        if (purchasedItems.isEmpty()) {
            Toast.makeText(getContext(), "No items to move to expenses.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in. Please log in.", Toast.LENGTH_SHORT).show();
            return;
        }

        String purchaseGroupId = "group_" + System.currentTimeMillis();
        String dateTime = java.text.DateFormat.getDateTimeInstance().format(new java.util.Date());

        // Use user's email instead of userID
        String userEmail = currentUser.getEmail();

        if (userEmail == null) {
            Toast.makeText(getContext(), "Email not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Reference to expenses: groupId -> dateTime -> userEmail
        DatabaseReference expensesReference = FirebaseDatabase.getInstance().getReference("expenses")
                .child(purchaseGroupId)
                .child(dateTime)
                .child(userEmail.replace(".", ",")); // replace "," with "."

        expensesReference.child("dateTime").setValue(dateTime);

        expensesReference.child("email").setValue(userEmail);

        for (HelperClass.ShoppingItem item : purchasedItems) {
            if (item != null) {
                expensesReference.child("items").child(item.getItemId()).setValue(item);
            }
        }

        final double totalAmountSpent = purchasedItems.stream().mapToDouble(HelperClass.ShoppingItem::getPrice).sum();

        // Reference to roommate_expenses: roommate_expenses -> email -> totalAmountSpent
        DatabaseReference roommateExpensesReference = FirebaseDatabase.getInstance().getReference("roommate_expenses")
                .child(userEmail.replace(".", ","));

        roommateExpensesReference.child("totalSpent").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double currentTotal = snapshot.exists() ? snapshot.getValue(Double.class) : 0.0;
                double updatedTotal = currentTotal + totalAmountSpent;

                // Update total amount spent in the roommate_expenses node
                roommateExpensesReference.child("totalSpent").setValue(updatedTotal)
                        .addOnSuccessListener(aVoid -> {
                            purchasesReference.setValue(null).addOnSuccessListener(aVoid1 -> {
                                purchasedItems.clear();
                                adapter.notifyDataSetChanged();
                                updateTotalUI(0, 0, 0);

                                Toast.makeText(getContext(), "Items moved to expenses and roommate expenses updated.", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Failed to update roommate expenses: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to fetch current total spent: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void updateTotalUI(double subtotal, double tax, double total) {
        String subtotalFormatted = String.format("$%.2f", subtotal);
        String taxFormatted = String.format("$%.2f", tax);
        String totalFormatted = String.format("$%.2f", total);

        subtotalAmountTextView.setText(subtotalFormatted);
        taxAmountTextView.setText(taxFormatted);
        totalAmountTextView.setText(totalFormatted);
    }
}
