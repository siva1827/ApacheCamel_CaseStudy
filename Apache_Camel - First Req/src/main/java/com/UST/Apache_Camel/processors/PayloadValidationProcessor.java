package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.model.ItemResult;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PayloadValidationProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(PayloadValidationProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Map<String, Object> request = exchange.getIn().getBody(Map.class);
        if (request == null || !request.containsKey("items")) {
            throw new InventoryValidationException("Missing 'items' in inventory payload.");
        }

        Object itemsObj = request.get("items");
        if (!(itemsObj instanceof List)) {
            throw new InventoryValidationException("'items' must be a list.");
        }

        List<?> items = (List<?>) itemsObj;
        if (items.isEmpty()) {
            throw new InventoryValidationException("Inventory items list is empty.");
        }

        for (Object item : items) {
            if (!(item instanceof Map)) {
                throw new InventoryValidationException("Each item must be an object.");
            }
        }

        List<ItemResult> itemResults = new ArrayList<>();
        exchange.setProperty("itemResults", itemResults);
        exchange.setProperty("inventoryList", items);
        logger.info("Initialized itemResults, processing {} items", items.size());
    }
}