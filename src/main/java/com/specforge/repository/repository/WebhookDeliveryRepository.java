package com.specforge.repository.repository;

import com.specforge.repository.entity.WebhookDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface WebhookDeliveryRepository extends JpaRepository<WebhookDeliveryEntity, String> {}
