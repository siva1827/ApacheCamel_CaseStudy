package com.UST.Apache_Camel.processors;

import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.mongodb.MongoException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ErrorResponseProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(ErrorResponseProcessor.class);

    @Override
    public void process(Exchange exchange) throws Exception {
        Exception e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
        String errorMessage = e.getMessage();
        logger.warn("Request failed: {}", errorMessage);
        exchange.getMessage().setBody(Map.of(
                "status", "error",
                "message", errorMessage
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
    }

    public void processValidationError(Exchange exchange) {
        InventoryValidationException e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, InventoryValidationException.class);
        String errorMessage = e.getMessage();
        logger.warn("Validation error: {}", errorMessage);
        exchange.getMessage().setBody(Map.of(
                "status", "error",
                "message", errorMessage
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
    }

    public void processMongoError(Exchange exchange) {
        MongoException e = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, MongoException.class);
        String errorMessage = "MongoDB error: " + e.getMessage();
        logger.error("MongoDB error: {}", errorMessage, e);
        exchange.getMessage().setBody(Map.of(
                "status", "error",
                "message", errorMessage
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
    }

    public void processJmsError(Exchange exchange) {
        String errorMessage = "Something went wrong with ActiveMQ";
        logger.warn("JMS error: {}", errorMessage);
        exchange.getMessage().setBody(Map.of(
                "status", "error",
                "message", errorMessage
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 503);
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
    }

    public void processGenericError(Exchange exchange) {
        Throwable t = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Throwable.class);
        String errorMessage = "Unexpected error: " + t.getMessage();
        logger.error("Unexpected error: {}", errorMessage, t);
        exchange.getMessage().setBody(Map.of(
                "status", "error",
                "message", errorMessage
        ));
        exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 500);
        exchange.removeProperty(Exchange.EXCEPTION_CAUGHT);
    }
}