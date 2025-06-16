package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.model.ItemResult;
import org.apache.camel.Exchange;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.UncategorizedJmsException;

import javax.jms.JMSException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AsyncInventoryUpdateProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AsyncInventoryUpdateProcessor.class);

    public void handleException(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String correlationId = exchange.getProperty("correlationId", String.class);
        String errorMessage;
        int httpStatus;

        if (exception instanceof UncategorizedJmsException || exception instanceof JMSException) {
            errorMessage = "ActiveMQ failure";
            httpStatus = 503;
            logger.warn("JMS error for correlationId {}: {}", correlationId, errorMessage);
        } else if (exception instanceof InventoryValidationException) {
            errorMessage = exception.getMessage();
            httpStatus = 400;
            logger.warn("Validation error for correlationId {}: {}", correlationId, errorMessage);
        } else {
            errorMessage = exception != null ? exception.getMessage() : "Unknown error";
            httpStatus = 500;
            logger.error("Error for correlationId {}: {}", correlationId, errorMessage, exception);
        }
        exchange.removeProperty("correlationId");
        exchange.removeProperty("inventoryList");
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus);
        exchange.getIn().setBody(Map.of(
                "status", "error",
                "message", errorMessage,
                "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }

    public void handleQueueException(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
        String errorMessage = exception != null ? exception.getMessage() : "Unknown error";
        logger.error("Queue error for correlationId {}: {}", correlationId, errorMessage, exception);
        Map<String, Object> auditRecord = new HashMap<>();
        auditRecord.put("_id", UUID.randomUUID().toString());
        auditRecord.put("correlationId", correlationId);
        auditRecord.put("status", "error");
        auditRecord.put("message", "Queue processing failed: " + errorMessage);
        auditRecord.put("results", Collections.emptyList());
        auditRecord.put("timestamp", LocalDateTime.now().toString());
        auditRecord.put("itemCount", exchange.getProperty("itemCount", 0));
        exchange.getIn().setBody(auditRecord);
    }

    public void storeSummaryAuditRecord(Exchange exchange) {
        String correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
        List<ItemResult> itemResults = exchange.getProperty("itemResults", List.class);
        Integer itemCount = exchange.getProperty("itemCount", Integer.class);
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);

        Map<String, Object> auditRecord = new HashMap<>();
        auditRecord.put("_id", UUID.randomUUID().toString());
        auditRecord.put("correlationId", correlationId);
        auditRecord.put("timestamp", LocalDateTime.now().toString());
        auditRecord.put("itemCount", itemCount != null ? itemCount : 0);

        if (exception != null) {
            auditRecord.put("status", "error");
            auditRecord.put("message", "Transaction failed: " + exception.getMessage());
            auditRecord.put("results", Collections.emptyList());
        } else if (itemResults != null && !itemResults.isEmpty()) {
            long successCount = itemResults.stream().filter(r -> "success".equals(r.getStatus())).count();
            auditRecord.put("status", successCount == itemResults.size() ? "completed" : "partial");
            auditRecord.put("results", itemResults.stream()
                    .map(r -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("itemId", r.getItemId());
                        result.put("status", r.getStatus());
                        result.put("message", r.getMessage());
                        return result;
                    })
                    .collect(Collectors.toList()));
        } else {
            auditRecord.put("status", "error");
            auditRecord.put("message", "No items processed");
            auditRecord.put("results", Collections.emptyList());
        }
        exchange.getIn().setBody(auditRecord);
        logger.info("Stored summary audit for correlationId: {}", correlationId);
    }

}