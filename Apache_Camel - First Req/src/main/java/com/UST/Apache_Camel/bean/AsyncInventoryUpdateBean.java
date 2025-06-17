package com.UST.Apache_Camel.bean;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.UncategorizedJmsException;

import javax.jms.JMSException;
import java.io.NotSerializableException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AsyncInventoryUpdateBean {
    private static final Logger logger = LoggerFactory.getLogger(AsyncInventoryUpdateBean.class);

    public void handleException(Exchange exchange) {
        Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String correlationId = exchange.getProperty("correlationId", String.class);
        String errorMessage;
        int httpStatus;

        if (exception instanceof UncategorizedJmsException || exception instanceof JMSException) {
            Throwable cause = exception.getCause();
            if (cause instanceof NotSerializableException) {
                errorMessage = "Serialization error: " + cause.getMessage();
                httpStatus = 400;
                logger.error("Serialization error for correlationId {}: {}", correlationId, errorMessage, exception);
            } else {
                errorMessage = "ActiveMQ connection failed: " + (cause != null ? cause.getMessage() : exception.getMessage());
                httpStatus = 503;
                logger.error("JMS error for correlationId {}: {}", correlationId, errorMessage, exception);
            }
        } else if (exception instanceof InventoryValidationException) {
            errorMessage = exception.getMessage();
            httpStatus = 400;
            logger.error("Validation error for correlationId {}: {}", correlationId, errorMessage, exception);
        } else {
            errorMessage = exception != null ? exception.getMessage() : "Unknown error";
            httpStatus = 500;
            logger.error("Error in async update for correlationId {}: {}", correlationId, errorMessage, exception);
        }

        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatus);
        exchange.getIn().setBody(Map.of(
                "status", "error",
                "message", errorMessage,
                "correlationId", correlationId != null ? correlationId : "unknown"
        ));
    }

    public void initializeCorrelationId(Exchange exchange) {
        String correlationId = UUID.randomUUID().toString();
        exchange.setProperty("correlationId", correlationId);
        exchange.getIn().setHeader("JMSCorrelationID", correlationId);
        logger.info("Generated correlationId: {}", correlationId);
    }



    public void prepareQueueMessage(Exchange exchange) throws InventoryValidationException {
        Map<String, Object> request = exchange.getIn().getBody(Map.class);
        if (request == null || !request.containsKey("items")) {
            throw new InventoryValidationException("Payload must contain an 'items' key.");
        }

        Object itemsObj = request.get("items");
        if (!(itemsObj instanceof List)) {
            throw new InventoryValidationException("'items' must be a list.");
        }

        List<?> items = (List<?>) itemsObj;
        if (items.isEmpty()) {
            throw new InventoryValidationException("Items list cannot be empty.");
        }

        for (Object item : items) {
            if (!(item instanceof Map)) {
                throw new InventoryValidationException("Each item must be an object.");
            }
        }

        // Convert the List<Map<String, Object>> to a JSON string
        ObjectMapper mapper = new ObjectMapper();
        try {
            String jsonPayload = mapper.writeValueAsString(items);
            exchange.getIn().setBody(jsonPayload);  // Set JSON string as body (TextMessage)
        } catch (Exception e) {
            throw new InventoryValidationException("Failed to serialize items to JSON.");
        }

        // Set correlation ID header
        exchange.getIn().setHeader("JMSCorrelationID", exchange.getProperty("correlationId"));
        logger.debug("Prepared queue message for {} items with correlationId: {}", items.size(), exchange.getProperty("correlationId"));
    }


    public void buildEnqueueResponse(Exchange exchange) {
        String correlationId = exchange.getProperty("correlationId", String.class);
        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 202);
        exchange.getIn().setBody(Map.of(
                "status", "enqueued",
                "message", "Items enqueued for async processing",
                "correlationId", correlationId
        ));
        logger.info("Enqueued items with correlationId: {}", correlationId);
    }
}