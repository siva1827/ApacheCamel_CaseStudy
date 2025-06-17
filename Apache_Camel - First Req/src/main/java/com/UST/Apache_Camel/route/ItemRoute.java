package com.UST.Apache_Camel.route;

import com.UST.Apache_Camel.bean.*;
import com.UST.Apache_Camel.config.ApplicationConstants;
import com.UST.Apache_Camel.exception.InventoryValidationException;
import com.UST.Apache_Camel.model.Category;
import com.UST.Apache_Camel.model.Item;
import com.UST.Apache_Camel.processors.*;
import com.UST.Apache_Camel.strategies.ItemAggregationStrategy;
import com.UST.Apache_Camel.strategies.ItemResponseCatAggregationStrategy;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import com.mongodb.MongoTimeoutException;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.jms.JMSException;

import static org.apache.camel.model.rest.RestParamType.query;

@Component
public class ItemRoute extends RouteBuilder {

    @Autowired
    private ObjectMapper objectMapper;

    private static final Logger logger = LoggerFactory.getLogger(ItemRoute.class);

    @Value("${app.error.itemNotFound:Item not found}")
    private String itemNotFoundMessage;

    @Value("${app.error.categoryNotFound:Category not found}")
    private String categoryNotFoundMessage;

    @Value("${mongodb.retry.maxRetries:3}")
    private int mongoRetryMax;

    @Value("${mongodb.retry.initialDelay:60000}")
    private long mongoRetryDelay;

    @Value("${mongodb.retry.backOffMultiplier:2.5}")
    private double mongoRetryBackoff;

