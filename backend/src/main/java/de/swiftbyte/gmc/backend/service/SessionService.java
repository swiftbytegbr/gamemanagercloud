package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.exception.AuthenticationException;
import de.swiftbyte.gmc.backend.repository.SessionRepository;
import de.swiftbyte.gmc.backend.util.Generators;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SessionService {

    @Autowired
    SessionRepository sessionRepository;


    public void save(Session session) {
        if (session.getSessionId() == null) session.setSessionId(Generators.generateSessionId());
        sessionRepository.save(session);
    }

    public Session findById(String sessionId) {
        Session session = sessionRepository.findById(sessionId).orElse(null);
        if (session != null) checkExpired(session);
        return session;
    }

    public void checkExpired(Session session) {
        if (session.getExpiresAt().getTime() < System.currentTimeMillis()) {
            delete(session.getSessionId());
            throw new AuthenticationException("session_expired");
        }
    }

    public List<Session> findByUserId(String userId) {
        return sessionRepository.findByUserId(userId);
    }

    public void delete(String sessionId) {
        sessionRepository.deleteById(sessionId);
    }

    public void deleteAllByUserId(String userId) {
        sessionRepository.deleteAllByUserId(userId);
    }

}
