package com.specforge.repository.service;

import com.specforge.repository.ProjectRef;
import com.specforge.repository.Projects;
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
class ProjectsImpl implements Projects {

    private final RepositoryConnectionRepository connections;
    private final ProjectRepository projects;

    @Override
    public Optional<ProjectRef> forConnection(final UUID connectionId) {
        return connections.findById(connectionId)
                .flatMap(connection -> projects.findById(connection.projectId()))
                .map(project -> new ProjectRef(project.id(), project.name()));
    }
}
