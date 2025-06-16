package com.UST.Apache_Camel.model;

import java.util.ArrayList;
import java.util.List;

public class CategoryItemsResponse {
    private String categoryName;
    private String categoryDepartment;
    private List<ItemResponseCat> items;

    public CategoryItemsResponse() {
        this.items = new ArrayList<>();
    }

    public CategoryItemsResponse(String categoryName, String categoryDepartment, List<ItemResponseCat> items) {
        this.categoryName = categoryName;
        this.categoryDepartment = categoryDepartment;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDepartment() {
        return categoryDepartment;
    }

    public void setCategoryDepartment(String categoryDepartment) {
        this.categoryDepartment = categoryDepartment;
    }

    public List<ItemResponseCat> getItems() {
        return items;
    }

    public void setItems(List<ItemResponseCat> items) {
        this.items = items;
    }
}