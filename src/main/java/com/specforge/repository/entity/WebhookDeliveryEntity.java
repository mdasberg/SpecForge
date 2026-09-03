package com.specforge.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * A delivery already processed. GitHub redelivers whenever it is unsure a delivery arrived, and
 * this row is what makes the second arrival a no-op: importing twice would be harmless, but
 * updating a proposal and writing a commit status twice would not be.
 */
@Entity
@Table(name = "webhook_delivery")
public class WebhookDeliveryEntity {

    @Id
    @Column(name = "delivery_id", nullable = false, length = 128)
    private String deliveryId;

    @Column(name = "event", nullable = false, length = 64)
    private String event;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WebhookDeliveryEntity() {
        // for JPA
    }

    public WebhookDeliveryEntity(final String deliveryId, final String event, final Instant receivedAt) {
        this.deliveryId = deliveryId;
        this.event = event;
        this.receivedAt = receivedAt;
    }

    public String deliveryId() {
        return deliveryId;
    }

    public String event() {
        return event;
    }

    public Instant receivedAt() {
        return receivedAt;
    }
}
