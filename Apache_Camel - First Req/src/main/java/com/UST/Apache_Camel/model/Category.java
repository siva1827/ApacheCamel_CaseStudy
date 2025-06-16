package com.UST.Apache_Camel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "category")
public class Category {

    @Id
    @JsonProperty("_id")
    private String id;
    private String categoryName;
    private String categoryDep;
    private String categoryTax;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryDep() {
        return categoryDep;
    }

    public void setCategoryDep(String categoryDep) {
        this.categoryDep = categoryDep;
    }

    public String getCategoryTax() {
        return categoryTax;
    }

    public void setCategoryTax(String categoryTax) {
        this.categoryTax = categoryTax;
    }
}