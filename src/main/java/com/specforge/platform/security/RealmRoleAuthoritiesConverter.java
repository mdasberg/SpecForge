package com.specforge.platform.security;

import com.specforge.platform.identity.Role;
import com.specforge.platform.identity.TokenIdentity;
import java.util.Collection;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Turns the token's realm roles into granted authorities. Realm roles are the only source of a
 * role in SpecForge, so there is deliberately nothing else here to read.
 */
@Component
class RealmRoleAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        return TokenIdentity.of(jwt).roles().stream()
                .map(Role::authority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableList());
    }
}
