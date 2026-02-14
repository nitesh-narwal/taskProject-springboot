package com.example.shopapi.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableMongoAuditing
@EnableMongoRepositories(basePackages = "com.example.shopapi.repository.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    @Bean
    @Primary
    public MongoClient mongoClient() {
        log.info("Connecting to MongoDB with URI: {}", mongoUri.replaceAll(":[^:@]+@", ":****@"));

        ConnectionString connectionString = new ConnectionString(mongoUri);

        MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .applyToConnectionPoolSettings(builder -> builder
                        .maxSize(10)
                        .minSize(0)  // Allow 0 minimum connections
                        .maxWaitTime(10, TimeUnit.SECONDS))
                .applyToSocketSettings(builder -> builder
                        .connectTimeout(5, TimeUnit.SECONDS)
                        .readTimeout(5, TimeUnit.SECONDS))
                .applyToClusterSettings(builder -> builder
                        .serverSelectionTimeout(10, TimeUnit.SECONDS));

        // Configure SSL - let the driver handle it automatically for mongodb+srv
        // Don't override with custom TrustAll as it can cause issues
        log.info("MongoDB client configuration created (SSL handled by driver)");

        try {
            MongoClient client = MongoClients.create(settingsBuilder.build());
            log.info("MongoDB client created successfully");
            return client;
        } catch (Exception e) {
            log.error("=========================================================================");
            log.error("MONGODB CONNECTION FAILED: {}", e.getMessage());
            log.error("=========================================================================");
            log.error("This error usually means your IP is NOT whitelisted in MongoDB Atlas.");
            log.error("");
            log.error("TO FIX THIS:");
            log.error("1. Go to: https://cloud.mongodb.com");
            log.error("2. Select your project > Network Access (left sidebar)");
            log.error("3. Click '+ ADD IP ADDRESS'");
            log.error("4. Click 'ADD CURRENT IP ADDRESS' or 'ALLOW ACCESS FROM ANYWHERE'");
            log.error("5. Wait 1-2 minutes and restart the application");
            log.error("=========================================================================");
            throw e;
        }
    }


    @Override
    protected boolean autoIndexCreation() {
        return true;
    }
}
