package com.specforge.repository.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Scans and imports run off the request thread: a repository with hundreds of specification files
 * takes longer than a wizard step should wait, so both are started, persisted and polled.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(GitHubProperties.class)
public class RepositoryConfiguration {}
