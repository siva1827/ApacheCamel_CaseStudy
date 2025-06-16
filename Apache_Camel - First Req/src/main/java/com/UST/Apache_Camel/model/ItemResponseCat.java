package com.UST.Apache_Camel.model;

import org.springframework.data.annotation.Id;

import java.util.List;

public class ItemResponseCat {
    @Id
    private String id;
    private String itemName;
    private String categoryId;
    private ItemPrice itemPrice;
    private StockDetails stockDetails;

    public ItemResponseCat(String id, String itemName, String categoryId, ItemPrice itemPrice, StockDetails stockDetails, boolean specialProduct) {
        this.id = id;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.itemPrice = itemPrice;
        this.stockDetails = stockDetails;
        this.specialProduct = specialProduct;
    }

    private boolean specialProduct;

    public ItemResponseCat() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public ItemPrice getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(ItemPrice itemPrice) {
        this.itemPrice = itemPrice;
    }

    public StockDetails getStockDetails() {
        return stockDetails;
    }

    public void setStockDetails(StockDetails stockDetails) {
        this.stockDetails = stockDetails;
    }

    public boolean isSpecialProduct() {
        return specialProduct;
    }

    public void setSpecialProduct(boolean specialProduct) {
        this.specialProduct = specialProduct;
    }

}