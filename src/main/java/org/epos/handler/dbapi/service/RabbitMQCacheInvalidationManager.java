package org.epos.handler.dbapi.service;

import com.rabbitmq.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A robust cache invalidation manager that uses RabbitMQ to publish and subscribe to cache invalidation events
 * with automatic reconnection and retry capabilities
 */
public class RabbitMQCacheInvalidationManager {

    private static final Logger LOGGER = Logger.getLogger(RabbitMQCacheInvalidationManager.class.getName());
    private static volatile RabbitMQCacheInvalidationManager instance;
    private static final Object lock = new Object();

    private final String EXCHANGE_NAME = "epos-cache-invalidation";
    private final String ROUTING_KEY = "cache.invalidation";
    private final String QUEUE_NAME_PREFIX = "epos-cache-invalidation-queue";

    // Connection parameters
    private final String host;
    private final String username;
    private final String password;
    private final String vhost;
    private final int port;

    // Connection management
    private volatile Connection connection;
    private volatile Channel publishChannel;
    private volatile Channel subscribeChannel;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    private final AtomicBoolean isSubscribed = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);

    // Retry and reconnection
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final int maxRetries;
    private final long retryDelayMs;
    private final long reconnectDelayMs;

    private RabbitMQCacheInvalidationManager() {
        // Load RabbitMQ connection properties from environment variables
        this.host = System.getenv().getOrDefault("BROKER_HOST", "localhost");
        this.username = System.getenv().getOrDefault("BROKER_USERNAME", "guest");
        this.password = System.getenv().getOrDefault("BROKER_PASSWORD", "guest");
        this.vhost = System.getenv().getOrDefault("BROKER_VHOST", "/");
        this.port = Integer.parseInt(System.getenv().getOrDefault("BROKER_PORT", "5672"));
        this.maxRetries = Integer.parseInt(System.getenv().getOrDefault("BROKER_MAX_RETRIES", "3"));
        this.retryDelayMs = Long.parseLong(System.getenv().getOrDefault("BROKER_RETRY_DELAY_MS", "5000"));
        this.reconnectDelayMs = Long.parseLong(System.getenv().getOrDefault("BROKER_RECONNECT_DELAY_MS", "10000"));

        // Initialize connection
        initializeConnection();

        // Register shutdown hook to close resources
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static RabbitMQCacheInvalidationManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new RabbitMQCacheInvalidationManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize connection to RabbitMQ with retry logic
     */
    private void initializeConnection() {
        if (isShuttingDown.get()) {
            return;
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setVirtualHost(vhost);

            // Configure connection recovery
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(reconnectDelayMs);
            factory.setRequestedHeartbeat(30);
            factory.setConnectionTimeout(10000);

            // Create connection
            connection = factory.newConnection();

            // Add connection recovery listeners
            ((Recoverable) connection).addRecoveryListener(new RecoveryListener() {
                @Override
                public void handleRecovery(Recoverable recoverable) {
                    LOGGER.info("RabbitMQ connection recovered");
                    isConnected.set(true);
                    reinitializeChannels();
                }

                @Override
                public void handleRecoveryStarted(Recoverable recoverable) {
                    LOGGER.info("RabbitMQ connection recovery started");
                    isConnected.set(false);
                }
            });

            // Add blocked connection listeners
            connection.addBlockedListener(new BlockedListener() {
                @Override
                public void handleBlocked(String reason) throws IOException {
                    LOGGER.warning("RabbitMQ connection blocked: " + reason);
                }

                @Override
                public void handleUnblocked() throws IOException {
                    LOGGER.info("RabbitMQ connection unblocked");
                }
            });

            // Initialize channels
            initializeChannels();

            isConnected.set(true);
            LOGGER.info("Successfully connected to RabbitMQ broker at " + host + ":" + port);

        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to RabbitMQ: " + e.getMessage(), e);
            isConnected.set(false);
            scheduleReconnection();
        }
    }

    /**
     * Initialize channels for publishing and subscribing
     */
    private void initializeChannels() throws IOException {
        if (connection == null || !connection.isOpen()) {
            throw new IOException("Connection is not available");
        }

        // Create publishing channel
        publishChannel = connection.createChannel();
        publishChannel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

        // Create subscribing channel
        subscribeChannel = connection.createChannel();
        subscribeChannel.exchangeDeclare(EXCHANGE_NAME, "topic", true);

        LOGGER.info("RabbitMQ channels initialized successfully");
    }

    /**
     * Reinitialize channels after connection recovery
     */
    private void reinitializeChannels() {
        try {
            if (publishChannel != null && !publishChannel.isOpen()) {
                initializeChannels();
            }

            // Re-subscribe if we were subscribed before
            if (isSubscribed.get()) {
                isSubscribed.set(false);
                subscribeToInvalidationEvents();
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to reinitialize channels after recovery", e);
        }
    }

    /**
     * Schedule reconnection attempt
     */
    private void scheduleReconnection() {
        if (isShuttingDown.get()) {
            return;
        }

        scheduler.schedule(() -> {
            LOGGER.info("Attempting to reconnect to RabbitMQ...");
            initializeConnection();
        }, reconnectDelayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Subscribe to cache invalidation events with automatic queue creation
     */
    public void subscribeToInvalidationEvents() {
        if (!isConnected.get() || isSubscribed.get() || isShuttingDown.get()) {
            if (!isConnected.get()) {
                LOGGER.warning("Cannot subscribe: not connected to RabbitMQ");
            }
            return;
        }

        try {
            String queueName = QUEUE_NAME_PREFIX + "-" + System.getProperty("service.name", "unknown");

            // Declare queue as exclusive to this service instance
            subscribeChannel.queueDeclare(queueName, false, true, true, null);
            subscribeChannel.queueBind(queueName, EXCHANGE_NAME, ROUTING_KEY);

            // Set up consumer with error handling
            subscribeChannel.basicConsume(queueName, true, new DefaultConsumer(subscribeChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) throws IOException {
                    try {
                        String message = new String(body, StandardCharsets.UTF_8);
                        handleInvalidationMessage(message);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error handling invalidation message", e);
                    }
                }

                @Override
                public void handleCancel(String consumerTag) throws IOException {
                    LOGGER.warning("Consumer was cancelled: " + consumerTag);
                    isSubscribed.set(false);
                    // Try to resubscribe after a delay
                    if (!isShuttingDown.get()) {
                        scheduler.schedule(() -> subscribeToInvalidationEvents(), 5000, TimeUnit.MILLISECONDS);
                    }
                }
            });

            isSubscribed.set(true);
            LOGGER.info("Subscribed to cache invalidation events on exchange: " + EXCHANGE_NAME + ", queue: " + queueName);

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to subscribe to cache invalidation events: " + e.getMessage(), e);
            isSubscribed.set(false);

            // Retry subscription after delay
            if (!isShuttingDown.get()) {
                scheduler.schedule(() -> subscribeToInvalidationEvents(), retryDelayMs, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * Publish a cache invalidation event with retry logic
     *
     * @param entityType The entity type to invalidate
     * @return A CompletableFuture that completes when the message is sent
     */
    public CompletableFuture<Void> publishInvalidationEvent(String entityType) {
        return publishInvalidationEvent(entityType, 0);
    }

    /**
     * Internal method for publishing with retry count
     */
    private CompletableFuture<Void> publishInvalidationEvent(String entityType, int retryCount) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (isShuttingDown.get()) {
            future.completeExceptionally(new IllegalStateException("Service is shutting down"));
            return future;
        }

        if (!isConnected.get()) {
            if (retryCount < maxRetries) {
                // Schedule retry
                scheduler.schedule(() ->
                                publishInvalidationEvent(entityType, retryCount + 1)
                                        .whenComplete((result, throwable) -> {
                                            if (throwable != null) {
                                                future.completeExceptionally(throwable);
                                            } else {
                                                future.complete(result);
                                            }
                                        }),
                        retryDelayMs, TimeUnit.MILLISECONDS);
                return future;
            } else {
                future.completeExceptionally(new IllegalStateException("Not connected to RabbitMQ after " + maxRetries + " retries"));
                return future;
            }
        }

        try {
            if (publishChannel == null || !publishChannel.isOpen()) {
                initializeChannels();
            }

            LOGGER.fine("Publishing cache invalidation event for entity type: " + entityType);

            // Publish message with confirmation
            publishChannel.basicPublish(
                    EXCHANGE_NAME,
                    ROUTING_KEY,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    entityType.getBytes(StandardCharsets.UTF_8)
            );

            future.complete(null);
            LOGGER.fine("Successfully published cache invalidation event for: " + entityType);

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to publish cache invalidation event (attempt " + (retryCount + 1) + "): " + e.getMessage(), e);

            if (retryCount < maxRetries) {
                // Schedule retry
                scheduler.schedule(() ->
                                publishInvalidationEvent(entityType, retryCount + 1)
                                        .whenComplete((result, throwable) -> {
                                            if (throwable != null) {
                                                future.completeExceptionally(throwable);
                                            } else {
                                                future.complete(result);
                                            }
                                        }),
                        retryDelayMs, TimeUnit.MILLISECONDS);
            } else {
                future.completeExceptionally(e);
            }
        }

        return future;
    }

    /**
     * Handle a cache invalidation message
     *
     * @param entityType The cache invalidation message (entity type)
     */
    private void handleInvalidationMessage(String entityType) {
        try {
            LOGGER.fine("Received cache invalidation event for entity type: " + entityType);

            // Get the cache service and invalidate the entries
            DatabaseCacheService cacheService = EntityManagerService.getCacheService();
            if (cacheService != null) {
                cacheService.invalidateByEntityType(entityType);
                LOGGER.info("Successfully invalidated cache for entity type: " + entityType);
            } else {
                LOGGER.warning("Cache service not available for invalidation");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error handling cache invalidation for entity type: " + entityType, e);
        }
    }

    /**
     * Check if the manager is connected to RabbitMQ
     */
    public boolean isConnected() {
        return isConnected.get() && connection != null && connection.isOpen();
    }

    /**
     * Check if subscribed to invalidation events
     */
    public boolean isSubscribed() {
        return isSubscribed.get();
    }

    /**
     * Get connection statistics
     */
    public String getConnectionInfo() {
        if (connection != null && connection.isOpen()) {
            return String.format("Connected to %s:%d (vhost: %s)", host, port, vhost);
        } else {
            return "Disconnected";
        }
    }

    /**
     * Shutdown the manager and release resources
     */
    public void shutdown() {
        if (isShuttingDown.getAndSet(true)) {
            return; // Already shutting down
        }

        LOGGER.info("Shutting down RabbitMQCacheInvalidationManager...");

        // Shutdown scheduler first
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Close channels and connection
        try {
            if (subscribeChannel != null && subscribeChannel.isOpen()) {
                subscribeChannel.close();
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.WARNING, "Error closing subscribe channel", e);
        }

        try {
            if (publishChannel != null && publishChannel.isOpen()) {
                publishChannel.close();
            }
        } catch (IOException | TimeoutException e) {
            LOGGER.log(Level.WARNING, "Error closing publish channel", e);
        }

        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }

        isConnected.set(false);
        isSubscribed.set(false);

        LOGGER.info("RabbitMQCacheInvalidationManager shutdown completed");
    }
}