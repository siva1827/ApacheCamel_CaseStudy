package com.UST.fileExport.route;

import com.UST.fileExport.config.ApplicationConstants;
import com.UST.fileExport.processor.ControlRefProcessor;
import com.UST.fileExport.processor.ItemProcessor;
import com.UST.fileExport.model.ReviewXml;
import com.UST.fileExport.model.StoreJson;
import com.UST.fileExport.model.TrendXml;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class FileExportRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("timer:fileExport?period={{scheduler.interval}}")
                .routeId("fileExport")
                .bean(ItemProcessor.class, "setCurrentTimestamp")
                .to("direct:fetchControlRef")
                .to("direct:processItems")
                .log("File export completed");

        from("direct:fetchControlRef")
                .routeId("fetchControlRef")
                .bean(ControlRefProcessor.class, "fetchControlRefs")
                .to("mongodb:mongoBean?database={{camel.component.mongodb.database}}&collection=" + ApplicationConstants.MONGO_CONTROL_REF_COLLECTION + "&operation=aggregate")
                .bean(ControlRefProcessor.class, "processControlRefs")
                .log("Fetched controlRefMap with ${exchangeProperty.controlRefMap.size()} entries");

        from("direct:processItems")
                .routeId("processItems")
//                .throttle(20).timePeriodMillis(60000).asyncDelayed()
                .bean(ItemProcessor.class, "prepareItemQuery")
                .to("mongodb:mongoBean?database={{camel.component.mongodb.database}}&collection=" + ApplicationConstants.MONGO_ITEM_COLLECTION + "&operation=findAll")
                .bean(ItemProcessor.class, "filterValidItems")
                .bean(ItemProcessor.class, "logFetchedItems")
                .split(body()).aggregationStrategy((oldExchange, newExchange) -> newExchange)
                .bean(ItemProcessor.class, "enrichWithCategory")
                .log(LoggingLevel.DEBUG, "Item ID after enrich: ${exchangeProperty.itemId}")
                .to("mongodb:mongoBean?database={{camel.component.mongodb.database}}&collection=" + ApplicationConstants.MONGO_CATEGORY_COLLECTION + "&operation=findOneByQuery")
                .log(LoggingLevel.DEBUG, "Category query result for item ${exchangeProperty.itemId}: ${body}")
                .bean(ItemProcessor.class, "captureCategoryResult")
                .bean(ItemProcessor.class, "mapItemData")
                .log(LoggingLevel.DEBUG, "Item ID after mapItemData: ${exchangeProperty.itemId}")
                .multicast().parallelProcessing()
                .to("direct:writeTrendXml", "direct:writeReviewXml", "direct:writeStoreJson")
                .end()
                .setHeader("ControlRefId", simple("${exchangeProperty.itemId}"))
                .log(LoggingLevel.DEBUG, "ControlRefId before update: ${header.ControlRefId}")
                .to("direct:updateControlRef")
                .end();

        from("direct:writeTrendXml")
                .routeId("writeTrendXml")
                .throttle(100).timePeriodMillis(60000).asyncDelayed()
                .bean(ItemProcessor.class, "prepareTrendXml")
                .choice()
                .when(body().isNotNull())
                .marshal(new org.apache.camel.converter.jaxb.JaxbDataFormat(TrendXml.class.getPackage().getName()))
                .to("file:C:/export/trend?fileName=${header.CamelFileName}")
                .log("Saved trend XML: ${header.CamelFileName}")
                .otherwise()
                .log("Skipping null trend XML")
                .end();

        from("direct:writeReviewXml")
                .routeId("writeReviewXml")
                .throttle(100).timePeriodMillis(60000).asyncDelayed()
                .bean(ItemProcessor.class, "prepareReviewXml")
                .choice()
                .when(body().isNotNull())
                .marshal(new org.apache.camel.converter.jaxb.JaxbDataFormat(ReviewXml.class.getPackage().getName()))
                .to("file:C:/export/reviews?fileName=${header.CamelFileName}")
                .log("Saved review XML: ${header.CamelFileName}")
                .otherwise()
                .log("Skipping null review XML")
                .end();

        from("direct:writeStoreJson")
                .routeId("writeStoreJson")
                .throttle(100).timePeriodMillis(60000).asyncDelayed()
                .bean(ItemProcessor.class, "prepareStoreJson")
                .choice()
                .when(body().isNotNull())
                .marshal().json(JsonLibrary.Jackson)
                .to("file:C:/export/storefront?fileName=${header.CamelFileName}")
                .log("Saved store JSON: ${header.CamelFileName}")
                .otherwise()
                .log("Skipping null store JSON")
                .end();

        from("direct:updateControlRef")
                .routeId("updateControlRef")
                .setHeader("ControlRefOperation", constant("update"))
                .log(LoggingLevel.DEBUG, "Updating ControlRef for item: ${header.ControlRefId}")
                .bean(ControlRefProcessor.class, "updateControlRef")
                .choice()
                .when(body().isNotNull())
                .process(exchange -> {
                    String itemId = exchange.getIn().getHeader("ControlRefId", String.class);
                    String currentTs = exchange.getProperty("currentTs", String.class);
                    Document query = new Document("_id", itemId);
                    Document update = new Document("$set", new Document("lastProcessTs", currentTs));
                    exchange.getIn().setBody(Arrays.asList(query, update));
                    exchange.getIn().setHeader("CamelMongoDbUpsert", true);
                })
                .to("mongodb:mongoBean?database={{camel.component.mongodb.database}}&collection=" + ApplicationConstants.MONGO_CONTROL_REF_COLLECTION + "&operation=update")
                .log("Updated ControlRef for item: ${header.ControlRefId}")
                .otherwise()
                .log("Skipped ControlRef update for item: ${header.ControlRefId} (null)")
                .end();
    }
}