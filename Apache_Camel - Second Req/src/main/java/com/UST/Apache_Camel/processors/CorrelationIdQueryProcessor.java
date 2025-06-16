package com.UST.Apache_Camel.processors;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationIdQueryProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdQueryProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        String correlationId = exchange.getIn().getHeader("correlationId", String.class);
        logger.debug("Processing correlationId: {}", correlationId);
        Document query = new Document("correlationId", correlationId);
        exchange.getIn().setBody(query);
    }
}