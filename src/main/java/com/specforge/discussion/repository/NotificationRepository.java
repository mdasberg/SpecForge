package com.specforge.discussion.repository;

import com.specforge.discussion.entity.NotificationEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientSubjectIdOrderByCreatedAtDesc(String recipientSubjectId);

    long countByRecipientSubjectIdAndReadAtIsNull(String recipientSubjectId);
}