    @Override
    public void configure() {
        logger.info("Configuring Camel routes for Item Service");

        // Global exception handling for all routes
        onException(InventoryValidationException.class)
                .handled(true)
                .bean(ErrorResponseProcessor.class, "processValidationError")
                .log("Validation error: ${exception.message}")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400));

        onException(MongoException.class)
                .handled(true)
                .log(LoggingLevel.WARN, "Action: ExceptionHandling | Type: System | MongoDB error occurred: ${exception.message}")
                .process(new ErrorResponseProcessor())
                .marshal().json(JsonLibrary.Jackson);

        onException(MongoTimeoutException.class, MongoSocketOpenException.class, MongoSocketReadException.class)
                .maximumRedeliveries(mongoRetryMax)
                .redeliveryDelay(mongoRetryDelay)
                .backOffMultiplier(mongoRetryBackoff)
                .useExponentialBackOff()
                .retryAttemptedLogLevel(LoggingLevel.WARN)
                .log(LoggingLevel.WARN, "Action: ExceptionHandling | Type: System | MongoDB connectivity issue: ${exception.message}")
                .handled(true)
                .process(new ErrorResponseProcessor())
                .marshal().json(JsonLibrary.Jackson);

        onException(Throwable.class)
                .handled(true)
                .bean(ErrorResponseProcessor.class, "processGenericError")
                .log("Unexpected error: ${exception.message} ")
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500));

        restConfiguration()
                .component(ApplicationConstants.REST_COMPONENT)
                .contextPath("/camel")
                .host(ApplicationConstants.REST_HOST)
                .port(ApplicationConstants.REST_PORT)
                .bindingMode(RestBindingMode.json)
                .dataFormatProperty("json.in.disableFeatures", "FAIL_ON_UNKNOWN_PROPERTIES");

        // GET item by itemId
        rest("/mycart/item/{itemId}")
                .get()
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_GET_ITEM_BY_ID);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_GET_ITEM_BY_ID)
                .routeId(ApplicationConstants.ROUTE_GET_ITEM_BY_ID)
                .onException(Exception.class)
                .handled(true)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("Error fetching item: ${exception.message}"))
                .log("Error fetching item: ${exception.message}")
                .end()
                .log("Fetching item with ID: ${header.itemId}")
                .bean(GetItemBean.class, "setItemId")
                .to(String.format(ApplicationConstants.MONGO_ITEM_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_READ_COLLECTION))
                .bean(GetItemBean.class, "processResult")
                .choice()
                .when(exchangeProperty("itemNotFound").isNull())
                .bean(GetItemBean.class, "setCategoryId")
                .setHeader("camelMongoDbFieldProjection", simple("{\"categoryName\": 1, \"_id\": 0}"))
                .to(String.format(ApplicationConstants.MONGO_CATEGORY_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_READ_COLLECTION))
                .bean(GetItemBean.class, "processCategoryResult")
                .endChoice();

        // GET items by categoryId
        rest("/mycart/items/{categoryId}")
                .get()
                .param()
                .name("includeSpecial")
                .type(query)
                .description("Include special items")
                .dataType("boolean")
                .defaultValue("false")
                .endParam()
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_GET_ITEMS_BY_CATEGORY);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_GET_ITEMS_BY_CATEGORY)
                .routeId(ApplicationConstants.ROUTE_GET_ITEMS_BY_CATEGORY)
                .onException(Exception.class)
                .handled(true)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500))
                .setBody(simple("Error fetching items: ${exception.message}"))
                .log("Error fetching items: ${exception.message}")
                .end()
                .log("Fetching items for categoryId: ${header.categoryId}")
                .bean(GetItemsByCategoryBean.class, "buildAggregationPipeline")
                .to(String.format(ApplicationConstants.MONGO_ITEM_AGGREGATE,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_READ_COLLECTION))
                .bean(GetItemsByCategoryBean.class, "processResult")
                .choice()
                .when(exchangeProperty("fetchCategory").isEqualTo(true))
                .setHeader("camelMongoDbFieldProjection", simple("{\"categoryName\": 1, \"_id\": 0}"))
                .to(String.format(ApplicationConstants.MONGO_CATEGORY_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_READ_COLLECTION))
                .bean(GetItemsByCategoryBean.class, "processCategoryResult")
                .otherwise()
                .split(body()).aggregationStrategy(new ItemResponseCatAggregationStrategy())
                .bean(GetItemsByCategoryBean.class, "transformItem")
                .end()
                .endChoice()
                .bean(GetItemsByCategoryBean.class, "buildFinalResponse");

        // POST new item
        rest("/mycart")
                .post()
                .consumes("application/json")
                .type(Item.class)
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_POST_NEW_ITEM);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_POST_NEW_ITEM)
                .routeId(ApplicationConstants.ROUTE_POST_NEW_ITEM)
                .onException(InventoryValidationException.class)
                .handled(true)
                .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(400))
                .setBody(simple("${exception.message}"))
                .log("Validation error: ${exception.message}")
                .end()
                .log("Received new item: ${body}")
                .bean(PostNewItemBean.class, "validateItem")
                .to(String.format(ApplicationConstants.MONGO_ITEM_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_READ_COLLECTION))
                .choice()
                .when(body().isNotNull())
                .bean(PostNewItemBean.class, "handleExistingItem")
                .otherwise()
                .bean(PostNewItemBean.class, "setCategoryId")
                .setHeader("camelMongoDbFieldProjection", simple("{\"categoryName\": 1, \"_id\": 0}"))
                .to(String.format(ApplicationConstants.MONGO_CATEGORY_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_READ_COLLECTION))
                .choice()
                .when(body().isNull())
                .bean(PostNewItemBean.class, "handleInvalidCategory")
                .otherwise()
                .bean(PostNewItemBean.class, "prepareItemForInsert")
                .to(String.format(ApplicationConstants.MONGO_ITEM_INSERT,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_WRITE_COLLECTION))
                .bean(PostNewItemBean.class, "handleInsertSuccess")
                .endChoice()
                .endChoice();

        // Route for sync update
        rest("/inventory/update")
                .post()
                .consumes("application/json")
                .produces("application/json")
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_PROCESS_INVENTORY_UPDATE);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_PROCESS_INVENTORY_UPDATE)
                .routeId(ApplicationConstants.ROUTE_PROCESS_INVENTORY_UPDATE)
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_UPDATE_INVENTORY)
                .process(new FinalResponseProcessor());

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_UPDATE_INVENTORY)
                .routeId(ApplicationConstants.ROUTE_UPDATE_INVENTORY)
                .process(new PayloadValidationProcessor())
                .split(simple("${exchangeProperty.inventoryList}"))
                .aggregationStrategy(new ItemAggregationStrategy())
                .streaming()
                .doTry()
                .bean(ItemBean.class, "processItem")
                .bean(ItemBean.class, "setItemId")
                .to(String.format(ApplicationConstants.MONGO_ITEM_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_READ_COLLECTION))
                .bean(ItemBean.class, "validateAndUpdateItem")
                .to(String.format(ApplicationConstants.MONGO_ITEM_SAVE,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_ITEM_WRITE_COLLECTION))
                .bean(ItemBean.class, "markSuccess")
                .doCatch(InventoryValidationException.class)
                .bean(ItemBean.class, "markFailure")
                .end()
                .log("Completed processing item ${exchangeProperty.itemId}, itemResult: ${exchangeProperty.itemResult}")
                .end()
                .log("Split completed, itemResults: ${exchangeProperty.itemResults}");

        // Route for async update
        rest("/inventory/async-update")
                .post()
                .consumes("application/json")
                .produces("application/json")
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_ASYNC_INVENTORY_UPDATE);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_ASYNC_INVENTORY_UPDATE)
                .routeId(ApplicationConstants.ROUTE_ASYNC_INVENTORY_UPDATE)
                .doTry()
                .bean(PayloadValidationProcessor.class)
                .bean(AsyncInventoryUpdateBean.class, "initializeCorrelationId")
                .bean(AsyncInventoryUpdateBean.class, "prepareQueueMessage")
                .log("Sending item list to ActiveMQ queue: ${header.JMSCorrelationID}")
                .to(String.format(ApplicationConstants.AMQ_INVENTORY_UPDATE_WRITE,
                        ApplicationConstants.AMQ_INVENTORY_UPDATE_WRITE_QUEUE))
                .bean(AsyncInventoryUpdateBean.class, "buildEnqueueResponse")
                .doCatch(InventoryValidationException.class, JMSException.class)
                .bean(AsyncInventoryUpdateBean.class, "handleException")
                .doCatch(Exception.class)
                .bean(AsyncInventoryUpdateBean.class, "handleException")
                .end();

        // POST new Category
        rest("/mycart/category")
                .post()
                .consumes("application/json")
                .type(Category.class)
                .to(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_POST_NEW_CATEGORY);

        from(ApplicationConstants.DIRECT_PREFIX + ApplicationConstants.ENDPOINT_POST_NEW_CATEGORY)
                .routeId(ApplicationConstants.ROUTE_POST_NEW_CATEGORY)
                .log("Received new category: ${body}")
                .bean(PostNewCategoryBean.class, "validateCategory")
                .to(String.format(ApplicationConstants.MONGO_CATEGORY_FIND_BY_ID,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_READ_COLLECTION))
                .choice()
                .when(body().isNull())
                .bean(PostNewCategoryBean.class, "prepareCategoryForInsert")
                .to(String.format(ApplicationConstants.MONGO_CATEGORY_INSERT,
                        ApplicationConstants.MONGO_DATABASE, ApplicationConstants.MONGO_CATEGORY_WRITE_COLLECTION))
                .bean(PostNewCategoryBean.class, "handleInsertSuccess")
                .otherwise()
                .bean(PostNewCategoryBean.class, "handleExistingCategory");
    }
}