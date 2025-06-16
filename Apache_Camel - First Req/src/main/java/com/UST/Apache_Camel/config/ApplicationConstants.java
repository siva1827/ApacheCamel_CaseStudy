package com.UST.Apache_Camel.config;

public final class ApplicationConstants {

    private ApplicationConstants() {
    }

    public static final String MONGO_DATABASE = "mycartdb";
    public static final String MONGO_ITEM_READ_COLLECTION = "item";
    public static final String MONGO_ITEM_WRITE_COLLECTION = "item";
    public static final String MONGO_CATEGORY_READ_COLLECTION = "category";
    public static final String MONGO_CATEGORY_WRITE_COLLECTION = "category";
    public static final String MONGO_INVENTORY_AUDIT_WRITE_COLLECTION = "inventory_audit";

    public static final String MONGO_ITEM_FIND_BY_ID = "mongodb:mongoClient?database=%s&collection=%s&operation=findById";
    public static final String MONGO_ITEM_AGGREGATE = "mongodb:mongoClient?database=%s&collection=%s&operation=aggregate";
    public static final String MONGO_ITEM_INSERT = "mongodb:mongoClient?database=%s&collection=%s&operation=insert";
    public static final String MONGO_ITEM_SAVE = "mongodb:mongoClient?database=%s&collection=%s&operation=save";
    public static final String MONGO_ITEM_DELETE = "mongodb:mongoClient?database=%s&collection=%s&operation=remove";
    public static final String MONGO_CATEGORY_FIND_BY_ID = "mongodb:mongoClient?database=%s&collection=%s&operation=findById";
    public static final String MONGO_CATEGORY_INSERT = "mongodb:mongoClient?database=%s&collection=%s&operation=insert";
    public static final String MONGO_CATEGORY_DELETE = "mongodb:mongoClient?database=%s&collection=%s&operation=remove";
    public static final String MONGO_INVENTORY_AUDIT_AGGREGATE = "mongodb:myMongoBean?database=%s&collection=%s&operation=aggregate";
    public static final String MONGO_INVENTORY_AUDIT_INSERT = "mongodb:myMongoBean?database=%s&collection=%s&operation=insert";

    public static final String AMQ_INVENTORY_UPDATE_WRITE_QUEUE = "inventory.update.queue";
    public static final String AMQ_INVENTORY_UPDATE_READ_QUEUE = "inventory.update.queue";
    public static final String AMQ_INVENTORY_UPDATE_WRITE = "activemq:queue:%s?exchangePattern=InOnly&deliveryMode=2";
    public static final String AMQ_INVENTORY_UPDATE_READ = "activemq:queue:%s?concurrentConsumers=6";

    public static final String REST_HOST = "0.0.0.0";
    public static final String REST_PORT = "8080";
    public static final String REST_COMPONENT = "servlet";

    public static final String ERROR_ITEM_NOT_FOUND = "Item not found";
    public static final String ERROR_CATEGORY_NOT_FOUND = "Category is invalid";
    public static final String ERROR_ITEM_ALREADY_EXISTS = "Item already exists";
    public static final String ERROR_CATEGORY_ALREADY_EXISTS = "Category already exists";

    public static final String DIRECT_PREFIX = "direct:";
    public static final String SEDA_PREFIX = "seda:";

    public static final String ROUTE_GET_ITEM_BY_ID = "getItemByIdRoute";
    public static final String ROUTE_GET_ITEMS_BY_CATEGORY = "getItemsByCategoryRoute";
    public static final String ROUTE_POST_NEW_ITEM = "postNewItemRoute";
    public static final String ROUTE_POST_NEW_CATEGORY = "postNewCategoryRoute";
    public static final String ROUTE_DELETE_ITEM = "deleteItemRoute";
    public static final String ROUTE_DELETE_CATEGORY = "deleteCategoryRoute";
    public static final String ROUTE_PROCESS_INVENTORY_UPDATE = "processInventoryUpdateRoute";
    public static final String ROUTE_UPDATE_INVENTORY = "updateInventoryRoute";
    public static final String ROUTE_ASYNC_INVENTORY_UPDATE = "asyncInventoryUpdateRoute";
    public static final String ROUTE_PROCESS_INVENTORY_QUEUE = "processInventoryQueueRoute";

    public static final String ENDPOINT_GET_ITEM_BY_ID = "getItemById";
    public static final String ENDPOINT_GET_ITEMS_BY_CATEGORY = "getItemsByCategory";
    public static final String ENDPOINT_POST_NEW_ITEM = "postNewItem";
    public static final String ENDPOINT_POST_NEW_CATEGORY = "postNewCategory";
    public static final String ENDPOINT_DELETE_ITEM = "deleteItem";
    public static final String ENDPOINT_DELETE_CATEGORY = "deleteCategory";
    public static final String ENDPOINT_PROCESS_INVENTORY_UPDATE = "processInventoryUpdate";
    public static final String ENDPOINT_UPDATE_INVENTORY = "updateInventory";
    public static final String ENDPOINT_ASYNC_INVENTORY_UPDATE = "asyncInventoryUpdate";
}