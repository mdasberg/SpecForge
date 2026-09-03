package com.specforge.repository.internal;

import org.springframework.data.jpa.repository.JpaRepository;

interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, String> {}
