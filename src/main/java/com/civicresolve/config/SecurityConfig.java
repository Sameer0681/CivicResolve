package com.civicresolve.config;

import com.civicresolve.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.core.env.Environment;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.time.LocalDateTime;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    private final Environment environment;

    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            CustomAuthenticationSuccessHandler authenticationSuccessHandler,
            Environment environment) {

        this.customUserDetailsService = customUserDetailsService;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
        this.environment = environment;
    }

    private boolean isDevProfileActive() {
        return environment != null && Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {

        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth

                        // Static web assets
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico",
                                "/webjars/**")
                        .permitAll()

                        // Public landing & informational views
                        .requestMatchers(
                                "/",
                                "/landing/**",
                                "/about",
                                "/features",
                                "/sectors",
                                "/report/**",
                                "/report",
                                "/pages",
                                "/preview",
                                "/dev/pages",
                                "/error")
                        .permitAll()

                        // Authentication and password reset views (GET and POST public)
                        .requestMatchers(
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/login",
                                "/register",
                                "/forgot-password",
                                "/reset-password")
                        .permitAll()

                        // Public REST endpoints
                        .requestMatchers(HttpMethod.POST, "/api/auth/register")
                        .permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/complaints/track/**")
                        .permitAll()

                        // Admin-only REST endpoints
                        .requestMatchers("/api/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.DELETE, "/api/complaints/**")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PATCH, "/api/complaints/*/assign")
                        .hasRole("ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/complaints/*/assign")
                        .hasRole("ADMIN")

                        // Citizen profile phone update (Point 4c)
                        .requestMatchers(HttpMethod.PUT, "/api/auth/profile/phone")
                        .hasRole("CITIZEN")

                        // OTP Verification endpoints: Citizens only (Point 7)
                        .requestMatchers("/api/otp/**")
                        .hasRole("CITIZEN")

                        // Role-based web dashboards
                        .requestMatchers("/admin/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/citizen/**")
                        .hasAnyRole("CITIZEN", "ADMIN")

                        .requestMatchers("/officer/**")
                        .hasAnyRole("OFFICER", "ADMIN")

                        // Citizen complaints retrieval
                        .requestMatchers(HttpMethod.GET, "/api/complaints/citizen/**")
                        .hasAnyRole("CITIZEN", "ADMIN")

                        // Create complaints: Citizens and Admins
                        .requestMatchers(HttpMethod.POST, "/api/complaints")
                        .hasAnyRole("CITIZEN", "ADMIN")

                        // List complaints: Citizens, Officers, Admins
                        .requestMatchers(HttpMethod.GET, "/api/complaints")
                        .hasAnyRole("CITIZEN", "OFFICER", "ADMIN")

                        // Status updates: Officers and Admins
                        .requestMatchers(HttpMethod.PATCH, "/api/complaints/*/status")
                        .hasAnyRole("OFFICER", "ADMIN")

                        .requestMatchers(HttpMethod.PUT, "/api/complaints/*/status")
                        .hasAnyRole("OFFICER", "ADMIN")

                        // Filter complaints: Admins and Officers
                        .requestMatchers(HttpMethod.GET, "/api/complaints/status/**", "/api/complaints/sector/**")
                        .hasAnyRole("ADMIN", "OFFICER")

                        // Individual complaint retrieval requires authentication
                        .requestMatchers(HttpMethod.GET, "/api/complaints/**")
                        .authenticated()

                        // Any other request must be authenticated
                        .anyRequest()
                        .authenticated())

                // Allow REST API requests without CSRF token
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**"))

                // HTTP Basic authentication restricted to "dev" profile only (Point 4)
                .httpBasic(basic -> {
                    if (!isDevProfileActive()) {
                        basic.disable();
                    }
                })

                // Custom 401 & 403 responses: JSON ONLY for /api/** requests, standard browser behavior otherwise
                .exceptionHandling(ex -> {
                    AccessDeniedHandler defaultDeniedHandler = new AccessDeniedHandlerImpl();
                    AuthenticationEntryPoint defaultEntryPoint = new LoginUrlAuthenticationEntryPoint("/login");

                    ex.authenticationEntryPoint((request, response, authException) -> {
                        if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api")) {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication required to access this resource\",\"path\":\"%s\"}",
                                    LocalDateTime.now(), request.getRequestURI()));
                        } else {
                            defaultEntryPoint.commence(request, response, authException);
                        }
                    });

                    ex.accessDeniedHandler((request, response, accessDeniedException) -> {
                        if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api")) {
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().write(String.format(
                                    "{\"timestamp\":\"%s\",\"status\":403,\"error\":\"FORBIDDEN\",\"message\":\"Access denied: insufficient permissions\",\"path\":\"%s\"}",
                                    LocalDateTime.now(), request.getRequestURI()));
                        } else {
                            defaultDeniedHandler.handle(request, response, accessDeniedException);
                        }
                    });
                })

                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()
                        .maximumSessions(2)
                        .maxSessionsPreventsLogin(false))

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())

                .rememberMe(remember -> remember
                        .key("CivicResolveSecretKey2026")
                        .tokenValiditySeconds(86400 * 30)
                        .userDetailsService(customUserDetailsService));

        return http.build();
    }
}
