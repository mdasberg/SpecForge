package com.specforge.discussion.repository;

import com.specforge.discussion.entity.CommentEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByThreadIdOrderByCreatedAtAsc(UUID threadId);

    List<CommentEntity> findByThreadIdInOrderByCreatedAtAsc(List<UUID> threadIds);
}
