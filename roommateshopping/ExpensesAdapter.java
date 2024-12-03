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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class ExpensesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final List<HelperClass.ShoppingItem> purchases;
    private final DatabaseReference expensesReference;

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    private static final int TYPE_TOTALS = 2;

    public ExpensesAdapter(List<HelperClass.ShoppingItem> purchases) {
        this.purchases = purchases;
        this.expensesReference = FirebaseDatabase.getInstance().getReference("expenses");
    }

    @Override
    public int getItemViewType(int position) {
        HelperClass.ShoppingItem item = purchases.get(position);
        if (item.getItemName() != null && item.getItemName().startsWith("Group: ")) {
            return TYPE_GROUP_HEADER;
        }
        return TYPE_ITEM;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_GROUP_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_group_header, parent, false);
            return new GroupHeaderViewHolder(view);
        } else if (viewType == TYPE_TOTALS) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.group_totals, parent, false);
            return new TotalsViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_expenses, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = purchases.get(position);

        if (holder instanceof GroupHeaderViewHolder) {
            GroupHeaderViewHolder groupHolder = (GroupHeaderViewHolder) holder;
            groupHolder.groupName.setText(item.getItemName());
        } else if (holder instanceof ItemViewHolder) {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            itemHolder.itemName.setText(item.getItemName());
            itemHolder.itemQuantity.setText("Qty: " + (item.getQuantity() != null ? item.getQuantity() : 0));
            itemHolder.itemPrice.setText("Price: $" + (item.getPrice() != null ? item.getPrice() : "Not set"));

            itemHolder.editPrice.setOnClickListener(v -> {
                String dateTime = item.getDate();
                showEditPriceDialog(holder.itemView.getContext(), item, dateTime);
            });
        }
    }



    @Override
    public int getItemCount() {
        return purchases.size();
    }

    public static class GroupHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView groupName, dateLabel;

        public GroupHeaderViewHolder(View itemView) {
            super(itemView);
            groupName = itemView.findViewById(R.id.group_header_name);
            dateLabel = itemView.findViewById(R.id.date_label);
        }
    }

    private void showEditPriceDialog(Context context, HelperClass.ShoppingItem item, String dateTime) {
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

                String groupId = item.getGroupId();
                DatabaseReference itemReference = FirebaseDatabase.getInstance().getReference("expenses")
                        .child(groupId)
                        .child(dateTime)
                        .child("items")
                        .child(item.getItemId());

                itemReference.child("price").setValue(price)
                        .addOnSuccessListener(aVoid -> {
                            // Recalculate and refresh only the affected group
                            recalculateAndUpdateGroup(groupId, dateTime, context);
                            Toast.makeText(context, "Price updated successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to update price.", Toast.LENGTH_SHORT).show());
            } catch (NumberFormatException e) {
                Toast.makeText(context, "Invalid price entered.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }



    private void recalculateAndUpdateGroup(String groupId, String dateTime, Context context) {
        DatabaseReference groupReference = expensesReference
                .child(groupId)
                .child(dateTime)
                .child("items");

        groupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double subtotal = 0.0;

                // Recalculate totals based on updated item prices
                for (DataSnapshot itemSnapshot : snapshot.getChildren()) {
                    HelperClass.ShoppingItem item = itemSnapshot.getValue(HelperClass.ShoppingItem.class);
                    if (item != null) {
                        double itemPrice = item.getPrice() != null ? item.getPrice() : 0.0;
                        subtotal += itemPrice;
                    }
                }

                double taxes = subtotal * 0.08;
                double total = subtotal + taxes;

                // Update the totals in Firebase
                DatabaseReference totalsReference = expensesReference.child(groupId).child(dateTime);
                totalsReference.child("subtotal").setValue(subtotal);
                totalsReference.child("taxes").setValue(taxes);
                totalsReference.child("total").setValue(total)
                        .addOnSuccessListener(aVoid -> {
                            // Update the UI
                            for (int i = 0; i < purchases.size(); i++) {
                                HelperClass.ShoppingItem purchase = purchases.get(i);

                                // Refresh the group header totals
                                if (purchase.getGroupId().equals(groupId) && purchase.getDate().equals(dateTime) && purchase.getItemName().startsWith("Group:")) {
                                    notifyItemChanged(i); // Refresh only the group header
                                    break;
                                }
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(context, "Failed to update totals.", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error recalculating totals: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }





    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView itemName, itemQuantity, itemPrice;
        ImageView editPrice;

        public ItemViewHolder(View itemView) {
            super(itemView);
            itemName = itemView.findViewById(R.id.item_name);
            itemQuantity = itemView.findViewById(R.id.item_quantity);
            itemPrice = itemView.findViewById(R.id.item_price);
            editPrice = itemView.findViewById(R.id.edit_price);
        }
    }

    public static class TotalsViewHolder extends RecyclerView.ViewHolder {
        TextView totalsText;

        public TotalsViewHolder(View itemView) {
            super(itemView);
            totalsText = itemView.findViewById(R.id.subtotal_value);
        }
    }
}
