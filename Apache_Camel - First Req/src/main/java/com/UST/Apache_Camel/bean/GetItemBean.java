package com.UST.Apache_Camel.bean;

import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.model.ItemPrice;
import com.UST.Apache_Camel.model.ItemResponse;
import com.UST.Apache_Camel.model.ItemResponse.StockInfo;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Map;

import static java.lang.Boolean.TRUE;

public class GetItemBean {

    private static final Logger logger = LoggerFactory.getLogger(GetItemBean.class);

    public void setItemId(Exchange exchange) {
        String itemId = exchange.getIn().getHeader("itemId", String.class);
        exchange.getIn().setBody(itemId);
        logger.debug("Set itemId for findById: {}", itemId);
    }

    public void setCategoryId(Exchange exchange) {
        Document itemDoc = exchange.getIn().getBody(Document.class);
        if (itemDoc == null || !itemDoc.containsKey("categoryId")) {
            String itemId = exchange.getIn().getHeader("itemId", String.class);
            logger.info("Item not found or missing categoryId for ID: {}", itemId);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_ITEM_NOT_FOUND));
            exchange.setProperty("itemNotFound", true);
            return;
        }
        exchange.setProperty("item", itemDoc);
        String categoryId = itemDoc.getString("categoryId");
        exchange.getIn().setBody(categoryId);
        logger.debug("Set categoryId for findById: {}", categoryId);
    }

    public void processCategoryResult(Exchange exchange) {
        if (TRUE.equals(exchange.getProperty("itemNotFound", Boolean.class))) {
            return;
        }

        Document categoryDoc = exchange.getIn().getBody(Document.class);
        Document itemDoc = exchange.getProperty("item", Document.class);
        String itemId = exchange.getIn().getHeader("itemId", String.class);

        logger.debug("Category document for item ID {} with categoryId {}: {}", 
                itemDoc.getString("_id"), itemDoc.getString("categoryId"), categoryDoc);

        if (categoryDoc == null || categoryDoc.getString("categoryName") == null) {
            logger.info("Category not found or missing categoryName for item ID: {}, categoryId: {}", 
                    itemDoc.getString("_id"), itemDoc.getString("categoryId"));
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_CATEGORY_NOT_FOUND));
            return;
        }

        // Create ItemResponse
        ItemResponse response = new ItemResponse();
        response.setId(itemDoc.getString("_id"));
        response.setItemName(itemDoc.getString("itemName"));
        response.setCategoryName(categoryDoc.getString("categoryName"));

        // Set itemPrice
        Document priceDoc = itemDoc.get("itemPrice", Document.class);
        if (priceDoc != null) {
            ItemPrice price = new ItemPrice();
            Number basePrice = priceDoc.get("basePrice", Number.class);
            Number sellingPrice = priceDoc.get("sellingPrice", Number.class);
            price.setBasePrice(basePrice != null ? BigDecimal.valueOf(basePrice.doubleValue()) : null);
            price.setSellingPrice(sellingPrice != null ? BigDecimal.valueOf(sellingPrice.doubleValue()) : null);
            response.setItemPrice(price);
        }

        // Set stockDetails
        Document stockDoc = itemDoc.get("stockDetails", Document.class);
        if (stockDoc != null) {
            StockInfo stockInfo = new StockInfo();
            Number availableStock = stockDoc.get("availableStock", Number.class);
            stockInfo.setAvailableStock(availableStock != null ? availableStock : 0);
            stockInfo.setUnitOfMeasure(stockDoc.getString("unitOfMeasure"));
            response.setStockDetails(stockInfo);
        }

        // Handle specialProduct safely
        Object specialProduct = itemDoc.get("specialProduct");
        response.setSpecialProduct(specialProduct instanceof Boolean ? (Boolean) specialProduct : 
                "true".equalsIgnoreCase(String.valueOf(specialProduct)));

        exchange.getIn().setBody(response);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        logger.info("ItemResponse constructed for item ID: {}", response.getId());
    }

    public void processResult(Exchange exchange) {
        if (exchange.getIn().getBody() == null) {
            String itemId = exchange.getIn().getHeader("itemId", String.class);
            logger.info("Item not found for ID: {}", itemId);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_ITEM_NOT_FOUND));
            exchange.setProperty("itemNotFound", true);
        } else {
            logger.debug("Item found, proceeding to fetch category: {}", exchange.getIn().getBody());
        }
    }
}