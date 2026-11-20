package de.swiftbyte.gmc.backend.service.admin;

import de.swiftbyte.gmc.backend.domain.model.Session;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.UserDto;
import de.swiftbyte.gmc.backend.exception.BadRequestException;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.mapper.UserMapper;
import de.swiftbyte.gmc.backend.repository.SessionRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.TeamActionLogRepository;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.service.SessionService;
import de.swiftbyte.gmc.backend.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UserAdminService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private UserService userService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private de.swiftbyte.gmc.backend.security.SessionCookieUtil sessionCookieUtil;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<UserDto> searchUsers(String q,
                                     User.AccountType accountType,
                                     Integer page,
                                     Integer size,
                                     String sortKey,
                                     Sort.Direction dir) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 50 : Math.max(1, size);

        Sort sort = Sort.by(dir == null ? Sort.Direction.ASC : dir, mapUserSortKey(sortKey));
        Pageable pageable = PageRequest.of(p, s, sort);

        Query qy = new Query();
        List<Criteria> ands = new ArrayList<>();
        if (q != null && !q.isBlank()) {
            String regex = ".*" + Pattern.quote(q.trim()) + ".*";
            Criteria any = new Criteria().orOperator(
                    Criteria.where("email").regex(regex, "i"),
                    Criteria.where("personalInformation.name").regex(regex, "i"),
                    Criteria.where("personalInformation.username").regex(regex, "i"),
                    Criteria.where("companyInformation.companyName").regex(regex, "i")
            );
            ands.add(any);
        }
        if (accountType != null) {
            ands.add(Criteria.where("accountType").is(accountType));
        }
        if (!ands.isEmpty()) qy.addCriteria(new Criteria().andOperator(ands));

        long total = mongoTemplate.count(qy, User.class);
        qy.with(pageable);
        List<User> content = mongoTemplate.find(qy, User.class);
        Page<User> pageUsers = new PageImpl<>(content, pageable, total);
        return pageUsers.map(userMapper::toDto);
    }

    private String mapUserSortKey(String sortKey) {
        String key = (sortKey == null || sortKey.isBlank()) ? "email" : sortKey;
        return switch (key) {
            case "email" -> "email";
            case "lastLogin" -> "lastLogin";
            case "accountType" -> "accountType";
            case "name" -> "personalInformation.name";
            case "username" -> "personalInformation.username";
            default -> "email";
        };
    }

    public UserDto toDto(User user) {
        return userMapper.toDto(user);
    }

    // ==== Safe updates (reuse domain service where possible) ====

    public void changeAccountType(String userId, User.AccountType accountType) {
        User user = userService.getOrThrow(userId);
        user.setAccountType(accountType);
        userService.save(user);
    }

    public void changePersonalInfo(String userId, User.PersonalInformation personalInformation) {
        User user = userService.getOrThrow(userId);
        userService.changePersonalInfo(user, personalInformation);
    }

    public void changeCompanyInfo(String userId, User.CompanyInformation companyInformation, User.PersonalInformation.Address billingAddress) {
        User user = userService.getOrThrow(userId);
        userService.changeCompanyInfo(user, companyInformation, billingAddress);
    }

    public void changeNotificationSettings(String userId, User.NotificationSettings settings) {
        User user = userService.getOrThrow(userId);
        userService.changeNotificationSettings(user, settings);
    }

    public void updateMaxOwnedTeams(String userId, Integer maxOwnedTeams) {
        if (maxOwnedTeams != null && maxOwnedTeams < 0) throw new BadRequestException("maxOwnedTeams_invalid");
        User user = userService.getOrThrow(userId);
        user.setMaxOwnedTeams(maxOwnedTeams);
        userService.save(user);
    }

    public void changeTwoFA(String userId, User.TwoFAType twoFAType) {
        User user = userService.getOrThrow(userId);
        userService.change2fa(user, twoFAType);
    }

    // ==== Admin-only account controls ====

    public void setEmailVerified(String userId, boolean verified) {
        User user = userService.getOrThrow(userId);
        user.setEmailVerified(verified);
        // If verified, clear any pending code; if unverified, do not auto-send a new code here.
        user.setEmailVerificationCode(verified ? 0 : user.getEmailVerificationCode());
        userService.save(user);
    }

    public void setPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) throw new BadRequestException("password_invalid");
        User user = userService.getOrThrow(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userService.save(user);
    }

    public void setEnabled(String userId, boolean enabled) {
        User user = userService.getOrThrow(userId);
        user.setEnabled(enabled);
        userService.save(user);
        if (!enabled) {
            // Revoke all sessions when disabling the account
            sessionRepository.deleteAllByUserId(userId);
        }
    }

    public void deleteUser(String userId, String confirm) {
        if (!"delete".equalsIgnoreCase(confirm)) throw new BadRequestException("delete_confirmation_required");
        User user = userService.getOrThrow(userId);
        // Must not own any teams
        if (!teamRepository.findByOwnerId(userId).isEmpty()) {
            throw new BadRequestException("user.deletion_failed_has_teams");
        }
        // Remove from all teams where they are member or application user
        for (Team team : teamRepository.findAllByUserIdIncludingApplications(userId)) {
            boolean changed = false;
            if (team.getMembers() != null && team.getMembers().containsKey(userId)) {
                team.getMembers().remove(userId);
                changed = true;
            }
            if (team.getApplications() != null && team.getApplications().containsKey(userId)) {
                team.getApplications().remove(userId);
                changed = true;
            }
            if (changed) teamRepository.save(team);
        }
        // Delete sessions and then user
        sessionRepository.deleteAllByUserId(userId);
        userService.delete(user);
    }

    // ==== Sessions listing & management ====

    public Page<Session> listSessionsByUser(String userId, boolean includeExpired, Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 100 : Math.max(1, size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "lastAccessed"));

        Query qy = new Query();
        qy.addCriteria(Criteria.where("userId").is(userId));
        if (!includeExpired) {
            qy.addCriteria(Criteria.where("expiresAt").gt(new java.util.Date()));
        }
        long total = mongoTemplate.count(qy, Session.class);
        qy.with(pageable);
        List<Session> items = mongoTemplate.find(qy, Session.class);
        maskSessionIds(items);
        return new PageImpl<>(items, pageable, total);
    }

    public void deleteAllSessionsForUser(String userId) {
        sessionRepository.deleteAllByUserId(userId);
    }

    public Page<Session> searchSessions(String userId, String email, String q, boolean includeExpired, Integer page, Integer size) {
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 100 : Math.max(1, size);
        Pageable pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "lastAccessed"));

        Query query = new Query();
        List<Criteria> ands = new ArrayList<>();
        if (userId != null && !userId.isBlank()) {
            ands.add(Criteria.where("userId").is(userId));
        }
        if (email != null && !email.isBlank()) {
            User user = userRepository.findByEmail(email.trim().toLowerCase()).orElseThrow(NotFoundException::new);
            ands.add(Criteria.where("userId").is(user.getId()));
        }
        if (q != null && !q.isBlank()) {
            String regex = ".*" + Pattern.quote(q.trim()) + ".*";
            ands.add(new Criteria().orOperator(
                    Criteria.where("sessionId").regex(regex, "i"),
                    Criteria.where("lastIp").regex(regex, "i"),
                    Criteria.where("lastUserAgent").regex(regex, "i")
            ));
        }
        if (!includeExpired) {
            ands.add(Criteria.where("expiresAt").gt(new java.util.Date()));
        }
        if (!ands.isEmpty()) query.addCriteria(new Criteria().andOperator(ands));

        long total = mongoTemplate.count(query, Session.class);
        query.with(pageable);
        List<Session> items = mongoTemplate.find(query, Session.class);
        maskSessionIds(items);
        return new PageImpl<>(items, pageable, total);
    }

    public void deleteSession(String sessionId) {
        sessionService.delete(sessionId);
    }

    private void maskSessionIds(List<Session> sessions) {
        if (sessions == null || sessions.isEmpty()) return;
        String currentId = null;
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest req = attrs.getRequest();
                currentId = sessionCookieUtil.readSessionId(req);
            }
        } catch (Exception ignored) {}
        final String current = currentId;
        for (Session s : sessions) {
            if (s == null) continue;
            if (current != null && current.equals(s.getSessionId())) {
                s.setSessionId("CURRENT");
            } else {
                s.setSessionId("HIDDEN");
            }
        }
    }
}
