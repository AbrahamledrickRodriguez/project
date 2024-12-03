package edu.uga.cs.roommateshopping;

public class HelperClass {
    // Authentication fields
    private String email;
    private String password;

    public HelperClass(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public HelperClass() {
        // No-argument constructor (required for Firebase)
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static class ShoppingItem {
        private String itemName;
        private Integer quantity;
        private String itemId;
        private Double price;
        private String userID;
        private boolean itemAdded;
        private String date;
        private String groupId;

        public ShoppingItem() {
            this.itemId = "";
            this.date = "";
            this.groupId = "";
        }

        public ShoppingItem(String itemName, Integer quantity, String itemId, String date, String groupId) {
            this.itemName = itemName;
            this.quantity = quantity != null ? quantity : 0;
            this.itemId = itemId != null ? itemId : "";
            this.price = 0.0;
            this.date = date != null ? date : "";
            this.groupId = groupId != null ? groupId : "defaultGroupId";
            this.itemAdded = false;
        }

        public ShoppingItem(String itemName, Integer quantity) {
            this.itemName = itemName;
            this.quantity = quantity != null ? quantity : 0;
            this.price = price != null ? price : 0.0;
            this.userID = userID != null ? userID : "";
            this.itemAdded = itemAdded;
            this.itemId = itemId != null ? itemId : "";
            this.date = date != null ? date : "";
            this.groupId = groupId != null ? groupId : "defaultGroupId";
        }

        public String getItemName() {
            return itemName;
        }

        public void setItemName(String itemName) {
            this.itemName = itemName;
        }

        public Integer getQuantity() {
            return quantity != null ? quantity : 0;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public String getItemId() {
            return itemId != null ? itemId : "";
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public Double getPrice() {
            return price != null ? price : 0.0;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public boolean isItemAdded() {
            return itemAdded;
        }

        public void setItemAdded(boolean itemAdded) {
            this.itemAdded = itemAdded;
        }

        public String getDate() {
            return date != null ? date : "";
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getGroupId() {
            return groupId != null ? groupId : "defaultGroupId";
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }
    }
}

