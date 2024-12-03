package edu.uga.cs.roommateshopping;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class PurchasesAdapter extends RecyclerView.Adapter<PurchasesAdapter.ViewHolder> {

    private List<HelperClass.ShoppingItem> purchases;
    private DatabaseReference purchasesReference;

    public PurchasesAdapter(List<HelperClass.ShoppingItem> purchases) {
        this.purchases = purchases;
        this.purchasesReference = FirebaseDatabase.getInstance().getReference("purchases");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_purchases, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = purchases.get(position);

        holder.itemName.setText(item.getItemName());
        holder.itemQuantity.setText("Qty: " + item.getQuantity());
        holder.itemPrice.setText(item.getPrice() != null ? "Price: $" + item.getPrice() : "Price: Not set");

        holder.editPrice.setOnClickListener(v -> showEditPriceDialog(holder.itemView.getContext(), item));

        holder.moveToShoppingList.setOnClickListener(v -> moveToShoppingList(item, holder.itemView.getContext()));
    }

    // Method to edit/add price to item
    private void showEditPriceDialog(Context context, HelperClass.ShoppingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Price");

        // Price input field
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter new price");
        builder.setView(input);

        // Save and cancel buttons
        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPrice = input.getText().toString();
            try {
                double price = Double.parseDouble(newPrice);
                item.setPrice(price);

                // Update only the existing item's price in Firebase
                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                purchasesReference.child(userID).child(item.getItemId()).child("price").setValue(price)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Price updated successfully!", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("PurchasesAdapter", "Failed to update price", e);
                            Toast.makeText(context, "Error updating price", Toast.LENGTH_SHORT).show();
                        });
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price entered", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Method to remove item from checkout and add back to shopping list
    private void moveToShoppingList(HelperClass.ShoppingItem item, Context context) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Remove item from checkout
        purchasesReference.child(userID).child(item.getItemId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Sucess: move item to shopping list
                    FirebaseDatabase.getInstance().getReference("shopping_list")
                            .child(item.getItemId()).setValue(item)
                            .addOnSuccessListener(aVoid1 -> {
                                purchases.remove(item);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Item moved to shopping list", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("PurchasesAdapter", "Failed to move item to shopping list", e);
                                Toast.makeText(context, "Error moving item to shopping list", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("PurchasesAdapter", "Failed to remove item from checkout", e);
                    Toast.makeText(context, "Error removing item from checkout", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return purchases.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemPrice;
        ImageView editPrice, moveToShoppingList;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            itemPrice = itemView.findViewById(R.id.item_price);
            editPrice = itemView.findViewById(R.id.edit_price);
            moveToShoppingList = itemView.findViewById(R.id.delete_icon);
        }
    }
}
