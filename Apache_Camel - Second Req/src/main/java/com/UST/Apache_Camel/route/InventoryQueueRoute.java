package com.UST.Apache_Camel.route;

import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.processors.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InventoryQueueRoute extends RouteBuilder {

    @Override
    public void configure() {
        // REST configuration
        restConfiguration()
                .component(ApplicationConstants.REST_COMPONENT)
                .contextPath("/camel")
                .host(ApplicationConstants.REST_HOST)
                .port(ApplicationConstants.REST_PORT)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("json.in.disableFeatures", "FAIL_ON_UNKNOWN_PROPERTIES");

        // Queue processing
        from(String.format(ApplicationConstants.AMQ_INVENTORY_UPDATE_READ,
                ApplicationConstants.AMQ_INVENTORY_UPDATE_READ_QUEUE))
                .routeId(ApplicationConstants.ROUTE_PROCESS_INVENTORY_QUEUE)
                .onException(Exception.class)
                .handled(true)
                .bean(AsyncInventoryUpdateProcessor.class, "handleQueueException")
                .bean(AsyncInventoryUpdateProcessor.class, "storeSummaryAuditRecord")
                .to(String.format(ApplicationConstants.MONGO_INVENTORY_AUDIT_INSERT,
                    ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_INVENTORY_AUDIT_WRITE_COLLECTION))
                .log("Stored summary audit after exception for correlationId: ${header.JMSCorrelationID}")
                .end()
                .log("Processing inventory update list, correlationId: ${header.JMSCorrelationID}")
                .process(new InventoryListValidationProcessor())
                .split(simple("${exchangeProperty.inventoryList}"), new ItemAggregationStrategy())
                .streaming()
                .parallelProcessing()
                .doTry()
                .bean(ItemProcessor.class, "processItem")
                .log("Processing item: ${exchangeProperty.itemId}")
                .bean(ItemProcessor.class, "setItemId")
                .to(String.format(ApplicationConstants.MONGO_ITEM_FIND_BY_ID,
                    ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_READ_COLLECTION))
                .bean(ItemProcessor.class, "validateAndUpdateItem")
                .to(String.format(ApplicationConstants.MONGO_ITEM_SAVE,
                    ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_WRITE_COLLECTION))
                .bean(ItemProcessor.class, "markSuccess")
                .doCatch(InventoryValidationException.class)
                .bean(ItemProcessor.class, "markFailure")
                .end()
                .log("Completed processing item ${exchangeProperty.itemId}")
                .end()
                .bean(AsyncInventoryUpdateProcessor.class, "storeSummaryAuditRecord")
                .to(String.format(ApplicationConstants.MONGO_INVENTORY_AUDIT_INSERT,
                    ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_INVENTORY_AUDIT_WRITE_COLLECTION))
                .log("Stored summary audit for correlationId: ${header.JMSCorrelationID}");
    }
}