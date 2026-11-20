package de.swiftbyte.gmc.backend.config.message;

import de.swiftbyte.gmc.backend.domain.model.Node;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@RequiredArgsConstructor
public class NodeAuthentication implements Authentication {

    private final Node node;

    private boolean authenticated = true;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public Object getCredentials() {
        return node.getSecret();
    }

    @Override
    public Object getDetails() {
        return node.getId();
    }

    @Override
    public Object getPrincipal() {
        return node;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return node.getId();
    }
}
