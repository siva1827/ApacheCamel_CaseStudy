package com.UST.Apache_Camel.model;

import java.math.BigDecimal;

public class ItemResponse {
    private String id;
    private String itemName;
    private String categoryName;
    private ItemPrice itemPrice;
    private StockInfo stockDetails;
    private boolean specialProduct;

    // Inner class to extract only relevant stock info
    public static class StockInfo {
        private Number availableStock;
        private String unitOfMeasure;

        public Number getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Number availableStock) {
            this.availableStock = availableStock;
        }

        public String getUnitOfMeasure() {
            return unitOfMeasure;
        }

        public void setUnitOfMeasure(String unitOfMeasure) {
            this.unitOfMeasure = unitOfMeasure;
        }
    }

    // Getters and Setters
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public ItemPrice getItemPrice() {
        return itemPrice;
    }

    public void setItemPrice(ItemPrice itemPrice) {
        this.itemPrice = itemPrice;
    }

    public StockInfo getStockDetails() {
        return stockDetails;
    }

    public void setStockDetails(StockInfo stockDetails) {
        this.stockDetails = stockDetails;
    }

    public boolean isSpecialProduct() {
        return specialProduct;
    }

    public void setSpecialProduct(boolean specialProduct) {
        this.specialProduct = specialProduct;
    }
}
