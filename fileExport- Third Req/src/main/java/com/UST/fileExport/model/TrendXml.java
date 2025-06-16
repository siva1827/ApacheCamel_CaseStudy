package com.UST.fileExport.model;

import jakarta.xml.bind.annotation.*;

@XmlRootElement(name = "inventory")
@XmlAccessorType(XmlAccessType.FIELD)
public class TrendXml {

    @XmlElement(name = "category")
    private Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Category {

        @XmlAttribute
        private String id;

        @XmlElement(name = "categoryName")
        private CategoryName categoryName;

        @XmlElement(name = "item")
        private Item item;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public CategoryName getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(CategoryName categoryName) {
            this.categoryName = categoryName;
        }

        public Item getItem() {
            return item;
        }

        public void setItem(Item item) {
            this.item = item;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CategoryName {

        @XmlAttribute
        private String name;

        @XmlValue
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Item {

        @XmlElement
        private Integer availableStock;

        @XmlElement
        private String categoryId;

        @XmlElement
        private String itemId;

        @XmlElement
        private Integer sellingPrice;

        public Integer getAvailableStock() {
            return availableStock;
        }

        public void setAvailableStock(Integer availableStock) {
            this.availableStock = availableStock;
        }

        public String getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(String categoryId) {
            this.categoryId = categoryId;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public Integer getSellingPrice() {
            return sellingPrice;
        }

        public void setSellingPrice(Integer sellingPrice) {
            this.sellingPrice = sellingPrice;
        }
    }
}