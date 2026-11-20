package de.swiftbyte.gmc.backend.config.message;

import de.swiftbyte.gmc.backend.domain.model.Node;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NodeDto;
import de.swiftbyte.gmc.backend.dto.UserDto;
import de.swiftbyte.gmc.backend.exception.GmcException;
import de.swiftbyte.gmc.backend.mapper.NodeMapper;
import de.swiftbyte.gmc.backend.mapper.UserMapper;
import de.swiftbyte.gmc.backend.security.UserAuthentication;
import io.sentry.SentryClient;
import io.sentry.context.Context;
import io.sentry.event.UserBuilder;
import io.sentry.event.EventBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;


@ControllerAdvice(annotations = Controller.class)
public class StompExceptionAdvice {

    @Autowired(required = false)
    private SentryClient sentryClient;

    @Autowired(required = false)
    private UserMapper userMapper;

    @Autowired(required = false)
    private NodeMapper nodeMapper;

    @MessageExceptionHandler
    public void capture(Throwable ex, Message<?> message) {
        if (sentryClient == null) {
            ex.printStackTrace();
            return;
        }
        try {
            Context ctx = sentryClient.getContext();
            ctx.addTag("transport", "websocket");

            // WebSocket details
            if (message != null) {
                SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
                if (accessor != null) {
                    ctx.addExtra("ws.session_id", accessor.getSessionId());
                    Object endpoint = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("endpoint") : null;
                    if (endpoint != null) ctx.addExtra("ws.endpoint", endpoint);
                    Object remoteIp = accessor.getSessionAttributes() != null ? accessor.getSessionAttributes().get("remote_ip") : null;
                    if (remoteIp != null) ctx.addExtra("ws.remote_addr", remoteIp);
                    String destination = accessor.getDestination();
                    if (destination != null) ctx.addExtra("ws.destination", destination);
                }
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof UserAuthentication ua) {
                Object principal = ua.getPrincipal();
                if (principal instanceof User user && userMapper != null) {
                    UserDto userDto = userMapper.toDto(user);
                    ctx.addExtra("user", userDto);
                    ctx.setUser(new UserBuilder().setId(user.getId()).setEmail(user.getEmail()).build());
                }
            } else if (auth instanceof NodeAuthentication na) {
                Object principal = na.getPrincipal();
                if (principal instanceof Node node && nodeMapper != null) {
                    NodeDto nodeDto = nodeMapper.toDto(node); // contains no secret
                    ctx.addExtra("node", nodeDto);
                    ctx.setUser(new UserBuilder().setId("node:" + node.getId()).setUsername(node.getId()).build());
                }
            }

            if (ex instanceof GmcException ge) {
                if (ge.getErrorKey() != null) ctx.addExtra("gmc.errorKey", ge.getErrorKey());
                if (ge.getCustomMessage() != null) ctx.addExtra("gmc.customMessage", ge.getCustomMessage());
            }

            // Add stacktrace extra for visibility
            ctx.addExtra("stacktrace", getStackTrace(ex));

            // Build and send event with explicit exception interface and a helpful title
            EventBuilder eb = new EventBuilder()
                    .withLevel(Event.Level.ERROR)
                    .withLogger("gmc-backend-stomp")
                    .withMessage(buildWsTitle(ex, message))
                    .withSentryInterface(new ExceptionInterface(ex));
            // culprit from top stack frame
            StackTraceElement[] st = ex.getStackTrace();
            if (st != null && st.length > 0) eb.withCulprit(st[0]);
            sentryClient.sendEvent(eb);
            ctx.clear();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String getStackTrace(Throwable t) {
        try {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            pw.flush();
            return sw.toString();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String buildWsTitle(Throwable ex, Message<?> message) {
        String name = ex.getClass().getSimpleName();
        String msg = ex.getMessage();
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (msg != null && !msg.isBlank()) sb.append(": ").append(msg);
        if (message != null) {
            SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(message);
            if (accessor != null) {
                String destination = accessor.getDestination();
                if (destination != null) sb.append(" [dest=").append(destination).append("]");
            }
        }
        return sb.toString();
    }
}
