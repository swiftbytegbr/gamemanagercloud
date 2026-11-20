package de.swiftbyte.gmc.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swiftbyte.gmc.backend.config.message.NodeAuthentication;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.domain.model.ApiUser;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.exception.manager.ExceptionManager;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import de.swiftbyte.gmc.backend.service.SessionService;
import de.swiftbyte.gmc.backend.service.UserService;
import de.swiftbyte.gmc.backend.repository.ApiUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Component
public class HttpSessionFilter extends OncePerRequestFilter {

    @Autowired
    private SessionService sessionService;

    @Autowired
    private UserService userService;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ApiUserRepository apiUserRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    @Autowired
    private SessionCookieUtil sessionCookieUtil;

    @Autowired
    private ExceptionManager exceptionManager;



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            // Session authentication via cookie
            String sessionId = sessionCookieUtil.readSessionId(request);
            if (sessionId != null) {
                Session session = sessionService.findById(sessionId);
                if (session == null) throw new AuthenticationException("session_invalid");
                // Snapshot request-derived values on the request thread to avoid
                // accessing a recycled HttpServletRequest in the async task.
                String resolvedIp = clientIpResolver.resolve(request);
                String userAgent = request.getHeader("User-Agent");
                taskExecutor.execute(() -> {
                    session.setLastAccessed(Instant.now());
                    session.setLastIp(resolvedIp);
                    session.setLastUserAgent(userAgent);

                    // If session is about to expire, extend it
                    Date sessionExtension = Date.from(Instant.now().plus(30, ChronoUnit.MINUTES));
                    if(session.getExpiresAt().before(sessionExtension)) session.setExpiresAt(sessionExtension);
                    sessionService.save(session);
                });

                Optional<User> user = userService.findById(session.getUserId());
                if (user.isEmpty()) {
                    sessionService.deleteAllByUserId(session.getUserId());
                    throw new AuthenticationException("session_invalid");
                }
                SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(user.get()));
                filterChain.doFilter(request, response);
                return;
            }


            // Node authentication
            String nodeId = request.getHeader("Node-Id");
            String secret = request.getHeader("Node-Secret");
            if (nodeId != null && secret != null) {
                Optional<Node> nodeOptional = nodeRepository.findById(nodeId);
                if (nodeOptional.isEmpty() || !nodeOptional.get().getSecret().equals(secret)) {
                    throw new AuthenticationException("invalid_node_auth");
                }
                NodeAuthentication authentication = new NodeAuthentication(nodeOptional.get());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }


            // API Account authentication
            String appId = request.getHeader("Application-Id");
            String appSecret = request.getHeader("Application-Secret");
            String appToken = request.getHeader("Application-Token");
            if ((appId != null && appSecret != null) || appToken != null) {
                if (appToken != null) {
                    appToken = appToken.replace("gmc-app-token-", "");
                    String[] parts = appToken.split(":");
                    if (parts.length != 2) {
                        throw new AuthenticationException("invalid_app_auth");
                    }
                    appId = parts[0];
                    appSecret = parts[1];
                }

                // Try ApiUser collection first
                Optional<ApiUser> apiUserOpt = apiUserRepository.findById(appId);
                if (apiUserOpt.isPresent()) {
                    ApiUser apiUser = apiUserOpt.get();
                    if (!passwordEncoder.matches(appSecret, apiUser.getPassword())) {
                        throw new AuthenticationException("invalid_app_auth");
                    }
                    SecurityContextHolder.getContext().setAuthentication(new UserAuthentication(apiUser));
                    filterChain.doFilter(request, response);
                    return;
                }
                throw new AuthenticationException("invalid_app_auth");
            }

            // If none of the above, just continue (unauthenticated)
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            ExceptionManager.ErrorResponse errorResponse = exceptionManager.base(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
            response.setStatus(401);
            response.addHeader("Content-Type", "application/json");
            // Clear potentially invalid/expired session cookie
            sessionCookieUtil.clearSessionCookie(response);
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}
