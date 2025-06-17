package com.UST.fileExport.processor;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
public class ControlRefProcessor implements Processor {
    private static final Logger logger = LoggerFactory.getLogger(ControlRefProcessor.class);
    private static final String OPERATION_HEADER = "ControlRefOperation";
    private final SimpleDateFormat formatter;

    public ControlRefProcessor() {
        this.formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String operation = exchange.getIn().getHeader(OPERATION_HEADER, "fetch", String.class);
        String itemId = exchange.getIn().getHeader("ControlRefId", String.class);
        logger.debug("Processing operation: {}, itemId: {}", operation, itemId);

        if ("update".equalsIgnoreCase(operation)) {
            updateControlRef(exchange, itemId);
        } else {
            fetchControlRefs(exchange);
        }
    }

    public void fetchControlRefs(Exchange exchange) {
        List<Document> pipeline = List.of(
            new Document("$project", new Document()
                .append("_id", 1)
                .append("lastProcessTs", 1))
        );
        exchange.getIn().setBody(pipeline);
        exchange.setProperty("controlRefQuery", pipeline);
        logger.debug("Prepared aggregation pipeline to fetch ControlRefs: {}", pipeline);
    }

    public void processControlRefs(Exchange exchange) {
        List<Document> controlRefs = exchange.getIn().getBody(List.class);
        Map<String, Date> controlRefMap = new HashMap<>();

        if (controlRefs == null || controlRefs.isEmpty()) {
            logger.warn("No ControlRef documents found, proceeding with empty map");
            exchange.setProperty("controlRefMap", controlRefMap);
            return;
        }

        for (Document doc : controlRefs) {
            String itemId = doc.getString("_id");
            String lastProcessTs = doc.getString("lastProcessTs");
            if (itemId != null && lastProcessTs != null) {
                try {
                    synchronized (formatter) {
                        Date lastProcessDate = formatter.parse(lastProcessTs);
                        controlRefMap.put(itemId, lastProcessDate);
                        logger.debug("Mapped ControlRef: itemId={}, lastProcessTs={}", itemId, lastProcessTs);
                    }
                } catch (ParseException e) {
                    logger.error("Invalid lastProcessTs format for itemId {}: {}", itemId, lastProcessTs, e);
                }
            } else {
                logger.warn("Invalid ControlRef document: {}", doc);
            }
        }

        logger.info("Fetched {} ControlRef documents", controlRefs.size());
        exchange.setProperty("controlRefMap", controlRefMap);
        logger.debug("Set controlRefMap with {} entries", controlRefMap.size());
    }

    public void updateControlRef(Exchange exchange, String itemId) {
        if (itemId == null) {
            logger.warn("No itemId provided for ControlRef update, skipping");
            exchange.getIn().setBody(null);
            return;
        }

        if (itemId.startsWith("{\"_id\":")) {
            logger.warn("Received JSON string as itemId: {}, expected clean string", itemId);
        }

        logger.debug("Preparing ControlRef update for itemId: {}", itemId);
        String currentTs = exchange.getProperty("currentTs", String.class);
        if (currentTs == null) {
            logger.warn("currentTs is null, cannot update ControlRef for itemId: {}", itemId);
            exchange.getIn().setBody(null);
            return;
        }

        Document controlRef = new Document()
            .append("_id", itemId)
            .append("lastProcessTs", currentTs);
        exchange.getIn().setBody(controlRef);
        logger.info("Prepared ControlRef update for itemId: {}, lastProcessTs: {}", itemId, currentTs);
    }

    public void prepareUpdate(Exchange exchange) {
        Document controlRef = exchange.getIn().getBody(Document.class);
        if (controlRef == null) {
            logger.warn("ControlRef document is null, skipping update");
            exchange.getIn().setBody(null);
            return;
        }

        String itemId = exchange.getIn().getHeader("ControlRefId", String.class);
        String currentTs = exchange.getProperty("currentTs", String.class);
        if (itemId == null || currentTs == null) {
            logger.warn("Missing itemId or currentTs, cannot prepare update: itemId={}, currentTs={}", itemId, currentTs);
            exchange.getIn().setBody(null);
            return;
        }

        Document query = new Document("_id", itemId);
        Document update = new Document("$set", new Document("lastProcessTs", currentTs));
        exchange.getIn().setBody(Arrays.asList(query, update));
        exchange.getIn().setHeader("CamelMongoDbUpsert", true);
        logger.debug("Prepared MongoDB update for itemId: {}, lastProcessTs: {}", itemId, currentTs);
    }
}