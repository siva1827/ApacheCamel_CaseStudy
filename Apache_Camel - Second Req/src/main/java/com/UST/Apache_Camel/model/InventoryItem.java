package com.UST.Apache_Camel.model;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class InventoryItem {

    private String id;  // Changed to `id` for consistency
    private StockUpdateDetails stockDetails;

    // Getters and Setters

}