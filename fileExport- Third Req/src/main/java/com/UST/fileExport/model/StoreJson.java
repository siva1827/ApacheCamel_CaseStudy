package com.UST.fileExport.model;

public class StoreJson {
    private String _id;
    private String itemName;
    private String categoryName;
    private Object itemPrice;
    private Object stockDetails;
    private boolean specialProduct;

    public String get_id() { return _id; }
    public void set_id(String _id) { this._id = _id; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public Object getItemPrice() { return itemPrice; }
    public void setItemPrice(Object itemPrice) { this.itemPrice = itemPrice; }

    public Object getStockDetails() { return stockDetails; }
    public void setStockDetails(Object stockDetails) { this.stockDetails = stockDetails; }

    public boolean isSpecialProduct() { return specialProduct; }
    public void setSpecialProduct(boolean specialProduct) { this.specialProduct = specialProduct; }
}