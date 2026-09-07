package com.specforge.repository.service;

import com.specforge.repository.ApprovalRule;
import com.specforge.repository.ApprovalRules;
import com.specforge.repository.entity.ProjectEntity;
import com.specforge.repository.repository.ProjectRepository;
import com.specforge.repository.repository.RepositoryConnectionRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional(readOnly = true)
class ApprovalRulesImpl implements ApprovalRules {

    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;

    @Override
    public Optional<ApprovalRule> forConnection(final UUID connectionId) {
        return connections.findById(connectionId)
                .flatMap(connection -> projects.findById(connection.projectId()))
                .map(ApprovalRulesImpl::rule);
    }

    private static ApprovalRule rule(final ProjectEntity project) {
        return new ApprovalRule(project.minApprovals(), project.requiredRoles());
    }
}
