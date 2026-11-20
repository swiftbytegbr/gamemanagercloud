package de.swiftbyte.gmc.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swiftbyte.gmc.backend.exception.manager.ExceptionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebSecurity
@Slf4j
public class WebSecurityConfig {


    @Autowired
    private HttpSessionFilter sessionFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExceptionManager exceptionManager;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.addFilterBefore(sessionFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/websocket-nodes/**").permitAll()
                // Frontend websocket requires authentication; no permitAll
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api-docs/**").permitAll()
                .requestMatchers("/images/**").permitAll()
                .requestMatchers("/auth/login", "/auth/register", "/auth/send-token", "/auth/reset-password/request", "/auth/reset-password/verify").permitAll()
                .requestMatchers("/auth/csrf").permitAll()
                .requestMatchers("/info/version").permitAll()
                .requestMatchers("/node/register").permitAll()
                // Public ASA admin list exposure for server integration
                .requestMatchers("/server/*/ark/asa/adminlist.txt").permitAll()
                .requestMatchers("/paddle/webhook").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            );


        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    ExceptionManager.ErrorResponse error = exceptionManager.base(HttpStatus.UNAUTHORIZED, "auth.unauthenticated", request);
                    objectMapper.writeValue(response.getWriter(), error);
                })
        );


        return http.build();
    }


    @Value("${gmc.cors.allowed-origins:*}")
    private String allowedOriginsStr;

    // Credentials are intentionally not allowed for CORS responses.

    @Bean
    WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var mapping = registry.addMapping("/**")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(3600);

                String trimmed = allowedOriginsStr == null ? "" : allowedOriginsStr.trim();
                boolean wildcard = trimmed.isEmpty() || "*".equals(trimmed);

                if (!wildcard) {
                    String[] origins = trimmed.split(",");
                    for (int i = 0; i < origins.length; i++) origins[i] = origins[i].trim();
                    // Explicit origins with credentials allowed
                    mapping.allowedOrigins(origins).allowCredentials(true);
                } else {
                    // Wildcard origins with credentials disabled per CORS spec
                    mapping.allowedOriginPatterns("*").allowCredentials(false);
                }
            }
        };
    }


}
