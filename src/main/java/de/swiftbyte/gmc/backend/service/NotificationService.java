package de.swiftbyte.gmc.backend.service;

import de.swiftbyte.gmc.backend.domain.model.Notification;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.domain.model.User;
import de.swiftbyte.gmc.backend.dto.NotificationDto;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.infrastructure.DiscordWebhookSender;
import de.swiftbyte.gmc.backend.infrastructure.packet.NotificationCreatedPacket;
import de.swiftbyte.gmc.backend.repository.NotificationRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.repository.UserRepository;
import de.swiftbyte.gmc.backend.infrastructure.EmailSender;
import de.swiftbyte.gmc.common.model.NotificationSeverity;
import de.swiftbyte.gmc.backend.domain.value.DiscordEmbed;
import de.swiftbyte.gmc.common.model.NotificationSource;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final TeamRepository teamRepository;
    private final StompSender stompSender;
    private final MongoTemplate mongoTemplate;
    private final UserRepository userRepository;
    private final EmailSender emailSender;
    private final DiscordWebhookSender discordWebhookSender;

    @Async
    public void create(String teamId, String type, NotificationSeverity severity, String title, String message, NotificationSource source) {
        Team team = teamRepository.findById(teamId).orElseThrow();
        Notification n = new Notification();
        n.setTeamId(team.getId());
        n.setType(type);
        n.setSeverity(severity);
        n.setTitle(title);
        n.setMessage(message);
        n.setSource(source);
        n.setCreatedAt(Instant.now());
        n = notificationRepository.save(n);

        pruneOld(team.getId());

        // Broadcast to team
        NotificationCreatedPacket pkt = new NotificationCreatedPacket();
        pkt.setNotification(toDto(n, null));
        // Notify team users on a single unified notifications destination
        stompSender.sendToTeam(team, "/notifications", pkt);

        // Forward via email based on user settings
        dispatchEmailNotifications(team, n);

        // Forward to team's Discord webhook if configured and threshold met
        dispatchDiscordWebhook(team, n);
    }

    @Async
    public void create(Team team, String type, NotificationSeverity severity, String title, String message, NotificationSource source) {
        create(team.getId(), type, severity, title, message, source);
    }

    public List<NotificationDto> listLatest(String teamId, User currentUser, boolean onlyUnread) {
        String uid = currentUser.getId();
        List<Notification> items = notificationRepository.findByTeamIdOrderByCreatedAtDesc(teamId, PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdAt")));
        if (items == null) items = List.of();
        List<NotificationDto> dtos = new ArrayList<>(items.size());
        for (Notification n : items) {
            boolean read = n.getReadByIds() != null && n.getReadByIds().contains(uid);
            if (onlyUnread && read) continue;
            dtos.add(toDto(n, read));
        }
        return dtos;
    }

    public long countUnread(String teamId, String userId) {
        return notificationRepository.countUnread(teamId, userId);
    }

    public void markRead(String teamId, User user, List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        Query q = Query.query(new Criteria().andOperator(
                Criteria.where("teamId").is(teamId),
                Criteria.where("_id").in(ids),
                new Criteria().orOperator(
                        Criteria.where("readByIds").exists(false),
                        Criteria.where("readByIds").nin(user.getId())
                )
        ));
        Update up = new Update().addToSet("readByIds").value(user.getId());
        mongoTemplate.updateMulti(q, up, Notification.class);

        // No WebSocket broadcast for read; clients refresh via API
    }

    public void markAllRead(String teamId, User user) {
        Query q = Query.query(new Criteria().andOperator(
                Criteria.where("teamId").is(teamId),
                new Criteria().orOperator(
                        Criteria.where("readByIds").exists(false),
                        Criteria.where("readByIds").nin(user.getId())
                )
        ));
        Update up = new Update().addToSet("readByIds").value(user.getId());
        mongoTemplate.updateMulti(q, up, Notification.class);

        // No WebSocket broadcast for read-all; clients refresh via API
    }

    private void pruneOld(String teamId) {
        // Fast path: if total docs <= 25, skip
        long total = mongoTemplate.count(Query.query(Criteria.where("teamId").is(teamId)), Notification.class);
        if (total <= 25) return;

        // Find the 25th newest item's createdAt as cutoff
        Query boundary = Query.query(Criteria.where("teamId").is(teamId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .skip(24)
                .limit(1);
        List<Notification> boundaryList = mongoTemplate.find(boundary, Notification.class);
        if (boundaryList == null || boundaryList.isEmpty()) return;
        var cutoff = boundaryList.get(0).getCreatedAt();
        if (cutoff == null) return;

        // Delete all older than cutoff in one operation
        Query del = Query.query(new Criteria().andOperator(
                Criteria.where("teamId").is(teamId),
                Criteria.where("createdAt").lt(cutoff)
        ));
        mongoTemplate.remove(del, Notification.class);
    }

    private static NotificationDto toDto(Notification n, Boolean readOverride) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setTeamId(n.getTeamId());
        dto.setType(n.getType());
        dto.setSeverity(n.getSeverity());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setSource(n.getSource());
        dto.setCreatedAt(n.getCreatedAt());
        if (readOverride != null) dto.setRead(readOverride);
        return dto;
    }

    private void dispatchEmailNotifications(Team team, Notification n) {
        var memberIds = new ArrayList<String>();
        if (team.getMembers() != null) memberIds.addAll(team.getMembers().keySet());
        if (team.getOwnerId() != null) memberIds.add(team.getOwnerId());
        if (memberIds.isEmpty()) return;
        var users = userRepository.findAllById(memberIds);
        for (var u : users) {
            var ns = u.getNotificationSettings();
            if (ns == null || !ns.isEmailNotificationsEnabled()) continue;
            var min = ns.getEmailNotificationsMinSeverity();
            NotificationSeverity threshold =
                    (min == null ? NotificationSeverity.ERROR : min);
            if (n.getSeverity() == null || n.getSeverity().ordinal() < threshold.ordinal()) continue;
            try {
                String createdAtIso = n.getCreatedAt() == null ? null : n.getCreatedAt().toString();
                emailSender.sendTeamNotificationEmail(u, team, n.getSeverity(), n.getTitle(), n.getMessage(), createdAtIso);
            } catch (Exception ignored) { }
        }
    }

    private void dispatchDiscordWebhook(Team team, Notification n) {
        try {
            Team.NotificationSettings s = team.getNotificationSettings();
            if (s == null) return;
            String url = s.getDiscordWebhookUrl();
            if (url == null || url.isBlank()) return;
            var min = s.getDiscordNotificationsMinSeverity();
            NotificationSeverity threshold = (min == null ? NotificationSeverity.ERROR : min);
            if (n.getSeverity() == null || n.getSeverity().ordinal() < threshold.ordinal()) return;

            DiscordEmbed embed = getDiscordEmbed(n);

            // Best-effort send; ignore result
            discordWebhookSender.send(url, null, embed);
        } catch (Exception ignored) { }
    }

    private static DiscordEmbed getDiscordEmbed(Notification n) {
        NotificationSeverity sev = n.getSeverity() == null ? NotificationSeverity.INFO : n.getSeverity();
        String title = n.getTitle();
        String message = n.getMessage();

        DiscordEmbed embed = new DiscordEmbed();
        // Prefer keeping title concise; fall back to severity if missing
        if (title != null && !title.isBlank()) embed.setTitle(title);
        // Use description for the text body; fall back to title when message is missing
        String desc = (message != null && !message.isBlank()) ? message : (title != null && !title.isBlank() ? title : "");
        embed.setDescription(desc);
        // Color indicates severity
        embed.setColor(mapSeverityColor(sev));
        return embed;
    }

    private static int mapSeverityColor(NotificationSeverity severity) {
        // Common Discord-like palette (decimal RGB)
        return switch (severity) {
            case INFO -> 3447003;      // #3498DB
            case WARNING -> 16776960;  // #FFFF00
            case ERROR -> 15158332;    // #E74C3C
            case CRITICAL -> 10038562; // #992D22
        };
    }
}
