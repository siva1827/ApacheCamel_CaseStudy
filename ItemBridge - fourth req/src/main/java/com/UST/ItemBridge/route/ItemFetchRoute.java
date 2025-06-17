package com.UST.ItemBridge.route;

import com.UST.ItemBridge.config.ApplicationConstants;
import com.UST.ItemBridge.pojo.ItemResponse;
import com.UST.ItemBridge.processor.ErrorResponseProcessor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
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
            .to(ApplicationConstants.FILE_PATH_ITEMS + "?fileName=error-${header.itemId}.json");

        // REST configuration
        restConfiguration()
                .component(ApplicationConstants.REST_COMPONENT)
                .contextPath("/camel")
                .host(ApplicationConstants.REST_HOST)
                .port(ApplicationConstants.REST_PORT)
//                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("json.in.disableFeatures", "FAIL_ON_UNKNOWN_PROPERTIES")
                .apiProperty("api.title", "MyCart Item Fetch API")
                .apiProperty("api.version", "1.0");

        // REST endpoint: GET /camel/mycart/fetch/item/{itemId}
        rest("/mycart/fetch/item")
                .get("/{itemId}")
                .routeId(ApplicationConstants.ROUTE_ITEM_FETCH)
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_FETCH_ITEM);

        // Fetch item details
        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ROUTE_FETCH_ITEM)
            .routeId(ApplicationConstants.ROUTE_FETCH_ITEM)
            .log(LoggingLevel.INFO, "Fetching item with ID: ${header.itemId}")
            .setHeader("CamelHttpMethod", constant("GET"))
            .setBody(simple("${header.itemId}"))
            .toD("http://localhost:8081/camel/mycart/item/${body}?bridgeEndpoint=true&throwExceptionOnFailure=false&preserveHostHeader=false")
            .unmarshal().json(JsonLibrary.Jackson, ItemResponse.class)
            .log(LoggingLevel.INFO, "Fetched item: ${body.itemName}, Category: ${body.categoryName}")
            .marshal().json(JsonLibrary.Jackson);
//            .to(ApplicationConstants.FILE_PATH_ITEMS + "?fileName=item-${header.itemId}.json")
//            .log(LoggingLevel.INFO, "Saved item details to file: item-${header.itemId}.json");
    }
}