package edu.uga.cs.roommateshopping;

import android.app.AlertDialog;
import android.content.Context;
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

public class ShoppingBasketAdapter extends RecyclerView.Adapter<ShoppingBasketAdapter.ViewHolder> {

    private List<HelperClass.ShoppingItem> shoppingBasket;
    private DatabaseReference basketReference;
    private DatabaseReference shoppingListReference;

    public ShoppingBasketAdapter(List<HelperClass.ShoppingItem> shoppingBasket) {
        this.shoppingBasket = shoppingBasket;
        this.basketReference = FirebaseDatabase.getInstance().getReference("shopping_basket");
        this.shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_basket, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = shoppingBasket.get(position);

        holder.itemName.setText(item.getItemName());
        holder.itemQuantity.setText("Quantity: " + item.getQuantity());
        holder.itemPrice.setText(item.getPrice() != null ? "Price: $" + item.getPrice() : "Price: Not set");

        holder.editPrice.setOnClickListener(v -> showEditPriceDialog(holder.itemView.getContext(), item));

        holder.deleteItem.setOnClickListener(v -> moveItemBackToShoppingList(item, holder.itemView.getContext()));
    }

    // Edit price method
    private void showEditPriceDialog(Context context, HelperClass.ShoppingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Edit Price");

        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setHint("Enter new price");
        builder.setView(input);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newPrice = input.getText().toString();
            try {
                double price = Double.parseDouble(newPrice);
                item.setPrice(price);

                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                basketReference.child(userID).child(item.getItemId()).child("price").setValue(price)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Price updated successfully!", Toast.LENGTH_SHORT).show();
                            notifyDataSetChanged();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("ShoppingBasketAdapter", "Failed to update price", e);
                            Toast.makeText(context, "Error updating price", Toast.LENGTH_SHORT).show();
                        });
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price entered", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Remove item from basket; move back to shopping list or checkout/purchases
    private void moveItemBackToShoppingList(HelperClass.ShoppingItem item, Context context) {
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        // Remove item from shopping basket
        basketReference.child(userID).child(item.getItemId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // On successful removal, move item to shopping list or purchases
                    FirebaseDatabase.getInstance().getReference("shopping_list")
                            .child(item.getItemId()).setValue(item)
                            .addOnSuccessListener(aVoid1 -> {
                                shoppingBasket.remove(item);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Item moved to shopping list", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ShoppingBasketAdapter", "Failed to move item to shopping list", e);
                                Toast.makeText(context, "Error moving item to shopping list", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ShoppingBasketAdapter", "Failed to remove item from basket", e);
                    Toast.makeText(context, "Error removing item from basket", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return shoppingBasket.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemPrice;
        ImageView editPrice, deleteItem;

        public ViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            itemPrice = itemView.findViewById(R.id.item_price);
            editPrice = itemView.findViewById(R.id.edit_price);
            deleteItem = itemView.findViewById(R.id.delete_icon);
        }
    }
}
