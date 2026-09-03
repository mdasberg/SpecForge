package com.specforge.repository.internal;

import com.specforge.repository.internal.forge.ForgeConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Scans and imports run off the request thread: a repository with hundreds of specification files
 * takes longer than a wizard step should wait, so both are started, persisted and polled.
 */
@Configuration
@EnableAsync
@Import(ForgeConfiguration.class)
class RepositoryConfiguration {}
