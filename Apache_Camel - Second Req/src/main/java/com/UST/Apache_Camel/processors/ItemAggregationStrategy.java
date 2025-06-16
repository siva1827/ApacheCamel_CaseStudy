package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.model.ItemResult;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ItemAggregationStrategy implements AggregationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ItemAggregationStrategy.class);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        ItemResult itemResult = newExchange.getProperty("itemResult", ItemResult.class);
        if (itemResult == null) {
            logger.warn("No itemResult found in newExchange for itemId: {}", newExchange.getProperty("itemId"));
            return oldExchange != null ? oldExchange : newExchange;
        }

        if (oldExchange == null) {
            List<ItemResult> results = new ArrayList<>();
            results.add(itemResult);
            newExchange.setProperty("itemResults", results);
            logger.debug("Initialized itemResults with itemId: {}", itemResult.getItemId());
            return newExchange;
        }

        List<ItemResult> results = oldExchange.getProperty("itemResults", List.class);
        results.add(itemResult);
        oldExchange.setProperty("itemResults", results);
        logger.debug("Added itemResult for itemId: {} to itemResults, total: {}", itemResult.getItemId(), results.size());
        return oldExchange;
    }
}