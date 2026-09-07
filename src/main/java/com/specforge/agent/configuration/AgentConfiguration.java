package com.specforge.agent.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Checks run off the request thread — {@code repository} already enables asynchronous execution
 * application-wide, and dispatch here rides on the same executor.
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfiguration {}
