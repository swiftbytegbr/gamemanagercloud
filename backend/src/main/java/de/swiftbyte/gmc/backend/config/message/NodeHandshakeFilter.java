package de.swiftbyte.gmc.backend.config.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.exception.manager.ExceptionManager;
import de.swiftbyte.gmc.backend.security.ClientIpResolver;
import de.swiftbyte.gmc.backend.repository.NodeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class NodeHandshakeFilter implements HandshakeInterceptor {


    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

        attributes.put("endpoint", "websocket-nodes");
        // Resolve and store the client IP for later error reporting
        String remoteIp = clientIpResolver.resolve(request);
        if (remoteIp == null && request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            remoteIp = request.getRemoteAddress().getAddress().getHostAddress();
        }
        if (remoteIp != null) attributes.put("remote_ip", remoteIp);

        try {
            String nodeId = request.getHeaders().getFirst("Node-Id");
            String secret = request.getHeaders().getFirst("Node-Secret");

            if (nodeId == null || secret == null) throw new AuthenticationException("invalid_node_auth");

            Optional<Node> nodeOptional = nodeRepository.findById(nodeId);
            if (nodeOptional.isEmpty()) throw new AuthenticationException("invalid_node_auth");
            Node node = nodeOptional.get();
            if (!node.getSecret().equals(secret)) throw new AuthenticationException("invalid_node_auth");

            NodeAuthentication authentication = new NodeAuthentication(node);
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (Exception ex) {
            ExceptionManager.ErrorResponse errorResponse = new ExceptionManager.ErrorResponse("invalid_node_auth");
            errorResponse.setStatus(HttpStatus.UNAUTHORIZED);
            response.setStatusCode(HttpStatusCode.valueOf(401));
            response.getHeaders().add("Content-Type", "application/json");
            response.getBody().write(objectMapper.writeValueAsBytes(errorResponse));
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {

    }
}
