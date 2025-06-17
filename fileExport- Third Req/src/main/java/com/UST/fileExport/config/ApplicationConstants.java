package com.UST.fileExport.config;

public final class ApplicationConstants {

    private ApplicationConstants() {
        // Private constructor to prevent instantiation
    }

    // MongoDB Database
    public static final String MONGO_DATABASE = "mycartdb";

    // MongoDB Collections
    public static final String MONGO_ITEM_COLLECTION = "item";
    public static final String MONGO_CATEGORY_COLLECTION = "category";
    public static final String MONGO_CONTROL_REF_COLLECTION = "ControlRef";

    // MongoDB Endpoint URIs
    public static final String MONGO_ITEM_FIND_ALL = "mongodb:mongoBean?database=%s&collection=%s&operation=findAll";
    public static final String MONGO_CATEGORY_FIND_ONE_BY_QUERY = "mongodb:mongoBean?database=%s&collection=%s&operation=findOneByQuery";
    public static final String MONGO_CONTROL_REF_AGGREGATE = "mongodb:mongoBean?database=%s&collection=%s&operation=aggregate";
    public static final String MONGO_CONTROL_REF_UPDATE = "mongodb:mongoBean?database=%s&collection=%s&operation=update";

    // Route IDs
    public static final String ROUTE_FILE_EXPORT = "fileExport";
    public static final String ROUTE_FETCH_CONTROL_REF = "fetchControlRef";
    public static final String ROUTE_PROCESS_ITEMS = "processItems";
    public static final String ROUTE_WRITE_TREND_XML = "writeTrendXml";
    public static final String ROUTE_WRITE_REVIEW_XML = "writeReviewXml";
    public static final String ROUTE_WRITE_STORE_JSON = "writeStoreJson";
    public static final String ROUTE_UPDATE_CONTROL_REF = "updateControlRef";

    // Endpoint Prefixes
    public static final String DIRECT_PREFIX = "direct:";
}