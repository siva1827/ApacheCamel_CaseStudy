package com.UST.ItemBridge.pojo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class ItemResponse {
    @JsonProperty("_id")
    @JsonAlias("id")
    private String id;
    private String itemName;
    private String categoryName;
    private ItemPrice itemPrice;
    private StockInfo stockDetails;
    private boolean specialProduct;

    // Inner class for stock info
    public static class StockInfo {
        private Number availableStock;
        private String unitOfMeasure;

        public Number getAvailableStock() { return availableStock; }
        public void setAvailableStock(Number availableStock) { this.availableStock = availableStock; }
        public String getUnitOfMeasure() { return unitOfMeasure; }
        public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    }

    // Inner class for item price
    public static class ItemPrice {
        private BigDecimal basePrice;
        private BigDecimal sellingPrice;

        public BigDecimal getBasePrice() { return basePrice; }
        public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public ItemPrice getItemPrice() { return itemPrice; }
    public void setItemPrice(ItemPrice itemPrice) { this.itemPrice = itemPrice; }
    public StockInfo getStockDetails() { return stockDetails; }
    public void setStockDetails(StockInfo stockDetails) { this.stockDetails = stockDetails; }
    public boolean isSpecialProduct() { return specialProduct; }
    public void setSpecialProduct(boolean specialProduct) { this.specialProduct = specialProduct; }
}