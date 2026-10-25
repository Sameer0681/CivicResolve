package com.civicresolve.config;

import com.civicresolve.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomUserDetailsService customUserDetailsService;
        private final CustomAuthenticationSuccessHandler authenticationSuccessHandler;

        public SecurityConfig(
                        CustomUserDetailsService customUserDetailsService,
                        CustomAuthenticationSuccessHandler authenticationSuccessHandler) {

                this.customUserDetailsService = customUserDetailsService;
                this.authenticationSuccessHandler = authenticationSuccessHandler;
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
                                                                "/sectors")
                                                .permitAll()

                                                // Authentication and password reset views
                                                // Authentication and password reset views
                                                .requestMatchers(
                                                                "/login",
                                                                "/register",
                                                                "/forgot-password",
                                                                "/reset-password",
                                                                "/report",
                                                                "/report/**")
                                                .permitAll()

                                                // Role-based dashboard access`
                                                .requestMatchers("/admin/**")
                                                .hasRole("ADMIN")

                                                .requestMatchers("/citizen/**")
                                                .hasAnyRole("CITIZEN", "ADMIN")

                                                .requestMatchers("/officer/**")
                                                .hasAnyRole("OFFICER", "ADMIN")

                                                // Public complaint tracking — response must contain limited information
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/complaints/track/**")
                                                .permitAll()

                                                // Citizens can create complaints
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/complaints")
                                                .hasRole("CITIZEN")

                                                // Citizens can view their own complaints; admins can view all
                                                .requestMatchers(HttpMethod.GET, "/api/complaints")
                                                .hasAnyRole("CITIZEN", "ADMIN")

                                                // Only admins can use global complaint filters
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/complaints/status/**",
                                                                "/api/complaints/sector/**")
                                                .hasRole("ADMIN")

                                                // Only admins can delete complaints
                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/complaints/**")
                                                .hasRole("ADMIN")

                                                // Individual complaint retrieval requires authentication.
                                                // Ownership must also be checked in the controller.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/complaints/**")
                                                .authenticated())

                                // Allow REST API requests without CSRF token
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/api/**"))

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
