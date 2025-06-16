package com.UST.Apache_Camel.processors;

import com.mongodb.MongoException;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MongoRetryProcessor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(MongoRetryProcessor.class);

    @Value("${mongodb.retry.maxRetries:3}")
    private int maxRetries;

    @Value("${mongodb.retry.delays:60000,180000,360000}")
    private String retryDelays;

    @Override
    public void process(Exchange exchange) throws Exception {
        List<Long> delays = Arrays.stream(retryDelays.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());

        String mongoEndpoint = exchange.getProperty("mongoEndpoint", String.class);
        if (mongoEndpoint == null) {
            throw new IllegalStateException("Mongo endpoint not set in exchange property");
        }

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                exchange.getContext().createProducerTemplate().send(mongoEndpoint, exchange);
                logger.info("MongoDB operation successful on attempt {}", attempt + 1);
                return;
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                if (!(cause instanceof MongoException) || !isTransientError((MongoException) cause) || attempt == maxRetries) {
                    logger.error("Non-transient error or max retries reached on attempt {}: {}", attempt + 1, cause.getMessage());
                    throw e;
                }
                long delay = delays.get(Math.min(attempt, delays.size() - 1));
                logger.warn("Transient MongoDB error on attempt {}. Retrying after {} ms: {}", 
                            attempt + 1, delay, cause.getMessage());
                Thread.sleep(delay);
            }
        }
    }

    private boolean isTransientError(MongoException e) {
        return e.getCode() == 50 || 
               e.getMessage().contains("Timed out") ||
               e.getMessage().contains("connection") ||
               e instanceof com.mongodb.MongoTimeoutException ||
               e instanceof com.mongodb.MongoSocketException;
    }
}