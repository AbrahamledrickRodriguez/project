package edu.uga.cs.roommateshopping;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoommateExpensesFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoommateExpensesAdapter adapter;
    private List<HelperClass.ShoppingItem> shoppingItemList = new ArrayList<>();
    private Map<String, Double> roommateSpendingMap = new HashMap<>();
    private double totalSpent = 0;
    private double averageSpent = 0;

    private DatabaseReference expensesReference;
    private TextView totalSpentTextView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_roommate_expenses, container, false);

        recyclerView = rootView.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        totalSpentTextView = rootView.findViewById(R.id.total_spent);

        adapter = new RoommateExpensesAdapter(shoppingItemList, roommateSpendingMap, totalSpent, averageSpent);
        recyclerView.setAdapter(adapter);

        // Firebase reference
        expensesReference = FirebaseDatabase.getInstance().getReference("expenses");

        fetchUser();

        return rootView;
    }

    // method to display the roommate (user email) and their spending
    // TODO: FIX SO SPENDING AMOUNTS IS CALCULATED CORRECTLY AND DISPLAYED PROPERLY IN THE DB AND IN APP
    private void fetchUser() {
        expensesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shoppingItemList.clear();
                roommateSpendingMap.clear();
                totalSpent = 0;

                for (DataSnapshot groupSnapshot : snapshot.getChildren()) {
                    String groupId = groupSnapshot.getKey();

                    for (DataSnapshot dateSnapshot : groupSnapshot.getChildren()) {
                        String date = dateSnapshot.getKey();

                        for (DataSnapshot userSnapshot : dateSnapshot.getChildren()) {
                            String userEmailWithComma = userSnapshot.getKey();
                            String userEmail = userEmailWithComma.replace(",", ".");

                            HelperClass.ShoppingItem groupHeader = new HelperClass.ShoppingItem();
                            groupHeader.setItemName("Roommate: " + userEmail);
                            groupHeader.setDate(date);
                            shoppingItemList.add(groupHeader);

                            double userTotalSpent = 0;

                            for (DataSnapshot itemSnapshot : userSnapshot.child("items").getChildren()) {
                                HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                                if (item != null) {
                                    shoppingItemList.add(item);
                                    if (item.getPrice() != null && item.getQuantity() != null) {
                                        double itemTotal = item.getPrice() * item.getQuantity();
                                        userTotalSpent += itemTotal;
                                        Log.d("RoommateExpenses", "Item: " + item.getItemName() + ", Price: " + item.getPrice() + ", Quantity: " + item.getQuantity() + ", Total: " + itemTotal);
                                    }
                                }
                            }

                            roommateSpendingMap.put(userEmail, userTotalSpent);
                            totalSpent += userTotalSpent;
                            Log.d("RoommateExpenses", "User: " + userEmail + ", Total Spent: " + userTotalSpent);
                        }
                    }
                }

                calculateAverageSpent();

                if (totalSpentTextView != null) {
                    totalSpentTextView.setText("Total Spending: $" + String.format("%.2f", totalSpent));
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load expenses: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateAverageSpent() {
        int numRoommates = roommateSpendingMap.size();
        if (numRoommates > 0) {
            averageSpent = totalSpent / numRoommates;
        } else {
            averageSpent = 0;
        }
    }
}