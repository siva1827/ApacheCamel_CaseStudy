package com.UST.fileExport.config;

import com.UST.fileExport.processor.ControlRefProcessor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Collections;
@Configuration
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Collections.emptyList());
    }

    @Autowired
    private MongoClient mongoClient;

    @PostConstruct
    public void debugControlRef() {
        MongoDatabase db = mongoClient.getDatabase("mycartdb");
        MongoCollection<Document> collection = db.getCollection("ControlRef");
        Document doc = collection.find(new Document("_id", "fileExport")).first();
        logger.info("Direct MongoDB query for ControlRef: {}", doc != null ? doc.toJson() : "null");
    }
}