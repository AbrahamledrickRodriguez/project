package edu.uga.cs.roommateshopping;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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

public class ShoppingListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<HelperClass.ShoppingItem> shoppingList;
    private DatabaseReference shoppingListReference;
    private DatabaseReference basketReference;
    private FirebaseAuth firebaseAuth;

    public ShoppingListFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        shoppingList = new ArrayList<>();
        adapter = new ShoppingListAdapter(shoppingList);
        recyclerView.setAdapter(adapter);

        shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");
        basketReference = FirebaseDatabase.getInstance().getReference("shopping_basket");
        firebaseAuth = FirebaseAuth.getInstance();

        fetchShoppingList();

        view.findViewById(R.id.button2).setOnClickListener(v -> showAddItemDialog(view));

        view.findViewById(R.id.basketButton).setOnClickListener(v -> moveToBasket());

        return view;
    }

    private void showAddItemDialog(View view) {
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_edit_item, null);
        EditText itemNameEditText = dialogView.findViewById(R.id.dialog_item_name);
        EditText itemQuantityEditText = dialogView.findViewById(R.id.dialog_item_quantity);

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Add Item")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String itemName = itemNameEditText.getText().toString().trim();
                    String itemQuantity = itemQuantityEditText.getText().toString().trim();

                    if (!itemName.isEmpty() && !itemQuantity.isEmpty()) {
                        // Fetch the current user's UID
                        String userId = firebaseAuth.getCurrentUser().getUid();

                        HelperClass.ShoppingItem item = new HelperClass.ShoppingItem(itemName, Integer.parseInt(itemQuantity));
                        item.setUserID(userId);

                        shoppingListReference.push().setValue(item)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(view.getContext(), "Item added successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(view.getContext(), "Failed to add item", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(view.getContext(), "Please enter valid item name and quantity", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void fetchShoppingList() {
        shoppingListReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                shoppingList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                    if (item != null) {
                        item.setItemId(itemSnapshot.getKey());
                        shoppingList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load shopping list", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void moveToBasket() {
        // Navigate to the ShoppingBasketFragment
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, new ShoppingBasketFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }
}


