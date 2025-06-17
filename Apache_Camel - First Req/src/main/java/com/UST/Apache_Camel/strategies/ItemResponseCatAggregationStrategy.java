package com.UST.Apache_Camel.strategies;

import com.UST.Apache_Camel.model.ItemResponseCat;
import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ItemResponseCatAggregationStrategy implements AggregationStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ItemResponseCatAggregationStrategy.class);

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        String routeId = newExchange.getFromRouteId();
        ItemResponseCat newItem = newExchange.getIn().getBody(ItemResponseCat.class);
        List<ItemResponseCat> items;

        if (oldExchange == null) {
            items = new ArrayList<>();
            if (newItem != null) {
                items.add(newItem);
                logger.debug("Added ItemResponseCat for item {} to new aggregation for route {}", newItem.getId(), routeId);
            }
            newExchange.getIn().setBody(items);
            return newExchange;
        } else {
            items = oldExchange.getIn().getBody(List.class);
            if (newItem != null) {
                items.add(newItem);
                logger.debug("Added ItemResponseCat for item {} to existing aggregation for route {}", newItem.getId(), routeId);
            }
            oldExchange.getIn().setBody(items);
            return oldExchange;
        }
    }
}