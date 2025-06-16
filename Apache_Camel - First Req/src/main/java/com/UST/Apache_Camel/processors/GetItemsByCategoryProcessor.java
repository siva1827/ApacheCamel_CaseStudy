package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.model.*;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class GetItemsByCategoryProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(GetItemsByCategoryProcessor.class);

    @Override
    public void process(Exchange exchange) {
        // Default process method (empty as per route logic)
    }

    public void buildAggregationPipeline(Exchange exchange) {
        String categoryId = exchange.getIn().getHeader("categoryId", String.class);
        boolean includeSpecial = Boolean.parseBoolean(exchange.getIn().getHeader("includeSpecial", "false", String.class));

        List<Document> pipeline = new ArrayList<>();
        Document matchStage = new Document("$match", new Document("categoryId", categoryId));
        if (!includeSpecial) {
            matchStage.get("$match", Document.class).append("specialProduct", false);
        }
        pipeline.add(matchStage);

        pipeline.add(new Document("$lookup", new Document()
                .append("from", ApplicationConstants.MONGO_CATEGORY_READ_COLLECTION)
                .append("localField", "categoryId")
                .append("foreignField", "_id")
                .append("as", "categoryDetails")));

        pipeline.add(new Document("$unwind", new Document()
                .append("path", "$categoryDetails")
                .append("preserveNullAndEmptyArrays", false)));

        pipeline.add(new Document("$group", new Document()
                .append("_id", "$categoryId")
                .append("categoryName", new Document("$first", "$categoryDetails.categoryName"))
                .append("categoryDepartment", new Document("$first", "$categoryDetails.categoryDep"))
                .append("items", new Document("$push", new Document()
                        .append("id", "$_id")
                        .append("itemName", "$itemName")
                        .append("categoryId", "$categoryId")
                        .append("itemPrice", "$itemPrice")
                        .append("stockDetails", new Document()
                                .append("availableStock", "$stockDetails.availableStock")
                                .append("unitOfMeasure", "$stockDetails.unitOfMeasure"))
                        .append("specialProduct", "$specialProduct")))));

        exchange.getIn().setBody(pipeline);
        logger.debug("Built aggregation pipeline for categoryId: {}, includeSpecial: {}, matchStage: {}", 
                categoryId, includeSpecial, matchStage);
    }

    public void processResult(Exchange exchange) {
        List<?> result = exchange.getIn().getBody(List.class);
        String categoryId = exchange.getIn().getHeader("categoryId", String.class);

        if (result == null || result.isEmpty()) {
            // Query category collection directly to get category details
            Document categoryQuery = new Document("_id", categoryId);
            exchange.getIn().setBody(categoryQuery);
            exchange.setProperty("fetchCategory", true);
            logger.info("No items found for categoryId: {}, fetching category details", categoryId);
            return;
        }

        logger.debug("Raw pipeline result for categoryId {}: {}", categoryId, result);

        Document resultDoc = (Document) result.get(0);
        CategoryItemsResponse resultResponse = new CategoryItemsResponse();
        resultResponse.setCategoryName(resultDoc.getString("categoryName"));
        resultResponse.setCategoryDepartment(resultDoc.getString("categoryDepartment"));

        if (resultDoc.getString("categoryName") == null) {
            // Category not found
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_CATEGORY_NOT_FOUND));
            logger.info("Category not found for categoryId: {}", categoryId);
            exchange.setProperty("fetchCategory", false); // Ensure further processing is skipped
            return;
        }

        List<Document> itemDocs = resultDoc.getList("items", Document.class, new ArrayList<>());
        logger.info("Retrieved {} item documents for categoryId: {}, itemIds: {}", 
                itemDocs.size(), categoryId, 
                itemDocs.stream().map(doc -> doc.getString("id")).collect(Collectors.toList()));

        exchange.setProperty("resultResponse", resultResponse);
        exchange.getIn().setBody(itemDocs);
    }

    public void processCategoryResult(Exchange exchange) {
        if (!(Boolean)exchange.getProperty("fetchCategory", false)) {
            return; // Skip if not fetching category directly
        }

        Document categoryDoc = exchange.getIn().getBody(Document.class);
        String categoryId = exchange.getIn().getHeader("categoryId", String.class);
        CategoryItemsResponse resultResponse = new CategoryItemsResponse();

        if (categoryDoc == null || categoryDoc.getString("categoryName") == null) {
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_CATEGORY_NOT_FOUND));
            logger.info("Category not found for categoryId: {}", categoryId);
            return;
        }

        resultResponse.setCategoryName(categoryDoc.getString("categoryName"));
        resultResponse.setCategoryDepartment(categoryDoc.getString("categoryDep"));
        resultResponse.setItems(new ArrayList<>());

        exchange.getIn().setBody(resultResponse);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        logger.info("Category details retrieved for categoryId: {}, no items found", categoryId);
    }

    public void transformItem(Exchange exchange) {
        Document itemDoc = exchange.getIn().getBody(Document.class);
        String categoryId = exchange.getIn().getHeader("categoryId", String.class);

        if (itemDoc == null) {
            logger.error("Item document is null for categoryId: {}", categoryId);
            exchange.getIn().setBody(null);
            return;
        }

        logger.debug("Processing item document: {}", itemDoc);

        ItemResponseCat item = new ItemResponseCat();
        item.setId(itemDoc.getString("id"));
        item.setItemName(itemDoc.getString("itemName"));
        item.setCategoryId(itemDoc.getString("categoryId"));

        // Handle specialProduct with type checking
        Object specialProduct = itemDoc.get("specialProduct");
        item.setSpecialProduct(specialProduct instanceof Boolean ? (Boolean) specialProduct : 
                "true".equalsIgnoreCase(String.valueOf(specialProduct)));
        logger.debug("Processed specialProduct for item {}: {}", item.getId(), item.isSpecialProduct());

        // Handle nested itemPrice
        Document priceDoc = itemDoc.get("itemPrice", Document.class);
        if (priceDoc != null) {
            ItemPrice itemPrice = new ItemPrice();
            Number basePrice = priceDoc.get("basePrice", Number.class);
            Number sellingPrice = priceDoc.get("sellingPrice", Number.class);
            itemPrice.setBasePrice(basePrice != null ? BigDecimal.valueOf(basePrice.doubleValue()) : null);
            itemPrice.setSellingPrice(sellingPrice != null ? BigDecimal.valueOf(sellingPrice.doubleValue()) : null);
            item.setItemPrice(itemPrice);
        }

        // Handle nested stockDetails
        Document stockDoc = itemDoc.get("stockDetails", Document.class);
        if (stockDoc != null) {
            StockDetails stockDetails = new StockDetails();
            Number availableStock = stockDoc.get("availableStock", Number.class); // Handle Decimal128
            stockDetails.setAvailableStock(availableStock != null ? availableStock.intValue() : 0);
            stockDetails.setUnitOfMeasure(stockDoc.getString("unitOfMeasure"));
            item.setStockDetails(stockDetails);
        }

        exchange.getIn().setBody(item);
        logger.debug("Transformed item: {}, stockDetails: {}, specialProduct: {}", 
                item.getId(), item.getStockDetails(), item.isSpecialProduct());
    }

    public void buildFinalResponse(Exchange exchange) {
        Object body = exchange.getIn().getBody();
        String categoryId = exchange.getIn().getHeader("categoryId", String.class);
        CategoryItemsResponse response;

        // If body is already a CategoryItemsResponse (from processCategoryResult), use it
        if (body instanceof CategoryItemsResponse) {
            response = (CategoryItemsResponse) body;
        } else {
            // Otherwise, body is a list of ItemResponseCat from split/aggregation
            List<ItemResponseCat> items = body != null ? (List<ItemResponseCat>) body : new ArrayList<>();
            CategoryItemsResponse resultResponse = exchange.getProperty("resultResponse", CategoryItemsResponse.class);

            if (resultResponse == null && items.isEmpty()) {
                // Should not happen if fetchCategory is handled, but log as fallback
                logger.warn("No resultResponse and no items for categoryId: {}, returning empty response", categoryId);
                response = new CategoryItemsResponse(null, null, items);
            } else {
                response = new CategoryItemsResponse(
                        resultResponse != null ? resultResponse.getCategoryName() : null,
                        resultResponse != null ? resultResponse.getCategoryDepartment() : null,
                        items);
            }
        }

        exchange.getIn().setBody(response);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
        logger.info("Final response with {} items for categoryId: {}, specialItemsIncluded: {}, itemIds: {}", 
                response.getItems().size(), categoryId,
                response.getItems().stream().anyMatch(ItemResponseCat::isSpecialProduct),
                response.getItems().stream().map(ItemResponseCat::getId).collect(Collectors.toList()));
    }
}