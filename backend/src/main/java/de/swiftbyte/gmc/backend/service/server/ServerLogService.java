package de.swiftbyte.gmc.backend.service.server;

import de.swiftbyte.gmc.backend.domain.model.GameServer;
import de.swiftbyte.gmc.backend.domain.model.server.ServerLogFile;
import de.swiftbyte.gmc.backend.domain.model.Team;
import de.swiftbyte.gmc.backend.exception.NotFoundException;
import de.swiftbyte.gmc.backend.infrastructure.StompSender;
import de.swiftbyte.gmc.backend.repository.ServerLogRepository;
import de.swiftbyte.gmc.backend.repository.ServerRepository;
import de.swiftbyte.gmc.backend.repository.TeamRepository;
import de.swiftbyte.gmc.backend.service.LimitService;
import de.swiftbyte.gmc.common.packet.from.daemon.server.ServerLogChunkPacket;
import de.swiftbyte.gmc.common.packet.from.backend.server.ServerLogBackfillRequestPacket;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ServerLogService {

    @Autowired
    private ServerLogRepository serverLogRepository;
    @Autowired
    private ServerRepository serverRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private StompSender stompSender;
    @Autowired
    private LimitService limitService;
    

    // Per-append chunk ceiling; for APPEND mode only
    @Value("${gmc.serverlog.max-append-bytes:131072}")
    private int maxAppendBytes;

    // TTL is enforced via TTL index on updatedAt (7 days). No dynamic config here.

    private int resolveMaxBytes() {
        return 2 * 1024 * 1024; // hard-coded 2 MiB cap per log file
    }


    public void writeChunkFromDaemon(ServerLogChunkPacket packet) {
        GameServer server = serverRepository.findById(packet.getServerId()).orElseThrow(NotFoundException::new);
        Team team = teamRepository.findById(server.getTeamId()).orElseThrow(NotFoundException::new);

        String payload = packet.getContent();
        Instant now = Instant.now();
        var existingOpt = serverLogRepository
                .findByServerIdAndFilenameAndCreatedAt(server.getId(), packet.getFilename(), packet.getCreatedAt());
        boolean isNew = existingOpt.isEmpty();
        ServerLogFile doc = existingOpt.orElseGet(() -> {
            ServerLogFile f = new ServerLogFile();
            f.setServerId(server.getId());
            f.setTeamId(server.getTeamId());
            f.setFilename(packet.getFilename());
            f.setCreatedAt(packet.getCreatedAt());
            f.setUpdatedAt(now);
            f.setSizeBytes(0);
            f.setContent("");
            return f;
        });

        if (isNew) {
            int maxLogs = limitService.getTeamLimit(team, LimitService.LimitType.SERVER_LOGS);
            if (maxLogs > 0) {
                int count = serverLogRepository.countByServerId(server.getId());
                while (count >= maxLogs) {
                    var oldest = serverLogRepository.findTopByServerIdOrderByUpdatedAtAsc(server.getId());
                    if (oldest.isEmpty()) break;
                    serverLogRepository.deleteById(oldest.get().getId());
                    count--;
                }
            }
        }

        String current = doc.getContent() == null ? "" : doc.getContent();
        byte[] currentBytes = current.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] payloadBytes = payload.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int currentSize = currentBytes.length;
        int offset = Math.max(0, packet.getOffset());

        // If there's a gap, request backfill and do not write this out-of-order chunk
        if (offset > currentSize) {
            int gapStart = currentSize;
            int gapLen = offset - currentSize;
            ServerLogBackfillRequestPacket req = new ServerLogBackfillRequestPacket();
            req.setServerId(server.getId());
            req.setFilename(packet.getFilename());
            req.setCreatedAt(packet.getCreatedAt());
            req.setOffset(gapStart);
            req.setLength(gapLen);
            stompSender.sendToNode(server.getNodeId(), "/server/log-backfill-request", req);
            return;
        }

        // Merge by byte offset: extend or overwrite starting at offset
        int newSize = Math.max(currentSize, offset + payloadBytes.length);
        byte[] out = new byte[newSize];
        if (currentSize > 0) System.arraycopy(currentBytes, 0, out, 0, Math.min(currentSize, out.length));
        System.arraycopy(payloadBytes, 0, out, offset, payloadBytes.length);

        int maxBytes = resolveMaxBytes();
        // Apply tail cap: keep last maxBytes
        byte[] trimmed;
        if (out.length > maxBytes) {
            int start = out.length - maxBytes;
            trimmed = new byte[maxBytes];
            System.arraycopy(out, start, trimmed, 0, maxBytes);
        } else {
            trimmed = out;
        }

        String merged = new String(trimmed, StandardCharsets.UTF_8);
        doc.setContent(merged);
        doc.setSizeBytes(trimmed.length);
        doc.setUpdatedAt(now);
        serverLogRepository.save(doc);

        stompSender.sendToTeam(team, "/server/log-write", packet);
    }

    public List<ServerLogFile> listByServer(String serverId) {
        return serverLogRepository.findByServerIdOrderByUpdatedAtDesc(serverId);
    }

    public List<ServerLogFile> listByServer(String serverId, boolean includeContent, boolean latestOnly) {
        if (latestOnly) {
            return includeContent
                    ? serverLogRepository.findLatestPerFilename(serverId)
                    : serverLogRepository.findLatestPerFilenameExcludeContent(serverId);
        }
        return includeContent
                ? serverLogRepository.findByServerIdOrderByCreatedAtDesc(serverId)
                : serverLogRepository.findAllByServerIdExcludeContent(serverId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    

    public ServerLogFile getById(String serverId, String logId) {
        ServerLogFile doc = serverLogRepository.findById(logId).orElseThrow(NotFoundException::new);
        if (!serverId.equals(doc.getServerId())) throw new NotFoundException();
        return doc;
    }

    public void deleteById(String serverId, String logId) {
        ServerLogFile doc = serverLogRepository.findById(logId).orElseThrow(NotFoundException::new);
        if (!serverId.equals(doc.getServerId())) throw new NotFoundException();
        serverLogRepository.deleteById(logId);
    }

    public int deleteAllForServer(String serverId) {
        long removed = serverLogRepository.deleteByServerId(serverId);
        return (int) removed;
    }
}
