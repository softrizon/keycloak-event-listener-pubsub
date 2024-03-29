package com.softrizon.keycloak.providers.events.pubsub;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.softrizon.keycloak.providers.events.pubsub.config.PubSubConfig;
import com.softrizon.keycloak.providers.events.pubsub.events.AdminEventMessage;
import com.softrizon.keycloak.providers.events.pubsub.events.EventPattern;
import com.softrizon.keycloak.providers.events.pubsub.events.UserEventMessage;
import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.softrizon.keycloak.providers.events.pubsub.config.PubSubConfig.PLUGIN_NAME;
import static com.softrizon.keycloak.providers.events.pubsub.config.PubSubConfig.createEventName;

public class PubSubEventListenerProvider implements EventListenerProvider {

    private static final Logger logger = Logger.getLogger(PubSubEventListenerProvider.class);

    private final EventListenerTransaction transaction;
    private final PubSubConfig config;
    private final Publisher publisher;
    private final ObjectMapper objectMapper;

    public PubSubEventListenerProvider(Publisher publisher, KeycloakSession session, PubSubConfig config) {
        Objects.requireNonNull(publisher, String.format("%s: pub/pub publisher is required.", PLUGIN_NAME));
        Objects.requireNonNull(session, String.format("%s: a valid keycloak session is required.", PLUGIN_NAME));
        Objects.requireNonNull(config, String.format("%s: a valid config object is required.", PLUGIN_NAME));

        this.publisher = publisher;
        this.config = config;
        objectMapper = createObjectMapper();
        transaction = new EventListenerTransaction(this::publishAdminEvent, this::publishEvent);
        session.getTransactionManager().enlistAfterCompletion(transaction);
    }

    @Override
    public void onEvent(Event event) {
        transaction.addEvent(event.clone());
    }

    @Override
    public void onEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        transaction.addAdminEvent(adminEvent, includeRepresentation);
    }

    @Override
    public void close() {
        // Intentionally left blank
    }

    private void publishAdminEvent(AdminEvent adminEvent, boolean includeRepresentation) {
        // Ignore events that are not registered
        final String eventName = createEventName(adminEvent);
        final Optional<EventPattern> optionalEvent = config.getAdminEventTypes().stream()
                .filter(e -> e.pattern.matcher(eventName.toUpperCase(Locale.US)).matches())
                .findFirst();
        if (!optionalEvent.isPresent()) {
            logger.infof("%s: ignored admin event '%s'.", PLUGIN_NAME, eventName);
            return;
        }

        // Processing the event
        try {
            AdminEventMessage message = AdminEventMessage.create(adminEvent);
            String messageJsonString = objectMapper.writeValueAsString(message);
            Map<String, String> attributes = PubSubConfig.getMessageAttributes(adminEvent, optionalEvent.get());

            publishMessage(messageJsonString, attributes);
        } catch (JsonProcessingException exception) {
            logger.warnf(exception, "%s: failed to process JSON for admin event id '%s'.",
                    PLUGIN_NAME, adminEvent.getId());
        }
    }

    private void publishEvent(Event event) {
        // Ignore events that are not registered
        final String eventName = createEventName(event);
        final Optional<EventPattern> optionalEvent = config.getUserEventTypes().stream()
                .filter(e -> e.pattern.matcher(eventName.toUpperCase(Locale.US)).matches())
                .findFirst();
        if (!optionalEvent.isPresent()) {
            logger.infof("%s: ignored user event '%s'.", PLUGIN_NAME, eventName);
            return;
        }

        // Processing the event
        try {
            UserEventMessage message = UserEventMessage.create(event);
            String messageJsonString = objectMapper.writeValueAsString(message);
            Map<String, String> attributes = PubSubConfig.getMessageAttributes(event, optionalEvent.get());

            publishMessage(messageJsonString, attributes);
        } catch (JsonProcessingException exception) {
            logger.warnf(exception, "%s: failed to process JSON for client event id '%s'.",
                    PLUGIN_NAME, event.getId());
        }
    }

    private void publishMessage(String message, Map<String, String> attributes) {
        try {
            // Log message attributes and body
            logger.infof("%s: message attributes: %s.", PLUGIN_NAME, attributes.toString());
            logger.infof("%s: message body: %s.", PLUGIN_NAME, message);

            ByteString data = ByteString.copyFromUtf8(message);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder()
                    .putAllAttributes(attributes)
                    .setData(data)
                    .build();
            ApiFuture<String> messageIdFuture = publisher.publish(pubsubMessage);
            ApiFutures.addCallback(messageIdFuture, new ApiFutureCallback<String>() {

                // Handle message success
                public void onSuccess(String messageId) {
                    logger.infof("%s: sent message id '%s' to pub/sub topic '%s' successfully.",
                            PLUGIN_NAME, messageId, config.getTopicId());
                }

                // Handle message failure
                public void onFailure(Throwable throwable) {
                    logger.errorf(throwable, "%s: failed to send message to pub/sub topic '%s'.",
                            PLUGIN_NAME, config.getTopicId());
                }
            }, MoreExecutors.directExecutor());
        } catch (Exception exception) {
            logger.errorf(exception, "%s: failed to send message to pub/sub topic '%s'.",
                    PLUGIN_NAME, config.getTopicId());
        }
    }

    private ObjectMapper createObjectMapper() {
        final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(dateFormat);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return mapper;
    }
}
