package com.thapar.gpf.login;

import com.thapar.gpf.backend.service.UserService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class GoogleLoginService extends OidcUserService {
    private final UserService users;

    public GoogleLoginService(UserService users) { this.users = users; }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser identity = super.loadUser(request);
        return provision(identity);
    }

    public OidcUser provision(OidcUser identity) {
        if (!Boolean.TRUE.equals(identity.getEmailVerified())
                || !UserService.isThaparEmail(identity.getEmail())
                || !"thapar.edu".equals(identity.getClaimAsString("hd"))) {
            throw new OAuth2AuthenticationException("institute_account_required");
        }
        var user = users.googleLogin(identity.getEmail(), identity.getFullName(), identity.getSubject());
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                identity.getIdToken(), identity.getUserInfo(), "email");
    }
}
