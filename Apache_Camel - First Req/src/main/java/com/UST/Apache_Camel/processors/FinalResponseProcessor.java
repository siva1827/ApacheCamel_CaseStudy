package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.model.ItemResult;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FinalResponseProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(FinalResponseProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        List<ItemResult> itemResults = exchange.getProperty("itemResults", List.class);
        if (itemResults == null) {
            itemResults = new ArrayList<>();
            logger.warn("itemResults is null in final response, initializing as empty list");
        }
        String status = itemResults.stream().allMatch(r -> "success".equals(r.getStatus())) ? "completed" : "partial";
        logger.info("Final response itemResults: {}, status: {}", itemResults, status);
        exchange.getMessage().setBody(Map.of(
                "status", status,
                "results", itemResults
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
    }
}