package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.model.ItemResult;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class ItemProcessor {
    private static final Logger logger = LoggerFactory.getLogger(ItemProcessor.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void processItem(Exchange exchange) throws InventoryValidationException {
        Map<String, Object> item = exchange.getIn().getBody(Map.class);
        if (item == null || item.get("_id") == null || item.get("stockDetails") == null) {
            throw new InventoryValidationException("Each item must have '_id' and 'stockDetails'.");
        }

        String id = item.get("_id").toString();
        Object stockDetailsObj = item.get("stockDetails");
        if (!(stockDetailsObj instanceof Map)) {
            throw new InventoryValidationException("stockDetails must be an object for item: " + id);
        }

        Map<String, Object> stock = (Map<String, Object>) stockDetailsObj;
        if (!stock.containsKey("soldOut") || !stock.containsKey("damaged")) {
            throw new InventoryValidationException("Missing 'soldOut' or 'damaged' in stockDetails for item: " + id);
        }

        Object soldOutObj = stock.get("soldOut");
        Object damagedObj = stock.get("damaged");

        if (!(soldOutObj instanceof Integer)) {
            throw new InventoryValidationException("soldOut must be an integer for item: " + id);
        }
        if (!(damagedObj instanceof Integer)) {
            throw new InventoryValidationException("damaged must be an integer for item: " + id);
        }

        int soldOut = (Integer) soldOutObj;
        int damaged = (Integer) damagedObj;

        if (soldOut < 0) {
            throw new InventoryValidationException("soldOut cannot be negative for item: " + id);
        }
        if (damaged < 0) {
            throw new InventoryValidationException("damaged cannot be negative for item: " + id);
        }

        exchange.setProperty("itemId", id);
        exchange.setProperty("stockReduction", soldOut + damaged);
        logger.debug("Processing item: {}, stockReduction: {}", id, soldOut + damaged);
    }

    public void setItemId(Exchange exchange) {
        String itemId = exchange.getProperty("itemId", String.class);
        exchange.getIn().setBody(itemId);
        logger.debug("Set itemId for findById: {}", itemId);
    }

    public void validateAndUpdateItem(Exchange exchange) throws InventoryValidationException {
        Document itemDoc = exchange.getIn().getBody(Document.class);
        String itemId = exchange.getProperty("itemId", String.class);
        if (itemDoc == null) {
            throw new InventoryValidationException("Item not found for ID: " + itemId);
        }

        Document stockDetails = itemDoc.get("stockDetails", Document.class);
        if (stockDetails == null) {
            throw new InventoryValidationException("Missing stockDetails in DB for item: " + itemId);
        }

        Integer availableStock = stockDetails.getInteger("availableStock");
        if (availableStock == null) {
            throw new InventoryValidationException("availableStock must be an integer for item: " + itemId);
        }
        if (availableStock < 0) {
            throw new InventoryValidationException("availableStock cannot be negative for item: " + itemId);
        }

        int stockReduction = exchange.getProperty("stockReduction", Integer.class);
        if (stockReduction > availableStock) {
            throw new InventoryValidationException("Requested stock reduction (" + stockReduction + ") exceeds available stock (" + availableStock + ") for item ID: " + itemId);
        }

        stockDetails.put("availableStock", availableStock - stockReduction);
        itemDoc.put("lastUpdateDate", LocalDateTime.now().format(DATE_TIME_FORMATTER));

        exchange.setProperty("updatedItem", itemDoc);
        exchange.getIn().setBody(itemDoc);
        logger.debug("Updated item: {}, availableStock: {}, lastUpdateDate: {}", 
                itemId, stockDetails.get("availableStock"), itemDoc.get("lastUpdateDate"));
    }

    public void markSuccess(Exchange exchange) {
        String itemId = exchange.getProperty("itemId", String.class);
        ItemResult itemResult = new ItemResult(itemId, "success", "Inventory updated successfully for item " + itemId);
        exchange.setProperty("itemResult", itemResult);
        logger.info("✅ Inventory updated for {}", itemId);
    }

    public void markFailure(Exchange exchange) {
        String itemId = exchange.getProperty("itemId", String.class);
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        ItemResult itemResult = new ItemResult(itemId, "error", errorMessage);
        exchange.setProperty("itemResult", itemResult);
        logger.warn("Inventory update failed for item {}: {}", itemId, errorMessage);
    }
}