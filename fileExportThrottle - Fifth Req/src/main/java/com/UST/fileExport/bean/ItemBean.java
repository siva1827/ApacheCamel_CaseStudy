package com.UST.fileExport.bean;

import com.UST.fileExport.model.ReviewXml;
import com.UST.fileExport.model.StoreJson;
import com.UST.fileExport.model.TrendXml;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ItemBean {
    private static final Logger logger = LoggerFactory.getLogger(ItemBean.class);
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public void setCurrentTimestamp(Exchange exchange) {
        String currentTs;
        synchronized (FORMATTER) {
            currentTs = FORMATTER.format(new Date());
        }
        exchange.setProperty("currentTs", currentTs);
        logger.debug("Set currentTs: {}", currentTs);
    }

    public void prepareItemQuery(Exchange exchange) {
        Map<String, Date> controlRefMap = exchange.getProperty("controlRefMap", Map.class);
        Document query = new Document();
        if (controlRefMap != null && !controlRefMap.isEmpty()) {
            Date latestProcessTs = Collections.max(controlRefMap.values());
            String latestProcessTsStr;
            synchronized (FORMATTER) {
                latestProcessTsStr = FORMATTER.format(latestProcessTs);
            }
            query.append("lastUpdateDate", new Document("$gt", latestProcessTsStr));
            logger.debug("Prepared item query with lastUpdateDate > {}", latestProcessTsStr);
        } else {
            logger.warn("controlRefMap is null or empty, fetching all items");
        }
        exchange.getIn().setBody(query);
        logger.debug("Prepared item query: {}", query);
    }

    public void filterValidItems(Exchange exchange) {
        List<Document> items = exchange.getIn().getBody(List.class);
        Map<String, Date> controlRefMap = exchange.getProperty("controlRefMap", Map.class);
        List<Document> validItems = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            logger.info("No items fetched from MongoDB");
            exchange.getIn().setBody(validItems);
            return;
        }
        if (controlRefMap == null || controlRefMap.isEmpty()) {
            logger.warn("controlRefMap is null or empty, processing all items as new");
            validItems.addAll(items);
            exchange.getIn().setBody(validItems);
            logger.info("Fetched {} items, all considered valid (no ControlRef map)", items.size());
            return;
        }
        for (Document item : items) {
            String id = item.getString("_id");
            String lastUpdateDateStr = item.getString("lastUpdateDate");
            if (lastUpdateDateStr == null) {
                logger.warn("Skipping item {}: lastUpdateDate is null", id);
                continue;
            }
            Date lastUpdateDate;
            try {
                synchronized (FORMATTER) {
                    lastUpdateDate = FORMATTER.parse(lastUpdateDateStr);
                }
            } catch (ParseException e) {
                logger.error("Invalid lastUpdateDate format for item {}: {}", id, lastUpdateDateStr, e);
                continue;
            }
            Date lastProcessTs = controlRefMap.get(id);
            if (lastProcessTs == null || lastUpdateDate.after(lastProcessTs)) {
                validItems.add(item);
                logger.info("Valid item: {} with lastUpdateDate: {} (lastProcessTs: {})",
                    id, lastUpdateDateStr, lastProcessTs != null ? FORMATTER.format(lastProcessTs) : "none");
            } else {
                logger.debug("Skipping item {}: lastUpdateDate {} not after lastProcessTs {}",
                    id, lastUpdateDateStr, FORMATTER.format(lastProcessTs));
            }
        }
        logger.info("Fetched {} items, filtered to {} valid items: {}",
            items.size(), validItems.size(), validItems.stream().map(doc -> doc.getString("_id")).toList());
        exchange.getIn().setBody(validItems);
    }

    public void logFetchedItems(Exchange exchange) {
        List<Document> items = exchange.getIn().getBody(List.class);
        if (items != null && !items.isEmpty()) {
            List<String> itemSummaries = items.stream()
                .map(doc -> doc.getString("_id") + "@" + doc.getString("lastUpdateDate"))
                .toList();
            logger.info("Processing {} items: {}", items.size(), itemSummaries);
        } else {
            logger.info("No items to process after filtering");
        }
    }

    public void enrichWithCategory(Exchange exchange) {
        Document item = exchange.getIn().getBody(Document.class);
        if (item != null) {
            String itemId = item.getString("_id");
            String categoryId = item.getString("categoryId");
            exchange.setProperty("itemId", itemId);
            exchange.setProperty("item", item);
            logger.debug("Set itemId: {} for item: {}", itemId, item.getString("_id"));
            if (categoryId != null && !categoryId.trim().isEmpty()) {
                Document query;
                try {
                    query = new Document("_id", new ObjectId(categoryId.trim()));
                } catch (IllegalArgumentException ex) {
                    query = new Document("_id", categoryId.trim());
                }
                exchange.getIn().setBody(query);
                Document projection = new Document("categoryName", 1).append("_id", 0);
                exchange.getIn().setHeader("CamelMongoDbFieldsProjection", projection);
                logger.debug("Querying categoryId {} with projection {}", categoryId, projection);
            } else {
                logger.warn("Item {} has no categoryId or is empty", itemId);
                exchange.setProperty("category", new Document("categoryName", "unknown"));
            }
        }
    }

    public void captureCategoryResult(Exchange exchange) {
        Document categoryDoc = exchange.getIn().getBody(Document.class);
        if (categoryDoc == null) {
            logger.warn("Category document not found for item {}", exchange.getProperty("itemId"));
            categoryDoc = new Document("categoryName", "unknown");
        }
        exchange.setProperty("category", categoryDoc);
        logger.info("Captured category for item {}: {}", exchange.getProperty("itemId"), categoryDoc);
    }

    public void mapItemData(Exchange exchange) {
        Document itemDoc = exchange.getProperty("item", Document.class);
        Document categoryDoc = exchange.getProperty("category", Document.class);
        String itemId = exchange.getProperty("itemId", String.class);
        if (itemId == null) {
            logger.error("itemId is null, cannot map item data");
            return;
        }

        String categoryName = (categoryDoc != null && categoryDoc.getString("categoryName") != null)
            ? categoryDoc.getString("categoryName") : "unknown";
        String categoryId = itemDoc.getString("categoryId");

        logger.info("Mapping item {}: categoryName={}", itemId, categoryName);

        // TrendXml
        TrendXml trendXml = new TrendXml();
        TrendXml.Category category = new TrendXml.Category();
        TrendXml.CategoryName catName = new TrendXml.CategoryName();
        TrendXml.Item item = new TrendXml.Item();

        // Populate Item
        item.setItemId(itemId);
        item.setCategoryId(categoryId != null ? categoryId : "unknown");
        Document stock = itemDoc.get("stockDetails", Document.class);
        item.setAvailableStock(stock != null ? stock.getInteger("availableStock", 0) : 0);
        Document price = itemDoc.get("itemPrice", Document.class);
        if (price != null && price.get("sellingPrice") != null) {
            Object priceObj = price.get("sellingPrice");
            if (priceObj instanceof Decimal128) {
                item.setSellingPrice(((Decimal128) priceObj).bigDecimalValue().intValue());
            } else if (priceObj instanceof Number) {
                item.setSellingPrice(((Number) priceObj).intValue());
            } else {
                item.setSellingPrice(0);
            }
        } else {
            item.setSellingPrice(0);
        }

        // Populate CategoryName
        catName.setName(categoryName.toLowerCase());
        catName.setValue(categoryName.toUpperCase());

        // Populate Category
        category.setId(categoryId != null ? categoryId : "unknown");
        category.setCategoryName(catName);
        category.setItem(item);

        // Populate TrendXml
        trendXml.setCategory(category);

        // ReviewXml
        ReviewXml reviewXml = new ReviewXml();
        reviewXml.setItemId(itemId);
        List<ReviewXml.Review> reviews = new ArrayList<>();
        List<Document> reviewDocs = itemDoc.getList("review", Document.class, Collections.emptyList());
        for (Document r : reviewDocs) {
            ReviewXml.Review review = new ReviewXml.Review();
            review.setReviewrating(r.getInteger("rating"));
            review.setReviewcomment(r.getString("comment"));
            reviews.add(review);
        }
        reviewXml.setReviews(reviews);

        // StoreJson
        StoreJson storeJson = new StoreJson();
        storeJson.set_id(itemId);
        storeJson.setItemName(itemDoc.getString("itemName"));
        storeJson.setCategoryName(categoryName);
        storeJson.setSpecialProduct(itemDoc.getBoolean("specialProduct", false));
        Document stockDoc = itemDoc.get("stockDetails", Document.class);
        Map<String, Object> stockMap = stockDoc != null ? stockDoc : new HashMap<>();
        storeJson.setStockDetails(stockMap);

        Document priceDoc = itemDoc.get("itemPrice", Document.class);
        if (priceDoc != null) {
            Map<String, Object> priceMap = new LinkedHashMap<>();
            Object basePrice = priceDoc.get("basePrice");
            Object sellingPrice = price.get("sellingPrice");
            if (basePrice instanceof Decimal128) {
                priceMap.put("basePrice", ((Decimal128) basePrice).bigDecimalValue());
            } else {
                priceMap.put("basePrice", basePrice);
            }
            if (sellingPrice instanceof Decimal128) {
                priceMap.put("sellingPrice", ((Decimal128) sellingPrice).bigDecimalValue());
            } else {
                priceMap.put("sellingPrice", sellingPrice);
            }
            storeJson.setItemPrice(priceMap);
        }

        // Set mapped data and preserve itemId
        exchange.setProperty("trendXml", trendXml);
        exchange.setProperty("reviewXml", reviewXml);
        exchange.setProperty("storeJson", storeJson);
        exchange.setProperty("originalItemId", itemId);
        logger.debug("Preserved itemId after mapping: {}", itemId);
    }

    public void prepareTrendXml(Exchange exchange) {
        TrendXml trendXml = exchange.getProperty("trendXml", TrendXml.class);
        if (trendXml == null || trendXml.getCategory() == null || trendXml.getCategory().getItem() == null) {
            logger.warn("trendXml is null or invalid, skipping");
            exchange.getIn().setBody(null);
            return;
        }
        exchange.getIn().setHeader("CamelFileName", String.format("trend_%s.xml", trendXml.getCategory().getItem().getItemId()));
        exchange.getIn().setBody(trendXml);
        logger.debug("Prepared trend XML for item: {}", trendXml.getCategory().getItem().getItemId());
    }

    public void prepareStoreJson(Exchange exchange) {
        StoreJson storeJson = exchange.getProperty("storeJson", StoreJson.class);
        String itemId = exchange.getProperty("originalItemId", String.class);
        if (storeJson == null || storeJson.get_id() == null) {
            logger.warn("storeJson is null or invalid for itemId: {}, skipping", itemId);
            exchange.getIn().setBody(null);
            return;
        }
        exchange.getIn().setHeader("CamelFileName", String.format("storefront_%s.json", storeJson.get_id()));
        exchange.getIn().setBody(storeJson);
        exchange.setProperty("itemId", itemId); // Restore itemId
        logger.debug("Prepared store JSON for item: {}, restored itemId: {}", storeJson.get_id(), itemId);
    }

    public void prepareReviewXml(Exchange exchange) {
        ReviewXml reviewXml = exchange.getProperty("reviewXml", ReviewXml.class);
        String itemId = exchange.getProperty("originalItemId", String.class);
        if (reviewXml == null || reviewXml.getItemId() == null) {
            logger.warn("reviewXml is null or invalid for itemId: {}, skipping", itemId);
            exchange.getIn().setBody(null);
            return;
        }
        exchange.getIn().setHeader("CamelFileName", String.format("review_%s.xml", reviewXml.getItemId()));
        exchange.getIn().setBody(reviewXml);
        exchange.setProperty("itemId", itemId); // Restore itemId
        logger.debug("Prepared review XML for item: {}, restored itemId: {}", reviewXml.getItemId(), itemId);
    }

}