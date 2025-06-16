package com.UST.Apache_Camel.model;

public class ItemResult {
    private String itemId;
    private String status;
    private String message;

    public ItemResult(String itemId, String status, String message) {
        this.itemId = itemId;
        this.status = status;
        this.message = message;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}