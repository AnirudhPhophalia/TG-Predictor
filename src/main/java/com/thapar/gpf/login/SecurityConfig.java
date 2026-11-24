package com.thapar.gpf.login;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Admins go to /admin, students to /student after logging in.
    @Bean
    public AuthenticationSuccessHandler roleBasedSuccessHandler() {
        return (request, response, authentication) -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            response.sendRedirect(request.getContextPath() + (isAdmin ? "/admin" : "/student"));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationSuccessHandler successHandler,
            ObjectProvider<ClientRegistrationRepository> registrations, GoogleLoginService oidcUsers,
            @Value("${gpf.demo.enabled:false}") boolean demoEnabled) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/css/**", "/images/**", "/error", "/oauth2/**", "/login/oauth2/**").permitAll()
                // Admins cannot open any student page, so they have no route to student marks.
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/student/**").hasRole("STUDENT")
                .anyRequest().authenticated())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(
                    new org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint("/login")))
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        if (demoEnabled) {
            http.formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(successHandler)
                .failureUrl("/login?error"));
        }
        if (registrations.getIfAvailable() != null) {
            http.oauth2Login(oauth -> oauth.loginPage("/login")
                    .userInfoEndpoint(info -> info.oidcUserService(oidcUsers))
                    .successHandler(successHandler).failureUrl("/login?oauthError"));
        }
        return http.build();
    }
}
