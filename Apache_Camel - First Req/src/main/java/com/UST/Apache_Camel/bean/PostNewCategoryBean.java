package com.UST.Apache_Camel.bean;

import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.model.Category;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PostNewCategoryBean {
    private static final Logger logger = LoggerFactory.getLogger(PostNewCategoryBean.class);

    public void validateCategory(Exchange exchange) throws Exception {
        Category category = exchange.getIn().getBody(Category.class);
        exchange.setProperty("newCategory", category);

        if (category == null || category.getId() == null || category.getId().isBlank() ||
            category.getCategoryName() == null || category.getCategoryName().isBlank()) {
            throw new IllegalArgumentException("Category ID and Category Name must not be empty");
        }

        String categoryId = category.getId();
        exchange.getIn().setBody(categoryId);
        logger.debug("Validated category and set categoryId for findById: {}", categoryId);
    }

    public void prepareCategoryForInsert(Exchange exchange) {
        Category category = exchange.getProperty("newCategory", Category.class);

        // Convert Category to Document for MongoDB
        Document document = new Document();
        document.append("_id", category.getId());
        document.append("categoryName", category.getCategoryName());
        if (category.getCategoryDep() != null) {
            document.append("categoryDep", category.getCategoryDep());
        }
        if (category.getCategoryTax() != null) {
            document.append("categoryTax", category.getCategoryTax());
        }

        exchange.getIn().setBody(document);
        logger.debug("Prepared category for insert: {}", category.getId());
    }

    public void handleInsertSuccess(Exchange exchange) {
        Category category = exchange.getProperty("newCategory", Category.class);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 201);
        exchange.getIn().setBody(Map.of(
                "message", "Category inserted successfully",
                "categoryId", category.getId()
        ));
        logger.info("Successfully inserted category: {}", category.getId());
    }

    public void handleExistingCategory(Exchange exchange) {
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        exchange.getIn().setBody(Map.of("message", ApplicationConstants.ERROR_CATEGORY_ALREADY_EXISTS));
        logger.warn("Category already exists: {}", exchange.getProperty("newCategory", Category.class).getId());
    }
}