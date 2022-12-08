package com.softrizon.keycloak.providers.events.pubsub.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.keycloak.events.admin.AdminEvent;

import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "_class")
public class AdminEventMessage extends AdminEvent implements Serializable {

    private static final long serialVersionUID = -1L;

    private Date createdAt;

    public Date getCreatedAt() {
        return createdAt;
    }

    protected void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public static AdminEventMessage create(AdminEvent event) {
        AdminEventMessage message = new AdminEventMessage();
        message.setAuthDetails(event.getAuthDetails());
        message.setError(event.getError());
        message.setOperationType(event.getOperationType());
        message.setRealmId(event.getRealmId());
        message.setRepresentation(event.getRepresentation());
        message.setResourcePath(event.getResourcePath());
        message.setResourceType(event.getResourceType());
        message.setResourceTypeAsString(event.getResourceTypeAsString());
        message.setTime(event.getTime());
        message.setCreatedAt(new Date(event.getTime()));

        return message;
    }
}