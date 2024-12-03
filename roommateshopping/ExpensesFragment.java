package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExpensesAdapter adapter;
    private List<HelperClass.ShoppingItem> purchases;
    private DatabaseReference expensesReference;

    private String currentDate;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.group_expenses, container, false);

        recyclerView = rootView.findViewById(R.id.expenses_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        purchases = new ArrayList<>();
        adapter = new ExpensesAdapter(purchases);
        recyclerView.setAdapter(adapter);

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        currentDate = dateFormat.format(new java.util.Date());

        // Firebase reference to expenses
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses");

        fetchExpenses();

        return rootView;
    }

    private void fetchExpenses() {
        expensesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                purchases.clear(); // Clear the list before rebuilding it

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    String groupId = groupSnapshot.getKey();

                    for (DataSnapshot dateSnapshot : groupSnapshot.getChildren()) {
                        String date = dateSnapshot.getKey();

                        // Add the group header (only once per group)
                        HelperClass.ShoppingItem groupHeader = new HelperClass.ShoppingItem();
                        groupHeader.setItemName("Group: " + groupId);
                        groupHeader.setGroupId(groupId);
                        groupHeader.setDate(date);
                        groupHeader.setPrice(null); // Group headers don't have prices
                        purchases.add(groupHeader);

                        for (DataSnapshot userSnapshot : dateSnapshot.getChildren()) {
                            for (DataSnapshot itemSnapshot : userSnapshot.child("items").getChildren()) {
                                HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                                if (item != null) {
                                    item.setGroupId(groupId); // Ensure groupId is set
                                    item.setDate(date);       // Ensure date is set
                                    purchases.add(item);      // Add the item to the list
                                }
                            }
                        }
                    }
                }

                adapter.notifyDataSetChanged(); // Refresh the UI after rebuilding the list
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load expenses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateGroupTotalPrice(String purchaseGroupId, String dateTime, double total) {
        // Reference the specific group in Firebase
        DatabaseReference groupReference = FirebaseDatabase.getInstance().getReference("expenses")
                .child(purchaseGroupId)
                .child(dateTime);

        // Update the total price in Firebase
        groupReference.child("totalPrice").setValue(total)
                .addOnSuccessListener(aVoid -> {
                    // Successfully updated in Firebase
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update group total price in Firebase.", Toast.LENGTH_SHORT).show();
                });
    }
}
