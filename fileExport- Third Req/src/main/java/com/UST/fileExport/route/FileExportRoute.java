package com.UST.fileExport.route;

import com.UST.fileExport.config.ApplicationConstants;
import com.UST.fileExport.model.ReviewXml;
import com.UST.fileExport.model.TrendXml;
import com.UST.fileExport.processor.ControlRefProcessor;
import com.UST.fileExport.bean.ItemBean;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.stereotype.Component;

@Component
public class FileExportRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("timer:fileExport?period={{scheduler.interval}}")
            .routeId(ApplicationConstants.ROUTE_FILE_EXPORT)
            .bean(ItemBean.class, "setCurrentTimestamp")
            .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_FETCH_CONTROL_REF)
            .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_PROCESS_ITEMS)
            .log("File export completed");

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_FETCH_CONTROL_REF)
            .routeId(ApplicationConstants.ROUTE_FETCH_CONTROL_REF)
            .bean(ControlRefProcessor.class, "fetchControlRefs")
            .to(String.format(ApplicationConstants.MONGO_CONTROL_REF_AGGREGATE,
                ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CONTROL_REF_COLLECTION))
            .bean(ControlRefProcessor.class, "processControlRefs")
            .log("Fetched controlRefMap with ${exchangeProperty.controlRefMap.size()} entries");

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_PROCESS_ITEMS)
            .routeId(ApplicationConstants.ROUTE_PROCESS_ITEMS)
            .bean(ItemBean.class, "prepareItemQuery")
            .to(String.format(ApplicationConstants.MONGO_ITEM_FIND_ALL,
                ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_COLLECTION))
            .bean(ItemBean.class, "filterValidItems")
            .bean(ItemBean.class, "logFetchedItems")
            .split(body()).aggregationStrategy((oldExchange, newExchange) -> newExchange)
            .bean(ItemBean.class, "enrichWithCategory")
            .log(LoggingLevel.DEBUG, "Item ID after enrich: ${exchangeProperty.itemId}")
            .to(String.format(ApplicationConstants.MONGO_CATEGORY_FIND_ONE_BY_QUERY,
                ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_COLLECTION))
            .log(LoggingLevel.DEBUG, "Category query result for item ${exchangeProperty.itemId}: ${body}")
            .bean(ItemBean.class, "captureCategoryResult")
            .bean(ItemBean.class, "mapItemData")
            .log(LoggingLevel.DEBUG, "Item ID after mapItemData: ${exchangeProperty.itemId}")
            .multicast().parallelProcessing()
            .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_TREND_XML,
                ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_REVIEW_XML,
                ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_STORE_JSON)
            .end()
            .setHeader("ControlRefId", simple("${exchangeProperty.itemId}"))
            .log(LoggingLevel.DEBUG, "ControlRefId before update: ${header.ControlRefId}")
            .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_UPDATE_CONTROL_REF)
            .end();

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_TREND_XML)
            .routeId(ApplicationConstants.ROUTE_WRITE_TREND_XML)
            .bean(ItemBean.class, "prepareTrendXml")
            .choice()
            .when(body().isNotNull())
            .marshal().jaxb(TrendXml.class.getPackage().getName())
            .to("file:C:/export/trend?fileName=${header.CamelFileName}")
            .log("Saved trend XML: ${header.CamelFileName}")
            .otherwise()
            .log("Skipping null trend XML")
            .end();

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_REVIEW_XML)
            .routeId(ApplicationConstants.ROUTE_WRITE_REVIEW_XML)
            .bean(ItemBean.class, "prepareReviewXml")
            .choice()
            .when(body().isNotNull())
            .marshal().jaxb(ReviewXml.class.getPackage().getName())
            .to("file:C:/export/reviews?fileName=${header.CamelFileName}")
            .log("Saved review XML: ${header.CamelFileName}")
            .otherwise()
            .log("Skipping null review XML")
            .end();

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_WRITE_STORE_JSON)
            .routeId(ApplicationConstants.ROUTE_WRITE_STORE_JSON)
            .bean(ItemBean.class, "prepareStoreJson")
            .choice()
            .when(body().isNotNull())
            .marshal().json(JsonLibrary.Jackson)
            .to("file:C:/export/storefront?fileName=${header.CamelFileName}")
            .log("Saved store JSON: ${header.CamelFileName}")
            .otherwise()
            .log("Skipping null store JSON")
            .end();

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_UPDATE_CONTROL_REF)
            .routeId(ApplicationConstants.ROUTE_UPDATE_CONTROL_REF)
            .setHeader("ControlRefOperation", constant("update"))
            .log(LoggingLevel.DEBUG, "Updating ControlRef for item: ${header.ControlRefId}")
            .bean(ControlRefProcessor.class, "prepareUpdate")
            .choice()
            .when(body().isNotNull())
            .to(String.format(ApplicationConstants.MONGO_CONTROL_REF_UPDATE,
                ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CONTROL_REF_COLLECTION))
            .log("Updated ControlRef for item: ${header.ControlRefId}")
            .otherwise()
            .log("Skipped ControlRef update for item: ${header.ControlRefId} (null)")
            .end();
    }
}