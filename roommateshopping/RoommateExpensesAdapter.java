package edu.uga.cs.roommateshopping;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Map;

// TODO: FIX CALCULATIONS; FIX SO TOTALS ARE UPDATED IN DATABASE AND IN APP
// XMLS: item_roommate_expenses, fragment_roommate_expenses

public class RoommateExpensesAdapter extends RecyclerView.Adapter<RoommateExpensesAdapter.ViewHolder> {

    private List<HelperClass.ShoppingItem> shoppingItemList;
    private Map<String, Double> roommateSpendingMap;
    private double totalSpent;
    private double averageSpent;

    public RoommateExpensesAdapter(List<HelperClass.ShoppingItem> shoppingItemList, Map<String, Double> roommateSpendingMap, double totalSpent, double averageSpent) {
        this.shoppingItemList = shoppingItemList;
        this.roommateSpendingMap = roommateSpendingMap;
        this.totalSpent = totalSpent;
        this.averageSpent = averageSpent;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_roommate_expenses, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        HelperClass.ShoppingItem item = shoppingItemList.get(position);

        String roommate = item.getItemName();

        Double spent = roommateSpendingMap.get(roommate);
        if (spent == null) {
            spent = 0.0;
        }

        // Calculate the difference from the average
        double difference = spent - averageSpent;

        holder.roommateName.setText(roommate);
        holder.roommateSpent.setText("Money Spent: $" + String.format("%.2f", spent));
        holder.roommateAverage.setText("Average Spending: $" + String.format("%.2f", averageSpent));
        holder.roommateDifference.setText("Difference from Average: $" + String.format("%.2f", difference));
    }



    @Override
    public int getItemCount() {
        return shoppingItemList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView roommateName;
        TextView roommateSpent;
        TextView roommateAverage;
        TextView roommateDifference;
        TextView totalSpent;

        public ViewHolder(View view) {
            super(view);
            roommateName = view.findViewById(R.id.roommate_name);
            roommateSpent = view.findViewById(R.id.roommate_spent);
            roommateAverage = view.findViewById(R.id.roommate_average);
            roommateDifference = view.findViewById(R.id.roommate_difference);
            totalSpent = view.findViewById(R.id.total_spent);

            Log.d("RoommateExpensesAdapter", "ViewHolder created. roommateName: " + (roommateName != null) +
                    ", roommateSpent: " + (roommateSpent != null) +
                    ", roommateAverage: " + (roommateAverage != null) +
                    ", roommateDifference: " + (roommateDifference != null) +
                    ", totalSpent: " + (totalSpent != null));
        }

    }
}
