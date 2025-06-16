package com.UST.fileExport.config;

public class ApplicationConstants {
    public static final String MONGO_DATABASE = "mycartdb";
    public static final String MONGO_ITEM_COLLECTION = "item";
    public static final String MONGO_CATEGORY_COLLECTION = "category"; // Updated to match route
    public static final String MONGO_CONTROL_REF_COLLECTION = "ControlRef"; // Updated to lowercase
    public static final String DIRECT_FETCH_CONTROL_REF = "direct:fetchControlRef";
    public static final String DIRECT_PROCESS_ITEMS = "direct:processItems";
}