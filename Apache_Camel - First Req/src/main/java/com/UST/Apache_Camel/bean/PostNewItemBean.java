package com.UST.Apache_Camel.bean;

import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.model.Item;
import com.UST.Apache_Camel.model.ItemPrice;
import com.UST.Apache_Camel.model.StockDetails;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostNewItemBean {
    private static final Logger logger = LoggerFactory.getLogger(PostNewItemBean.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void validateItem(Exchange exchange) throws Exception {
        Item item = exchange.getIn().getBody(Item.class);
        exchange.setProperty("newItem", item);

        // Validate required fields
        if (item == null || item.getId() == null || item.getItemName() == null ||
            item.getCategoryId() == null || item.getItemPrice() == null || 
            item.getStockDetails() == null) {
            throw new InventoryValidationException("Item must have '_id', 'itemName', 'categoryId', 'itemPrice', and 'stockDetails'");
        }

        String itemId = item.getId();
        ItemPrice price = item.getItemPrice();
        StockDetails stock = item.getStockDetails();

        // Validate itemPrice
        if (price.getBasePrice() == null || price.getSellingPrice() == null) {
            throw new InventoryValidationException("itemPrice must contain 'basePrice' and 'sellingPrice' for item: " + itemId);
        }

        double basePrice = price.getBasePrice().doubleValue();
        double sellingPrice = price.getSellingPrice().doubleValue();

        if (basePrice <= 0) {
            throw new InventoryValidationException("basePrice must be greater than zero for item: " + itemId);
        }
        if (sellingPrice <= 0) {
            throw new InventoryValidationException("sellingPrice must be greater than zero for item: " + itemId);
        }

        // Validate stockDetails
        if (stock.getAvailableStock() == null || stock.getUnitOfMeasure() == null) {
            throw new InventoryValidationException("stockDetails must contain 'availableStock' and 'unitOfMeasure' for item: " + itemId);
        }

        int availableStock = stock.getAvailableStock().intValue();
        if (availableStock < 0) {
            throw new InventoryValidationException("availableStock cannot be negative for item: " + itemId);
        }

        exchange.getIn().setBody(itemId);
        logger.debug("Validated item and set itemId for findById: {}", itemId);
    }

    public void handleExistingItem(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_ITEM_ALREADY_EXISTS));
        logger.warn("Item already exists: {}", exchange.getProperty("newItem", Item.class).getId());
    }

    public void setCategoryId(Exchange exchange) {
        Item item = exchange.getProperty("newItem", Item.class);
        exchange.setProperty("validatedItem", item);
        String categoryId = item.getCategoryId();
        exchange.getIn().setBody(categoryId);
        logger.debug("Set categoryId for findById: {}", categoryId);
    }

    public void handleInvalidCategory(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_CATEGORY_NOT_FOUND));
        logger.warn("Invalid category for item: {}", exchange.getProperty("validatedItem", Item.class).getId());
    }

    public void prepareItemForInsert(Exchange exchange) {
        Item item = exchange.getProperty("validatedItem", Item.class);
        item.setLastUpdateDate(LocalDateTime.now());

        Document document = new Document();
        document.append("_id", item.getId());
        document.append("itemName", item.getItemName());
        document.append("categoryId", item.getCategoryId());
        document.append("lastUpdateDate", item.getLastUpdateDate().format(DATE_TIME_FORMATTER));
        
        Document priceDoc = new Document();
        priceDoc.append("basePrice", item.getItemPrice().getBasePrice());
        priceDoc.append("sellingPrice", item.getItemPrice().getSellingPrice());
        document.append("itemPrice", priceDoc);

        Document stockDoc = new Document();
        stockDoc.append("availableStock", item.getStockDetails().getAvailableStock());
        stockDoc.append("unitOfMeasure", item.getStockDetails().getUnitOfMeasure());
        document.append("stockDetails", stockDoc);

        document.append("specialProduct", item.isSpecialProduct());

        if (item.getReview() != null && !item.getReview().isEmpty()) {
            List<Document> reviewDocs = item.getReview().stream()
                    .map(review -> new Document()
                            .append("rating", review.getRating())
                            .append("comment", review.getComment()))
                    .collect(Collectors.toList());
            document.append("review", reviewDocs);
        } else {
            document.append("review", List.of());
        }

        exchange.getIn().setBody(document);
        logger.debug("Prepared item for insert: {}, lastUpdateDate: {}", 
                item.getId(), item.getLastUpdateDate().format(DATE_TIME_FORMATTER));
    }

    public void handleInsertSuccess(Exchange exchange) {
        Item item = exchange.getProperty("validatedItem", Item.class);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        exchange.getIn().setBody(Map.of(
                "itemId", item.getId(),
                "message", "Item inserted successfully"
        ));
        logger.info("Successfully inserted item: {}", item.getId());
    }
}