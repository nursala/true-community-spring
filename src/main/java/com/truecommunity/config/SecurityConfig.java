package com.truecommunity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Defines route authorization, form login/logout behavior, password encoding,
 * and the session registry used to expire sessions after role changes.
 */
@Configuration
public class SecurityConfig {

    /**
     * Builds the application's single security filter chain. Public routes stay
     * open, editor/admin routes require {@code ROLE_EDITOR}, session fixation
     * changes the session id on login, and sessions are registered without a
     * concurrency limit so they can be expired after promotion or demotion.
     *
     * @param http the Spring Security HTTP builder
     * @param sessionRegistry registry used for active-session tracking
     * @return the configured security filter chain
     * @throws Exception when Spring Security cannot build the chain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SessionRegistry sessionRegistry) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/**").hasRole("EDITOR")
                        .requestMatchers("/editor/**", "/api/editor/**").hasRole("EDITOR")
                        .requestMatchers("/dashboard/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/arena/comments/*/vote").authenticated()
                        .requestMatchers(HttpMethod.POST, "/arena/*/vote", "/arena/*/comment").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/statements/*/votes").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/statements/*/comments").authenticated()
                        .requestMatchers(
                                "/",
                                "/feed",
                                "/feed/events",
                                "/arena/**",
                                "/leaderboard",
                                "/leaderboard/events",
                                "/login",
                                "/register",
                                "/error",
                                "/api/statements/**",
                                "/api/users/leaderboard",
                                "/css/**",
                                "/js/**",
                                "/images/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        // Track sessions in the registry (no concurrency limit) so role
                        // changes can expire a user's sessions on demand. Expired sessions
                        // are bounced to the login page on their next request.
                        .maximumSessions(-1)
                        .maxSessionsPreventsLogin(false)
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/login?expired")
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/feed", false)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .build();
    }

    /**
     * Encodes user passwords with BCrypt before persistence and during login checks.
     *
     * @return the password encoder used by security and registration flows
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Tracks active principals and sessions so role changes can expire the
     * affected user's sessions.
     *
     * @return the session registry
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Bridges servlet session lifecycle events to the {@link SessionRegistry}
     * so destroyed sessions are removed from the registry.
     *
     * @return the session event publisher
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }
}
