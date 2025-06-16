package com.UST.Apache_Camel.model;

public class StockDetails {

    private Number  availableStock;
    private String unitOfMeasure;


    // Getters and Setters

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