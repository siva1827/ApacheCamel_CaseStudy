package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InventoryListValidationProcessor implements Processor {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void process(Exchange exchange) throws InventoryValidationException {
        String body = exchange.getIn().getBody(String.class);

        List<Map<String, Object>> items;
        try {
            items = mapper.readValue(body, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            throw new InventoryValidationException("Invalid JSON in queue message");
        }

        if (items == null || items.isEmpty()) {
            throw new InventoryValidationException("Queue message contains an empty list of items");
        }

        for (Object item : items) {
            if (!(item instanceof Map)) {
                throw new InventoryValidationException("Each item must be a JSON object");
            }
        }

        exchange.setProperty("inventoryList", items);
        exchange.setProperty("itemCount", items.size());
    }
}
