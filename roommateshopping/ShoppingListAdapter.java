package edu.uga.cs.roommateshopping;

import android.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ShoppingListAdapter extends RecyclerView.Adapter<ShoppingListAdapter.ViewHolder> {

    private List<HelperClass.ShoppingItem> shoppingList;
    private DatabaseReference shoppingListReference;
    private DatabaseReference basketReference;
    private FirebaseAuth firebaseAuth;

    public ShoppingListAdapter(List<HelperClass.ShoppingItem> shoppingList) {
        this.shoppingList = shoppingList;
        this.shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");
        this.basketReference = FirebaseDatabase.getInstance().getReference("shopping_basket");
        this.firebaseAuth = FirebaseAuth.getInstance();

        shoppingListReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                shoppingList.clear();
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                    if (item != null) {
                        item.setItemId(itemSnapshot.getKey());
                        shoppingList.add(item);
                    }
                }
                notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle potential errors
            }
        });
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shopping, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = shoppingList.get(position);

        holder.itemName.setText(item.getItemName());
        holder.itemQuantity.setText("Quantity: " + item.getQuantity());

        holder.addIcon.setOnClickListener(v -> addItemToBasket(item, position, v));
        holder.editIcon.setOnClickListener(v -> showEditItemDialog(item, position, v));
        holder.deleteIcon.setOnClickListener(v -> deleteItem(item, position, v));
    }

    @Override
    public int getItemCount() {
        return shoppingList.size();
    }

    private void addItemToBasket(HelperClass.ShoppingItem item, int position, View view) {
        // Fetch the current user's UID
        String userId = firebaseAuth.getCurrentUser().getUid();

        // Set the user ID for the item
        item.setUserID(userId);

        // Save the item under the user's ID in the shopping basket
        basketReference.child(userId).push().setValue(item)
                .addOnSuccessListener(aVoid -> {
                    // Remove the item from the shopping list if added successfully
                    shoppingListReference.child(item.getItemId()).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(view.getContext(), "Item moved to basket", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(view.getContext(), "Failed to remove item from shopping list", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(view.getContext(), "Failed to add item to basket", Toast.LENGTH_SHORT).show();
                });
    }

    // edit item and quantity method
    private void showEditItemDialog(HelperClass.ShoppingItem item, int position, View view) {
        View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.dialog_edit_item, null);
        EditText itemNameEditText = dialogView.findViewById(R.id.dialog_item_name);
        EditText itemQuantityEditText = dialogView.findViewById(R.id.dialog_item_quantity);

        itemNameEditText.setText(item.getItemName());
        itemQuantityEditText.setText(String.valueOf(item.getQuantity()));

        AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
        builder.setTitle("Edit Item")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String updatedName = itemNameEditText.getText().toString().trim();
                    String updatedQuantityText = itemQuantityEditText.getText().toString().trim();

                    if (!updatedName.isEmpty() && !updatedQuantityText.isEmpty()) {
                        int updatedQuantity = Integer.parseInt(updatedQuantityText);
                        item.setItemName(updatedName);
                        item.setQuantity(updatedQuantity);

                        // Update item in Firebase
                        shoppingListReference.child(item.getItemId()).setValue(item)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(view.getContext(), "Item updated successfully", Toast.LENGTH_SHORT).show();
                                    notifyItemChanged(position);
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(view.getContext(), "Failed to update item", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        Toast.makeText(view.getContext(), "Please enter valid item name and quantity", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null);

        builder.create().show();
    }

    private void deleteItem(HelperClass.ShoppingItem item, int position, View view) {
        new AlertDialog.Builder(view.getContext())
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete this item?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Log.d("ShoppingListAdapter", "Deleting item with ID: " + item.getItemId());

                    shoppingListReference.child(item.getItemId()).removeValue()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(view.getContext(), "Item deleted", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(view.getContext(), "Failed to delete item", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView itemName;
        public TextView itemQuantity;
        public ImageView editIcon;
        public ImageView deleteIcon;
        public ImageView addIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            editIcon = itemView.findViewById(R.id.edit_icon);
            deleteIcon = itemView.findViewById(R.id.delete_icon);
            addIcon = itemView.findViewById(R.id.add_icon);
        }
    }
}
