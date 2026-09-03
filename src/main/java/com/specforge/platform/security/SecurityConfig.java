package com.specforge.platform.security;

import java.util.Collection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The API is a stateless resource server: it validates Keycloak-issued JWTs and holds no session,
 * no login form and no account management of its own. Anything a user needs to do to their own
 * account happens in Keycloak.
 */
@Configuration
@EnableWebSecurity
// Roles gate individual operations as well as the chain: connecting a repository is an
// administrator's act, and the endpoint says so rather than the frontend hiding a button.
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter(final Converter<Jwt, Collection<GrantedAuthority>> authorities) {
        final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    SecurityFilterChain apiFilterChain(
            final HttpSecurity http,
            final JwtAuthenticationConverter jwtAuthenticationConverter,
            final IdentityMirrorFilter identityMirrorFilter,
            final ProblemAuthenticationEntryPoint entryPoint,
            final ProblemAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .securityMatcher("/api/**")
                .authorizeHttpRequests(requests -> requests
                        // The forge cannot present a Keycloak token. This one route authenticates
                        // itself with an HMAC signature over the raw body, verified before the
                        // payload is parsed; everything else needs a bearer token.
                        .requestMatchers(HttpMethod.POST, "/api/webhooks/github").permitAll()
                        .anyRequest().authenticated())
                // A bearer-token API carries no cookie, so there is no CSRF vector to protect and
                // no session to create.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterAfter(identityMirrorFilter, BearerTokenAuthenticationFilter.class)
                .build();
    }
}
