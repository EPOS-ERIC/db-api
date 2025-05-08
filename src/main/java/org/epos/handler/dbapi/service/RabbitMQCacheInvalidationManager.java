package org.epos.handler.dbapi.service;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A cache invalidation manager that uses RabbitMQ to publish and subscribe to cache invalidation events
 */
public class RabbitMQCacheInvalidationManager {

    private static final Logger LOGGER = Logger.getLogger(RabbitMQCacheInvalidationManager.class.getName());
    private static RabbitMQCacheInvalidationManager instance;

    private final String EXCHANGE_NAME = "epos-cache-invalidation";
    private final String ROUTING_KEY = "cache.invalidation";
    private final String QUEUE_NAME = "epos-cache-invalidation-queue";

    private Connection connection;
    private Channel channel;
    private boolean isConnected = false;
    private boolean isSubscribed = false;

    private RabbitMQCacheInvalidationManager() {
        // Load RabbitMQ connection properties from environment variables
        String host = System.getenv("BROKER_HOST");
        String username = System.getenv("BROKER_USERNAME");
        String password = System.getenv("BROKER_PASSWORD");
        String vhost = System.getenv("BROKER_VHOST");

        // Set default values if environment variables are not set
        host = (host != null) ? host : "localhost";
        username = (username != null) ? username : "guest";
        password = (password != null) ? password : "guest";
        vhost = (vhost != null) ? vhost : "/";

        try {
            // Create connection factory
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(vhost);

            // Create connection and channel
            connection = factory.newConnection();
            channel = connection.createChannel();

            // Declare exchange
            channel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

            isConnected = true;
            LOGGER.info("Connected to RabbitMQ broker at " + host);

            // Register shutdown hook to close resources
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to RabbitMQ: " + e.getMessage(), e);
        }
    }

    public static synchronized RabbitMQCacheInvalidationManager getInstance() {
        if (instance == null) {
            instance = new RabbitMQCacheInvalidationManager();
        }
        return instance;
    }

    /**
     * Subscribe to cache invalidation events
     */
    public void subscribeToInvalidationEvents() {
        if (!isConnected || isSubscribed) {
            return;
        }

        try {
            // Declare queue and bind to exchange
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

            // Set up consumer
            channel.basicConsume(QUEUE_NAME, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    String message = new String(body, StandardCharsets.UTF_8);
                    handleInvalidationMessage(message);
                }
            });

            isSubscribed = true;
            LOGGER.info("Subscribed to cache invalidation events on exchange: " + EXCHANGE_NAME);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to subscribe to cache invalidation events: " + e.getMessage(), e);
        }
    }

    /**
     * Publish a cache invalidation event
     *
     * @param entityType The entity type to invalidate
     * @return A CompletableFuture that completes when the message is sent
     */
    public CompletableFuture<Void> publishInvalidationEvent(String entityType) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (!isConnected) {
            future.completeExceptionally(new IllegalStateException("Not connected to RabbitMQ"));
            return future;
        }

        try {
            LOGGER.info("Publishing cache invalidation event for entity type: " + entityType);

            // Publish message
            channel.basicPublish(
                    EXCHANGE_NAME,
                    ROUTING_KEY,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    entityType.getBytes(StandardCharsets.UTF_8)
            );

            future.complete(null);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to publish cache invalidation event: " + e.getMessage(), e);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Handle a cache invalidation message
     *
     * @param message The cache invalidation message (entity type)
     */
    private void handleInvalidationMessage(String entityType) {
        LOGGER.info("Received cache invalidation event for entity type: " + entityType);

        // Get the cache service and invalidate the entries
        DatabaseCacheService cacheService = EntityManagerService.getCacheService();
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
        }
    }

    /**
     * Shutdown the manager and release resources
     */
    public void shutdown() {
        if (connection != null) {
            try {
                if (channel != null) {
                    channel.close();
                }
                connection.close();
                isConnected = false;
                isSubscribed = false;
                LOGGER.info("Disconnected from RabbitMQ broker");
            } catch (IOException | TimeoutException e) {
                LOGGER.log(Level.SEVERE, "Error during shutdown: " + e.getMessage(), e);
            }
        }
    }
}