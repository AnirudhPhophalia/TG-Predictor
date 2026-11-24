package com.thapar.gpf.login;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

@Configuration
@ConditionalOnProperty(name = "gpf.google.enabled", havingValue = "true")
public class GoogleLoginConfig {
    @Bean
    ClientRegistrationRepository googleRegistration(
            @Value("${GOOGLE_CLIENT_ID}") String clientId,
            @Value("${GOOGLE_CLIENT_SECRET}") String clientSecret) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Google login requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET.");
        }
        return new InMemoryClientRegistrationRepository(ClientRegistration.withRegistrationId("google")
                .clientId(clientId).clientSecret(clientSecret).clientName("Google")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName("email").build());
    }
}
