package com.UST.ItemBridge.route;

import com.UST.ItemBridge.config.ApplicationConstants;
import com.UST.ItemBridge.pojo.ItemResponse;
import com.UST.ItemBridge.processor.ErrorResponseProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ItemFetchRoute extends RouteBuilder {

    @Value("${http.retry.max}")
    private int httpRetryMax;

    @Value("${http.retry.delay}")
    private long httpRetryDelay;

    @Value("${http.retry.backoff}")
    private double httpRetryBackoff;

    @Override
    public void configure() throws Exception {
        // Error handling
        onException(org.apache.hc.client5.http.HttpHostConnectException.class)
            .maximumRedeliveries(httpRetryMax)
            .redeliveryDelay(httpRetryDelay)
            .backOffMultiplier(httpRetryBackoff)
            .useExponentialBackOff()
            .retryAttemptedLogLevel(LoggingLevel.WARN)
            .log(LoggingLevel.ERROR, "HTTP connection refused: ${exception.message}")
            .handled(true)
            .process(new ErrorResponseProcessor())
            .marshal().json(JsonLibrary.Jackson)
            .to("file:C:/store/items?fileName=error-${header.itemId}.json");

        // REST configuration
        restConfiguration()
                .component(ApplicationConstants.REST_COMPONENT)
                .contextPath("/camel")
                .host(ApplicationConstants.REST_HOST)
                .port(ApplicationConstants.REST_PORT)
                .apiProperty("api.title", "MyCart Item Fetch API")
                .apiProperty("api.version", "1.0");

        // REST endpoint: GET /camel/mycart/fetch/item/{itemId}
        rest("/mycart/fetch/item")
                .get("/{itemId}")
                .routeId("RouteItemFetch")
                .to("direct:fetchItem");


        // Fetch item details
        from("direct:fetchItem")
            .routeId("fetchItem")
            .log(LoggingLevel.INFO, "Fetching item with ID: ${header.itemId}")
            .setHeader("CamelHttpMethod", constant("GET"))
            .setBody(simple("${header.itemId}"))
            .toD("http://localhost:8081/camel/mycart/item/${body}?bridgeEndpoint=true&throwExceptionOnFailure=false&preserveHostHeader=false")
            .unmarshal().json(JsonLibrary.Jackson, ItemResponse.class)
            .log(LoggingLevel.INFO, "Fetched item: ${body.itemName}, Category: ${body.categoryName}")
            .marshal().json(JsonLibrary.Jackson)
            .to("file:C:/store/items?fileName=item-${header.itemId}.json")
            .log(LoggingLevel.INFO, "Saved item details to file: item-${header.itemId}.json");

//        // Timer for testing
//        from("timer:testFetch?repeatCount=1")
//            .routeId("testFetch")
//            .setHeader("itemId", constant("item6"))
//            .to("direct:fetchItem");
    }
}